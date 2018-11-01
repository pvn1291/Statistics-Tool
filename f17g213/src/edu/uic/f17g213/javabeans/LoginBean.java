package edu.uic.f17g213.javabeans;

import javax.faces.bean.ManagedBean;
import javax.faces.bean.SessionScoped;

@ManagedBean(name = "loginBean")
@SessionScoped
public class LoginBean {
	private String username;
	private String password;
	private String dbHost;
	private String dbSchema;
	private String dbmsType;
	private String dbmsPort;

	public LoginBean() {}
	
	public String getDbSchema() {
		return dbSchema;
	}
	public void setDbSchema(String dbSchema) {
		this.dbSchema = dbSchema;
	}
	public String getUsername() {
		return username;
	}
	public void setUsername(String username) {
		this.username = username;
	}
	public String getPassword() {
		return password;
	}
	public void setPassword(String password) {
		this.password = password;
	}
	public String getDbHost() {
		return dbHost;
	}
	public void setDbHost(String dbHost) {
		this.dbHost = dbHost;
	}
	public String getDbmsType() {
		return dbmsType;
	}
	public void setDbmsType(String dbmsType) {
		this.dbmsType = dbmsType;
	}
	public String getDbmsPort() {
		return dbmsPort;
	}
	public void setDbmsPort(String dbmsPort) {
		this.dbmsPort = dbmsPort;
	}
}