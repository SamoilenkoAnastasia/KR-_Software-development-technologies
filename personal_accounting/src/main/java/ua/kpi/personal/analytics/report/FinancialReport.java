package ua.kpi.personal.analytics.report;

import ua.kpi.personal.analytics.output.OutputRenderer;
import ua.kpi.personal.model.analytics.ReportDataPoint;
import ua.kpi.personal.model.analytics.ReportParams;
import ua.kpi.personal.model.User;
import java.util.List;

public abstract class FinancialReport {
    
    protected OutputRenderer renderer;

    public FinancialReport() {
    }

    public void setOutputRenderer(OutputRenderer renderer) {
        this.renderer = renderer;
    }

    protected abstract List<ReportDataPoint> analyze(ReportParams params, User user);

    
    protected abstract void render(List<ReportDataPoint> dataPoints);
    
    public final void generate(ReportParams params, User user) {
        if (renderer == null) {
            throw new IllegalStateException("OutputRenderer (Міст) не встановлено. Викличте setOutputRenderer().");
        }

        List<ReportDataPoint> dataPoints = analyze(params, user);
        render(dataPoints);
    }
}