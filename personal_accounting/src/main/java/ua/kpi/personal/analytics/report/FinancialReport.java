package ua.kpi.personal.analytics.report;

import ua.kpi.personal.analytics.output.OutputRenderer;
import ua.kpi.personal.model.analytics.ReportDataPoint; // Змінено з ReportDataSet
import ua.kpi.personal.model.analytics.ReportParams;
import ua.kpi.personal.repo.TransactionDao;
import ua.kpi.personal.model.User;
import java.util.List;

/**
 * Абстрактний базовий клас для всіх фінансових звітів (Шаблон Міст).
 */
public abstract class FinancialReport {
    
    protected OutputRenderer renderer;
    protected final TransactionDao transactionDao;

    // Конструктор для звітів, які використовують DAO напряму
    public FinancialReport(TransactionDao dao) {
        this.transactionDao = dao;
    }
    
    // Додано конструктор без DAO для звітів, що використовують ReportingService
    public FinancialReport() {
        this.transactionDao = null;
    }

    public void setOutputRenderer(OutputRenderer renderer) {
        this.renderer = renderer;
    }
    
    /**
     * @return Універсальний список точок даних для рендерингу.
     */
    protected abstract List<ReportDataPoint> analyze(ReportParams params, User user);

    /**
     * Абстрактний метод для рендерингу (викликає renderReport у OutputRenderer).
     */
    protected abstract void render(List<ReportDataPoint> dataPoints);
    
    public final void generate(ReportParams params, User user) {
        if (renderer == null) {
            throw new IllegalStateException("OutputRenderer (Міст) не встановлено. Викличте setOutputRenderer().");
        }
        
        // analyze повертає List<ReportDataPoint>
        List<ReportDataPoint> dataPoints = analyze(params, user);
        
        // render використовує List<ReportDataPoint> для виклику renderer.renderReport(...)
        render(dataPoints);
    }
}