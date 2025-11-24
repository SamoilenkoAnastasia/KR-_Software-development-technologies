package ua.kpi.personal.analytics.output;

import ua.kpi.personal.model.analytics.ReportDataPoint;
import java.util.List;

public interface OutputRenderer {
    void renderReport(String reportTitle, List<ReportDataPoint> dataPoints, String summary);
}