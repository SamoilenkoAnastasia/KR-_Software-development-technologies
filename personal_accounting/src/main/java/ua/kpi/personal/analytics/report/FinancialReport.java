package ua.kpi.personal.analytics.report;

import ua.kpi.personal.analytics.output.OutputRenderer;
import ua.kpi.personal.model.analytics.ReportDataSet;
import ua.kpi.personal.model.analytics.ReportParams;
import ua.kpi.personal.repo.TransactionDao;
import ua.kpi.personal.model.User;

public abstract class FinancialReport {
    
    protected OutputRenderer renderer;
    protected final TransactionDao transactionDao;

    public FinancialReport(TransactionDao dao) {
        this.transactionDao = dao;
    }

    public void setOutputRenderer(OutputRenderer renderer) {
        this.renderer = renderer;
    }
    
    protected abstract void renderTable(ReportDataSet dataSet);
    protected abstract void renderChart(ReportDataSet dataSet);
    
    protected abstract ReportDataSet analyze(ReportParams params, User user);

   
    public final void generate(ReportParams params, User user) {
        if (renderer == null) {
            throw new IllegalStateException("OutputRenderer (Міст) не встановлено. Викличте setOutputRenderer().");
        }
        
        ReportDataSet dataSet = analyze(params, user);
        
        renderTable(dataSet);
        renderChart(dataSet);
    }
}