package ua.kpi.personal.analytics.output;

import ua.kpi.personal.model.analytics.ReportDataPoint;
import java.util.List;

public interface OutputRenderer {

    /**
     * Уніфікований метод для рендерингу будь-якого звіту.
     * @param reportTitle Заголовок.
     * @param dataPoints Основні дані (універсальний DTO).
     * @param summary Підсумок.
     */
    void renderReport(String reportTitle, List<ReportDataPoint> dataPoints, String summary);
}