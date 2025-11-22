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
import ua.kpi.personal.processor.*;
import ua.kpi.personal.service.ExchangeRateService;
import java.time.LocalDateTime;
import java.time.LocalDate;
import java.util.Optional;
import java.util.Locale;
import ua.kpi.personal.state.ApplicationSession;

public class TransactionsController {

    @FXML private TableView<Transaction> table;
    @FXML private TableColumn<Transaction, String> colType;
    @FXML private TableColumn<Transaction, Double> colAmount;
    @FXML private TableColumn<Transaction, String> colCategory;
    @FXML private TableColumn<Transaction, String> colAccount;
    @FXML private TableColumn<Transaction, LocalDateTime> colDate;
    @FXML private TableColumn<Transaction, String> colDesc;
    @FXML private TableColumn<Transaction, Long> colTemplateId; 

    @FXML private ChoiceBox<String> typeChoice;
    @FXML private TextField amountField;
    @FXML private ChoiceBox<String> currencyChoice;
    @FXML private ChoiceBox<Category> categoryChoice;
    @FXML private ChoiceBox<Account> accountChoice;
    @FXML private DatePicker datePicker;
    @FXML private TextField descField;
    
    // --- Поля для налаштування періодичності (з нового FXML) ---
    @FXML private ChoiceBox<RecurringType> recurringTypeChoice;
    @FXML private TextField intervalField;
    @FXML private TextField dayOfMonthField;
    @FXML private DatePicker startDatePicker;
    @FXML private Label intervalLabel;
    @FXML private Label dayOfMonthLabel;
    @FXML private Label startDateLabel;
    
    
    @FXML private Button backBtn;
    @FXML private Button editBtn;
    @FXML private Button deleteBtn;
    @FXML private Button addBtn;
    @FXML private Button scanReceiptBtn; 

    @FXML private Label messageLabel;

    private TransactionProcessor transactionProcessor;
    private final TransactionDao transactionDao = new TransactionDao();
    private final CategoryDao categoryDao = new CategoryDao();
    private final AccountDao accountDao = new AccountDao();
    private final TemplateDao templateDao = new TemplateDao();
    private User user;
    
    // ? ВАЖЛИВО: selectedTransaction – це оригінальна (стара) транзакція
    private Transaction selectedTransaction = null;

    @FXML
    private void initialize(){
        this.user = ApplicationSession.getInstance().getCurrentUser();
        setupProcessor();
        
        typeChoice.getItems().addAll("EXPENSE", "INCOME");
        typeChoice.setValue("EXPENSE");
        
        currencyChoice.getItems().addAll("UAH", "USD", "EUR");
        currencyChoice.setValue("UAH"); 
        
        // Ініціалізація вибору періодичності
        if (recurringTypeChoice != null) {
            recurringTypeChoice.getItems().addAll(RecurringType.values());
            recurringTypeChoice.setValue(RecurringType.NONE);
            
            // Слухач для приховування/показу полів залежно від типу
            recurringTypeChoice.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> {
                updateRecurringFieldsVisibility(newVal);
            });
            updateRecurringFieldsVisibility(RecurringType.NONE); // Початковий стан
        }

        colType.setCellValueFactory(data -> new javafx.beans.property.SimpleStringProperty(data.getValue().getType()));
        colAmount.setCellValueFactory(data -> new javafx.beans.property.SimpleObjectProperty<>(data.getValue().getAmount()));
        
        colCategory.setCellValueFactory(data -> new javafx.beans.property.SimpleStringProperty(
            data.getValue().getCategory() != null ? data.getValue().getCategory().getName() : ""));
            
        colAccount.setCellValueFactory(data -> new javafx.beans.property.SimpleStringProperty(
            data.getValue().getAccount() != null ? data.getValue().getAccount().getName() : ""));
            
        colDate.setCellValueFactory(data -> new javafx.beans.property.SimpleObjectProperty<>(data.getValue().getCreatedAt()));
        colDesc.setCellValueFactory(data -> new javafx.beans.property.SimpleStringProperty(data.getValue().getDescription()));
        
        if (colTemplateId != null) {
            colTemplateId.setCellValueFactory(data -> {
                Long templateId = data.getValue().getTemplateId();
                return templateId != null ? new javafx.beans.property.SimpleObjectProperty<>(templateId) : null;
            });
        }
        
        datePicker.setValue(LocalDate.now());
        if (startDatePicker != null) startDatePicker.setValue(LocalDate.now());

        table.getSelectionModel().selectedItemProperty().addListener((obs, oldSelection, newSelection) -> {
            if (newSelection != null) {
                // ? ВАЖЛИВО: Зберігаємо обрану транзакцію
                selectedTransaction = newSelection;
                fillFormWithTransaction(selectedTransaction);
                setEditMode(true);
            } else {
                setEditMode(false);
            }
        });
        
        if (editBtn != null) editBtn.setOnAction(event -> onEdit());
        if (deleteBtn != null) deleteBtn.setOnAction(event -> onDelete());
        
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
        boolean isMonthly = type == RecurringType.MONTHLY || type == RecurringType.YEARLY;
        
        if (intervalField != null) {
            intervalField.setVisible(isRecurring);
            intervalLabel.setVisible(isRecurring);
        }
        if (startDatePicker != null) {
            startDatePicker.setVisible(isRecurring);
            startDateLabel.setVisible(isRecurring);
        }
        if (dayOfMonthField != null) {
            dayOfMonthField.setVisible(isMonthly);
            dayOfMonthLabel.setVisible(isMonthly);
        }
    }
    
    private void setupProcessor() {
        ExchangeRateService rateService = new ExchangeRateService(); 
        TransactionProcessor baseProcessor = new TransactionDao();
        TransactionProcessor currencyProcessor = new CurrencyDecorator(baseProcessor, rateService); 
        TransactionProcessor balanceProcessor = new BalanceCheckDecorator(currencyProcessor);
        this.transactionProcessor = new UiNotificationDecorator(balanceProcessor, this);
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
    
    public User getUser() {
        return user;
    }

    void refresh(){
        if (user == null) return;
        table.setItems(FXCollections.observableArrayList(transactionDao.findByUserId(user.getId())));
        categoryChoice.setItems(FXCollections.observableArrayList(categoryDao.findByUserId(user.getId())));
        accountChoice.setItems(FXCollections.observableArrayList(accountDao.findByUserId(user.getId())));
        clearForm();
        setEditMode(false);
    }
    
    private void clearForm() {
        amountField.clear();
        descField.clear();
        typeChoice.setValue("EXPENSE");
        currencyChoice.setValue("UAH");
        datePicker.setValue(LocalDate.now());
        categoryChoice.getSelectionModel().clearSelection();
        accountChoice.getSelectionModel().clearSelection();
        
        if (!categoryChoice.getItems().isEmpty()) categoryChoice.setValue(categoryChoice.getItems().get(0));
        if (!accountChoice.getItems().isEmpty()) accountChoice.setValue(accountChoice.getItems().get(0));
        
        // Скидаємо поля періодичності
        if (recurringTypeChoice != null) recurringTypeChoice.setValue(RecurringType.NONE);
        if (intervalField != null) intervalField.setText("1");
        if (dayOfMonthField != null) dayOfMonthField.clear();
        if (startDatePicker != null) startDatePicker.setValue(LocalDate.now());
        
        selectedTransaction = null;
    }

    private void fillFormWithTransaction(Transaction tx) {
        if (tx == null) return;
        typeChoice.setValue(tx.getType());
        amountField.setText(String.format(Locale.US, "%.2f", tx.getAmount())); 
        descField.setText(tx.getDescription());
        currencyChoice.setValue(tx.getCurrency() != null ? tx.getCurrency() : "UAH");
        
        if (tx.getCategory() != null) {
            categoryChoice.getItems().stream()
                .filter(c -> c.getId().equals(tx.getCategory().getId()))
                .findFirst()
                .ifPresent(categoryChoice::setValue);
        }

        if (tx.getAccount() != null) {
            accountChoice.getItems().stream()
                .filter(a -> a.getId().equals(tx.getAccount().getId()))
                .findFirst()
                .ifPresent(accountChoice::setValue);
        }
        
        if (tx.getCreatedAt() != null) {
            datePicker.setValue(tx.getCreatedAt().toLocalDate());
        } else {
            datePicker.setValue(LocalDate.now());
        }
        
        // При редагуванні транзакції ми не редагуємо шаблон, тому поля періодичності приховуємо або скидаємо
        if (recurringTypeChoice != null) recurringTypeChoice.setValue(RecurringType.NONE);
        
        messageLabel.setText("Вибрано транзакцію ID: " + tx.getId() + ". Режим редагування активний.");
    }
    
    private void setEditMode(boolean isEdit) {
        if (editBtn != null) editBtn.setVisible(isEdit);
        if (deleteBtn != null) deleteBtn.setVisible(isEdit);
        if (addBtn != null) addBtn.setVisible(!isEdit);
        
        if (!isEdit) {
            clearForm();
            messageLabel.setText("");
            table.getSelectionModel().clearSelection();
        }
    }

    public void handleScannedTransaction(ScanData data, Account account, Category category) {
        clearForm();
        typeChoice.setValue("EXPENSE");
        amountField.setText(String.format(Locale.US, "%.2f", data.getAmount())); 
        descField.setText(data.getVendor()); 
        datePicker.setValue(data.getDate()); 
        accountChoice.setValue(account);
        categoryChoice.setValue(category);
        currencyChoice.setValue("UAH"); 
        setEditMode(false);

        try {
            Transaction tx = createTransactionFromForm(); 
            if (tx == null) {
                messageLabel.setText(messageLabel.getText() + " Автоматичне збереження скасовано через помилку у даних.");
                return;
            }
            transactionProcessor.create(tx);
            clearForm();
            refresh();
            displaySuccessDialog("Транзакцію з чека успішно додано та збережено.");
        } catch (RuntimeException ex) {
            System.err.println("Transaction creation failed: " + ex.getMessage());
            displayErrorDialog("Помилка автоматичного додавання транзакції: " + ex.getMessage());
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
            messageLabel.setText("Помилка завантаження вікна сканування: " + e.getMessage());
            e.printStackTrace();
            throw e;
        }
    }

    public void fillFormWithTemplate(TransactionTemplate template) {
        Transaction clonedTx = template.createTransactionFromTemplate(LocalDate.now()); 
        
        typeChoice.setValue(clonedTx.getType());
        amountField.setText(clonedTx.getAmount() != 0.0 ? String.format(Locale.US, "%.2f", clonedTx.getAmount()) : ""); 
        descField.setText(clonedTx.getDescription());
        currencyChoice.setValue("UAH");
        
        if (clonedTx.getCategory() != null) {
            categoryChoice.getItems().stream()
                .filter(c -> c.getId().equals(clonedTx.getCategory().getId()))
                .findFirst()
                .ifPresent(categoryChoice::setValue);
        }
        
        if (clonedTx.getAccount() != null) {
            accountChoice.getItems().stream()
                .filter(a -> a.getId().equals(clonedTx.getAccount().getId()))
                .findFirst()
                .ifPresent(accountChoice::setValue);
        }
        
        // Заповнюємо поля періодичності з шаблону
        if (recurringTypeChoice != null) recurringTypeChoice.setValue(template.getRecurringType());
        if (intervalField != null && template.getRecurrenceInterval() != null) intervalField.setText(template.getRecurrenceInterval().toString());
        if (dayOfMonthField != null && template.getDayOfMonth() != null) dayOfMonthField.setText(template.getDayOfMonth().toString());
        if (startDatePicker != null && template.getStartDate() != null) startDatePicker.setValue(template.getStartDate());
        
        datePicker.setValue(LocalDate.now());
        setEditMode(false);
        messageLabel.setText("Форма заповнена шаблоном '" + template.getName() + "'.");
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
            messageLabel.setText("Помилка завантаження вікна шаблонів: " + e.getMessage());
            e.printStackTrace();
        }
    }

    @FXML
    private void onSaveAsTemplate() {
        Optional<String> result = showTemplateNameDialog();
        if (result.isPresent() && !result.get().isBlank()) {
            TransactionTemplate t = new TransactionTemplate();
            t.setName(result.get());
            t.setType(typeChoice.getValue());
            t.setDefaultAmount(getDoubleFromField(amountField.getText()));
            t.setCategory(categoryChoice.getValue());
            t.setAccount(accountChoice.getValue());
            t.setDescription(descField.getText());
            t.setUser(user);
            
            // Збереження параметрів періодичності
            if (recurringTypeChoice != null) {
                t.setRecurringType(recurringTypeChoice.getValue());
                if (t.getRecurringType() != RecurringType.NONE) {
                    if (intervalField != null) t.setRecurrenceInterval((int)getDoubleFromField(intervalField.getText()));
                    if (dayOfMonthField != null) t.setDayOfMonth((int)getDoubleFromField(dayOfMonthField.getText()));
                    if (startDatePicker != null) t.setStartDate(startDatePicker.getValue());
                }
            }
            
            templateDao.create(t);
            messageLabel.setText("Шаблон '" + t.getName() + "' успішно збережено.");
            refresh();
        }
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
            String cleanText = text.trim().replace(',', '.');
            if (cleanText.isEmpty()) return 0.0;
            return Double.parseDouble(cleanText); 
        } catch (NumberFormatException e) {
            return 0.0;
        }
    }

    private Transaction createTransactionFromForm() {
        String amountText = amountField.getText();
        String type = typeChoice.getValue();
        Category cat = categoryChoice.getValue();
        Account acc = accountChoice.getValue();
        String currency = currencyChoice.getValue();
        LocalDate date = datePicker.getValue();
        
        if (amountText == null || amountText.trim().isEmpty()) {
            messageLabel.setText("Поле 'Сума' не може бути порожнім.");
            return null;
        }

        if (type == null || acc == null || cat == null || currency == null || date == null) { 
            messageLabel.setText("Заповніть усі обов'язкові поля: Тип, Рахунок, Категорія, Валюта, Дата."); 
            return null; 
        }

        try {
            String cleanAmountText = amountText.trim().replace(',', '.');
            double amount = Double.parseDouble(cleanAmountText); 
            
            if (amount <= 0) {
                messageLabel.setText("Сума має бути додатною.");
                return null;
            }

            Transaction tx = new Transaction();
            tx.setAmount(amount);
            tx.setType(type);
            tx.setCategory(cat);
            tx.setAccount(acc);
            tx.setDescription(descField.getText());
            tx.setCreatedAt(date.atTime(LocalDateTime.now().toLocalTime())); 
            tx.setCurrency(currency);
            tx.setUser(user);
            return tx;
        } catch(NumberFormatException ex){
            messageLabel.setText("Некоректна сума.");
            return null;
        }
    }

    @FXML
    private void onAdd(){
        Transaction tx = createTransactionFromForm();
        if (tx == null) return;
        
        try {
            transactionProcessor.create(tx);
            clearForm();
            refresh();
            displaySuccessDialog("Транзакцію успішно додано.");
        } catch (RuntimeException ex) {
            System.err.println("Transaction creation failed: " + ex.getMessage());
            displayErrorDialog("Помилка додавання: " + ex.getMessage());
        }
    }
    
    @FXML
    private void onEdit() {
        // ? КРОК 1: Перевірка та отримання оригінальної транзакції
        if (selectedTransaction == null) {
            messageLabel.setText("Спочатку виберіть транзакцію для редагування.");
            return;
        }
        
        // ? КРОК 2: Створення об'єкта НОВОЇ транзакції (updatedTx) з форми
        Transaction updatedTx = createTransactionFromForm();
        if (updatedTx == null) return;

        // Перенесення ID та незмінних полів з оригінальної транзакції
        updatedTx.setId(selectedTransaction.getId());
        updatedTx.setCreatedAt(selectedTransaction.getCreatedAt()); 
        updatedTx.setTemplateId(selectedTransaction.getTemplateId());
        // ВАЖЛИВО: Оскільки ми використовуємо декоратори, які читають баланс,
        // ми повинні передати originalTx як джерело "старого" балансу.
        // Ми не передаємо selectedTransaction безпосередньо,
        // щоб уникнути побічних ефектів при модифікації,
        // хоча selectedTransaction є ідентифікатором оригінальної транзакції.
        
        try {
            // ? КРОК 3: ВИКЛИК З ОНОВЛЕНОЮ СИГНАТУРОЮ
            // Передаємо selectedTransaction як "стару" транзакцію (originalTx)
            transactionProcessor.update(selectedTransaction, updatedTx); 
            displaySuccessDialog("Транзакція ID " + updatedTx.getId() + " успішно оновлена.");
            refresh();
        } catch (RuntimeException ex) {
            System.err.println("Transaction update failed: " + ex.getMessage());
            displayErrorDialog("Помилка оновлення: " + ex.getMessage());
        }
    }
    
    @FXML
    private void onDelete() {
        if (selectedTransaction == null) {
            messageLabel.setText("Спочатку виберіть транзакцію для видалення.");
            return;
        }

        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Підтвердження Видалення");
        alert.setHeaderText("Ви збираєтеся видалити транзакцію ID " + selectedTransaction.getId());
        alert.setContentText("Ви впевнені, що хочете видалити цю транзакцію? Ця дія вплине на баланс рахунку.");

        Optional<ButtonType> result = alert.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            try {
                // Видалення
                transactionProcessor.delete(selectedTransaction);
                displaySuccessDialog("Транзакція ID " + selectedTransaction.getId() + " успішно видалена.");
                refresh();
            } catch (RuntimeException ex) {
                System.err.println("Transaction deletion failed: " + ex.getMessage());
                displayErrorDialog("Помилка видалення: " + ex.getMessage());
            }
        }
    }

    @FXML
    private void onBack() throws IOException {
        ApplicationSession.getInstance().login(user);
    }
}