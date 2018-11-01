
package edu.uic.f17g213.actions;

import java.util.Date;
import java.text.*;

import java.text.SimpleDateFormat;

public class IPLogs
{
	private static final String dbSchemaName = "f17x321";
	private static final String tableName = ".f17g213_iplog";
	
	private String userName;
	private String dbmsType;
	private String sessionID;
	private String ipAddress;
	private String loginTime;
	private String logoutTime;
	
	
	public String captureLogIn()
	{	
		String sqlQuery = "Create table if not exists " + dbSchemaName + tableName + " (SessionID VARCHAR(45)"
				+ " NOT NULL , Username char(50) not null, "
				+ "dbms char(50) ,LoginTime VARCHAR(45) null, LogoutTime VARCHAR(45) null, "
				+ "IPAddress char(50), PRIMARY KEY (SessionID)) "
				+ "ENGINE=InnoDB AUTO_INCREMENT=1 DEFAULT CHARSET=latin1;";
		
		loginTime = getSystemDateAndTime();
		return sqlQuery;
		
	}
	
	public String captureLogOut()
	{
		logoutTime = getSystemDateAndTime();
		
		String sqlQuery = "Insert into " + dbSchemaName + tableName + " (SessionID, Username, dbms,"
				+ "LoginTime, LogoutTime, IPAddress) " + "values (" + "\"" + sessionID + "\"" + "," + "\"" + userName + "\"" + "," 
				+ "\"" + dbmsType + "\"" + "," + "\"" + loginTime + "\"" + "," + "\"" + logoutTime + "\"" + "," 
				+ "\"" + ipAddress + "\"" + ")";  
		return sqlQuery;
	}
	
	public String getSystemDateAndTime()
	{
		DateFormat df = new SimpleDateFormat("dd/MM/yy HH:mm:ss");
		Date date = new Date();
		String tmpDate = df.format(date);
		return tmpDate;
	}

	public String getUserName() {
		return userName;
	}

	public void setUserName(String userName) {
		this.userName = userName;
	}

	public String getDbmsType() {
		return dbmsType;
	}

	public void setDbmsType(String dbmsType) {
		this.dbmsType = dbmsType;
	}

	public String getSessionID() {
		return sessionID;
	}

	public void setSessionID(String sessionID) {
		this.sessionID = sessionID;
	}

	public String getIpAddress() {
		return ipAddress;
	}

	public void setIpAddress(String ipAddress) {
		this.ipAddress = ipAddress;
	}

	public String getLoginTime() {
		return loginTime;
	}

	public void setLoginTime(String loginTime) {
		this.loginTime = loginTime;
	}

	public String getLogoutTime() {
		return logoutTime;
	}

	public void setLogoutTime(String logoutTime) {
		this.logoutTime = logoutTime;
	}
}
