package edu.uic.f17g213.actions;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;

import javax.annotation.PostConstruct;
import javax.faces.bean.ManagedBean;
import javax.faces.bean.SessionScoped;
import javax.faces.context.FacesContext;
import javax.servlet.jsp.jstl.sql.Result;

import org.apache.commons.math3.distribution.FDistribution;
import org.apache.commons.math3.distribution.TDistribution;
import org.apache.commons.math3.stat.StatUtils;
import org.apache.commons.math3.stat.regression.SimpleRegression;
import org.jfree.data.xy.XYSeries;
import org.jfree.data.xy.XYSeriesCollection;

import edu.uic.f17g213.javabeans.DescriptiveStatsBean;

@ManagedBean(name = "statsActionsBean")
@SessionScoped
public class StatsActions {
	private int columnCount;
	private int rowCount;
	private String message;
	private String predictorValue;
	private String responseValue;
	private boolean columnRender;
	private boolean renderMessage;
	private boolean renderReport;
	private boolean renderTabledata;
	private boolean renderRegressionResult;
	private boolean renderNumberOfObservations;
	private boolean renderNumberOfColumns;
	private List<String> numericData;
	private List<String> categoricalData;
	private List<String> columnSelected;
	private List<String> columnsList;
	private List<String> tableList;
	private List<String> columns;
	private List<String> list;
	private XYSeries xySeries;
	private XYSeriesCollection xySeriesVariables;
	private double quartile1;
	private double quartile3;
	private double median1;
	private List<DescriptiveStatsBean> descriptiveAnalysisBeanList;
	private Result result;
	private ResultSet resultSet;

	private DBAccess databaseBean;
	private List<String> schemaList;

	private ResultSetMetaData resultSetMetaData;
	private XYSeriesCollection xySeriesVariable;
	private XYSeriesCollection xyTimeSeriesCollection;
	private XYSeries predictorSeries;
	private XYSeries responseSeries;
	private String errorMessage;

	private double intercept;
	private double interceptStandardError;
	private double tStatistic;
	private double interceptPValue;
	private double slope;
	private double predictorDF;
	private double residualErrorDF;
	private double totalDF;
	private double regressionSumSquares;
	private double sumSquaredErrors;
	private double totalSumSquares;
	private double meanSquare;
	private double meanSquareError;
	private double fValue;
	private double pValue;
	private double slopeStandardError;
	private double tStatisticPredictor;
	private double pValuePredictor;
	private double standardErrorModel;
	private double rSquare;
	private double rSquareAdjusted;
	private String tableName;
	private String selectedSchema;

	public StatsActions() {
		columnSelected = new ArrayList<String>();
		columnsList = new ArrayList<String>();
		columns = new ArrayList<String>();
		renderTabledata = false;
		descriptiveAnalysisBeanList = new ArrayList<DescriptiveStatsBean>();
		categoricalData = new ArrayList<String>();
		numericData = new ArrayList<String>();
		renderReport = false;
		tableList = new ArrayList<String>();
		list = new ArrayList<String>();
		xySeries = new XYSeries("Random");
		xySeriesVariable = new XYSeriesCollection();
		xyTimeSeriesCollection = new XYSeriesCollection();
		predictorSeries = new XYSeries("Predictor");
		responseSeries = new XYSeries("Response");
	}

	@PostConstruct
	public void init() {
		Map<String, Object> m = FacesContext.getCurrentInstance().getExternalContext().getSessionMap();
		databaseBean = (DBAccess) m.get("databaseBean");
		getSchemas();
		getTables();
		getColumnNames();
		displayColumnsforRegression();
	}
	
	public String getSchemas() {
		try {
			reset();
			schemaList = new ArrayList<String>();
			schemaList = databaseBean.getSchemaList();
			selectedSchema = databaseBean.getSelectedSchema();
			return "SUCCESS";
		} catch (Exception e) {
			message = e.getMessage();
			renderMessage = true;
			return "FAIL";
		}
	}


	public String getTables() {
		try {
			reset();
			tableList = new ArrayList<String>();
			tableList = databaseBean.getTableList();
			tableName = databaseBean.getTableName();
			return "SUCCESS";
		} catch (Exception e) {
			message = e.getMessage();
			renderMessage = true;
			return "FAIL";
		}
	}
	
	public void setDB(DBAccess dbBean) {
		if(databaseBean == null) {
			this.databaseBean = dbBean;
		}
	}

	public String getRegressionColumnNames() {
		reset();
		if (tableList.isEmpty()) {
			message = "No tables found in the schema.";
			renderMessage = true;
			return "FAIL";
		}
		if (databaseBean.getTableName().isEmpty()) {
			message = "Please select a table.";
			renderMessage = true;
			return "FAIL";
		}
		if (generateRegressionColumns()) {
			return "SUCCESS";
		} else {
			renderMessage = true;
			return "FAIL";
		}
	}

	public boolean generateRegressionColumns() {
		try {
			String sqlQuery = "select * from " + databaseBean.getLoginBean().getDbSchema() + "." + databaseBean.getTableName();
			String status = databaseBean.dbExecute(databaseBean.getTableName(), sqlQuery);
			if (status.equalsIgnoreCase("SUCCESS")) {
				resultSet = databaseBean.getResultSet();
			}
			if (resultSet != null) {
				categoricalData.clear();
				numericData.clear();

				resultSet.last();
				rowCount = resultSet.getRow();
				resultSet.beforeFirst();
				ResultSetMetaData resultSetMetaData = (ResultSetMetaData) resultSet.getMetaData();
				columnCount = resultSetMetaData.getColumnCount();
				for (int i = 1; i <= columnCount; i++) {
					String name = resultSetMetaData.getColumnName(i);
					String datatype = resultSetMetaData.getColumnTypeName(i);
					if (datatype.equalsIgnoreCase("char") || datatype.equalsIgnoreCase("varchar")) {
						categoricalData.add(name);
					} else
						numericData.add(name);
				}
				columnRender = true;
			} else {

				renderNumberOfObservations = false;
				renderNumberOfColumns = false;
				return false;
			}
			return true;
		} catch (Exception e) {
			message = "Error generating Regression Columns";
			renderMessage = true;
			renderNumberOfObservations = false;
			renderNumberOfColumns = false;
			return false;
		}
	}

	public String splitColumns() {
		try {
			reset();
			if (databaseBean.getTableName() != null && columnSelected != null) {
				List<String> columnSeperated = new ArrayList<String>();
				for (int i = 0; i < columnSelected.size(); i++) {
					String data = columnSelected.get(i);
					int index = data.indexOf(" ");
					String column = data.substring(0, index);
					String datatype = data.substring((index + 1), data.length());
					if (datatype.equalsIgnoreCase("CHAR") || datatype.equalsIgnoreCase("VARCHAR")) {
						message = "Categorical variables are not allowed";
						return "FAIL";
					} else {
						columnSeperated.add(column);
					}
				}
				columnSelected = new ArrayList<String>();
				columnSelected = columnSeperated;
				list.clear();
				list = columnSelected;
				columnSeperated = null;
				return "SUCCESS";
			} else {
				message = "Please select a table and a column.";
				return "FAIL";
			}
		} catch (Exception e) {
			message = e.getMessage();
			renderMessage = true;
			return "FAIL";
		}
	}

	public String generateDescriptiveStatistics() {
		reset();
		if (tableList.isEmpty()) {
			message = "No tables found in the schema.";
			renderMessage = true;
			return "FAIL";
		}
		if (databaseBean.getTableName().isEmpty()) {
			message = "Please select a table and a column.";
			renderMessage = true;
			return "FAIL";
		}
		if (columnSelected.isEmpty()) {
			message = "Please select a Column";
			renderMessage = true;
			return "FAIL";
		} else {
			if (calculateDescriptiveVariables().equals("FAIL")) {
				renderMessage = true;
				return "FAIL";
			} else {
				return "SUCCESS";
			}
		}
	}

	public String calculateDescriptiveVariables() {
		try {
			for (int k = 0; k < columnSelected.size(); k++) {
				String sqlQuery = "select " + columnSelected.get(k) + " from " + databaseBean.getLoginBean().getDbSchema() + "."
						+ databaseBean.getTableName();
				String status = databaseBean.dbExecute(databaseBean.getTableName(), sqlQuery);
				if (status.equalsIgnoreCase("Success")) {

					resultSet = databaseBean.getResultSet();
					resultSetMetaData = (ResultSetMetaData) resultSet.getMetaData();
					columnCount = resultSetMetaData.getColumnCount();
					renderNumberOfColumns = true;
					resultSet.last();
					rowCount = resultSet.getRow();
					resultSet.beforeFirst();

					renderNumberOfObservations = true;
					String columnName;
					for (int j = 1; j < columnCount + 1; j++) {
						List<Double> values = new ArrayList<Double>();
						columnName = resultSetMetaData.getColumnName(j);
						String columnType = resultSetMetaData.getColumnTypeName(j);
						resultSet.beforeFirst();
						while (resultSet.next()) {
							try {
								values.add((double) resultSet.getInt(columnName));
							} catch (Exception e) {
								renderMessage = true;
								message = "Cannot Process Analysis on " + columnName + " of type " + columnType + " \n Entry should be of type Number";
		                        continue;
							}
						}
						double[] valuesArray = new double[values.size()];
						for (int i = 0; i < values.size(); i++) {
							valuesArray[i] = (double) values.get(i);
						}

						double minValue = StatUtils.min(valuesArray);
						double maxValue = StatUtils.max(valuesArray);
						double mean = StatUtils.mean(valuesArray);
						double variance = StatUtils.variance(valuesArray, mean);
						double std = Math.sqrt(variance);
						double median = StatUtils.percentile(valuesArray, 50.0);
						double q1 = StatUtils.percentile(valuesArray, 25.0);
						double q3 = StatUtils.percentile(valuesArray, 75.0);
						double iqr = q3 - q1;
						double range = maxValue - minValue;
						DescriptiveStatsBean dsb = new DescriptiveStatsBean(columnName, minValue, maxValue,
								mean, variance, std, median, q1, q3, iqr, range, databaseBean.getTableName(), rowCount);
						if (!descriptiveAnalysisBeanList.contains(dsb))
							descriptiveAnalysisBeanList.add(dsb);
					}
					renderTabledata = true;
				}
			}
			return "SUCCESS";
		} catch (Exception e) {
			message = "Error generating Descriptive Analysis";
			renderMessage = true;
			renderNumberOfObservations = false;
			renderNumberOfColumns = false;
			return "FAIL";
		}
	}

	public String getColumnNames() {
		try {
			reset();
			if (tableList.isEmpty()) {
				message = "No tables found in the Schema";
				renderMessage = true;
				return "FAIL";
			}
			if (databaseBean.getTableName().isEmpty()) {
				message = "Please select a Table";
				renderMessage = true;
				return "FAIL";
			} else {
				columnsList.clear();
				columnsList = databaseBean.getColumnList();
				columnSelected = databaseBean.getColumnNamesSelected();
			}
			return "SUCCESS";
		} catch (Exception e) {
			message = e.getMessage();
			renderMessage = true;
			return "FAIL";
		}
	}

	public String displayColumnsforRegression() {
		reset();
		if (tableList.isEmpty()) {
			message = "No tables found in the schema.";
			renderMessage = true;
			renderReport = true;
			return "FAIL";
		}
		if (databaseBean.getTableName() == null) {
			message = "Please select a table.";
			renderMessage = true;
			renderReport = true;
			return "FAIL";
		}
		String status = getRegressionColumnNames();
		if (status.equalsIgnoreCase("SUCCESS")) {
			renderReport = true;
			return "SUCCESS";
		} else {
			renderMessage = true;
			return "FAIL";
		}
	}

	public String generateRegressionReport() {
		reset();
		message = "";
		getTables();
		if (tableList.isEmpty()) {
			message = "No Tables found in the Schema";
			renderMessage = true;
			renderReport = true;
			return "FAIL";
		}
		if (databaseBean.getTableName() == null) {
			message += " Please select a Table";
			renderMessage = true;
			return "FAIL";
		}
		if ((predictorValue == null || predictorValue.equals(""))) {
			message += " Please select a Predictor Variable";
			renderMessage = true;
			return "FAIL";
		}
		if ((responseValue == null || responseValue.equals(""))) {
			message += " Please select a Response Variable";
			renderMessage = true;
			return "FAIL";
		}

		if ((responseValue.equals("0") && predictorValue.equals("0"))
				|| (responseValue == null && predictorValue == null)
				|| (responseValue.equals("0") && predictorValue == null)
				|| (responseValue == null && predictorValue.equals("0"))) {
			message = "Please select a Predictor and a Response Variable";
			renderMessage = true;
			return "FAIL";
		}
		if (calculateRegressionVariables()) {
			return "SUCCESS";
		} else
			return "FAIL";
	}

	public boolean calculateRegressionVariables() {
		try {
			responseSeries.clear();
			predictorSeries.clear();
			xySeries.clear();
			xySeriesVariable.removeAllSeries();
			String sqlQuery = "select " + predictorValue + ", " + responseValue + " from "
					+ databaseBean.getLoginBean().getDbSchema() + "." + databaseBean.getTableName();
			String status = databaseBean.dbExecute(databaseBean.getTableName(), sqlQuery);
			if (status.equalsIgnoreCase("SUCCESS")) {
				resultSet = databaseBean.getResultSet();
				if (resultSet != null) {
					resultSetMetaData = (ResultSetMetaData) resultSet.getMetaData();
					String predictorName = resultSetMetaData.getColumnTypeName(1);
					String responseName = resultSetMetaData.getColumnTypeName(2);
					List<Double> predictorList = new ArrayList<Double>();
					List<Double> responseList = new ArrayList<Double>();
					resultSet.beforeFirst();
					while (resultSet.next()) {
						switch (predictorName.toLowerCase()) {
						case "int":
							predictorList.add((double) resultSet.getInt(1));
							break;
						case "smallint":
							predictorList.add((double) resultSet.getInt(1));
							break;
						case "float":
							predictorList.add((double) resultSet.getFloat(1));
							break;
						case "double":
							predictorList.add((double) resultSet.getDouble(1));
							break;
						case "long":
							predictorList.add((double) resultSet.getLong(1));
							break;
						default:
							predictorList.add((double) resultSet.getDouble(1));
							break;
						}
						switch (responseName.toLowerCase()) {
						case "int":
							responseList.add((double) resultSet.getInt(2));
							break;
						case "smallint":
							responseList.add((double) resultSet.getInt(2));
							break;
						case "float":
							responseList.add((double) resultSet.getFloat(2));
							break;
						case "double":
							responseList.add((double) resultSet.getDouble(2));
							break;
						case "long":
							responseList.add((double) resultSet.getLong(2));
							break;
						default:
							responseList.add((double) resultSet.getDouble(2));
							break;
						}
					}
					double[] predictorArray = new double[predictorList.size()];
					for (int i = 0; i < predictorList.size(); i++) {
						predictorArray[i] = (double) predictorList.get(i);
						predictorSeries.add(i + 1, (double) predictorList.get(i));
					}
					double[] responseArray = new double[responseList.size()];
					for (int i = 0; i < responseList.size(); i++) {
						responseArray[i] = (double) responseList.get(i);
						responseSeries.add(i + 1, (double) responseList.get(i));
					}
					SimpleRegression sr = new SimpleRegression();
					if (responseArray.length >= predictorArray.length) {
						for (int i = 0; i < predictorArray.length; i++) {
							sr.addData(responseArray[i], predictorArray[i]);
							xySeries.add(predictorArray[i], responseArray[i]);
						}
					} else {
						for (int i = 0; i < responseArray.length; i++) {
							sr.addData(predictorArray[i], responseArray[i]);
							xySeries.add(predictorArray[i], responseArray[i]);
						}
					}
					xySeriesVariable.addSeries(xySeries);
					totalDF = responseArray.length - 1;
					TDistribution tDistribution = new TDistribution(totalDF);
					intercept = sr.getIntercept();
					interceptStandardError = sr.getInterceptStdErr();
					tStatistic = 0;
					predictorDF = 1;
					residualErrorDF = totalDF - predictorDF;
					rSquare = sr.getRSquare();
					rSquareAdjusted = rSquare - (1 - rSquare) / (totalDF - predictorDF - 1);
					if (interceptStandardError != 0) {
						tStatistic = (double) intercept / interceptStandardError;
					} else
						tStatistic = Double.NaN;
					interceptPValue = (double) 2 * tDistribution.cumulativeProbability(-Math.abs(tStatistic));
					slope = sr.getSlope();
					slopeStandardError = sr.getSlopeStdErr();
					double tStatisticpredict = 0;
					if (slopeStandardError != 0) {
						tStatisticpredict = (double) slope / slopeStandardError;
					}
					pValuePredictor = (double) 2 * tDistribution.cumulativeProbability(-Math.abs(tStatisticpredict));
					standardErrorModel = Math.sqrt(StatUtils.variance(responseArray))
							* (Math.sqrt(1 - rSquareAdjusted));
					regressionSumSquares = sr.getRegressionSumSquares();
					sumSquaredErrors = sr.getSumSquaredErrors();
					totalSumSquares = sr.getTotalSumSquares();
					meanSquare = 0;
					if (predictorDF != 0) {
						meanSquare = regressionSumSquares / predictorDF;
					}
					meanSquareError = 0;
					if (residualErrorDF != 0) {
						meanSquareError = (double) (sumSquaredErrors / residualErrorDF);
					}
					fValue = 0;
					if (meanSquareError != 0) {
						fValue = meanSquare / meanSquareError;
						
					}
					if(slope > 0) {
						regressionEquation = predictorValue + " = " + intercept + " + (" + slope + ") " + responseValue;
					} else {
						regressionEquation = predictorValue + " = " + intercept + " + (" + slope + ") " + responseValue;
					}
					
					FDistribution fDistribution = new FDistribution(predictorDF, residualErrorDF);
					pValue = (double) (1 - fDistribution.cumulativeProbability(fValue));
					renderRegressionResult = true;
					renderNumberOfColumns = true;
					renderNumberOfObservations = true;
					return true;
				} else {
					message = "Result Set Null";
					renderMessage = true;
					return false;
				}
			}
			return false;

		} catch (Exception e) {
			message = "Failure generating Regression";
			renderMessage = true;
			return false;
		}
	}
	
	public String processFileDownload() 
	{
		FacesContext fc = FacesContext.getCurrentInstance();
		FileOutputStream fos = null;
		String path = fc.getExternalContext().getRealPath("/tmp");
		String fileName = path + "/" + "F17G213_" + "descriptiveStats " + getCurrentTimeStamp() + ".csv";
		File f = new File(fileName);
		StringBuffer sb = new StringBuffer();
		String s = "tableName,rowCount,columnSelected,minValue,maxValue,mean,variance,std,median,q1,q3,iqr,range";

		try {
			fos = new FileOutputStream(fileName);
			sb.append(s);
			sb.append("\n");
			fos.write(sb.toString().getBytes());
			for (int i = 0; i < descriptiveAnalysisBeanList.size(); i++) {
				DescriptiveStatsBean dsBean = descriptiveAnalysisBeanList.get(i);
				String rowValue = dsBean.toString();
				StringBuilder builder = new StringBuilder();
				builder.append(rowValue);
				builder.append("\n");
				fos.write(builder.toString().getBytes());
			}

			fos.flush();
			fos.close();
			message = "Exported as CSV! " + "\n";
			renderMessage = true;
			String mimeType = fc.getExternalContext().getMimeType(fileName);

			FileInputStream in = null;
			byte b;
			fc.getExternalContext().responseReset();
			fc.getExternalContext().setResponseContentType(mimeType);
			fc.getExternalContext().setResponseContentLength((int) f.length());
			fc.getExternalContext().setResponseHeader("Content-Disposition",
					"attachment; filename=\"" + "F17G213_" + "descriptiveStats " + getCurrentTimeStamp() +"\\.csv"+ "\"");
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
				renderMessage = true;
				return "FAIL";
			} finally {
				try {
					in.close();
				} catch (Exception e) {
					message = "Error Exporting File";
					renderMessage = true;
					return "FAIL";
				}
			}
			fc.responseComplete();
			return "SUCCESS";
		} catch (Exception e) {
			message = e.getMessage();
			renderMessage = true;
			e.printStackTrace();
			return "FAIL";
		}
	}


	public boolean onChartTypeChange() {
		if (getTables().equals("SUCCESS")) {
			return true;
		} else {
			errorMessage = message;
			return false;
		}
	}

	public boolean generateResultsforGraph() {
		if (calculateDescriptiveVariables().equals("SUCCESS")) {
			renderTabledata = false;
			return true;
		}
		renderTabledata = false;
		errorMessage = message;
		return false;
	}

	public boolean onTableChange() {
		if (generateRegressionColumns()) {
			return true;
		} else {
			errorMessage = message;
			return false;
		}
	}

	public boolean generateRegressionResults() {
		xySeries.clear();
		xySeriesVariable.removeAllSeries();
		if (calculateRegressionVariables()) {
			renderRegressionResult = false;
			renderNumberOfColumns = false;
			renderNumberOfObservations = false;
			return true;
		} else {
			errorMessage = message;
			return false;
		}
	}

	public String resetButton() {
		if(descriptiveAnalysisBeanList != null)
			descriptiveAnalysisBeanList.clear();
		columnRender = false;
		renderTabledata = false;
		renderRegressionResult = false;
		renderNumberOfObservations = false;
		renderNumberOfColumns = false;
		renderReport = false;
		renderMessage = false;
		if(columnSelected != null)
			columnSelected.clear();
		return "SUCCESS";
	}

	public void reset() {
		renderMessage = false;
		renderTabledata = false;
		renderRegressionResult = false;
		renderNumberOfObservations = false;
		renderNumberOfColumns = false;
	}

	public List<String> getColumnSelected() {
		return columnSelected;
	}

	public void setColumnSelected(List<String> columnSelected) {
		this.columnSelected = columnSelected;
	}

	public List<String> getColumnsList() {
		return columnsList;
	}

	public void setColumnsList(List<String> columnsList) {
		this.columnsList = columnsList;
	}

	public boolean isColumnRender() {
		return columnRender;
	}

	public void setColumnRender(boolean columnRender) {
		this.columnRender = columnRender;
	}

	public ResultSet getResultSet() {
		return resultSet;
	}

	public void setResultSet(ResultSet resultSet) {
		this.resultSet = resultSet;
	}

	public ResultSetMetaData getResultSetMetaData() {
		return resultSetMetaData;
	}

	public List<DescriptiveStatsBean> getDescriptiveAnalysisBeanList() {
		return descriptiveAnalysisBeanList;
	}

	public void setDescriptiveAnalysisBeanList(List<DescriptiveStatsBean> descriptiveAnalysisBeanList) {
		this.descriptiveAnalysisBeanList = descriptiveAnalysisBeanList;
	}

	public void setResultSetMetaData(ResultSetMetaData resultSetMetaData) {
		this.resultSetMetaData = resultSetMetaData;
	}

	public Result getResult() {
		return result;
	}

	public void setResult(Result result) {
		this.result = result;
	}

	public List<String> getTableList() {
		return tableList;
	}

	public void setTableList(List<String> tableList) {
		this.tableList = tableList;
	}

	public String getMessage() {
		return message;
	}

	public void setMessage(String message) {
		this.message = message;
	}

	public boolean isRenderMessage() {
		return renderMessage;
	}

	public void setRenderMessage(boolean renderMessage) {
		this.renderMessage = renderMessage;
	}

	public List<String> getColumns() {
		return columns;
	}

	public void setColumns(List<String> columns) {
		this.columns = columns;
	}

	public int getColumnCount() {
		return columnCount;
	}

	public void setColumnCount(int columnCount) {
		this.columnCount = columnCount;
	}

	public int getRowCount() {
		return rowCount;
	}

	public void setRowCount(int rowCount) {
		this.rowCount = rowCount;
	}

	public boolean isRenderTabledata() {
		return renderTabledata;
	}

	public void setRenderTabledata(boolean renderTabledata) {
		this.renderTabledata = renderTabledata;
	}

	public List<String> getCategoricalData() {
		return categoricalData;
	}

	public void setCategoricalData(List<String> categoricalData) {
		this.categoricalData = categoricalData;
	}

	public List<String> getNumericData() {
		return numericData;
	}

	public void setNumericData(List<String> numericData) {
		this.numericData = numericData;
	}

	public String getPredictorValue() {
		return predictorValue;
	}

	public void setPredictorValue(String predictorValue) {
		this.predictorValue = predictorValue;
	}

	public String getResponseValue() {
		return responseValue;
	}

	public void setResponseValue(String responseValue) {
		this.responseValue = responseValue;
	}

	public boolean isRenderReport() {
		return renderReport;
	}

	public void setRenderReport(boolean renderReport) {
		this.renderReport = renderReport;
	}

	public boolean isRenderRegressionResult() {
		return renderRegressionResult;
	}

	public void setRenderRegressionResult(boolean renderRegressionResult) {
		this.renderRegressionResult = renderRegressionResult;
	}

	public boolean isRenderNumberOfObservations() {
		return renderNumberOfObservations;
	}

	public void setRenderNumberOfObservations(boolean renderNumberOfObservations) {
		this.renderNumberOfObservations = renderNumberOfObservations;
	}

	public boolean isRenderNumberOfColumns() {
		return renderNumberOfColumns;
	}

	public void setRenderNumberOfColumns(boolean renderNumberOfColumns) {
		this.renderNumberOfColumns = renderNumberOfColumns;
	}

	public double getMedian1() {
		return median1;
	}

	public void setMedian1(double median1) {
		this.median1 = median1;
	}

	public double getQuartile1() {
		return quartile1;
	}

	public void setQuartile1(double quartile1) {
		this.quartile1 = quartile1;
	}

	public double getQuartile3() {
		return quartile3;
	}

	public void setQuartile3(double quartile3) {
		this.quartile3 = quartile3;
	}

	public XYSeriesCollection getXySeriesVariable() {
		return xySeriesVariable;
	}

	public void setXySeriesVariable(XYSeriesCollection xySeriesVariable) {
		this.xySeriesVariable = xySeriesVariable;
	}

	public List<String> getList() {
		return list;
	}

	public void setList(List<String> list) {
		this.list = list;
	}

	public String getErrorMessage() {
		return errorMessage;
	}

	public void setErrorMessage(String errorMessage) {
		this.errorMessage = errorMessage;
	}

	public XYSeriesCollection getXyTimeSeriesCollection() {
		return xyTimeSeriesCollection;
	}

	public void setXyTimeSeriesCollection(XYSeriesCollection xyTimeSeriesCollection) {
		this.xyTimeSeriesCollection = xyTimeSeriesCollection;
	}

	public XYSeriesCollection getXySeriesVariables() {
		return xySeriesVariables;
	}

	public void setXySeriesVariables(XYSeriesCollection xySeriesVariables) {
		this.xySeriesVariables = xySeriesVariables;
	}

	private String regressionEquation;

	public XYSeries getPredictorSeries() {
		return predictorSeries;
	}

	public void setPredictorSeries(XYSeries predictorSeries) {
		this.predictorSeries = predictorSeries;
	}

	public XYSeries getResponseSeries() {
		return responseSeries;
	}

	public void setResponseSeries(XYSeries responseSeries) {
		this.responseSeries = responseSeries;
	}

	public String getRegressionEquation() {
		return regressionEquation;
	}

	public void setRegressionEquation(String regressionEquation) {
		this.regressionEquation = regressionEquation;
	}

	public double getIntercept() {
		return intercept;
	}

	public void setIntercept(double intercept) {
		this.intercept = intercept;
	}

	public double getInterceptStandardError() {
		return interceptStandardError;
	}

	public void setInterceptStandardError(double interceptStandardError) {
		this.interceptStandardError = interceptStandardError;
	}

	public double gettStatistic() {
		return tStatistic;
	}

	public void settStatistic(double tStatistic) {
		this.tStatistic = tStatistic;
	}

	public double getInterceptPValue() {
		return interceptPValue;
	}

	public void setInterceptPValue(double interceptPValue) {
		this.interceptPValue = interceptPValue;
	}

	public double getSlope() {
		return slope;
	}

	public void setSlope(double slope) {
		this.slope = slope;
	}

	public double getPredictorDF() {
		return predictorDF;
	}

	public void setPredictorDF(double predictorDF) {
		this.predictorDF = predictorDF;
	}

	public double getResidualErrorDF() {
		return residualErrorDF;
	}

	public void setResidualErrorDF(double residualErrorDF) {
		this.residualErrorDF = residualErrorDF;
	}

	public double getTotalDF() {
		return totalDF;
	}

	public void setTotalDF(double totalDF) {
		this.totalDF = totalDF;
	}

	public double getRegressionSumSquares() {
		return regressionSumSquares;
	}

	public void setRegressionSumSquares(double regressionSumSquares) {
		this.regressionSumSquares = regressionSumSquares;
	}

	public double getSumSquaredErrors() {
		return sumSquaredErrors;
	}

	public void setSumSquaredErrors(double sumSquaredErrors) {
		this.sumSquaredErrors = sumSquaredErrors;
	}

	public double getTotalSumSquares() {
		return totalSumSquares;
	}

	public void setTotalSumSquares(double totalSumSquares) {
		this.totalSumSquares = totalSumSquares;
	}

	public double getMeanSquare() {
		return meanSquare;
	}

	public void setMeanSquare(double meanSquare) {
		this.meanSquare = meanSquare;
	}

	public double getMeanSquareError() {
		return meanSquareError;
	}

	public void setMeanSquareError(double meanSquareError) {
		this.meanSquareError = meanSquareError;
	}

	public double getfValue() {
		return fValue;
	}

	public void setfValue(double fValue) {
		this.fValue = fValue;
	}

	public double getpValue() {
		return pValue;
	}

	public void setpValue(double pValue) {
		this.pValue = pValue;
	}

	public double getSlopeStandardError() {
		return slopeStandardError;
	}

	public void setSlopeStandardError(double slopeStandardError) {
		this.slopeStandardError = slopeStandardError;
	}

	public double gettStatisticPredictor() {
		return tStatisticPredictor;
	}

	public void settStatisticPredictor(double tStatisticPredictor) {
		this.tStatisticPredictor = tStatisticPredictor;
	}

	public double getpValuePredictor() {
		return pValuePredictor;
	}

	public void setpValuePredictor(double pValuePredictor) {
		this.pValuePredictor = pValuePredictor;
	}

	public double getStandardErrorModel() {
		return standardErrorModel;
	}

	public void setStandardErrorModel(double standardErrorModel) {
		this.standardErrorModel = standardErrorModel;
	}

	public double getrSquare() {
		return rSquare;
	}

	public void setrSquare(double rSquare) {
		this.rSquare = rSquare;
	}

	public double getrSquareAdjusted() {
		return rSquareAdjusted;
	}

	public void setrSquareAdjusted(double rSquareAdjusted) {
		this.rSquareAdjusted = rSquareAdjusted;
	}

	public List<String> getSchemaList() {
		return schemaList;
	}

	public String getTableName() {
		return tableName;
	}

	public void setTableName(String tableName) {
		this.tableName = tableName;
		databaseBean.setTableName(tableName);
		getColumnNames();
		displayColumnsforRegression();
	}
	
	public void setSelectedSchema(String selectedSchema) {
		this.selectedSchema = selectedSchema;
		databaseBean.setSelectedSchema(selectedSchema);
		getTables();
		getColumnNames();
		displayColumnsforRegression();
	}
	
	public String getSelectedSchema() {
		return selectedSchema;
	}
	
	public String getCurrentTimeStamp() {
		return new SimpleDateFormat("ddMMyy").format(new Date());
	}
}