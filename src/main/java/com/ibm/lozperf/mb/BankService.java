package com.ibm.lozperf.mb;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Savepoint;
import java.sql.Timestamp;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.annotation.Resource;
import javax.ejb.AsyncResult;
import javax.ejb.Asynchronous;
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
	private final static String MODEL_CLASS_NAME = System.getenv("ModelAdapterClass");

	// private static DataSource ds = retrieveDataSource();
	@Resource(name = "jdbc/MegaBankDataSource")
	private DataSource ds;
	private static boolean insertIntoHistory = true;
	private static boolean autoCommit = false;
	private static boolean allowDupLogon = false;

	private ModelAdapter model;

	private static Class<?> modelClass;
	static {
		try {
			System.out.println("Model Class Name: " + MODEL_CLASS_NAME);
			modelClass = Class.forName(MODEL_CLASS_NAME);
			System.out.println("Model Class: " + modelClass);
		} catch (Throwable e) {
			e.printStackTrace();
			throw new RuntimeException();
		}
	}

	public BankService() throws ClassNotFoundException, IllegalAccessException, InstantiationException {

	}

	@PostConstruct
	public void init() {
		try (Connection con = ds.getConnection()) {
			System.out.println("Isolation Level: " + con.getMetaData().getDefaultTransactionIsolation());
		} catch (SQLException e1) {

			e1.printStackTrace();
		}
		try {
			model = (ModelAdapter) modelClass.newInstance();
		} catch (IllegalAccessException | InstantiationException e) {
			e.printStackTrace();
		}
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

	private ModelInputs getUserHistory(Connection con, CardAccount cardAccount, int merchantAccId, BigDecimal amount,
			CreditcardTransactionType useChip, long timestamp) throws SQLException, MegaBankException {
		final String historyQueryStr = "SELECT ch.METHOD, ISNULL(err.errorstr,'None'), ISNULL(c.state,'None'), ISNULL(c.zipcode,'0'), ISNULL(c.city,'ONLINE'), m.merchant_name, m.mcc, hd.amount, hd.time "
				+ "FROM CARDHISTORY ch " + "LEFT JOIN ERROR err ON ch.errid=err.errid "
				+ "LEFT JOIN HISTORY hd ON hd.reftxid=ch.txid " + "LEFT JOIN customeraccs ca ON hd.accid=ca.accid "
				+ "LEFT JOIN customer c ON ca.custid=c.custid " + "LEFT JOIN merchantacc ma ON ma.accid=hd.accid "
				+ "LEFT JOIN merchant m ON ma.merchantid=m.merchantid " + "WHERE CCARDID=? ORDER BY ch.txid DESC LIMIT "
				+ (model.numberTimesteps() - 1) + " WITH ur";

		ModelInputs modelInputs = new ModelInputs(model.numberTimesteps());

		int i;
		int trys = 0;
		try (PreparedStatement prep = con.prepareStatement(historyQueryStr)) {
			prep.setInt(1, cardAccount.card);
			ResultSet rs = prep.executeQuery();
			i = model.numberTimesteps() - 1;
			while (rs.next()) {
				i--;
				Timestamp ts = rs.getTimestamp(9);
				if (ts == null) {
					System.out.println("null: " + i);
					break;
				}
				modelInputs.TimeDelta[0][i] = ts.getTime();
				modelInputs.UseChip[0][i] = rs.getInt(1);
				modelInputs.Errors[0][i] = rs.getString(2);
				modelInputs.MerchantState[0][i] = rs.getString(3);
				modelInputs.Zip[0][i] = rs.getString(4);
				modelInputs.MerchantCity[0][i] = rs.getString(5);
				modelInputs.MerchantName[0][i] = rs.getString(6);
				modelInputs.MCC[0][i] = Integer.toString(rs.getInt(7));
				modelInputs.Amount[0][i] = rs.getBigDecimal(8);
			}

		}
		// System.out.println(cardAccount.card);
		if (i != 0) {
			System.out.println("Not Enough history (" + i + ") to check Fraud for card " + cardAccount.card);
			//System.out.println(Arrays.toString(modelInputs.Amount[0]));
			//System.out.println(historyQueryStr);
			// System.exit(0);
			return null;
		}

		i = model.numberTimesteps() - 1;

		modelInputs.UseChip[0][i] = useChip.ordinal();
		modelInputs.Errors[0][i] = "None";
		modelInputs.Amount[0][i] = amount;
		modelInputs.TimeDelta[0][i] = timestamp;

		final String merchQueryStr = "SELECT  ISNULL(c.state,'None'), ISNULL(c.zipcode,'0'), ISNULL(c.city,'ONLINE'), m.merchant_name, m.mcc "
				+ "FROM merchantacc ma  JOIN merchant m ON ma.merchantid=m.merchantid JOIN customeraccs ca ON ma.accid=ca.accid JOIN customer c ON ca.custid=c.custid "
				+ "WHERE ma.accid=?";
		try (PreparedStatement prep = con.prepareStatement(merchQueryStr)) {
			prep.setInt(1, merchantAccId);
			ResultSet rs = prep.executeQuery();
			if (!rs.next())
				throw new MegaBankException("Merchant not found");
			modelInputs.MerchantState[0][i] = rs.getString(1);
			modelInputs.Zip[0][i] = rs.getString(2);
			modelInputs.MerchantCity[0][i] = rs.getString(3);
			modelInputs.MerchantName[0][i] = rs.getString(4);
			modelInputs.MCC[0][i] = Integer.toString(rs.getInt(5));
		}

		Calendar calendar = Calendar.getInstance();
		long lastTime = modelInputs.TimeDelta[0][0];
		for (int idx = 0; idx < modelInputs.TimeDelta[0].length; idx++) {
			long thisTime = modelInputs.TimeDelta[0][idx];
			calendar.setTimeInMillis(thisTime);
			modelInputs.Month[0][idx] = calendar.get(Calendar.MONTH);
			modelInputs.Day[0][idx] = calendar.get(Calendar.DAY_OF_MONTH);
			modelInputs.Hour[0][idx] = calendar.get(Calendar.HOUR_OF_DAY);
			modelInputs.Minute[0][idx] = calendar.get(Calendar.MINUTE);

			lastTime = modelInputs.TimeDelta[0][idx];
			modelInputs.TimeDelta[0][idx] = (thisTime - lastTime) * 1000000;
		}

		return modelInputs;
	}

	private boolean checkFraud(Connection con, CardAccount cardAccount, int merchantAccId, BigDecimal amount,
			CreditcardTransactionType useChip, long timestamp) throws SQLException, MegaBankException {
		ModelInputs in = getUserHistory(con, cardAccount, merchantAccId, amount, useChip, timestamp);
		if (in == null)
			return false;
		return model.checkFraud(in);
	}

	@Asynchronous
	private Future<Boolean> asyncCallModel(ModelInputs in) {
		boolean fraud = model.checkFraud(in);
		return new AsyncResult<>(fraud);
	}

	private Future<Boolean> asyncCheckFraud(Connection con, CardAccount cardAccount, int merchantAccId,
			BigDecimal amount, CreditcardTransactionType useChip, long timestamp)
			throws SQLException, MegaBankException {
		ModelInputs in = getUserHistory(con, cardAccount, merchantAccId, amount, useChip, timestamp);
		if (in == null)
			return null;

		return asyncCallModel(in);
	}

	@POST
	@Path("Transfer")
	@Consumes(MediaType.APPLICATION_JSON)
	@Produces({ MediaType.APPLICATION_JSON })
	public String doCardTransaction(CreditCardTransaction transaction) throws MegaBankException {
		if (transaction.timestamp == 0)
			transaction.timestamp = System.currentTimeMillis();
		// System.out.println(transaction.transactionUuid);
		// System.out.println(transaction.cardNumber);
		// System.out.println(transaction.amount);

		CreditcardTransactionType method = CreditcardTransactionType.getType(transaction.method);

		CardAccount cardAccount;
		ModelInputs in = null;
		try (Connection con = getConnection()) {
			// con.setReadOnly(true);
			if (!checkMerchantToken(con, transaction.merchantAcc, transaction.merchantToken)) {
				// System.out.println("Merchant not found");
				return "merchant auth failed";
			}

			cardAccount = getUserCard(con, transaction.cardNumber, transaction.cvv, transaction.expirationDate);
			if (cardAccount == null) {
				// System.out.println("Card not found");
				return "card auth failed";
			}

			if (CHECK_FRAUD) {
				in = getUserHistory(con, cardAccount, transaction.merchantAcc, transaction.amount, method,
						transaction.timestamp);
			}
//		} catch (SQLException e1) {
//			e1.printStackTrace();
//			return false;
//		}

			if (in != null) {
				if (model.checkFraud(in))
					return "fraud";
			}

//		try (Connection con = getConnection()) {
			boolean success = transfer(con, cardAccount, transaction.merchantAcc, transaction.amount, method,
					transaction.timestamp);
			con.commit();
//            boolean success = true;
			return success ? "success" : "insufficant balance";
		} catch (SQLException e) {
			e.printStackTrace();
			return "Technical Error";
		}
	}

	@POST
	@Path("TransferAsync")
	@Consumes(MediaType.APPLICATION_JSON)
	@Produces({ MediaType.APPLICATION_JSON })
	public boolean doAsyncCardTransaction(CreditCardTransaction transaction)
			throws MegaBankException, InterruptedException, ExecutionException {
		// System.out.println(transaction.transactionUuid);
		// System.out.println(transaction.cardNumber);
		// System.out.println(transaction.amount);
		try (Connection con = getConnection()) {
			if (transaction.timestamp == 0)
				transaction.timestamp = System.currentTimeMillis();
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

			Future<Boolean> isFraud = null;
			if (CHECK_FRAUD) {
				isFraud = asyncCheckFraud(con, cardAccount, transaction.merchantAcc, transaction.amount,
						CreditcardTransactionType.ONLINE, transaction.timestamp);
			}
			transfer(con, cardAccount, transaction.merchantAcc, transaction.amount, CreditcardTransactionType.ONLINE,
					transaction.timestamp);
			if (isFraud != null && isFraud.get()) {
				con.rollback();
				return false;
			}
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
			CreditcardTransactionType type, long timestamp) throws MegaBankException, SQLException {

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
					return false;
					//throw new MegaBankException("MegaBankJDBC.transfer.withdraw: failed");
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

			insertCardHistory(con, amount, accidTo, card, type, lastFromTX, lastToTX, null, timestamp);
		} catch (SQLException e) {
			System.err.println("ERROR MegaBankJDBCService: transfer()"); // FIX RM 2020-04-29
			System.err.println(e.getMessage()); // FIX RM 2020-04-29
			throw e;
			// throw new MegaBankException(e.getMessage());
		}

		return true;
	}

	private void insertCardHistory(Connection con, BigDecimal amount, int accidTo, CardAccount card,
			CreditcardTransactionType type, long lastFromTX, long lastToTX, Integer errId, long timestamp)
			throws MegaBankException, SQLException {
		insertHistoryTransfer(con, "w", amount, accidTo, card.accountId, lastToTX, lastFromTX, timestamp);
		insertHistoryTransfer(con, "d", amount, card.accountId, accidTo, lastFromTX, lastToTX, timestamp);

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
			long lastREFTX, long lastTX, long timestamp) throws MegaBankException {

		final String insertHistoryTransferSQL = "INSERT INTO HISTORY (TIME, TXID, TRANSTYPE, AMOUNT, REFTXID, ACCID) "
				+ " VALUES (?, ?, ?, ?, ?, ?)";

		if (insertIntoHistory)
			try (PreparedStatement prep = con.prepareStatement(insertHistoryTransferSQL)) {
				prep.setTimestamp(1, new Timestamp(timestamp));
				prep.setLong(2, (long) ACCID * 10000000000l + lastTX); // Use the ACCID to generate the "base" TXID
				prep.setString(3, TRANSTYPE);
				prep.setBigDecimal(4, AMOUNT);
				prep.setLong(5, (long) REFACCID * 10000000000l + lastREFTX);
				prep.setInt(6, ACCID);

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
		return "OK\n";
	}
}
