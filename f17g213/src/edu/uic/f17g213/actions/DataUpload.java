package edu.uic.f17g213.actions;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Scanner;

import javax.annotation.PostConstruct;
import javax.faces.bean.ManagedBean;
import javax.faces.bean.SessionScoped;
import javax.faces.context.ExternalContext;
import javax.faces.context.FacesContext;
import javax.servlet.jsp.jstl.sql.Result;
import javax.servlet.jsp.jstl.sql.ResultSupport;

import org.apache.commons.lang.StringUtils;
import org.apache.myfaces.custom.fileupload.UploadedFile;

import edu.uic.f17g213.javabeans.DataUploadBean;
import edu.uic.f17g213.actions.DBAccess;


@ManagedBean(name = "dataUploadActions")
@SessionScoped
public class DataUpload {
	private DBAccess dbAccess;
	private static final String DEFAULT_DELIMITER = ";";
	private DataUploadBean dataUploadBean;
	private Map<String, List<Result>> scriptDetailsForSelect;
	private Map<String, List<String>> scriptDetailsForOtherQueries;
	private Map<String, DataUploadBean> uploadBeanList;
	private Map<String, List<List<String>>> columnListsInUploadedSelect;
	private Map<String, List<String>> selectQueryList;
	private String errorMessage;
	private boolean renderErrorMessage;
	private boolean renderScriptNames;
	private List<String> sciptNames;
	private String selectedScript;
	private List<Result> scriptResults;
	private boolean renderScriptResults;
	private List<String> queryStatements;
	private boolean renderQueryStatements;
	private List<List<String>> resultColumns;
	private List<String> selectQueryStatements;
	private String selectedSelectQuery;
	private List<String> resultColumnList;
	private Result selectedResult;
	private boolean showResultSet;
	private String message;
	private boolean renderMessage;

	@PostConstruct
	public void init() {
		FacesContext facesContext = FacesContext.getCurrentInstance();
		ExternalContext ec = facesContext.getExternalContext();
		Map<String, Object> contextMap = ec.getSessionMap();
		dataUploadBean = (DataUploadBean) contextMap.get("dataUpload");
		dbAccess = (DBAccess) contextMap.get("databaseBean");
		scriptDetailsForSelect = new HashMap<String, List<Result>>();
		scriptDetailsForOtherQueries = new HashMap<String, List<String>>();
		uploadBeanList = new HashMap<String, DataUploadBean>();
		columnListsInUploadedSelect = new HashMap<String, List<List<String>>>();
		selectQueryList = new HashMap<String, List<String>>();
		resetButton();
		listFilesForFolder(new File(ec.getRealPath("/tmp")));
	}

	public String executeExistingScript() {
		try {
			renderMessage = false;
			message = "";
			errorMessage = "";
			renderErrorMessage = false;
			showResultSet = false;
			renderScriptResults = false;
			if(scriptDetailsForSelect != null && !scriptDetailsForSelect.isEmpty() && scriptDetailsForSelect.get(selectedScript) != null) {
				scriptResults = new ArrayList<Result>(scriptDetailsForSelect.get(selectedScript));
			}
			if(scriptDetailsForOtherQueries != null && !scriptDetailsForOtherQueries.isEmpty() && scriptDetailsForOtherQueries.get(selectedScript) != null) {
			    queryStatements = new ArrayList<String>(scriptDetailsForOtherQueries.get(selectedScript));
			}
			if(columnListsInUploadedSelect != null && !columnListsInUploadedSelect.isEmpty() && columnListsInUploadedSelect != null) {
			    resultColumns = new ArrayList<List<String>>(columnListsInUploadedSelect.get(selectedScript));
			}
			if(selectQueryList != null && !selectQueryList.isEmpty()) {
			    selectQueryStatements = new ArrayList<String>(selectQueryList.get(selectedScript));
			}
			if(queryStatements != null && !queryStatements.isEmpty() && scriptDetailsForOtherQueries.get(selectedScript) != null) {
				renderQueryStatements = true;
			} else {
				renderQueryStatements = false;
			}
			if(scriptResults != null && !scriptResults.isEmpty() && scriptDetailsForSelect.get(selectedScript) != null) {
				renderScriptResults = true;
			} else {
				renderScriptResults = false;
			}
			System.out.println(">>: " + queryStatements.toString());
			return "SUCCESS";
		} catch (Exception e) {
			System.out.println("DataUploadAction Exception Occured in executeExistingScript : " +  e.getMessage());
			return "FAIL";
		}
	}

	public void listFilesForFolder(final File folder) {
		if(folder != null && folder.listFiles() != null) {
			for (final File fileEntry : folder.listFiles()) {
				if (StringUtils.containsIgnoreCase(fileEntry.getName(), ".sql")) {
					FacesContext context = FacesContext.getCurrentInstance();
					String filePath = context.getExternalContext().getRealPath("/tmp");
					dataUploadBean.setContext(context);
					dataUploadBean.setFilePath(filePath);
					String fileName = fileEntry.getAbsolutePath();
					String fullyQualifiedFileName[] = fileName.split("\\\\");
					int arrayOfFileNames = fullyQualifiedFileName.length - 1;
					String tempFileName = filePath + "/" + fullyQualifiedFileName[arrayOfFileNames];
					dataUploadBean.setTempFileName(tempFileName);
					dataUploadBean.setFileName(fullyQualifiedFileName[arrayOfFileNames]);
					dataUploadBean.setRenderUploadResults(true);
					processFile(fileEntry.getAbsolutePath(), true);
				}
			}
		}
	}

	public String processFileUpload() throws SQLException {
		renderErrorMessage = false;
		renderMessage = false;
		UploadedFile uploadedFile = dataUploadBean.getUploadedFile();
		dataUploadBean.setRenderUploadResults(false);
		if (uploadedFile == null) {
			return "FAIL";
		}
		FacesContext context = FacesContext.getCurrentInstance();
		String filePath = context.getExternalContext().getRealPath("/tmp");
		dataUploadBean.setContext(context);
		dataUploadBean.setFilePath(filePath);
		File tempFile = null;
		FileOutputStream fos = null;
		int n = 0;
		try {
			String fileName = uploadedFile.getName();
			String fullyQualifiedFileName[] = fileName.split("\\\\");
			int arrayOfFileNames = fullyQualifiedFileName.length - 1;
			String tempFileName = filePath + "/" + fullyQualifiedFileName[arrayOfFileNames];
			dataUploadBean.setTempFileName(tempFileName);
			dataUploadBean.setFileName(fullyQualifiedFileName[arrayOfFileNames]);
			tempFile = new File(tempFileName);
			fos = new FileOutputStream(tempFile);
			fos.write(uploadedFile.getBytes());
			fos.close();
			Scanner s = new Scanner(tempFile);
			while (s.hasNext()) {
				s.nextLine();
				n++;
			}
			s.close();
			dataUploadBean.setNumberRows(n);
		} catch (IOException e) {
			e.printStackTrace();
			return "FAIL";
		}
		dataUploadBean.setRenderUploadResults(true);
		String result = processFile(dataUploadBean.getTempFileName(), false);
		if(result.equalsIgnoreCase("SUCCESS")) {
			message = "Upload SuccessFull " + dataUploadBean.getFileName();
			renderMessage = true;
		}
		return result;
	}

	//Do Error handling for comment in code and the code which does not starts with a ddl ot dml statements.
	private String processFile(String fullQualifiedFileName, boolean isExistinFile) {
		renderScriptResults = false;
		renderQueryStatements = false;
		showResultSet = false;
		if(sciptNames == null || sciptNames.isEmpty()) {
			renderScriptNames = false;
		}
		renderErrorMessage = false;
		errorMessage = "";
		message = "";
		renderMessage = false;
		String s = new String();
		StringBuffer sb = new StringBuffer();
		List<String> columnNames = new ArrayList<String>();
		List<Result> queryResults = new ArrayList<Result>();
		List<String> statements = new ArrayList<String>();
		List<List<String>> multipleColumns = new ArrayList<List<String>>();
		List<String> selectStatements = new ArrayList<String>();
		try {
			FileReader fr = new FileReader(fullQualifiedFileName);
			BufferedReader br = new BufferedReader(fr);
			try {
				while ((s = br.readLine()) != null) {
					sb.append(s);
				}
				br.close();
				String[] inst = sb.toString().split(DEFAULT_DELIMITER);
				for (int i = 0; i < inst.length; i++) {
					// in order to not execute empty statements
					if (!inst[i].trim().equals("")) {
						System.out.println(">>" + inst[i]);
						if(!isExistinFile) {
							if(dbAccess.dbExecute(null, inst[i]).equalsIgnoreCase("SUCCESS")) {
								if(StringUtils.containsIgnoreCase(inst[i], "select")) {
									queryResults.add(ResultSupport.toResult(dbAccess.getResultSet()));
									columnNames.clear();
									columnNames.addAll(dbAccess.getSelectedColumnNames());
									multipleColumns.add(columnNames);
									selectStatements.add(inst[i]);
								} else {
									statements.add(inst[i]);
								}
							} else {
								renderErrorMessage = true;
								errorMessage = dbAccess.getSqlMessage();
								return "FAIL";
							}
						} else {
							if (!StringUtils.containsIgnoreCase(inst[i], "select")) {
								statements.add(inst[i]);
							} else {
								if(dbAccess.dbExecute(null, inst[i]).equalsIgnoreCase("SUCCESS")) {
									queryResults.add(ResultSupport.toResult(dbAccess.getResultSet()));
									columnNames.clear();
									columnNames.addAll(dbAccess.getSelectedColumnNames());
									multipleColumns.add(columnNames);
									selectStatements.add(inst[i]);
								}else {
									renderErrorMessage = true;
									errorMessage = dbAccess.getSqlMessage();
									return "FAIL";
								}
							}
						}
					}
				}
				if(!renderErrorMessage) {
					if(!queryResults.isEmpty()) {
						scriptDetailsForSelect.put(dataUploadBean.getFileName(), queryResults); 
					} // Will be Null if file already there
					if(!statements.isEmpty()) {
						scriptDetailsForOtherQueries.put(dataUploadBean.getFileName(), statements);
					}
					if (!columnNames.isEmpty()) {
						columnListsInUploadedSelect.put(dataUploadBean.getFileName(), multipleColumns); // Will be Null if file already there.
						selectQueryList.put(dataUploadBean.getFileName(), selectStatements);
					}
					renderScriptNames = true;
					uploadBeanList.put(dataUploadBean.getFileName(), dataUploadBean);
				}
			} catch (IOException e) {
				e.printStackTrace();
				renderErrorMessage = true;
				errorMessage = "Cannot Read the file";
				return "FAIL";
			}
		} catch (FileNotFoundException e) {
			e.printStackTrace();
			errorMessage = "File Not Found";
			renderErrorMessage = true;
			return "FAIL";
		}
		return "SUCCESS";
	}

	public void resetButton() {
		renderMessage = false;
		message = "";
		errorMessage = "";
		renderErrorMessage = false;
		showResultSet = false;
		renderScriptResults = false;
	}

	public String showSelectResults() {
		int indexOfQuery = selectQueryStatements.indexOf(selectedSelectQuery);
		selectedResult = scriptResults.get(indexOfQuery);
		if(resultColumnList != null) {
			resultColumnList.clear();
		} else {
			resultColumnList = new ArrayList<String>();
		}
		System.out.println(">>" + selectedSelectQuery);
		System.out.println("Results >>>>>> " + selectedResult.getRows());
		dbAccess.dbExecute(null, selectedSelectQuery);
		resultColumnList.addAll(dbAccess.getSelectedColumnNames());
		showResultSet = true;
		return "SUCCESS";
	}

	public List<String> getSciptNames() {
		sciptNames = new ArrayList<String>();
		sciptNames.addAll(uploadBeanList.keySet());
		return sciptNames;
	}

	public String getSelectedScript() {
		return selectedScript;
	}

	public List<List<String>> getResultColumns() {
		return resultColumns;
	}

	public boolean isRenderQueryStatements() {
		return renderQueryStatements;
	}

	public List<String> getSelectQueryStatements() {
		return selectQueryStatements;
	}

	public boolean isRenderScriptResults() {
		return renderScriptResults;
	}

	public List<String> getQueryStatements() {
		return queryStatements;
	}

	public void setSelectedScript(String selectedScript) {
		this.selectedScript = selectedScript;
	}

	public String getSelectedSelectQuery() {
		return selectedSelectQuery;
	}

	public void setSelectedSelectQuery(String selectedSelectQuery) {
		this.selectedSelectQuery = selectedSelectQuery;
	}

	public List<String> getResultColumnList(){
		return resultColumnList;
	}

	public boolean isShowResultSet() {
		return showResultSet;
	}

	public String getMessage() {
		return message;
	}

	public boolean isRenderMessage() {
		return renderMessage;
	}

	public Result getSelectedResult() {
		return selectedResult;
	}

	public String getErrorMessage() {
		return errorMessage;
	}

	public boolean isRenderErrorMessage() {
		return renderErrorMessage;
	}
	
	public boolean isRenderScriptNames() {
		return renderScriptNames;
	}
}