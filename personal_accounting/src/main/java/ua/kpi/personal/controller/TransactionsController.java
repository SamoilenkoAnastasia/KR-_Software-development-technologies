package ua.kpi.personal.controller;

import java.io.IOException;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.stage.Modality;
import javafx.stage.Stage;
import ua.kpi.personal.model.*;
import ua.kpi.personal.model.TransactionTemplate.RecurringType;
import ua.kpi.personal.repo.*;
import ua.kpi.personal.service.TransactionService;
import ua.kpi.personal.service.AccountService;
import ua.kpi.personal.state.ApplicationSession;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.SimpleObjectProperty;


import java.time.LocalDateTime;
import java.time.LocalDate;
import java.util.Optional;
import java.util.Locale;
import java.util.List;

public class TransactionsController {

    @FXML private TableView<Transaction> table;
    @FXML private TableColumn<Transaction, String> colType;
    @FXML private TableColumn<Transaction, Double> colAmount;
    @FXML private TableColumn<Transaction, String> colCategory;
    @FXML private TableColumn<Transaction, String> colAccount;
    @FXML private TableColumn<Transaction, LocalDateTime> colDate;
    @FXML private TableColumn<Transaction, String> colDesc;
    @FXML private TableColumn<Transaction, String> colCreatedBy; 
    
    @FXML private ChoiceBox<String> typeChoice;
    @FXML private TextField amountField;
    @FXML private ChoiceBox<String> currencyChoice;
    @FXML private ChoiceBox<Category> categoryChoice;
    @FXML private ChoiceBox<Account> accountChoice;
    @FXML private DatePicker datePicker;
    @FXML private TextField descField;

    @FXML private ChoiceBox<RecurringType> recurringTypeChoice;
    @FXML private TextField recurrenceIntervalField;
    @FXML private TextField dayOrWeekField;
    @FXML private DatePicker startDatePicker;

    @FXML private Button backBtn;
    @FXML private Button editBtn;
    @FXML private Button deleteBtn;
    @FXML private Button addBtn;
    @FXML private Button scanReceiptBtn;

    @FXML private Label messageLabel;
    
    private final TransactionService transactionService;
    private final AccountService accountService;
    
    private final CategoryDao categoryDao = new CategoryDao();
    private final TemplateDao templateDao = new TemplateDao();

    private User user;
    private Transaction selectedTransaction = null;
    
    public TransactionsController() {
        ApplicationSession session = ApplicationSession.getInstance();
        this.transactionService = session.getTransactionService();
        this.accountService = session.getAccountService(); 
    }

    @FXML
    private void initialize(){
        this.user = ApplicationSession.getInstance().getCurrentUser();

        if (typeChoice != null) {
            typeChoice.getItems().addAll("EXPENSE", "INCOME");
            typeChoice.setValue("EXPENSE");
        }

        if (currencyChoice != null) {
            currencyChoice.getItems().addAll("UAH", "USD", "EUR");
            currencyChoice.setValue("UAH");
        }

        if (recurringTypeChoice != null) {
            recurringTypeChoice.getItems().addAll(RecurringType.values());
            recurringTypeChoice.setValue(RecurringType.NONE);

            recurringTypeChoice.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> {
                updateRecurringFieldsVisibility(newVal);
            });
            updateRecurringFieldsVisibility(RecurringType.NONE);
        }

        if (colType != null) colType.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getType()));
        if (colAmount != null) colAmount.setCellValueFactory(data -> new SimpleObjectProperty<>(data.getValue().getAmount()));
        if (colCategory != null) colCategory.setCellValueFactory(data -> new SimpleStringProperty(
            data.getValue().getCategory() != null ? data.getValue().getCategory().getName() : ""));
        if (colAccount != null) colAccount.setCellValueFactory(data -> new SimpleStringProperty(
            data.getValue().getAccount() != null ? data.getValue().getAccount().getName() : ""));
        if (colDate != null) colDate.setCellValueFactory(data -> new SimpleObjectProperty<>(data.getValue().getCreatedAt()));

        if (colDesc != null) colDesc.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getDescription()));
        if (colCreatedBy != null) {
            colCreatedBy.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getCreatedByName()));
        }
        
        if (datePicker != null) datePicker.setValue(LocalDate.now());
        if (startDatePicker != null) startDatePicker.setValue(LocalDate.now());
 
        if (table != null) {
            table.getSelectionModel().selectedItemProperty().addListener((obs, oldSelection, newSelection) -> {
                if (newSelection != null) {
                    selectedTransaction = newSelection;
                    fillFormWithTransaction(selectedTransaction);
                    setEditMode(true);
                } else {
                    setEditMode(false);
                }
            });
        }
        if (editBtn != null) editBtn.setOnAction(event -> onEdit());
        if (deleteBtn != null) deleteBtn.setOnAction(event -> onDelete());
        if (addBtn != null) addBtn.setOnAction(event -> onAdd());
        if (scanReceiptBtn != null) scanReceiptBtn.setOnAction(event -> {
            try {
                onScanReceipt();
            } catch (IOException e) {
                displayErrorDialog("Помилка завантаження вікна сканування: " + e.getMessage());
                e.printStackTrace();
            }
        });
        
        refresh();
    }

    private void updateRecurringFieldsVisibility(RecurringType type) {
        boolean isRecurring = type != RecurringType.NONE;

        if (recurrenceIntervalField != null) recurrenceIntervalField.setVisible(isRecurring);
        if (startDatePicker != null) startDatePicker.setVisible(isRecurring);

        if (dayOrWeekField != null) {
            boolean showDayField = type == RecurringType.MONTHLY || type == RecurringType.YEARLY;

            dayOrWeekField.setVisible(showDayField);

            if (showDayField) {
                 dayOrWeekField.setPromptText("День місяця (1-31)");
            } else {
                 dayOrWeekField.setPromptText("");
                 dayOrWeekField.clear();
            }
        }
    }

    public void displaySuccessDialog(String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Операція Успішна");
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    public void displayErrorDialog(String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Помилка Обробки Транзакції");
        alert.setHeaderText("Операцію не вдалося виконати.");
        alert.setContentText(message);
        alert.showAndWait();
    }

    private Optional<String> showTemplateNameDialog() {
        TextInputDialog dialog = new TextInputDialog("");
        dialog.setTitle("Зберегти Шаблон");
        dialog.setHeaderText("Введіть назву для цього шаблону:");
        dialog.setContentText("Назва:");
        return dialog.showAndWait();
    }

    private double getDoubleFromField(String text) {
        try {
            if (text == null) return 0.0;
            String cleanText = text.trim().replace(',', '.'); 
            if (cleanText.isEmpty()) return 0.0;
            return Double.parseDouble(cleanText);
        } catch (NumberFormatException e) {
            return 0.0;
        }
    }

    public User getUser() {
        return user;
    }

    void refresh(){
        Long currentBudgetId = ApplicationSession.getInstance().getCurrentBudgetId();
        if (user == null || table == null || currentBudgetId == null) return;

        try {
            List<Transaction> transactions = transactionService.getTransactionsByBudgetId(currentBudgetId);
            table.setItems(FXCollections.observableArrayList(transactions));
        } catch (SecurityException e) {
            displayErrorDialog("Помилка доступу до транзакцій: " + e.getMessage());
            table.setItems(FXCollections.emptyObservableList());
        } catch (RuntimeException e) {
             displayErrorDialog("Помилка завантаження транзакцій: " + e.getMessage());
             table.setItems(FXCollections.emptyObservableList());
        }

        if (categoryChoice != null) {
            categoryChoice.setItems(FXCollections.observableArrayList(categoryDao.findByUserId(user.getId())));
        }

        if (accountChoice != null) {
            try {
                List<Account> accessibleAccounts = accountService.getAccessibleAccountsForTransactions();
                accountChoice.setItems(FXCollections.observableArrayList(accessibleAccounts));

            } catch (Exception e) {
                System.err.println("Помилка завантаження доступних рахунків для ComboBox: " + e.getMessage());
                accountChoice.setItems(FXCollections.emptyObservableList());
            }
        }

        clearForm();
        setEditMode(false);
    }

    private void clearForm() {
        if (amountField != null) amountField.clear();
        if (descField != null) descField.clear();
        if (typeChoice != null) typeChoice.setValue("EXPENSE");
        if (currencyChoice != null) currencyChoice.setValue("UAH");
        if (datePicker != null) datePicker.setValue(LocalDate.now());
        if (categoryChoice != null) {
            categoryChoice.getSelectionModel().clearSelection();
            if (!categoryChoice.getItems().isEmpty()) categoryChoice.setValue(categoryChoice.getItems().get(0));
        }
        
        if (accountChoice != null) {
            accountChoice.getSelectionModel().clearSelection();
            if (!accountChoice.getItems().isEmpty()) accountChoice.setValue(accountChoice.getItems().get(0));
        }

        if (recurringTypeChoice != null) recurringTypeChoice.setValue(RecurringType.NONE);
        if (recurrenceIntervalField != null) recurrenceIntervalField.setText("1");
        if (dayOrWeekField != null) dayOrWeekField.clear();
        if (startDatePicker != null) startDatePicker.setValue(LocalDate.now());

        selectedTransaction = null;
    }

    private void fillFormWithTransaction(Transaction tx) {
        if (tx == null) return;

        if (typeChoice != null) typeChoice.setValue(tx.getType()); 
        if (amountField != null) amountField.setText(String.format(Locale.US, "%.2f", tx.getAmount()));
        if (descField != null) descField.setText(tx.getDescription());
        if (currencyChoice != null) currencyChoice.setValue(tx.getCurrency() != null ? tx.getCurrency() : "UAH");
        if (categoryChoice != null && tx.getCategory() != null) {
            categoryChoice.setValue(tx.getCategory());
        }
        if (accountChoice != null && tx.getAccount() != null) {
             accountChoice.setValue(tx.getAccount());
        }

        if (datePicker != null && tx.getCreatedAt() != null) {
            datePicker.setValue(tx.getCreatedAt().toLocalDate());
        } else if (datePicker != null) {
            datePicker.setValue(LocalDate.now());
        }

        if (recurringTypeChoice != null) {
             recurringTypeChoice.setValue(RecurringType.NONE);
             updateRecurringFieldsVisibility(RecurringType.NONE);
        }

        if (messageLabel != null) messageLabel.setText("Вибрано транзакцію ID: " + tx.getId() + ". Режим редагування активний.");
    }

    private void setEditMode(boolean isEdit) {
        if (editBtn != null) editBtn.setVisible(isEdit);
        if (deleteBtn != null) deleteBtn.setVisible(isEdit);
        if (addBtn != null) addBtn.setVisible(!isEdit);

        if (!isEdit) {
            clearForm();
            if (messageLabel != null) messageLabel.setText("");
            if (table != null) table.getSelectionModel().clearSelection();
        }
    }

    private Transaction createTransactionFromForm() {
        String amountText = (amountField != null) ? amountField.getText() : null;
        String type = (typeChoice != null) ? typeChoice.getValue() : null;
        Category cat = (categoryChoice != null) ? categoryChoice.getValue() : null;
        Account acc = (accountChoice != null) ? accountChoice.getValue() : null;
        String currency = (currencyChoice != null) ? currencyChoice.getValue() : null;
        LocalDate date = (datePicker != null) ? datePicker.getValue() : null;

        if (amountText == null || amountText.trim().isEmpty()) {
            if (messageLabel != null) messageLabel.setText("Поле 'Сума' не може бути порожнім.");
            return null;
        }

        if (type == null || acc == null || cat == null || currency == null || date == null) {
            if (messageLabel != null) messageLabel.setText("Заповніть усі обов'язкові поля: Тип, Рахунок, Категорія, Валюта, Дата.");
            return null;
        }

        try {
            double amount = getDoubleFromField(amountText);

            if (amount <= 0) {
                if (messageLabel != null) messageLabel.setText("Сума має бути додатною.");
                return null;
            }

            Transaction tx = new Transaction();
            tx.setAmount(amount);
            tx.setType(type);
            tx.setCategory(cat);
            tx.setAccount(acc);
            if (descField != null) tx.setDescription(descField.getText());
            tx.setCreatedAt(date.atTime(LocalDateTime.now().toLocalTime())); 
            tx.setCurrency(currency);
            tx.setUser(user);
            tx.setCreatedBy(user); 
            
            return tx;
        } catch(NumberFormatException ex){
            if (messageLabel != null) messageLabel.setText("Некоректна сума.");
            return null;
        }
    }

    @FXML
    private void onAdd(){
        Transaction tx = createTransactionFromForm();
        if (tx == null) return;

        try {
            Transaction savedTx = transactionService.saveTransaction(tx);

            clearForm();
            refresh();
            displaySuccessDialog("Транзакцію успішно додано. ID: " + savedTx.getId());
        } catch (SecurityException ex) {
            System.err.println("Transaction creation failed (Security): " + ex.getMessage());
            displayErrorDialog("Помилка прав доступу при додаванні: " + ex.getMessage());
        } catch (RuntimeException ex) {
            System.err.println("Transaction creation failed: " + ex.getMessage());
            displayErrorDialog("Помилка додавання: " + ex.getMessage());
        }
    }

    @FXML
    private void onEdit() {
        if (selectedTransaction == null) {
            if (messageLabel != null) messageLabel.setText("Спочатку виберіть транзакцію для редагування.");
            return;
        }

        Transaction updatedTx = createTransactionFromForm();
        if (updatedTx == null) return;

        updatedTx.setId(selectedTransaction.getId());
        updatedTx.setCreatedAt(selectedTransaction.getCreatedAt()); 
        updatedTx.setTemplateId(selectedTransaction.getTemplateId());
        updatedTx.setCreatedBy(selectedTransaction.getCreatedBy()); 
        
        try {
            transactionService.updateTransaction(selectedTransaction, updatedTx);
            displaySuccessDialog("Транзакція ID " + updatedTx.getId() + " успішно оновлена.");
            refresh();
        } catch (SecurityException ex) {
            System.err.println("Transaction update failed (Security): " + ex.getMessage());
            displayErrorDialog("Помилка прав доступу при оновленні: " + ex.getMessage());
        } catch (RuntimeException ex) {
            System.err.println("Transaction update failed: " + ex.getMessage());
            displayErrorDialog("Помилка оновлення: " + ex.getMessage());
        }
    }

    @FXML
    private void onDelete() {
        if (selectedTransaction == null) {
            if (messageLabel != null) messageLabel.setText("Спочатку виберіть транзакцію для видалення.");
            return;
        }

        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Підтвердження Видалення");
        alert.setHeaderText("Ви збираєтеся видалити транзакцію ID " + selectedTransaction.getId());
        alert.setContentText("Ви впевнені, що хочете видалити цю транзакцію? Ця дія вплине на баланс рахунку.");

        Optional<ButtonType> result = alert.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            try {
                transactionService.deleteTransaction(selectedTransaction);
                displaySuccessDialog("Транзакція ID " + selectedTransaction.getId() + " успішно видалена.");
                refresh();
            } catch (SecurityException ex) {
                System.err.println("Transaction deletion failed (Security): " + ex.getMessage());
                displayErrorDialog("Помилка прав доступу при видаленні: " + ex.getMessage());
            } catch (RuntimeException ex) {
                System.err.println("Transaction deletion failed: " + ex.getMessage());
                displayErrorDialog("Помилка видалення: " + ex.getMessage());
            }
        }
    }

    public void fillFormWithTemplate(TransactionTemplate template) {
        if (template == null) return;
        clearForm(); 

        if (typeChoice != null) {
            String type = template.getType() != null ? template.getType() : "EXPENSE";
            typeChoice.setValue(type);
        }

        if (amountField != null) {
            String amountText = "";
            if (template.getDefaultAmount() != null && template.getDefaultAmount() != 0.0) {
                 amountText = String.format(Locale.US, "%.2f", template.getDefaultAmount());
            }
            amountField.setText(amountText);
        }

        if (descField != null) {
            String desc = template.getDescription() != null ? template.getDescription() : "";
            descField.setText(desc);
        }

        if (currencyChoice != null) {
            String currency = template.getCurrency() != null ? template.getCurrency() : "UAH";
            currencyChoice.setValue(currency);
        }

        if (categoryChoice != null) {
            Long templateCatId = template.getCategory() != null ? template.getCategory().getId() : null;
            categoryChoice.getSelectionModel().clearSelection();

            if (templateCatId != null && !categoryChoice.getItems().isEmpty()) {
                categoryChoice.getItems().stream()
                     .filter(c -> c.getId() != null && c.getId().equals(templateCatId))
                     .findFirst()
                     .ifPresentOrElse(categoryChoice::setValue, () -> {});
            }
        }

        if (accountChoice != null) {
            Long templateAccId = template.getAccount() != null ? template.getAccount().getId() : null;
            accountChoice.getSelectionModel().clearSelection();

            if (templateAccId != null && !accountChoice.getItems().isEmpty()) {
                accountChoice.getItems().stream()
                     .filter(a -> a.getId() != null && a.getId().equals(templateAccId))
                     .findFirst()
                     .ifPresentOrElse(accountChoice::setValue, () -> {});
            }
        }

        if (recurringTypeChoice != null) {
            RecurringType recType = template.getRecurringType() != null ? template.getRecurringType() : TransactionTemplate.RecurringType.NONE;
            recurringTypeChoice.setValue(recType);
            updateRecurringFieldsVisibility(recType);
        }

        if (recurrenceIntervalField != null) {
            String interval = template.getRecurrenceInterval() != null ? template.getRecurrenceInterval().toString() : "1";
            recurrenceIntervalField.setText(interval);
        }

        if (dayOrWeekField != null) {
            String day = template.getDayOfMonth() != null ? template.getDayOfMonth().toString() : "";
            dayOrWeekField.setText(day);
        }

        if (startDatePicker != null) {
            LocalDate startDate = template.getStartDate() != null ? template.getStartDate() : LocalDate.now();
            startDatePicker.setValue(startDate);
        }

        if (datePicker != null) datePicker.setValue(LocalDate.now());

        if (messageLabel != null) messageLabel.setText("Форма заповнена шаблоном '" + template.getName() + "'.");
        setEditMode(false); 
    }

    @FXML
    private void onSaveAsTemplate() {
        Optional<String> result = showTemplateNameDialog();
        if (result.isPresent() && !result.get().isBlank()) {
            TransactionTemplate t = new TransactionTemplate();
            t.setName(result.get());
            if (typeChoice != null) t.setType(typeChoice.getValue());
            if (amountField != null) t.setDefaultAmount(getDoubleFromField(amountField.getText()));
            if (categoryChoice != null) t.setCategory(categoryChoice.getValue());
            if (accountChoice != null) t.setAccount(accountChoice.getValue());
            if (descField != null) t.setDescription(descField.getText());
            t.setUser(user);

            if (recurringTypeChoice != null) {
                t.setRecurringType(recurringTypeChoice.getValue());

                if (t.getRecurringType() != RecurringType.NONE) {
                    // Інтервал
                    if (recurrenceIntervalField != null) {
                        int interval = (int) getDoubleFromField(recurrenceIntervalField.getText());
                        t.setRecurrenceInterval(Math.max(1, interval));
                    } else {
                        t.setRecurrenceInterval(1);
                    }
                    if (dayOrWeekField != null && (t.getRecurringType() == RecurringType.MONTHLY || t.getRecurringType() == RecurringType.YEARLY)) {
                        int day = (int) getDoubleFromField(dayOrWeekField.getText());
                        t.setDayOfMonth(day > 0 ? day : null);
                    } else {
                        t.setDayOfMonth(null);
                    }
                
                    if (startDatePicker != null) {
                        t.setStartDate(startDatePicker.getValue());
                    } else {
                        t.setStartDate(LocalDate.now());
                    }
                } else {
                    t.setRecurrenceInterval(null);
                    t.setDayOfMonth(null);
                    t.setStartDate(null);
                }
            }

            templateDao.create(t);
            if (messageLabel != null) messageLabel.setText("Шаблон '" + t.getName() + "' успішно збережено.");
            refresh();
        }
    }

    @FXML
    private void onManageTemplates() throws IOException {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/template_manager.fxml"));
            Parent root = loader.load();
            TemplateManagerController controller = loader.getController();
            controller.setParentController(this);
            Stage stage = new Stage();
            stage.setTitle("Управління Шаблонами Транзакцій");
            stage.setScene(new Scene(root));
            stage.initModality(Modality.APPLICATION_MODAL);
            stage.showAndWait();
        } catch (IOException e) {
            if (messageLabel != null) messageLabel.setText("Помилка завантаження вікна шаблонів: " + e.getMessage());
            e.printStackTrace();
        }
    }

    @FXML
    private void onScanReceipt() throws IOException {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/receipt_scan_view.fxml"));
            Parent root = loader.load();
            ReceiptScanController controller = loader.getController();
            controller.setParentController(this); 
            Stage stage = new Stage();
            stage.setTitle("Сканування чека (Tesseract OCR)");
            stage.setScene(new Scene(root));
            stage.initModality(Modality.APPLICATION_MODAL);
            stage.showAndWait();
        } catch (IOException e) {
            if (messageLabel != null) messageLabel.setText("Помилка завантаження вікна сканування: " + e.getMessage());
            e.printStackTrace();
            throw e;
        }
    }
    
    public void handleScannedTransaction(ScanData data, Account account, Category category) {
        
        clearForm();
        if (typeChoice != null) typeChoice.setValue("EXPENSE");
        if (amountField != null) amountField.setText(String.format(Locale.US, "%.2f", data.getAmount()));
        if (descField != null) descField.setText(data.getVendor());
        if (datePicker != null) datePicker.setValue(data.getDate());
        if (accountChoice != null) accountChoice.setValue(account);
        if (categoryChoice != null) categoryChoice.setValue(category);
        if (currencyChoice != null) currencyChoice.setValue("UAH");
        setEditMode(false);

        try {
            Transaction tx = createTransactionFromForm();
            if (tx == null) {
                if (messageLabel != null) messageLabel.setText(messageLabel.getText() + " Автоматичне збереження скасовано через помилку у даних.");
                return;
            }
            
            Transaction savedTx = transactionService.saveTransaction(tx);

            clearForm();
            refresh();
            displaySuccessDialog("Транзакцію з чека успішно додано та збережено. ID: " + savedTx.getId());
        } catch (SecurityException ex) {
            System.err.println("Transaction creation failed (Security): " + ex.getMessage());
            displayErrorDialog("Помилка прав доступу при додаванні: " + ex.getMessage());
        } catch (RuntimeException ex) {
            System.err.println("Transaction creation failed: " + ex.getMessage());
            displayErrorDialog("Помилка автоматичного додавання транзакції: " + ex.getMessage());
        }
    }

    @FXML
    private void onBack() throws IOException { 
        ApplicationSession.getInstance().login(user);
    }
}