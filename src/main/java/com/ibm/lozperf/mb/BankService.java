package com.ibm.lozperf.mb;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Map;

import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.sql.DataSource;
import javax.ws.rs.ApplicationPath;
import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.core.Application;
import javax.ws.rs.core.MediaType;

@ApplicationPath("MegaCard")
@Path("Svc")
public class BankService extends Application {

	private final String TF_URL = System.getenv("TF_URL");
	private static Client client = ClientBuilder.newBuilder().build();
	
	private static DataSource ds = retrieveDataSource();
	
	
	public BankService() {
		
	}
	
	private UserCard getUserCard(Connection con, String cardNumber, String cvv, String expirationDate) throws SQLException {
		final String queryStr = "SELECT accid, ccardid FROM creditcard WHERE ccnumber=? AND cvv=? AND expiration=?";
		try (Connection con = getConnection()) {
			PreparedStatement prep = con.prepareStatement(queryStr);
			prep.setLong(1, Long.parseLong(cardNumber));
			prep.setInt(2, Integer.parseInt(cvv));
			prep.setInt(3, Integer.parseInt(expirationDate.replace("/", "")));
			ResultSet rs = prep.executeQuery();
			if(!rs.next())
				return null;
			return new UserCard(rs.getInt(1),rs.getInt(2));
		}
	}

	private boolean checkMerchantToken(Connection con, int accid, String merchantToken) throws SQLException {
		final String queryStr = "SELECT count(accid) FROM merchantacc WHERE accid=? AND token=?";
		try (Connection con = getConnection()) {
			PreparedStatement prep = con.prepareStatement(queryStr);
			prep.setInt(1, accid);
			prep.setString(2, merchantToken);
			ResultSet rs = prep.executeQuery();
			rs.next();
			return rs.getInt(1)==1;
		}
	}

	
	private boolean checkFraud(UserCard userCard, int MerchenAccId, BigDecimal amount, TransactionType useChip) {
		//final String queryStr = "SELECT ";
		
		return false;
	}

	@POST
	@Path("Transfer")
	@Consumes(MediaType.APPLICATION_JSON)
	@Produces({ MediaType.APPLICATION_JSON })
	public boolean doCardTransaction(CreditCardTransaction transaction) throws MegaBankException {
		System.out.println(transaction.transactionUuid);
		System.out.println(transaction.cardNumber);
		System.out.println(transaction.amount);
		try (Connection con = getConnection()) {
			if(!checkMerchantToken(con, transaction.merchantAcc, transaction.merchantToken))
				return false;
			
			UserCard userCard = getUserCard(con, transaction.cardNumber, transaction.cvv, transaction.expirationDate);
			if (userCard == null)
				return false;

			if (checkFraud(userCard, transaction.merchantAcc, transaction.amount, TransactionType.ONLINE))
				return false;
			return mb.transfer(userCard.userId, transaction.merchantAcc, transaction.amount);
		} catch (SQLException e) {
			e.printStackTrace();
			return false;
		}
	}
	
	
	private static DataSource retrieveDataSource() {
		Map <String,String> envList = System.getenv();
		
		// If these are not set, set them to the default
		if ( envList.containsKey("INSERT_INTO_HISTORY") ) insertIntoHistory = Boolean.parseBoolean(System.getenv("INSERT_INTO_HISTORY"));
		if ( envList.containsKey("AUTOCOMMIT") ) autoCommit = Boolean.parseBoolean(System.getenv("AUTOCOMMIT"));
		if ( envList.containsKey("ALLOW_DUP_LOGON") ) allowDupLogon = Boolean.parseBoolean(System.getenv("ALLOW_DUP_LOGON"));

		String dsName = "UNKNOWN !! ";
		if ( envList.containsKey("JDBCNAME") ) dsName = System.getenv("JDBCNAME");

		System.out.println("MegaBankJDBC: datasource name = "+dsName);
		System.out.println("MegaBankJDBC: tx history table inserts = " + Boolean.toString(insertIntoHistory));
		System.out.println("MegaBankJDBC: autoCommit = " + Boolean.toString(autoCommit));
		System.out.println("MegaBankJDBC: allow Duplicate Logons = " + Boolean.toString(allowDupLogon));

		try {
			Context ctx = new InitialContext();
			ds = (DataSource)ctx.lookup(dsName);
			System.out.println("MegaBankJDBC: using datasource = "+dsName);
					
		} catch (NamingException e) {
			System.err.println("MegaBankJDBC: Error, cannot find datasource " + dsName  );
			e.printStackTrace();
		}
	}
	
	protected Connection getConnection() throws SQLException {
		Connection con = ds.getConnection();
		if( !autoCommit)
			con.setAutoCommit(false);
		return con;
	}

	private enum TransactionType {
		ONLINE, CHIP, STRIP
	}
}
