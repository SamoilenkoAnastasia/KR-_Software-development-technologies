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

    // Таблиця та Колонки
    @FXML private TableView<Transaction> table;
    @FXML private TableColumn<Transaction, String> colType;
    @FXML private TableColumn<Transaction, Double> colAmount;
    @FXML private TableColumn<Transaction, String> colCategory;
    @FXML private TableColumn<Transaction, String> colAccount;
    @FXML private TableColumn<Transaction, LocalDateTime> colDate;
    @FXML private TableColumn<Transaction, String> colDesc;
    @FXML private TableColumn<Transaction, Long> colTemplateId; 

    // Основна форма
    @FXML private ChoiceBox<String> typeChoice;
    @FXML private TextField amountField;
    @FXML private ChoiceBox<String> currencyChoice;
    @FXML private ChoiceBox<Category> categoryChoice;
    @FXML private ChoiceBox<Account> accountChoice;
    @FXML private DatePicker datePicker;
    @FXML private TextField descField;
    
    // Налаштування періодичності
    @FXML private ChoiceBox<RecurringType> recurringTypeChoice;
    @FXML private TextField recurrenceIntervalField;
    @FXML private TextField dayOrWeekField;
    @FXML private DatePicker startDatePicker;
    
    // Кнопки
    @FXML private Button backBtn;
    @FXML private Button editBtn;
    @FXML private Button deleteBtn;
    @FXML private Button addBtn;
    @FXML private Button scanReceiptBtn; 

    // Інші елементи UI
    @FXML private Label messageLabel;

    // Сервіси та Стан
    private TransactionProcessor transactionProcessor;
    private final TransactionDao transactionDao = new TransactionDao();
    private final CategoryDao categoryDao = new CategoryDao();
    private final AccountDao accountDao = new AccountDao();
    private final TemplateDao templateDao = new TemplateDao();
    private User user;
    
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
            
            recurringTypeChoice.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> {
                updateRecurringFieldsVisibility(newVal);
            });
            updateRecurringFieldsVisibility(RecurringType.NONE);
        }

        // Ініціалізація колонок
        if (colType != null) colType.setCellValueFactory(data -> new javafx.beans.property.SimpleStringProperty(data.getValue().getType()));
        if (colAmount != null) colAmount.setCellValueFactory(data -> new javafx.beans.property.SimpleObjectProperty<>(data.getValue().getAmount()));
        
        if (colCategory != null) colCategory.setCellValueFactory(data -> new javafx.beans.property.SimpleStringProperty(
            data.getValue().getCategory() != null ? data.getValue().getCategory().getName() : ""));
            
        if (colAccount != null) colAccount.setCellValueFactory(data -> new javafx.beans.property.SimpleStringProperty(
            data.getValue().getAccount() != null ? data.getValue().getAccount().getName() : ""));
            
        if (colDate != null) colDate.setCellValueFactory(data -> new javafx.beans.property.SimpleObjectProperty<>(data.getValue().getCreatedAt()));
        if (colDesc != null) colDesc.setCellValueFactory(data -> new javafx.beans.property.SimpleStringProperty(data.getValue().getDescription()));
        
        if (colTemplateId != null) {
            colTemplateId.setCellValueFactory(data -> {
                Long templateId = data.getValue().getTemplateId();
                return templateId != null ? new javafx.beans.property.SimpleObjectProperty<>(templateId) : new javafx.beans.property.SimpleObjectProperty<>(null);
            });
        }
        
        datePicker.setValue(LocalDate.now());
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
        
        // Обробники кнопок
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

        // ? КРИТИЧНО: Завантажуємо всі дані після ініціалізації UI
        refresh();
    }
    
    // --- Допоміжні методи ---

    // ? ВИПРАВЛЕНО: Керування видимістю полів періодичності
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
    
    private void setupProcessor() {
        ExchangeRateService rateService = new ExchangeRateService(); 
        TransactionDao baseDao = new TransactionDao(); 
        TransactionProcessor currencyProcessor = new CurrencyDecorator(baseDao, rateService); 
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

    // ? КЛЮЧОВИЙ МЕТОД ДЛЯ ЗАВАНТАЖЕННЯ ДАНИХ У ChoiceBox
    void refresh(){
        if (user == null || table == null) return;
        table.setItems(FXCollections.observableArrayList(transactionDao.findByUserId(user.getId())));
        
        // ? ЗАВАНТАЖЕННЯ КАТЕГОРІЙ ТА РАХУНКІВ
        if (categoryChoice != null) categoryChoice.setItems(FXCollections.observableArrayList(categoryDao.findByUserId(user.getId())));
        if (accountChoice != null) accountChoice.setItems(FXCollections.observableArrayList(accountDao.findByUserId(user.getId())));
        
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
        
        // Скидаємо поля періодичності
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
        
        // Заповнення ChoiceBox для транзакції (покладаємося на equals/hashCode)
        if (categoryChoice != null && tx.getCategory() != null) {
            // Тепер шукає об'єкт, використовуючи його ID
            categoryChoice.setValue(tx.getCategory());
        }

        if (accountChoice != null && tx.getAccount() != null) {
             // Тепер шукає об'єкт, використовуючи його ID
             accountChoice.setValue(tx.getAccount());
        }
        
        if (datePicker != null && tx.getCreatedAt() != null) {
            datePicker.setValue(tx.getCreatedAt().toLocalDate());
        } else if (datePicker != null) {
            datePicker.setValue(LocalDate.now());
        }
        
        // При редагуванні ТРАНЗАКЦІЇ, скидаємо поля періодичності
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
            if (messageLabel != null) messageLabel.setText("Помилка завантаження вікна сканування: " + e.getMessage());
            e.printStackTrace();
            throw e;
        }
    }
    
        public void fillFormWithTemplate(TransactionTemplate template) {
        if (template == null) return;

        // Починаємо логування
        System.out.println("\n*** DEBUG (TX Controller): Починаємо заповнення форми шаблоном: " + template.getName() + " ***");

        // --- 1. ПРИМІТИВНІ ПОЛЯ (Тип, Сума, Опис, Валюта) ---

        // Тип (EXPENSE/INCOME)
        if (typeChoice != null) {
            String type = template.getType() != null ? template.getType() : "EXPENSE";
            typeChoice.setValue(type);
            System.out.println("DEBUG (TX Controller): Встановлено Тип: " + type);
        }

        // Сума
        if (amountField != null) {
            String amountText = "";
            if (template.getDefaultAmount() != null && template.getDefaultAmount() != 0.0) {
                // Використовуємо Locale.US для формату з крапкою як десятковим роздільником (0.00)
                amountText = String.format(Locale.US, "%.2f", template.getDefaultAmount()); 
            }
            amountField.setText(amountText); 
            System.out.println("DEBUG (TX Controller): Встановлено Сума: " + amountText);
        }

        // Опис
        if (descField != null) {
            String desc = template.getDescription() != null ? template.getDescription() : "";
            descField.setText(desc);
            System.out.println("DEBUG (TX Controller): Встановлено Опис: " + desc);
        }

        // Валюта
        if (currencyChoice != null) {
            String currency = template.getCurrency() != null ? template.getCurrency() : "UAH";
            currencyChoice.setValue(currency);
            System.out.println("DEBUG (TX Controller): Встановлено Валюта: " + currency);
        }


        // --- 2. КАТЕГОРІЯ (ChoiceBox) ---
        if (categoryChoice != null) {
            Long templateCatId = template.getCategory() != null ? template.getCategory().getId() : null;
            categoryChoice.getSelectionModel().clearSelection(); 

            if (templateCatId != null && !categoryChoice.getItems().isEmpty()) {
                System.out.println("DEBUG (TX Controller): Шукаємо Category ID: " + templateCatId); 

                categoryChoice.getItems().stream()
                     .filter(c -> c.getId() != null && c.getId().equals(templateCatId)) 
                     .findFirst()
                     .ifPresentOrElse(c -> {
                         categoryChoice.setValue(c);
                         System.out.println("DEBUG (TX Controller): Category успішно встановлено: " + c.getName()); 
                     }, () -> {
                         System.out.println("ERROR (TX Controller): Категорія з ID " + templateCatId + " не знайдена в ChoiceBox.");
                     });
            }
        }

        // --- 3. РАХУНОК (ChoiceBox) ---
        if (accountChoice != null) {
            Long templateAccId = template.getAccount() != null ? template.getAccount().getId() : null;
            accountChoice.getSelectionModel().clearSelection(); 

            if (templateAccId != null && !accountChoice.getItems().isEmpty()) {
                System.out.println("DEBUG (TX Controller): Шукаємо Account ID: " + templateAccId); 

                accountChoice.getItems().stream()
                     .filter(a -> a.getId() != null && a.getId().equals(templateAccId)) 
                     .findFirst()
                     .ifPresentOrElse(a -> {
                         accountChoice.setValue(a);
                         System.out.println("DEBUG (TX Controller): Account успішно встановлено: " + a.getName()); 
                     }, () -> {
                          System.out.println("ERROR (TX Controller): Рахунок з ID " + templateAccId + " не знайдено в ChoiceBox.");
                     });
            }
        }

        // --- 4. ПОЛЯ ПЕРІОДИЧНОСТІ ---

        // Тип періодичності
        if (recurringTypeChoice != null) {
            RecurringType recType = template.getRecurringType() != null ? template.getRecurringType() : TransactionTemplate.RecurringType.NONE;
            recurringTypeChoice.setValue(recType);
            updateRecurringFieldsVisibility(recType); // Оновлення видимості полів
            System.out.println("DEBUG (TX Controller): Встановлено Періодичність: " + recType);
        }

        // Інтервал
        if (recurrenceIntervalField != null) {
            String interval = template.getRecurrenceInterval() != null ? template.getRecurrenceInterval().toString() : "1";
            recurrenceIntervalField.setText(interval);
            System.out.println("DEBUG (TX Controller): Встановлено Інтервал: " + interval);
        }

        // День/Тиждень
        if (dayOrWeekField != null) {
            String day = template.getDayOfMonth() != null ? template.getDayOfMonth().toString() : ""; 
            dayOrWeekField.setText(day);
            System.out.println("DEBUG (TX Controller): Встановлено День місяця/Тижня: " + day);
        }

        // Дата початку
        if (startDatePicker != null) {
            LocalDate startDate = template.getStartDate() != null ? template.getStartDate() : LocalDate.now();
            startDatePicker.setValue(startDate);
            System.out.println("DEBUG (TX Controller): Встановлено Дата початку: " + startDate);
        }

                // --- 5. ДАТА ТА РЕЖИМ ---
        // Дата транзакції (завжди поточна при завантаженні шаблону)
        if (datePicker != null) datePicker.setValue(LocalDate.now());
        // Скидаємо режим редагування (шаблон завжди створює нову транзакцію)
        // setEditMode(false); // ВИДАЛІТЬ (або закоментуйте) ЦЕЙ РЯДОК
        // Оновлюємо мітку повідомлення
        if (messageLabel != null) messageLabel.setText("Форма заповнена шаблоном '" + template.getName() + "'.");
        System.out.println("*** DEBUG (TX Controller): Заповнення форми шаблоном завершено. ***\n");
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
            
            // ? ЗБЕРЕЖЕННЯ ПАРАМЕТРІВ ПЕРІОДИЧНОСТІ
            if (recurringTypeChoice != null) {
                t.setRecurringType(recurringTypeChoice.getValue());
                
                if (t.getRecurringType() != RecurringType.NONE) {
                    
                    // Рекурсивний інтервал
                    if (recurrenceIntervalField != null) {
                        int interval = (int) getDoubleFromField(recurrenceIntervalField.getText());
                        t.setRecurrenceInterval(Math.max(1, interval));
                    } else {
                        t.setRecurrenceInterval(1);
                    }
                    
                    // День місяця
                    if (dayOrWeekField != null && (t.getRecurringType() == RecurringType.MONTHLY || t.getRecurringType() == RecurringType.YEARLY)) {
                        int day = (int) getDoubleFromField(dayOrWeekField.getText());
                        t.setDayOfMonth(day > 0 ? day : null);
                    } else {
                        t.setDayOfMonth(null); // Не використовується
                    }
                    
                    // Дата початку
                    if (startDatePicker != null) {
                        t.setStartDate(startDatePicker.getValue());
                    } else {
                        t.setStartDate(LocalDate.now());
                    }
                } else {
                    // Якщо NONE, скидаємо рекурсивні поля
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
            String cleanAmountText = amountText.trim().replace(',', '.');
            double amount = Double.parseDouble(cleanAmountText); 
            
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
        if (selectedTransaction == null) {
            if (messageLabel != null) messageLabel.setText("Спочатку виберіть транзакцію для редагування.");
            return;
        }
        
        Transaction updatedTx = createTransactionFromForm();
        if (updatedTx == null) return;

        updatedTx.setId(selectedTransaction.getId());
        // Зберігаємо оригінальну дату створення
        updatedTx.setCreatedAt(selectedTransaction.getCreatedAt()); 
        updatedTx.setTemplateId(selectedTransaction.getTemplateId());
        
        try {
            // Використовуємо selectedTransaction як старий об'єкт для порівняння
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
        ApplicationSession.getInstance().login(user); // Перенаправлення на головну сторінку користувача
    }
}