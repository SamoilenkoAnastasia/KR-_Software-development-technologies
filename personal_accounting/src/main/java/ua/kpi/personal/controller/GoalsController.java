package ua.kpi.personal.controller;

import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import ua.kpi.personal.model.Account;
import ua.kpi.personal.model.Goal;
import ua.kpi.personal.model.User;
import ua.kpi.personal.repo.AccountDao;
import ua.kpi.personal.repo.GoalDao;
import ua.kpi.personal.repo.TransactionDao;
import ua.kpi.personal.processor.TransactionProcessor;
import ua.kpi.personal.processor.BalanceCheckDecorator;
import ua.kpi.personal.processor.CurrencyDecorator;
import ua.kpi.personal.service.GoalService;
import ua.kpi.personal.state.ApplicationSession;
import ua.kpi.personal.service.ExchangeRateService; // ? Новий імпорт
import java.io.IOException;
import java.util.Date;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors; 

public class GoalsController {
    
    // UI Елементи для Створення Цілі
    @FXML private TextField newGoalName;
    @FXML private TextField newGoalTargetAmount;
    @FXML private ChoiceBox<String> newGoalCurrency;
    @FXML private DatePicker newGoalDeadline;
    @FXML private CheckBox isFamilyFundCheckbox;

    // UI Елементи для Внеску
    @FXML private ListView<Goal> goalsListView;
    @FXML private TextField contributionAmount;
    @FXML private ChoiceBox<Account> sourceAccountChoice;
    @FXML private Label progressLabel;
    
    @FXML private Label messageLabel;
    @FXML private Button backBtn;

    // Сервіси та Дані
    private GoalService goalService;
    private User user;
    private final GoalDao goalDao = new GoalDao();
    private final AccountDao accountDao = new AccountDao();
    private final TransactionProcessor transactionProcessor;
    private final ExchangeRateService exchangeRateService; // ? ДОДАНО: Сервіс для курсів

    // ===============================================
    // 				КОНСТРУКТОР
    // ===============================================

    public GoalsController() {
        this.exchangeRateService = new ExchangeRateService(); // ? Ініціалізація сервісу
        
        TransactionProcessor baseProcessor = new TransactionDao();
         
        // ? ВИПРАВЛЕНО: Передаємо exchangeRateService у CurrencyDecorator
        TransactionProcessor currencyProcessor = new CurrencyDecorator(baseProcessor, this.exchangeRateService); 
        
        TransactionProcessor balanceProcessor = new BalanceCheckDecorator(currencyProcessor);
        this.transactionProcessor = balanceProcessor; // Не використовуємо UiNotificationDecorator, як у Dashboard
    }

    // ? ЗМІНЕНО: initialize має бути public або package-private для FXML
    @FXML
    public void initialize(){
        this.user = ApplicationSession.getInstance().getCurrentUser();
        this.goalService = new GoalService(goalDao, accountDao, transactionProcessor);
        newGoalCurrency.getItems().addAll("UAH", "USD", "EUR");
        refreshData();
    }
    
    private void refreshData() {
        if (user == null) return;
        
        // 1. Оновлення списку цілей
        goalsListView.setItems(
            FXCollections.observableArrayList(goalService.getAllGoals(user))
        );
        
        // Додамо логіку відображення прогресу при виборі цілі
        goalsListView.getSelectionModel().selectedItemProperty().addListener((obs, oldV, newV) -> {
            if (newV != null) {
                updateProgressDisplay(newV);
                
                // ЛОГІКА ФІЛЬТРАЦІЇ ДЛЯ СІМЕЙНОГО ФОНДУ ТА КОНВЕРТАЦІЇ
                sourceAccountChoice.setItems(
                    FXCollections.observableArrayList(
                        accountDao.findByUserId(user.getId()).stream()
                            .filter(acc -> 
                                // Умова 1: Це сімейний фонд (дозволяємо більше свободи)
                                newV.getIsFamilyFund() || 
                                // Умова 2: Валюти збігаються (прямий переказ)
                                acc.getCurrency().equals(newV.getCurrency()) ||
                                // Умова 3: Дозволена конвертація (для UAH, USD, EUR)
                                (isCurrencyConvertible(acc.getCurrency(), newV.getCurrency()))
                            )
                            .collect(Collectors.toList())
                    )
                );
                
                // Встановлюємо перший елемент як вибраний за замовчуванням
                if (!sourceAccountChoice.getItems().isEmpty()) {
                    sourceAccountChoice.getSelectionModel().selectFirst();
                } else {
                    sourceAccountChoice.getSelectionModel().clearSelection(); 
                }
            }
        });
        
        // 2. Початкове оновлення списку рахунків (без фільтрації)
        sourceAccountChoice.setItems(
             FXCollections.observableArrayList(accountDao.findByUserId(user.getId()))
        );
    }
    
    // ДОПОМІЖНИЙ МЕТОД: Логіка перевірки, чи можлива конвертація
    private boolean isCurrencyConvertible(String source, String target) {
        if (source.equals(target)) return true;
        
        // Дозволяємо конвертацію між UAH, USD та EUR
        boolean isSupportedPair = 
            ("UAH".equals(source) && ("USD".equals(target) || "EUR".equals(target))) ||
            ("USD".equals(source) && "UAH".equals(target)) ||
            ("EUR".equals(source) && "UAH".equals(target));
            
        return isSupportedPair;
    }
    
    // ... (updateProgressDisplay залишається без змін) ...
    private void updateProgressDisplay(Goal goal) {
        double current = goal.getCurrentAmount();
        double target = goal.getTargetAmount();
        double progress = (target > 0) ? (current / target) : 0.0;
        long daysLeft = -1;
        if (goal.getDeadline() != null) {
            long diff = goal.getDeadline().getTime() - new Date().getTime();
            daysLeft = TimeUnit.DAYS.convert(diff, TimeUnit.MILLISECONDS);
        }

        String status = String.format("Прогрес: %.2f / %.2f %s (%.1f%%)", 
                                      current, target, goal.getCurrency(), progress * 100);
        
        if (current >= target) {
             status = "? Ціль досягнута! " + status;
        } else if (daysLeft >= 0) {
            status += String.format(" | Залишилось днів: %d", daysLeft);
        } else if (goal.getDeadline() != null && daysLeft < 0) {
             status += " | Термін вийшов!";
        }

        progressLabel.setText(status);
    }
    
    // ? ЗМІНЕНО: onCreateGoal має бути public для FXML (виправлення попередньої помилки)
    @FXML
    public void onCreateGoal() {
        try {
            if (newGoalName.getText().trim().isEmpty() || newGoalTargetAmount.getText().trim().isEmpty() || newGoalCurrency.getValue() == null) {
                throw new IllegalArgumentException("Заповніть всі обов'язкові поля.");
            }
            
            Goal goal = new Goal();
            goal.setName(newGoalName.getText());
            goal.setTargetAmount(Double.parseDouble(newGoalTargetAmount.getText()));
            goal.setCurrency(newGoalCurrency.getValue());
            goal.setDeadline(
                newGoalDeadline.getValue() != null ? 
                java.sql.Date.valueOf(newGoalDeadline.getValue()) : null
            );
            goal.setIsFamilyFund(isFamilyFundCheckbox.isSelected());

            goalService.createGoal(goal, user);
            messageLabel.setText("? Ціль успішно створена: " + goal.getName());
            clearNewGoalFields();
            refreshData();
        } catch (NumberFormatException e) {
            messageLabel.setText("? Помилка: Некоректна сума цілі. Використовуйте числа.");
        } catch (IllegalArgumentException e) {
             messageLabel.setText("? Помилка: " + e.getMessage());
        }
    }

    @FXML
    public void onContribute() {
        Goal selectedGoal = goalsListView.getSelectionModel().getSelectedItem();
        Account sourceAccount = sourceAccountChoice.getValue();
        
        if (selectedGoal == null) { messageLabel.setText("? Виберіть ціль."); return; }
        if (sourceAccount == null) { messageLabel.setText("? Виберіть рахунок для списання."); return; }

        try {
            double amount = Double.parseDouble(contributionAmount.getText());
            if (amount <= 0) {
                throw new IllegalArgumentException("Сума внеску має бути більше нуля.");
            }
            
            // Викликаємо сервіс, який використовує TransactionProcessor (Decorator Chain)
            goalService.contributeToGoal(selectedGoal.getId(), sourceAccount.getId(), amount, user);
            
            // Повідомлення про успіх
            messageLabel.setText(String.format("? Успішний внесок у %s на суму %.2f %s.", 
                                               selectedGoal.getName(), amount, sourceAccount.getCurrency()));
            
            contributionAmount.clear();
            refreshData();
            
            // Оновлюємо відображення прогресу для вибраної цілі
            Goal updatedGoal = goalService.getAllGoals(user).stream()
                 .filter(g -> g.getId().equals(selectedGoal.getId())).findFirst().orElse(null);
            if (updatedGoal != null) {
                 updateProgressDisplay(updatedGoal);
            }

        } catch (NumberFormatException e) {
            messageLabel.setText("? Помилка: Некоректна сума внеску.");
        } catch (Exception e) {
             // Це спіймає RuntimeException від декоратора
            messageLabel.setText("? Помилка внеску: " + e.getMessage());
        }
    }
    
    private void clearNewGoalFields() {
        newGoalName.clear();
        newGoalTargetAmount.clear();
        newGoalDeadline.setValue(null);
        isFamilyFundCheckbox.setSelected(false);
    }

    // ? ЗМІНЕНО: onBack має бути public для FXML
    @FXML
    public void onBack() throws IOException {
        ApplicationSession.getInstance().login(user);
    }
}
