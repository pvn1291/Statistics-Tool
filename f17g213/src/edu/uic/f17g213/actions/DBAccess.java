package edu.uic.f17g213.actions;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.net.Inet4Address;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;

import javax.annotation.PostConstruct;
import javax.faces.bean.ManagedBean;
import javax.faces.bean.SessionScoped;
import javax.faces.context.ExternalContext;
import javax.faces.context.FacesContext;
import javax.servlet.http.HttpSession;
import javax.servlet.jsp.jstl.sql.Result;
import javax.servlet.jsp.jstl.sql.ResultSupport;
import javax.swing.JFileChooser;

import edu.uic.f17g213.javabeans.LoginBean;

@ManagedBean(name = "databaseBean")
@SessionScoped
public class DBAccess {
	private LoginBean loginBean;
	private String tableName;
	private String selectedSchema;
	private String errorMessage;
	private String sqlQuery;
	private String url;
	private Connection connection;
	private String message;
	private Statement statement;
	private DatabaseMetaData databaseMetaData;
	private ResultSet resultSet;
	private ResultSetMetaData resultSetMetaData;
	private Result result;
	private boolean messageRendered;
	private boolean queryRendered;
	private boolean processQueryRendered;
	private List<String> schemaList;
	private List<String> tableList;
	private List<String> columnList;
	private List<String> dropTableList;
	private List<String[]> dataList;
	private List<String> columnNamesSelected;
	private List<String> selectedColumnNames;
	private String sqlMessage;
	private String sqlErrorCode;
	private String sqlState;
	private static final String[] TABLE_TYPES = {"TABLE", "VIEW"};
	private int numberOfColumns;
	private IPLogs ipLogs;
	
	public DBAccess() {
		ipLogs = new IPLogs();
	}

	@PostConstruct
	public void init() {
		FacesContext context = FacesContext.getCurrentInstance();
		Map<String, Object> m = context.getExternalContext().getSessionMap();
		loginBean = (LoginBean) m.get("loginBean");
	}

	public void fillSchemaList() {
		schemaList = new ArrayList<String>();
		ResultSet schemaResultSet;
		try {
			schemaResultSet = databaseMetaData.getCatalogs();
			while (schemaResultSet.next()) {
				schemaList.add(schemaResultSet.getString("TABLE_CAT"));
			}
			selectedSchema = loginBean.getDbSchema();
		} catch (SQLException excp) {
			this.sqlMessage = excp.getMessage();
			this.sqlErrorCode = String.valueOf(excp.getErrorCode());
			this.sqlState = excp.getSQLState();
			errorMessage = excp.getMessage();
		}
	}

	
	public String connect() {
		errorMessage = "";
		String jdbcDriver = null;
		switch (loginBean.getDbmsType()) {
		case "MySQL":
			loginBean.setDbmsPort("3306");
			jdbcDriver = "com.mysql.jdbc.Driver";
			url = "jdbc:mysql://" + loginBean.getDbHost() + ":" + loginBean.getDbmsPort() + "/" + loginBean.getDbSchema();
			break;
		case "DB2":
			loginBean.setDbmsPort("50000");
			jdbcDriver = "com.ibm.db2.jcc.DB2Driver";
			url = "jdbc:db2://" + loginBean.getDbHost() + ":" + loginBean.getDbmsPort() + "/" + loginBean.getDbSchema();
			break;
		case "Oracle":
			loginBean.setDbmsPort("1521");
			jdbcDriver = "oracle.jdbc.driver.OracleDriver";
			url = "jdbc:oracle:thin:@" + loginBean.getDbHost() + ":" + loginBean.getDbmsPort() + "/" + loginBean.getDbSchema();
			break;
		}
		try {
			Class.forName(jdbcDriver);
			connection = DriverManager.getConnection(url, loginBean.getUsername(), loginBean.getPassword());
			statement = connection.createStatement(ResultSet.TYPE_SCROLL_INSENSITIVE, ResultSet.CONCUR_UPDATABLE);
			databaseMetaData = connection.getMetaData();
			fillSchemaList();
			listTables();
			listColumns();
			
			FacesContext context = FacesContext.getCurrentInstance();
			HttpSession session = (HttpSession) context.getExternalContext().getSession(false);
			String sessionId = session.getId();
			String ipAddress = Inet4Address.getLocalHost().getHostAddress();
			
			ipLogs.setDbmsType(loginBean.getDbmsType());
			ipLogs.setUserName(loginBean.getUsername());
			ipLogs.setSessionID(sessionId);
			ipLogs.setIpAddress(ipAddress);
			
			statement.executeUpdate(ipLogs.captureLogIn());
			
			return "SUCCESS";
		} catch (ClassNotFoundException ce) {
			errorMessage = "Database: " + loginBean.getDbmsType() + " not supported.";
			return "FAIL";

		} catch (SQLException se) {
			this.sqlMessage = se.getMessage();
			this.sqlErrorCode = String.valueOf(se.getErrorCode());
			this.sqlState = se.getSQLState();
			if (se.getSQLState().equals("28000")) {
				errorMessage = "Invalid User/Password";
			} else if (se.getSQLState().equals("42000")) {
				errorMessage = "Schema does not exists";
			} else if (se.getSQLState().equals("08S01")) {
				errorMessage = "Application Timeout \n or check host and port";
			} else {
				errorMessage = "SQL Exception occurred!\n" + "Error Code: " + se.getErrorCode() + "\n" + "SQL State: "
						+ se.getSQLState() + "\n" + "Message :" + se.getMessage() + "\n\n";
			}
			return "FAIL";
		} catch (Exception e) {
			errorMessage = "Exception occurred: " + e.getMessage();
			return "SUCCESS";
		}
	}

	public String processFileDownload() 
	{
		FacesContext fc = FacesContext.getCurrentInstance();
		FileOutputStream fos = null;
		String path = fc.getExternalContext().getRealPath("/tmp");
		String fileName = path + "/" + "F17G213_" + tableName + ".csv";
		File f = new File(fileName);
		StringBuffer sb = new StringBuffer();
		try {
			fos = new FileOutputStream(fileName);
			for (int i = 0; i < columnNamesSelected.size(); i++) {
				sb.append(columnNamesSelected.get(i) + ",");
			}
			sb.deleteCharAt(sb.length() - 1);
			sb.append("\n");
			fos.write(sb.toString().getBytes());
			String sqlQuery = "select " + columnNamesSelected.toString().replace("[", "").replace("]", "") + " from "
					+ loginBean.getDbSchema() + "." + tableName + " ;";
			ResultSet rs = statement.executeQuery(sqlQuery);
			rs.beforeFirst();
			while (rs.next()) {
				StringBuilder builder = new StringBuilder();
				for (int i = 0; i < columnNamesSelected.size(); i++) {
					String cell = rs.getString(columnNamesSelected.get(i));
					//cell = cell.replaceAll("[^a-zA-Z0-9 -]", "");
					if (cell != null && !cell.isEmpty())
						cell = cell.replaceAll("\\W+", "");					
					builder.append(cell).append(",");
				}
				builder.deleteCharAt(builder.length() - 1);
				builder.append("\n");
				fos.write(builder.toString().getBytes());
			}
			fos.flush();
			fos.close();
			message = "Successfully exported " + tableName + " as CSV! " + "\n";
			messageRendered = true;
			String mimeType = fc.getExternalContext().getMimeType(fileName);

			FileInputStream in = null;
			byte b;
			fc.getExternalContext().responseReset();
			fc.getExternalContext().setResponseContentType(mimeType);
			fc.getExternalContext().setResponseContentLength((int) f.length());
			fc.getExternalContext().setResponseHeader("Content-Disposition",
					"attachment; filename=\"" + "F17G213_" + tableName + "\\.csv" + "\"");
			try {
				// use previously generated temp file as input
				in = new FileInputStream(f);
				OutputStream output = fc.getExternalContext().getResponseOutputStream();
				while (true) {
					b = (byte) in.read();
					if (b < 0)
						break;
					output.write(b);
				}
			} catch (Exception e) {
				message = "Error Exporting File";
				messageRendered = true;
				return "FAIL";
			} finally {
				try {
					in.close();
				} catch (Exception e) {
					message = "Failed to close input file";
					messageRendered = true;
					return "FAIL";
				}
			}
			fc.responseComplete();
		} catch (Exception ex) {
			ex.printStackTrace();
			message = ex.getMessage();
			messageRendered = true;
			return "FAIL";
		}
		return "SUCCESS";
	}
	
	public String dbExecute(String tableName, String query) {
		String status = "";
		queryRendered = false;
		try {
			if (connection == null) {
				connect();
			}
			statement = connection.createStatement();
			if (query.toLowerCase().contains("select")) {
				resultSet = statement.executeQuery(query);
				resultSetMetaData = resultSet.getMetaData();
				generateResult();
				numberOfColumns = resultSetMetaData.getColumnCount();
				if (resultSet != null) {
					resultSet.last();
					int numberOfRows = resultSet.getRow();
					dataList = new ArrayList<String[]>(numberOfRows);
					resultSet.beforeFirst();

					int numOfCols = resultSet.getMetaData().getColumnCount();
					selectedColumnNames = new ArrayList<String>(numOfCols);
					for (int i = 0; i < numOfCols; i++) {
						selectedColumnNames.add(resultSetMetaData.getColumnName(i + 1));
					}					
				}

				status = "SUCCESS";
				queryRendered = true;
			} else {
				if (query.toLowerCase().contains("create")) {
					String query1 = "DROP TABLE IF EXISTS " + loginBean.getDbSchema() + "." + tableName + "; ";
					statement.executeUpdate(query1);
				}
				statement.executeUpdate(query);
				status = "SUCCESS";
			}
		} catch (SQLException se) {
			this.sqlMessage = se.getMessage();
			this.sqlErrorCode = String.valueOf(se.getErrorCode());
			this.sqlState = se.getSQLState();
			message = se.getMessage();
			messageRendered = true;
			status = "FAIL";
			se.printStackTrace();
		} catch (Exception e) {
			message = e.getMessage();
			messageRendered = true;
			status = "FAIL";
			e.printStackTrace();
		}
		return status;

	}

	public String listTables() {
		String status = "";
		tableName ="";
		tableList = new ArrayList<String>();
		columnList = new ArrayList<String>();
		sqlQuery = "";
		queryRendered = false;
		try {
			if (connection == null) {
				connect();
			}
			databaseMetaData = connection.getMetaData();
			resultSet = databaseMetaData.getTables(loginBean.getDbSchema(), loginBean.getDbSchema(), null, TABLE_TYPES);
			if (resultSet != null) {
				resultSet.last();
				int numberOfRows = resultSet.getRow();
				tableList = new ArrayList<String>(numberOfRows);
				resultSet.beforeFirst();
				String eachTableName = "";
				while (resultSet.next()) {
					eachTableName = resultSet.getString("TABLE_NAME");
					if (!loginBean.getDbmsType().equalsIgnoreCase("oracle") || eachTableName.length() < 4)
						tableList.add(eachTableName);
					else if (!eachTableName.substring(0, 4).equalsIgnoreCase("BIN$"))
						tableList.add(eachTableName);
				}
			}
			tableName = tableList.get(0);
			status = "SUCCESS";
		} catch (SQLException se) {
			this.sqlMessage = se.getMessage();
			this.sqlErrorCode = String.valueOf(se.getErrorCode());
			this.sqlState = se.getSQLState();
			status = "FAIL";
		}
		return status;
	}

	public String listColumns() {
		String status = "";
		columnList = new ArrayList<String>();;
		sqlQuery = "";
		queryRendered = false;
		message = "";
		if (tableName == null || tableName.equals("")) {
			message = "Please select a Table";
			messageRendered = true;
			return "FAIL";
		}
		try {
			if (connection == null) {
				connect();
			}
			databaseMetaData = connection.getMetaData();
			resultSet = databaseMetaData.getColumns(loginBean.getDbSchema(), loginBean.getDbSchema(), tableName, null);
			String columnName = "";
			if (resultSet != null) {
				resultSet.last();
				int numberOfRows = resultSet.getRow();
				columnList = new ArrayList<String>(numberOfRows);
				resultSet.beforeFirst();
				while (resultSet.next()) {
					columnName = resultSet.getString("COLUMN_NAME");
					columnList.add(columnName);
				}
			}
			status = "SUCCESS";
		} catch (SQLException se) {
			this.sqlMessage = se.getMessage();
			this.sqlErrorCode = String.valueOf(se.getErrorCode());
			this.sqlState = se.getSQLState();
			status = "FAIL";
		}
		return status;
	}

	public List<String> getSelectedColumnNames() {
		return selectedColumnNames;
	}

	public void setSelectedColumnNames(List<String> selectedColumnNames) {
		this.selectedColumnNames = selectedColumnNames;
	}

	public String displayTable() {
		queryRendered = false;
		try {
			if (null != tableName && !"".equals(tableName)) {
				if(columnNamesSelected == null || columnNamesSelected.isEmpty()) {
					columnNamesSelected.addAll(columnList);
				}
				sqlQuery = "select " + columnNamesSelected.toString().replace("[", "").replace("]", "") + " from "
						+ loginBean.getDbSchema() + "." + tableName + " ;";
				dbExecute(tableName, sqlQuery);
				generateResult();
				queryRendered = true;
				return "SUCCESS";
			} else {
				message = "Kindly load and select a table.";
				messageRendered = true;
				return "FAIL";
			}
		} catch (Exception ex) {
			ex.printStackTrace();
			queryRendered = false;
			return "FAIL";
		}
	}

	public String processSQLQuery() {
		processQueryRendered = false;
		try {
			if (sqlQuery == null || sqlQuery.equals("")) {
				message = "Please enter a SQL Query";
				messageRendered = true;
				processQueryRendered = false;
				return "FAIL";
			}
			if (dbExecute(tableName, sqlQuery).equalsIgnoreCase("SUCCESS")) {
				if (sqlQuery.toLowerCase().startsWith("select")) {

					ResultSetMetaData resultSetMetaData = resultSet.getMetaData();
					int columnCount = resultSetMetaData.getColumnCount();
					if (columnList == null) {
						columnList = new ArrayList<String>(columnCount);
					} else {
						columnList.clear();
					}
					for (int i = 1; i <= columnCount; i++) {
						columnList.add(resultSetMetaData.getColumnName(i));
					}
					generateResult();
					processQueryRendered = true;
				} else {
					processQueryRendered = false;
				}
			} else {
				processQueryRendered = false;
				return "FAIL";
			}
			return "SUCCESS";
		} catch (Exception e) {
			message = e.getMessage();
			e.printStackTrace();
			messageRendered = true;
			return "FAIL";
		}
	}

	public void generateResult() {
		if (resultSet != null) {
			result = ResultSupport.toResult(resultSet);
		}
	}

	public String processLogout() {
		try {
			ExternalContext ec = FacesContext.getCurrentInstance().getExternalContext();
			this.setLoginBean(null);
			setTableList(new ArrayList<String>());
			dbExecute(null, ipLogs.captureLogOut());
			ec.invalidateSession();
			ec.redirect("login.xhtml");
			return "LOGOUT";
		} catch (Exception e) {
			message = e.getMessage();
			setMessage(message);
			setMessage(message);
			return "FAIL";
		}
	}
	
	public int insertLogdata(String sqlQuery, String username, String dbms, String ipAddress, String sessionID) {
		try {
			java.sql.PreparedStatement query = connection.prepareStatement(sqlQuery);
			query.setString(1, username);
			query.setString(2, dbms);
			query.setTimestamp(3, new java.sql.Timestamp(new Date().getTime()));
			query.setString(4, ipAddress);
			query.setString(5, sessionID);
			query.executeUpdate();
			return 0;
		} catch (SQLException se) {
			message = "Error Code: " + se.getErrorCode() + "\n" + "SQL State: " + se.getSQLState() + "\n" + "Message :"
					+ se.getMessage() + "\n\n" + "SQLException while getting column data.";
			messageRendered = true;
			return -1;
		} catch (Exception e) {
			message = "Exception occurred: " + e.getMessage();
			messageRendered = true;
			return -1;
		}
	}

	public int updateLogoutTime(String sqlQuery, String userName, int rowId) {
		try {
			java.sql.PreparedStatement query = connection.prepareStatement(sqlQuery);
			query.setTimestamp(1, new java.sql.Timestamp(new Date().getTime()));
			query.setString(2, userName);
			query.setInt(3, rowId);
			int rows = query.executeUpdate();
			return rows;
		} catch (SQLException se) {
			message = "Error Code: " + se.getErrorCode() + "\n" + "SQL State: " + se.getSQLState() + "\n" + "Message :"
					+ se.getMessage() + "\n\n" + "SQLException while getting column data.";
			return -1;
		} catch (Exception e) {
			message = "Exception occurred: " + e.getMessage();
			return -1;
		}
	}

	public String dropTables() {
		String status = "";
		if (tableName == null || tableName.equals("")) {
			listTables();
			message = "Please select a Table";
			messageRendered = true;
			return "FAIL";
		}
		try {
			if(!loginBean.getDbSchema().equalsIgnoreCase("world")) {
				connection.setAutoCommit(false);
				dbExecute("", "set foreign_key_checks=0");
				status = dbExecute(tableName, "drop table " + loginBean.getDbSchema()+ "." + tableName + ";");
			} else {
				message = "World Schema is the Read-Only Schema. Cannot drop a table from world schema";
				messageRendered = true;
				return "FAIL";
			}
			if (status.equals("FAIL")) {
				message = "Table drop failed";
				messageRendered = true;
				connection.rollback();
				connection.setAutoCommit(true);
				return "FAIL";
			}
			connection.commit();
			connection.setAutoCommit(true);
		} catch (Exception e) {
			message = e.getMessage();
			messageRendered = true;
			status = "FAIL";
		}
		if (status.equals("SUCCESS")) {
			message = "Table drop succesfull!";
			messageRendered = true;
			listTables();
			listColumns();
		}
		return status;
	}

	public String exportToCSV() {
		message = "";
		if (tableName == null || tableName.isEmpty()) {
			message = "Select a table!";
			messageRendered = true;
			return "FAIL";
		}
		try {
			JFileChooser chooser = new JFileChooser();
			chooser.setCurrentDirectory(new java.io.File("."));
			chooser.setDialogTitle("select folder");
			chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
			chooser.setAcceptAllFileFilterUsed(false);
			int status = chooser.showOpenDialog(null);
			if (status == JFileChooser.CANCEL_OPTION) {
				return "FAIL";
			}
			File f = chooser.getSelectedFile();
			String fileName = f.getPath() + "\\Export_" + tableName + ".csv";
			PrintWriter csvWriter = new PrintWriter(new File(fileName));
			sqlQuery = "Select * from " + loginBean.getDbSchema() + "." + tableName + ";";
			dbExecute(tableName, sqlQuery);
			int numberOfColumns = resultSetMetaData.getColumnCount();
			String dataHeaders = "\"" + resultSetMetaData.getColumnName(1) + "\"";
			for (int i = 2; i < numberOfColumns + 1; i++) {
				dataHeaders += ",\"" + resultSetMetaData.getColumnName(i).replaceAll("\"", "\\\"") + "\"";
			}
			csvWriter.println(dataHeaders);
			resultSet.beforeFirst();
			while (resultSet.next()) {
				String row = "\"" + resultSet.getString(1).replaceAll("\"", "\\\"") + "\"";
				for (int i = 2; i < numberOfColumns + 1; i++) {
					String cell = resultSet.getString(i);
					if (cell == null)
						cell = "";
					row += ",\"" + cell.replaceAll("\"", "\\\"") + "\"";
				}
				csvWriter.println(row);
			}
			csvWriter.close();
			message = "Successfully exported " + tableName + " as CSV!";
			messageRendered = true;
			return "SUCCESS";
		} catch (Exception ex) {
			ex.printStackTrace();
			message = ex.getMessage();
			messageRendered = true;
			return "FAIL";
		}
	}

	public String resetButton() {
		messageRendered = false;
		queryRendered = false;
		if (tableList != null)
			tableList.clear();
		if (columnList != null)
			columnList.clear();
		return "SUCCESS";
	}

	public LoginBean getLoginBean() {
		return loginBean;
	}

	public void setLoginBean(LoginBean loginBean) {
		this.loginBean = loginBean;
	}

	public boolean isProcessQueryRendered() {
		return processQueryRendered;
	}

	public void setProcessQueryRendered(boolean processQueryRendered) {
		this.processQueryRendered = processQueryRendered;
	}

	public String getSqlQuery() {
		return sqlQuery;
	}

	public void setSqlQuery(String sqlQuery) {
		this.sqlQuery = sqlQuery;
	}

	public boolean isQueryRendered() {
		return queryRendered;
	}

	public void setQueryRendered(boolean queryRendered) {
		this.queryRendered = queryRendered;
	}

	public String getErrorMessage() {
		return errorMessage;
	}

	public void setErrorMessage(String errorMessage) {
		this.errorMessage = errorMessage;
	}

	public List<String[]> getDataList() {
		return dataList;
	}

	public void setDataList(List<String[]> dataList) {
		this.dataList = dataList;
	}

	public List<String> getDropTableList() {
		return dropTableList;
	}

	public void setDropTableList(List<String> dropTableList) {
		this.dropTableList = dropTableList;
	}

	public List<String> getColumnNamesSelected() {
		return columnNamesSelected;
	}

	public void setColumnNamesSelected(List<String> columnNamesSelected) {
		this.columnNamesSelected = columnNamesSelected;
	}

	public Connection getConnection() {
		return connection;
	}

	public void setConnection(Connection connection) {
		this.connection = connection;
	}

	public String getMessage() {
		return message;
	}

	public void setMessage(String message) {
		this.message = message;
	}

	public Statement getStatement() {
		return statement;
	}

	public void setStatement(Statement statement) {
		this.statement = statement;
	}

	public DatabaseMetaData getDatabaseMetaData() {
		return databaseMetaData;
	}

	public void setDatabaseMetaData(DatabaseMetaData databaseMetaData) {
		this.databaseMetaData = databaseMetaData;
	}

	public ResultSetMetaData getResultSetMetaData() {
		return resultSetMetaData;
	}

	public void setResultSetMetaData(ResultSetMetaData resultSetMetaData) {
		this.resultSetMetaData = resultSetMetaData;
	}

	public String getTableName() {
		return tableName;
	}

	public void setTableName(String tableName) {
		this.tableName = tableName;
		listColumns();
	}

	public Result getResult() {
		return result;
	}

	public void setResult(Result result) {
		this.result = result;
	}

	public int getNumberOfColumns() {
		return numberOfColumns;
	}

	public void setNumberOfColumns(int numberOfColumns) {
		this.numberOfColumns = numberOfColumns;
	}

	public static String[] getTableTypes() {
		return TABLE_TYPES;
	}

	public void setTableList(List<String> tableList) {
		this.tableList = tableList;
	}

	public void setColumnList(List<String> columnList) {
		this.columnList = columnList;
	}

	public ResultSet getResultSet() {
		return resultSet;
	}

	public void setResultSet(ResultSet resultSet) {
		this.resultSet = resultSet;
	}

	public String getUrl() {
		return url;
	}

	public List<String> getTableList() {
		return tableList;
	}

	public List<String> getColumnList() {
		return columnList;
	}

	public List<String> getSchemaList() {
		return schemaList;
	}

	public void setSchemaList(List<String> schemaList) {
		this.schemaList = schemaList;
	}

	public boolean isMessageRendered() {
		return messageRendered;
	}

	public void setMessageRendered(boolean messageRendered) {
		this.messageRendered = messageRendered;
	}

	public String getSqlMessage() {
		return sqlMessage;
	}

	public String getSqlErrorCode() {
		return sqlErrorCode;
	}

	public String getSqlState() {
		return sqlState;
	}

	public String getSelectedSchema() {
		return selectedSchema;
	}

	public void setSelectedSchema(String selectedSchema) {
		this.selectedSchema = selectedSchema;
		loginBean.setDbSchema(selectedSchema);
		listTables();
		listColumns();
	}
}