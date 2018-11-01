package edu.uic.f17g213.actions;

import java.awt.Color;
import java.io.File;
import java.io.IOException;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javax.annotation.PostConstruct;
import javax.faces.bean.ManagedBean;
import javax.faces.bean.SessionScoped;
import javax.faces.context.FacesContext;

import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartUtilities;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.renderer.xy.XYLineAndShapeRenderer;
import org.jfree.data.Range;
import org.jfree.data.function.LineFunction2D;
import org.jfree.data.general.DatasetUtilities;
import org.jfree.data.statistics.Regression;
import org.jfree.data.xy.XYDataset;
import org.jfree.util.ShapeUtilities;

@ManagedBean(name = "dataVisualizationBean")
@SessionScoped
public class DataVisAction {
	private String columnName;
	private String predictorValue;
	private String responseValue;

	private List<String> tableList;
	private List<String> columnList;

	private boolean renderChart;

	private DBAccess databaseBean;
	private StatsActions StatsActionsBean;
	private ResultSet resultSet;

	private List<String> categoricalData;
	private List<String> numericData;
	private List<String> columnsList;
	private List<String> schemaList;
	private String tableName;
	private String selectedSchema;

	private String chartPath;

	public DataVisAction() {
		StatsActionsBean = new StatsActions();
	}

	@PostConstruct
	public void init() {
		Map<String, Object> m = FacesContext.getCurrentInstance().getExternalContext().getSessionMap();
		databaseBean = (DBAccess) m.get("databaseBean");
		StatsActionsBean.setDB(databaseBean);
		getSchemas();
		getTables();
		generateRegressionColumns();
	}

	public String getSchemas() {
		try {
			schemaList = new ArrayList<String>();
			schemaList = databaseBean.getSchemaList();
			selectedSchema = databaseBean.getSelectedSchema();
			return "SUCCESS";
		} catch (Exception e) {
			return "FAIL";
		}
	}

	public String getTables() {
		renderChart = false;
		try {
			tableList = new ArrayList<String>();
			tableList = databaseBean.getTableList();
			if (tableList == null && tableList.isEmpty()) {
				databaseBean.listTables();
			}
			tableName = databaseBean.getTableName();
			return "SUCCESS";
		} catch (Exception e) {
			return "FAIL";
		}
	}

	public String generateRegressionColumns() {
		try {
			String sqlQuery = "select * from " + databaseBean.getLoginBean().getDbSchema() + "."
					+ databaseBean.getTableName();
			String status = databaseBean.dbExecute(databaseBean.getTableName(), sqlQuery);
			if (status.equalsIgnoreCase("SUCCESS")) {
				resultSet = databaseBean.getResultSet();
			}
			if (resultSet != null) {
				columnsList = new ArrayList<String>();
				categoricalData = new ArrayList<String>();
				numericData = new ArrayList<String>();
				resultSet.last();
				resultSet.beforeFirst();
				ResultSetMetaData resultSetMetaData = (ResultSetMetaData) resultSet.getMetaData();
				int columnCount = resultSetMetaData.getColumnCount();
				for (int i = 1; i <= columnCount; i++) {
					String name = resultSetMetaData.getColumnName(i);
					String datatype = resultSetMetaData.getColumnTypeName(i);
					if (datatype.equalsIgnoreCase("char") || datatype.equalsIgnoreCase("varchar")) {
						categoricalData.add(name);
					} else
						numericData.add(name);
				}
			} else {
				return "FAIL";
			}
			return "SUCCESS";
		} catch (Exception e) {
			return "FAIL";
		}
	}

	public String processGraphicQuery() {
		renderChart = false;
		String status = prepareAndGetRegressionData();
		if (status.equalsIgnoreCase("FAIL"))
			return "FAIL";
		try {
			JFreeChart chart = createScatterplotChart();
			FacesContext context = FacesContext.getCurrentInstance();
			String path = context.getExternalContext().getRealPath("/charts");
			chartPath = "/charts/" + databaseBean.getLoginBean().getUsername() + "_" + "scatter_plot.png";
			String filePath = path + "/" + databaseBean.getLoginBean().getUsername() + "_" + "scatter_plot.png";

			File out = null;
			out = new File(filePath);
			ChartUtilities.saveChartAsPNG(out, chart, 600, 450);
			renderChart = true;
			return "SUCCESS";
		} catch (IOException e) {
			e.printStackTrace();
			return "FAIL";
		} catch (Exception e) {
			e.printStackTrace();
			return "FAIL";
		}
	}

	public String prepareAndGetRegressionData() {
		try {
			StatsActionsBean.setPredictorValue(predictorValue);
			StatsActionsBean.setResponseValue(responseValue);
			StatsActionsBean.generateRegressionReport();
			return "SUCCESS";
		} catch (Exception ex) {
			ex.printStackTrace();
			return "FAIL";
		}
	}

	public double[] prepareAndGetNonRegressionData() {
		try {
			List<Double> valuesList = new ArrayList<Double>();
			String query = "Select " + columnName + " from " + databaseBean.getLoginBean().getDbSchema() + "."
					+ databaseBean.getTableName() + ";";
			databaseBean.dbExecute(databaseBean.getTableName(), query);

			ResultSet resultSet = databaseBean.getResultSet();
			ResultSetMetaData resultSetMetaData = (ResultSetMetaData) resultSet.getMetaData();
			int columnCount = resultSetMetaData.getColumnCount();

			for (int j = 1; j < columnCount + 1; j++) {
				String columnType = resultSetMetaData.getColumnTypeName(j);
				resultSet.beforeFirst();
				while (resultSet.next()) {
					switch (columnType.toLowerCase()) {
					case "int":
						valuesList.add((double) resultSet.getInt(columnName));
						break;
					case "smallint":
						valuesList.add((double) resultSet.getInt(columnName));
						break;
					case "float":
						valuesList.add((double) resultSet.getFloat(columnName));
						break;
					case "double":
						valuesList.add((double) resultSet.getDouble(columnName));
						break;
					case "long":
						valuesList.add((double) resultSet.getLong(columnName));
						break;
					default:
						valuesList.add((double) resultSet.getDouble(columnName));
						break;
					}
				}
			}
			double[] values = valuesList.stream().mapToDouble(d -> d).toArray();
			return values;
		} catch (Exception ex) {
			ex.printStackTrace();
			return null;
		}
	}

	public JFreeChart createScatterplotChart() {
		prepareAndGetRegressionData();
		JFreeChart chart = ChartFactory.createScatterPlot("Scatter Plot for " + databaseBean.getTableName(), // chart
				predictorValue,// x axis label
				responseValue, // y axis label
				StatsActionsBean.getXySeriesVariable(), // data
				PlotOrientation.VERTICAL, 
				true, // include legend
				true, // tooltip
				false // URL
		);
		int count = StatsActionsBean.getXySeriesVariable().getItemCount(0);
		double regressionParameters[] = Regression.getOLSRegression(StatsActionsBean.getXySeriesVariable(), 0);
		LineFunction2D linefunction2d = new LineFunction2D(regressionParameters[0], regressionParameters[1]);
		Range r = DatasetUtilities.findDomainBounds(StatsActionsBean.getXySeriesVariable());
		XYDataset dataset = DatasetUtilities.sampleFunction2D(linefunction2d, r.getLowerBound(), r.getUpperBound(), count, "Fitted Regression Line");
		XYPlot xyplot = chart.getXYPlot();
		xyplot.getRenderer().setSeriesShape(0, ShapeUtilities.createDiagonalCross(3, 1));
		xyplot.getRenderer().setSeriesPaint(0, Color.RED);
		xyplot.setDataset(1, dataset);
		XYLineAndShapeRenderer xylineandshaperenderer = new XYLineAndShapeRenderer(true, false);
		xylineandshaperenderer.setSeriesPaint(0, Color.GREEN);
		xyplot.setRenderer(1, xylineandshaperenderer);
		return chart;
	}

	public String resetButton() {
		renderChart = false;
		return "SUCCESS";
	}

	public String getChartPath() {
		return chartPath;
	}

	public void setChartPath(String chartPath) {
		this.chartPath = chartPath;
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

	public List<String> getColumnsList() {
		return columnsList;
	}

	public void setColumnsList(List<String> columnsList) {
		this.columnsList = columnsList;
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

	public boolean isRenderChart() {
		return renderChart;
	}

	public void setRenderChart(boolean renderChart) {
		this.renderChart = renderChart;
	}

	public String getColumnName() {
		return columnName;
	}

	public void setColumnName(String columnName) {
		this.columnName = columnName;
	}

	public List<String> getTableList() {
		return tableList;
	}

	public void setTableList(List<String> tableList) {
		this.tableList = tableList;
	}

	public List<String> getColumnList() {
		return columnList;
	}

	public void setColumnList(List<String> columnList) {
		this.columnList = columnList;
	}

	public List<String> getSchemaList() {
		return schemaList;
	}

	public void setSchemaList(List<String> schemaList) {
		this.schemaList = schemaList;
	}

	public String getTableName() {
		return tableName;
	}

	public void setTableName(String tableName) {
		this.tableName = tableName;
		databaseBean.setTableName(tableName);
		generateRegressionColumns();
	}

	public void setSelectedSchema(String selectedSchema) {
		this.selectedSchema = selectedSchema;
		databaseBean.setSelectedSchema(selectedSchema);
		getTables();
		generateRegressionColumns();
	}

	public String getSelectedSchema() {
		return selectedSchema;
	}
}
