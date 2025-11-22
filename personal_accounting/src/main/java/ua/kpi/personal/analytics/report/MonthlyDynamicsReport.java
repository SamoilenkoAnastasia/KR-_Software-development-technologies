package ua.kpi.personal.analytics.report;

import ua.kpi.personal.model.User;
import ua.kpi.personal.model.analytics.ReportDataSet;
import ua.kpi.personal.model.analytics.ReportParams;
import ua.kpi.personal.repo.TransactionDao;
import ua.kpi.personal.model.analytics.ReportDataPoint; 


public class MonthlyDynamicsReport extends FinancialReport {

    public MonthlyDynamicsReport(TransactionDao dao) {
        super(dao);
    }

    @Override
    protected ReportDataSet analyze(ReportParams params, User user) {
        return new ReportDataSet("Динаміка по Місяцях", new String[]{"Місяць", "Сума", "Зміна"}, java.util.Collections.emptyList(), "Аналіз динаміки.");
    }

    @Override
    protected void renderTable(ReportDataSet dataSet) {

        renderer.render(dataSet);
    }

    @Override
    protected void renderChart(ReportDataSet dataSet) {
    
        renderer.renderChart(dataSet.getDataPoints());
    }
}