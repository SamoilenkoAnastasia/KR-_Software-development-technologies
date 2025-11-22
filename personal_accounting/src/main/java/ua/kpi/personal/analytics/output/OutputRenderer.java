package ua.kpi.personal.analytics.output;

import ua.kpi.personal.model.Transaction;
import ua.kpi.personal.model.analytics.ReportDataPoint;
import ua.kpi.personal.model.analytics.ReportDataSet;
import java.util.List;

public interface OutputRenderer {

    void render(ReportDataSet dataSet);

    void renderAllTransactionsTable(List<Transaction> transactions);

    void renderChart(List<? extends ReportDataPoint> chartData);
}