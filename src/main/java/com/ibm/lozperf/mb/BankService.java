package com.ibm.lozperf.mb;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Savepoint;
import java.util.Map;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.annotation.Resource;
import javax.ejb.ConcurrencyManagement;
import javax.ejb.ConcurrencyManagementType;
import javax.ejb.Singleton;
import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.sql.DataSource;
import javax.ws.rs.ApplicationPath;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Application;
import javax.ws.rs.core.MediaType;

@ApplicationPath("MegaCard")
@Path("Svc")
//@Stateless
@Singleton
@ConcurrencyManagement(ConcurrencyManagementType.BEAN)
public class BankService extends Application {

	private final static boolean CHECK_FRAUD = Boolean.parseBoolean(System.getenv("CHECK_FRAUD"));

	// private static DataSource ds = retrieveDataSource();
	@Resource(name = "jdbc/MegaBankDataSource")
	private DataSource ds;
	private static boolean insertIntoHistory = true;
	private static boolean autoCommit = false;
	private static boolean allowDupLogon = false;

	private ModelAdapter model;

	@PostConstruct
	public void init() {
		model = new TFServingAdapter();
		System.out.println("Ready");
	}

	@PreDestroy
	public void destroy() {
		System.out.println("destroy");
		try {
			model.close();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	private CardAccount getUserCard(Connection con, String cardNumber, String cvv, String expirationDate)
			throws SQLException {
		final String queryStr = "SELECT accid, ccardid FROM creditcard WHERE ccnumber=? AND cvv=? AND expiration=?";

		try (PreparedStatement prep = con.prepareStatement(queryStr)) {
			prep.setLong(1, Long.parseLong(cardNumber));
			prep.setInt(2, Integer.parseInt(cvv));
			prep.setInt(3, Integer.parseInt(expirationDate.replace("/", "")));
			ResultSet rs = prep.executeQuery();
			if (!rs.next())
				return null;
			return new CardAccount(rs.getInt(2), rs.getInt(1));
		}
	}

	private boolean checkMerchantToken(Connection con, int accid, String merchantToken) throws SQLException {
		final String queryStr = "SELECT count(accid) FROM merchantacc WHERE accid=? AND token=?";
		try (PreparedStatement prep = con.prepareStatement(queryStr)) {
			prep.setInt(1, accid);
			prep.setString(2, merchantToken);
			ResultSet rs = prep.executeQuery();
			rs.next();
			return rs.getInt(1) == 1;
		}
	}

	private boolean checkFraud(Connection con, CardAccount cardAccount, int merchantAccId, BigDecimal amount,
			CreditcardTransactionType useChip) throws SQLException, MegaBankException {
		final String historyQueryStr = "SELECT ch.METHOD, ISNULL(err.errorstr,'None'), ISNULL(c.state,'None'), ISNULL(c.zipcode,'0'), ISNULL(c.city,'ONLINE'), m.merchant_name, m.mcc, hw.amount, hw.time "
				+ "FROM (SELECT txid, ccardid, method, errid FROM CARDHISTORY WHERE CCARDID=? ORDER BY TXID ASC LIMIT "+(model.numberTimesteps()- 1)+") ch "
				+ "LEFT JOIN ERROR err ON ch.errid=err.errid "
				+ "JOIN HISTORY hw ON ch.txid=hw.txid JOIN HISTORY hd ON hd.reftxid=hw.txid "
				+ "JOIN customeraccs ca ON hd.accid=ca.accid JOIN customer c ON ca.custid=c.custid "
				+ "JOIN merchantacc ma ON ma.accid=hd.accid JOIN merchant m ON ma.merchantid=m.merchantid "
				+ "WHERE hw.transtype='w' AND hd.transtype='d' AND c.customertype='m' "
				+ "ORDER BY hd.time ASC";

		
		Inputs modelInputs = new Inputs(model.numberTimesteps());
		CreditcardTransactionType[] trantypes = CreditcardTransactionType.values();

		int i = 0;
		try (PreparedStatement prep = con.prepareStatement(historyQueryStr)) {
			prep.setInt(1, cardAccount.card);
			ResultSet rs = prep.executeQuery();
			while (rs.next()) {
				modelInputs.UseChip[i] = trantypes[rs.getInt(1)].stringValue;
				modelInputs.Errors[i] = rs.getString(2);
				modelInputs.MerchantState[i] = rs.getString(3);
				modelInputs.Zip[i] = rs.getString(4);
				modelInputs.MerchantCity[i] = rs.getString(5);
				modelInputs.MerchantName[i] = rs.getString(6);
				modelInputs.MCC[i] = Integer.toString(rs.getInt(7));
				modelInputs.Amount[i] = rs.getBigDecimal(8);
				modelInputs.YearMonthDayTime[i] = rs.getDate(9).getTime();
				i++;
			}
		}
		if (i != model.numberTimesteps() - 1) {
			System.out.println("Not Enough history to check Fraud");
			return false;
		}

		modelInputs.UseChip[i] = useChip.stringValue;
		modelInputs.Errors[i] = "None";
		modelInputs.Amount[i] = amount;
		modelInputs.YearMonthDayTime[i] = System.currentTimeMillis();

		final String merchQueryStr = "SELECT  ISNULL(c.state,'None'), ISNULL(c.zipcode,'0'), ISNULL(c.city,'ONLINE'), m.merchant_name, m.mcc "
				+ "FROM merchantacc ma  JOIN merchant m ON ma.merchantid=m.merchantid JOIN customeraccs ca ON ma.accid=ca.accid JOIN customer c ON ca.custid=c.custid "
				+ "WHERE ma.accid=?";
		try (PreparedStatement prep = con.prepareStatement(merchQueryStr)) {
			prep.setInt(1, merchantAccId);
			ResultSet rs = prep.executeQuery();
			if (!rs.next())
				throw new MegaBankException("Merchant not found");
			modelInputs.MerchantState[i] = rs.getString(1);
			modelInputs.Zip[i] = rs.getString(2);
			modelInputs.MerchantCity[i] = rs.getString(3);
			modelInputs.MerchantName[i] = rs.getString(4);
			modelInputs.MCC[i] = Integer.toString(rs.getInt(5));
		}

		return model.checkFraud(modelInputs);
	}

	@POST
	@Path("Transfer")
	@Consumes(MediaType.APPLICATION_JSON)
	@Produces({ MediaType.APPLICATION_JSON })
	public boolean doCardTransaction(CreditCardTransaction transaction) throws MegaBankException {
		// System.out.println(transaction.transactionUuid);
		// System.out.println(transaction.cardNumber);
		// System.out.println(transaction.amount);
		try (Connection con = getConnection()) {
			if (!checkMerchantToken(con, transaction.merchantAcc, transaction.merchantToken)) {
				// System.out.println("Merchant not found");
				return false;
			}

			CardAccount cardAccount = getUserCard(con, transaction.cardNumber, transaction.cvv,
					transaction.expirationDate);
			if (cardAccount == null) {
				// System.out.println("Card not found");
				return false;
			}

			if (CHECK_FRAUD && checkFraud(con, cardAccount, transaction.merchantAcc, transaction.amount,
					CreditcardTransactionType.ONLINE)) {
				con.rollback();
				return false;
			}
			transfer(con, cardAccount, transaction.merchantAcc, transaction.amount, CreditcardTransactionType.ONLINE);
			con.commit();
			return true;
		} catch (SQLException e) {
			e.printStackTrace();
			return false;
		}
	}

	private static final String withdrawSQL = "select lasttxid from final table (UPDATE account set balance = balance - ?, lasttxid = lasttxid + 1 WHERE"
			+ " accid = ? and balance >= ? ) ";
	private static final String depositSQL = "select lasttxid from final table (UPDATE account set balance = balance + ?, lasttxid = lasttxid + 1 WHERE accid = ? )";

	public boolean transfer(Connection con, CardAccount card, int accidTo, BigDecimal amount,
			CreditcardTransactionType type) throws MegaBankException, SQLException {

		try {
			Savepoint startTransferSP = con.setSavepoint("START TRANSFER");

			long lastFromTX;
			try (PreparedStatement prep = con.prepareStatement(withdrawSQL)) {
				prep.setBigDecimal(1, amount);
				prep.setInt(2, card.accountId);
				prep.setBigDecimal(3, amount);

				ResultSet rs = prep.executeQuery();
				if (!rs.next()) {
					con.rollback(startTransferSP);
					throw new MegaBankException("MegaBankJDBC.transfer.withdraw: failed");
				}
				lastFromTX = rs.getLong(1);
			}
			long lastToTX;
			try (PreparedStatement prep = con.prepareStatement(depositSQL)) {
				prep.setBigDecimal(1, amount);
				prep.setInt(2, accidTo);

				ResultSet rs = prep.executeQuery();

				if (!rs.next()) {
					con.rollback(startTransferSP);
					System.err.println("ERROR MegaBankJDBCService: transfer()"); // FIX RM 2020-04-29
					throw new MegaBankException("MegaBankJDBC.transfer.deposit: failed");
				}
				lastToTX = rs.getLong(1);
			}

			insertCardHistory(con, amount, accidTo, card, type, lastFromTX, lastToTX, null);
		} catch (SQLException e) {
			System.err.println("ERROR MegaBankJDBCService: transfer()"); // FIX RM 2020-04-29
			System.err.println(e.getMessage()); // FIX RM 2020-04-29
			throw e;
			// throw new MegaBankException(e.getMessage());
		}

		return true;
	}

	private void insertCardHistory(Connection con, BigDecimal amount, int accidTo, CardAccount card,
			CreditcardTransactionType type, long lastFromTX, long lastToTX, Integer errId)
			throws MegaBankException, SQLException {
		insertHistoryTransfer(con, "w", amount, accidTo, card.accountId, lastToTX, lastFromTX);
		insertHistoryTransfer(con, "d", amount, card.accountId, accidTo, lastFromTX, lastToTX);

		final String insertCardHistorySQL = "INSERT INTO CARDHISTORY (TXID, CCARDID, METHOD, ERRID) VALUES (?,?,?,?)";
		try (PreparedStatement prep = con.prepareStatement(insertCardHistorySQL)) {
			prep.setLong(1, card.accountId * 10000000000l + lastFromTX);
			prep.setLong(2, card.card);
			prep.setInt(3, type.ordinal());
			prep.setNull(4, java.sql.Types.INTEGER);
			prep.execute();
		}

	}

	private boolean insertHistoryTransfer(Connection con, String TRANSTYPE, BigDecimal AMOUNT, int REFACCID, int ACCID,
			long lastREFTX, long lastTX) throws MegaBankException {

		final String insertHistoryTransferSQL = "INSERT INTO HISTORY (TIME, TXID, TRANSTYPE, AMOUNT, REFTXID, ACCID) "
				+ " VALUES (CURRENT TIMESTAMP,  ?, ?, ?, ?, ?) ";

		if (insertIntoHistory)
			try (PreparedStatement prep = con.prepareStatement(insertHistoryTransferSQL)) {

				prep.setLong(1, (long) ACCID * 10000000000l + lastTX); // Use the ACCID to generate the "base" TXID
				prep.setString(2, TRANSTYPE);
				prep.setBigDecimal(3, AMOUNT);
				prep.setLong(4, (long) REFACCID * 10000000000l + lastREFTX);
				prep.setInt(5, ACCID);

				int res = prep.executeUpdate();

				if (res != 1) {
					System.err.println("ERROR MegaBankJDBCService: insertHistoryTransfer()"); // FIX RM 2020-04-29
					throw new MegaBankException("MegaBankJDBC.insertHistory: failed");
				}

			} catch (SQLException e) {
				System.err.println("ERROR MegaBankJDBCService: insertHistoryTransfer()"); // FIX RM 2020-04-29
				System.err.println(e.getMessage()); // FIX RM 2020-04-29
				throw new MegaBankException(e.getMessage());
			}

		return true;
	}

	protected Connection getConnection() throws SQLException {
		Connection con = ds.getConnection();
		if (!autoCommit)
			con.setAutoCommit(false);
		return con;
	}

	private static DataSource retrieveDataSource() {
		Map<String, String> envList = System.getenv();

		// If these are not set, set them to the default
		if (envList.containsKey("INSERT_INTO_HISTORY"))
			insertIntoHistory = Boolean.parseBoolean(System.getenv("INSERT_INTO_HISTORY"));
		if (envList.containsKey("AUTOCOMMIT"))
			autoCommit = Boolean.parseBoolean(System.getenv("AUTOCOMMIT"));
		if (envList.containsKey("ALLOW_DUP_LOGON"))
			allowDupLogon = Boolean.parseBoolean(System.getenv("ALLOW_DUP_LOGON"));

		String dsName = "UNKNOWN !! ";
		if (envList.containsKey("JDBCNAME"))
			dsName = System.getenv("JDBCNAME");

		System.out.println("MegaBankJDBC: datasource name = " + dsName);
		System.out.println("MegaBankJDBC: tx history table inserts = " + Boolean.toString(insertIntoHistory));
		System.out.println("MegaBankJDBC: autoCommit = " + Boolean.toString(autoCommit));
		System.out.println("MegaBankJDBC: allow Duplicate Logons = " + Boolean.toString(allowDupLogon));

		DataSource ds;
		try {
			Context ctx = new InitialContext();
			ds = (DataSource) ctx.lookup(dsName);
			System.out.println("MegaBankJDBC: using datasource = " + dsName);

		} catch (NamingException e) {
			System.err.println("MegaBankJDBC: Error, cannot find datasource " + dsName);
			throw new RuntimeException(e.getMessage());
		}
		return ds;
	}

	@GET
	@Path("test")
	public String test() {
		return "OK";
	}
}
