package ua.kpi.personal.controller;

import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.layout.AnchorPane;
import javafx.collections.FXCollections;
import ua.kpi.personal.analytics.output.ExcelRenderer;
import ua.kpi.personal.analytics.output.JavaFxScreenRenderer;
import ua.kpi.personal.analytics.output.OutputRenderer;
import ua.kpi.personal.analytics.output.PdfFileWriter;
import ua.kpi.personal.analytics.report.CategoryReport;
import ua.kpi.personal.analytics.report.FinancialReport;
import ua.kpi.personal.analytics.report.MonthlyDynamicsReport;
import ua.kpi.personal.analytics.report.AllTransactionsReport;
import ua.kpi.personal.model.analytics.ReportDataPoint;
import ua.kpi.personal.model.analytics.ReportParams;
import ua.kpi.personal.repo.AccountDao;
import ua.kpi.personal.repo.GoalDao;
import ua.kpi.personal.repo.TransactionDao;
import ua.kpi.personal.service.ReportingService;
import ua.kpi.personal.state.ApplicationSession;

import java.net.URL;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.ResourceBundle;

public class ReportsController implements Initializable {

    private FinancialReport currentReportLogic; 

    private final TransactionDao transactionDao;
    private final ReportingService reportingService;
    private final ApplicationSession session;
    
    private final List<String> REPORT_TYPES = Arrays.asList(
        "Загальний звіт по транзакціях",
        "Звіт за категоріями (Витрати)",
        "Динаміка по Місяцях"
    );

    @FXML private ComboBox<String> reportTypeCombo;
    @FXML private DatePicker startDatePicker;
    @FXML private DatePicker endDatePicker;
    @FXML private Button generateButton;
    @FXML private Button exportExcelButton;
    @FXML private Button exportPdfButton;
    @FXML private TabPane visualizationTabPane;
    @FXML private Label summaryLabel;

    @FXML private TableView<ReportDataPoint> reportTableView;
    @FXML private AnchorPane chartContainer;

    private JavaFxScreenRenderer screenRenderer; 

    // Конструктор
    public ReportsController() {
        this.session = ApplicationSession.getInstance();
        this.transactionDao = new TransactionDao();

        AccountDao accountDao = new AccountDao();
        GoalDao goalDao = new GoalDao();
        this.reportingService = new ReportingService(accountDao, goalDao, this.transactionDao);
    }
    
    @Override
    public void initialize(URL location, ResourceBundle resources) {
        
        screenRenderer = new JavaFxScreenRenderer(reportTableView, summaryLabel, chartContainer); 
        
        reportTypeCombo.setItems(FXCollections.observableArrayList(REPORT_TYPES));
        reportTypeCombo.getSelectionModel().selectFirst();
        
        endDatePicker.setValue(LocalDate.now());
        startDatePicker.setValue(LocalDate.now().minusMonths(1));

        generateButton.setOnAction(event -> generateReport(screenRenderer));
        exportExcelButton.setOnAction(event -> exportReport(new ExcelRenderer(generateButton.getScene().getWindow())));
        exportPdfButton.setOnAction(event -> exportReport(new PdfFileWriter(generateButton.getScene().getWindow())));

        generateReport(screenRenderer);
    }

    @FXML
    public void onBack() {
        MainController mainController = ApplicationSession.getInstance().getController();
        if (mainController != null) {
            mainController.onDashboard();
        } else {
            System.err.println("Помилка навігації: MainController недоступний.");
        }
    }

    private void generateReport(OutputRenderer renderer) {
        if (session.getCurrentUser() == null) return;
        
        ReportParams params = createReportParams();
        currentReportLogic = createReportLogic(reportTypeCombo.getValue());
        
        try {
            currentReportLogic.setOutputRenderer(renderer);
            currentReportLogic.generate(params, session.getCurrentUser()); 

            summaryLabel.setText("? Звіт '" + reportTypeCombo.getValue() + "' успішно згенеровано.");
        } catch (Exception e) {
            summaryLabel.setText("? Помилка при генерації звіту: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void exportReport(OutputRenderer fileWriter) {
        if (currentReportLogic == null) {
            summaryLabel.setText("Спочатку побудуйте звіт.");
            return;
        }

        ReportParams params = createReportParams();
        
        try {
            currentReportLogic.setOutputRenderer(fileWriter);
            currentReportLogic.generate(params, session.getCurrentUser()); 

            summaryLabel.setText("? Експорт у " + fileWriter.getClass().getSimpleName() + " завершено.");
        } catch (Exception e) {
            summaryLabel.setText("? Помилка експорту: " + e.getMessage());
            e.printStackTrace();
        }
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
            case "Загальний звіт по транзакціях":
                return new AllTransactionsReport(transactionDao); 
            case "Звіт за категоріями (Витрати)":
                // ? ВИПРАВЛЕННЯ: Змінено TransactionDao на ReportingService
                return new CategoryReport(reportingService); 
            case "Динаміка по Місяцях":
                return new MonthlyDynamicsReport(transactionDao, reportingService);
            default:
                throw new IllegalArgumentException("Невідомий тип звіту: " + type);
        }
    }
}