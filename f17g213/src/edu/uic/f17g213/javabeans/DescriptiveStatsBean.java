package edu.uic.f17g213.javabeans;

public class DescriptiveStatsBean {
	private String columnSelected;
	private double minValue;
	private double maxValue;
	private double mean;
	private double variance;
	private double std;
	private double median;
	private double q1;
	private double q3;
	private double iqr;
	private double range;
	private String tableName;
	private int rowCount;

	public DescriptiveStatsBean(String columnSelected, double minValue, double maxValue, double mean,
			double variance, double std, double median, double q1, double q3, double iqr, double range, String tableName, int rowCount) {
		this.columnSelected = columnSelected;
		this.minValue = minValue;
		this.maxValue = maxValue;
		this.mean = mean;
		this.variance = variance;
		this.std = std;
		this.median = median;
		this.q1 = q1;
		this.q3 = q3;
		this.iqr = iqr;
		this.range = range;
		this.tableName = tableName;
		this.rowCount = rowCount;
	}

	public void setVariablesforGraph(double q1, double q3, double median) {
		this.q1 = q1;
		this.q3 = q3;
		this.median = median;

	}

	public String getColumnSelected() {
		return columnSelected;
	}

	public void setColumnSelected(String columnSelected) {
		this.columnSelected = columnSelected;
	}

	public double getMinValue() {
		return minValue;
	}

	public void setMinValue(double minValue) {
		this.minValue = minValue;
	}

	public double getMaxValue() {
		return maxValue;
	}

	public void setMaxValue(double maxValue) {
		this.maxValue = maxValue;
	}

	public double getMean() {
		return mean;
	}

	public void setMean(double mean) {
		this.mean = mean;
	}

	public double getVariance() {
		return variance;
	}

	public void setVariance(double variance) {
		this.variance = variance;
	}

	public double getStd() {
		return std;
	}

	public void setStd(double std) {
		this.std = std;
	}

	public double getMedian() {
		return median;
	}

	public void setMedian(double median) {
		this.median = median;
	}

	public double getQ1() {
		return q1;
	}

	public void setQ1(double q1) {
		this.q1 = q1;
	}

	public double getQ3() {
		return q3;
	}

	public void setQ3(double q3) {
		this.q3 = q3;
	}

	public double getIqr() {
		return iqr;
	}

	public void setIqr(double iqr) {
		this.iqr = iqr;
	}

	public double getRange() {
		return range;
	}

	public void setRange(double range) {
		this.range = range;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((columnSelected == null) ? 0 : columnSelected.hashCode());
		long temp;
		temp = Double.doubleToLongBits(iqr);
		result = prime * result + (int) (temp ^ (temp >>> 32));
		temp = Double.doubleToLongBits(maxValue);
		result = prime * result + (int) (temp ^ (temp >>> 32));
		temp = Double.doubleToLongBits(mean);
		result = prime * result + (int) (temp ^ (temp >>> 32));
		temp = Double.doubleToLongBits(median);
		result = prime * result + (int) (temp ^ (temp >>> 32));
		temp = Double.doubleToLongBits(minValue);
		result = prime * result + (int) (temp ^ (temp >>> 32));
		temp = Double.doubleToLongBits(q1);
		result = prime * result + (int) (temp ^ (temp >>> 32));
		temp = Double.doubleToLongBits(q3);
		result = prime * result + (int) (temp ^ (temp >>> 32));
		temp = Double.doubleToLongBits(range);
		result = prime * result + (int) (temp ^ (temp >>> 32));
		temp = Double.doubleToLongBits(std);
		result = prime * result + (int) (temp ^ (temp >>> 32));
		temp = Double.doubleToLongBits(variance);
		result = prime * result + (int) (temp ^ (temp >>> 32));
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		DescriptiveStatsBean other = (DescriptiveStatsBean) obj;
		if (columnSelected == null) {
			if (other.columnSelected != null)
				return false;
		} else if (!columnSelected.equals(other.columnSelected))
			return false;
		if (Double.doubleToLongBits(iqr) != Double.doubleToLongBits(other.iqr))
			return false;
		if (Double.doubleToLongBits(maxValue) != Double.doubleToLongBits(other.maxValue))
			return false;
		if (Double.doubleToLongBits(mean) != Double.doubleToLongBits(other.mean))
			return false;
		if (Double.doubleToLongBits(median) != Double.doubleToLongBits(other.median))
			return false;
		if (Double.doubleToLongBits(minValue) != Double.doubleToLongBits(other.minValue))
			return false;
		if (Double.doubleToLongBits(q1) != Double.doubleToLongBits(other.q1))
			return false;
		if (Double.doubleToLongBits(q3) != Double.doubleToLongBits(other.q3))
			return false;
		if (Double.doubleToLongBits(range) != Double.doubleToLongBits(other.range))
			return false;
		if (Double.doubleToLongBits(std) != Double.doubleToLongBits(other.std))
			return false;
		if (Double.doubleToLongBits(variance) != Double.doubleToLongBits(other.variance))
			return false;
		return true;
	}

	public String getTableName() {
		return tableName;
	}

	public void setTableName(String tableName) {
		this.tableName = tableName;
	}

	public int getRowCount() {
		return rowCount;
	}

	public void setRowCount(int rowCount) {
		this.rowCount = rowCount;
	}

	@Override
	public String toString() {
		return tableName + "," + rowCount + "," + columnSelected + "," + minValue + "," + maxValue + "," + mean + "," + variance + "," + std + ","
				+ median + "," + q1 + "," + q3 + "," + iqr + "," + range;
	}
}