package ua.kpi.personal.model.analytics;

import java.util.List;

public class ReportDataSet {
    private final String title;
    private final String[] columnHeaders;
    private final List<ReportDataPoint> dataPoints;
    private final String summaryText;
    private List<?> chartData; 

    public ReportDataSet(String title, String[] columnHeaders, List<ReportDataPoint> dataPoints, String summaryText) {
        this.title = title;
        this.columnHeaders = columnHeaders;
        this.dataPoints = dataPoints;
        this.summaryText = summaryText;
    }

    
    public String getTitle() { return title; }
    public String[] getColumnHeaders() { return columnHeaders; }
    public List<ReportDataPoint> getDataPoints() { return dataPoints; }
    public String getSummaryText() { return summaryText; }
    public List<?> getChartData() {
        return chartData;
    }

    public void setChartData(List<?> chartData) {
        this.chartData = chartData;
    }
}