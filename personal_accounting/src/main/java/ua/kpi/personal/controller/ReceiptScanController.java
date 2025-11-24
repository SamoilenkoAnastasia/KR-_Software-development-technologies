package ua.kpi.personal.controller;

import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import ua.kpi.personal.model.Account;
import ua.kpi.personal.model.Category;
import ua.kpi.personal.model.ScanData;
import ua.kpi.personal.repo.AccountDao;
import ua.kpi.personal.repo.CategoryDao;
import ua.kpi.personal.service.ReceiptProcessor;
import ua.kpi.personal.state.ApplicationSession;

import java.io.File;
import java.io.IOException;
import java.time.LocalDate;
import java.util.List;
import java.util.Locale; 

public class ReceiptScanController {

    @FXML private TextField amountField;
    @FXML private TextField vendorField;
    @FXML private DatePicker datePicker; 
    @FXML private ChoiceBox<Category> categoryChoice;
    @FXML private ChoiceBox<Account> accountChoice;
    @FXML private TextArea rawTextField;
    @FXML private Label messageLabel;
    @FXML private Button scanBtn;
    @FXML private Button saveBtn; 

    private TransactionsController parentController;
    private final ReceiptProcessor receiptProcessor = new ReceiptProcessor();
    private final CategoryDao categoryDao = new CategoryDao();
    private final AccountDao accountDao = new AccountDao();
    private ScanData currentScanData;

    public void setParentController(TransactionsController controller) {
        this.parentController = controller;
        Long userId = ApplicationSession.getInstance().getCurrentUser().getId();

        List<Category> availableCategories = categoryDao.findByUserId(userId);
        categoryChoice.setItems(FXCollections.observableArrayList(availableCategories));

        List<Account> availableAccounts = accountDao.findByUserId(userId);
        accountChoice.setItems(FXCollections.observableArrayList(availableAccounts));

        if (!categoryChoice.getItems().isEmpty()) {
            categoryChoice.setValue(categoryChoice.getItems().get(0));
        }
        if (!accountChoice.getItems().isEmpty()) {
            accountChoice.setValue(accountChoice.getItems().get(0));
        }
    }

    @FXML
    private void initialize() {
        datePicker.setValue(LocalDate.now()); 
        if (saveBtn != null) saveBtn.setDisable(true);
    }

    @FXML
    private void onScanReceipt() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Вибрати зображення чека");
        fileChooser.getExtensionFilters().addAll(
            new FileChooser.ExtensionFilter("Зображення", "*.png", "*.jpg", "*.jpeg")
        );

        File file = fileChooser.showOpenDialog(scanBtn.getScene().getWindow());

        if (file != null) {
            messageLabel.setText("Обробка чека... Використовується локальний Tesseract OCR. Очікуйте...");
            scanBtn.setDisable(true);
            if (saveBtn != null) saveBtn.setDisable(true);

            new Thread(() -> {
                try {
                    ScanData data = receiptProcessor.processReceipt(file);
                    currentScanData = data;

                    Platform.runLater(() -> fillFormWithScanData(data));

                } catch (IOException e) {
                    Platform.runLater(() -> {
                        messageLabel.setText("Помилка сканування: " + e.getMessage()); 
                        rawTextField.setText("Помилка: " + e.getMessage());
                        amountField.setText("0.00");
                        datePicker.setValue(LocalDate.now()); 
                    });
                } finally {
                    Platform.runLater(() -> {
                        scanBtn.setDisable(false);
                        if (currentScanData != null && saveBtn != null) {
                            saveBtn.setDisable(false);
                        }
                    });
                }
            }).start();
        }
    }

    private void fillFormWithScanData(ScanData data) {
        amountField.setText(String.format(Locale.US, "%.2f", data.getAmount())); 
        vendorField.setText(data.getVendor());
        datePicker.setValue(data.getDate()); 
        rawTextField.setText(data.getRecognizedText());

        String suggestedName = data.getSuggestedCategoryName();
        categoryChoice.getItems().stream()
            .filter(c -> c.getName().equals(suggestedName))
            .findFirst()
            .ifPresentOrElse(
                categoryChoice::setValue,
                () -> messageLabel.setText("Дані розпізнано. Категорію '" + suggestedName + "' не знайдено, виберіть вручну.")
            );

        messageLabel.setText("Дані розпізнано. Перевірте та підтвердьте збереження.");
    }

    @FXML
    private void onSaveTransaction() {
        if (currentScanData == null) {
            messageLabel.setText("Спочатку скануйте чек.");
            return;
        }
        if (categoryChoice.getValue() == null || accountChoice.getValue() == null) {
            messageLabel.setText("Виберіть Категорію та Рахунок.");
            return;
        }
        if (datePicker.getValue() == null) {
            messageLabel.setText("Виберіть коректну дату.");
            return;
        }

        double finalAmount = getDoubleFromField(amountField.getText());
        
        if (finalAmount <= 0) {
            messageLabel.setText("Некоректна сума. Сума має бути додатною.");
            return;
        }

        currentScanData.setAmount(finalAmount);
        currentScanData.setVendor(vendorField.getText());
        currentScanData.setDate(datePicker.getValue());

        if (parentController != null) {
            parentController.handleScannedTransaction(
                currentScanData, 
                accountChoice.getValue(), 
                categoryChoice.getValue()
            );
        } else {
             messageLabel.setText("Помилка: батьківський контролер не встановлено.");
             return;
        }
        closeWindow();
    }
    

    private double getDoubleFromField(String text) {
        try {
            return Double.parseDouble(text.trim().replace(',', '.'));
        } catch (NumberFormatException e) {
            return 0.0;
        }
    }


    @FXML
    private void onCancel() {
        closeWindow();
    }

    private void closeWindow() {
        Stage stage = (Stage) (scanBtn != null ? scanBtn.getScene().getWindow() : saveBtn.getScene().getWindow());
        stage.close();
    }
}