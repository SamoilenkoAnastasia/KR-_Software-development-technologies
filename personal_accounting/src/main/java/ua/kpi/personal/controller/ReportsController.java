package ua.kpi.personal.controller;

import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.AnchorPane;
import ua.kpi.personal.analytics.output.ExcelRenderer;
import ua.kpi.personal.analytics.output.JavaFxScreenRenderer;
import ua.kpi.personal.analytics.output.OutputRenderer;
import ua.kpi.personal.analytics.report.AllTransactionsReport;
import ua.kpi.personal.analytics.report.FinancialReport;
import ua.kpi.personal.model.analytics.ReportDataPoint;
import ua.kpi.personal.model.analytics.ReportParams;
import ua.kpi.personal.repo.TransactionDao;
import ua.kpi.personal.state.ApplicationSession;
import ua.kpi.personal.model.User;

import java.io.IOException;
import java.time.LocalDate;
import java.util.Collections;
import java.util.List;

import ua.kpi.personal.analytics.output.PdfFileWriter;
import ua.kpi.personal.analytics.report.MonthlyDynamicsReport;

public class ReportsController {

    // Примітка: Ініціалізація DAO та Session часто відбувається через конструктор або FXML Factory.
    // Якщо ви не використовуєте ControllerFactory, ці поля можуть бути ініціалізовані в initialize().

    private final TransactionDao transactionDao;
    private final ApplicationSession session;
    // MainController видалено з полів, оскільки навігація відбувається через ApplicationSession.
    // private final MainController mainController; 


    @FXML private ComboBox<String> reportTypeCombo;
    @FXML private DatePicker startDatePicker;
    @FXML private DatePicker endDatePicker;
    @FXML private Button generateButton;
    @FXML private Button exportExcelButton;
    @FXML private Button exportPdfButton;
    @FXML private Button backButton; // Потрібен обробник події onBack
    @FXML private TabPane visualizationTabPane;
    @FXML private Label summaryLabel;

    @FXML private TableView<ReportDataPoint> reportTableView;
    @FXML private AnchorPane chartContainer;

    private FinancialReport currentReportLogic;
    private JavaFxScreenRenderer screenRenderer;

    // Конструктор залишено, але вам слід переконатися, що FXML-завантажувач його використовує.
    public ReportsController(TransactionDao dao, ApplicationSession session, MainController mainController) {
        this.transactionDao = dao;
        this.session = session;
        // this.mainController = mainController; // Поле mainController тепер не потрібне
    }

    // Додаємо конструктор за замовчуванням для стандартного FXML-завантажувача
    public ReportsController() {
        this.transactionDao = new TransactionDao(); // Приклад ініціалізації за замовчуванням
        this.session = ApplicationSession.getInstance();
    }

    @FXML
    public void initialize() {
        
        reportTypeCombo.getItems().addAll("Загальний звіт доходів і витрат", "Динаміка по Місяцях");
        reportTypeCombo.getSelectionModel().selectFirst();
        startDatePicker.setValue(LocalDate.now().minusMonths(1));
        endDatePicker.setValue(LocalDate.now());

        generateButton.setOnAction(event -> generateReport());
        exportExcelButton.setOnAction(event -> exportReport("Excel"));
        exportPdfButton.setOnAction(event -> exportReport("PDF"));
        // Підключення обробника до кнопки "Назад"
        if (backButton != null) {
            backButton.setOnAction(event -> onBack());
        }
        
        screenRenderer = new JavaFxScreenRenderer(reportTableView, summaryLabel, chartContainer);

        generateReport();
    }
    
    /**
     * Обробник для кнопки "Назад". Повертає користувача на головний дашборд.
     */
    @FXML
    private void onBack() {
        // Використовуємо ApplicationSession для навігації назад на дашборд
        MainController mainController = ApplicationSession.getInstance().getController();
        if (mainController != null) {
            mainController.onDashboard();
        } else {
            System.err.println("Помилка навігації: MainController недоступний.");
        }
    }
    
   


    private void generateReport() {
        if (session.getCurrentUser() == null) return;

        ReportParams params = createReportParams();
        currentReportLogic = createReportLogic(reportTypeCombo.getValue());

        currentReportLogic.setOutputRenderer(screenRenderer);
        currentReportLogic.generate(params, session.getCurrentUser());

        summaryLabel.setText("Звіт готовий. Виберіть вкладку для перегляду.");
    }

   private void exportReport(String format) {
        if (currentReportLogic == null) {
            summaryLabel.setText("Спочатку побудуйте звіт.");
            return;
        }

        OutputRenderer fileWriter;
        var ownerWindow = generateButton.getScene().getWindow();

        String reportName = reportTypeCombo.getValue().replace(" ", "_");

        if ("Excel".equals(format)) {

            fileWriter = new ExcelRenderer(ownerWindow);
        } else if ("PDF".equals(format)) {

            fileWriter = new PdfFileWriter(ownerWindow);
        } else {
            return;
        }

        currentReportLogic.setOutputRenderer(fileWriter);
        currentReportLogic.generate(createReportParams(), session.getCurrentUser());

        summaryLabel.setText("Експорт у " + format + " завершено.");
    }

    private ReportParams createReportParams() {
        return new ReportParams(
            startDatePicker.getValue(),
            endDatePicker.getValue(),
            Collections.emptyList(),
            Collections.emptyList(),
            "ALL"
        );
    }

    private FinancialReport createReportLogic(String type) {
        switch (type) {
            case "Загальний звіт доходів і витрат":
                return new AllTransactionsReport(transactionDao);
            case "Динаміка по Місяцях":
                return new MonthlyDynamicsReport(transactionDao);
            default:
               
                throw new IllegalArgumentException("Невідомий тип звіту.");
        }
    }
}
