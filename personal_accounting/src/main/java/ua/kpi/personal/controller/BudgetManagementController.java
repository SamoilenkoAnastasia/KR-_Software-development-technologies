package ua.kpi.personal.controller;

import ua.kpi.personal.model.User;
import ua.kpi.personal.model.access.BudgetAccess;
import ua.kpi.personal.model.access.SharedBudget;
import ua.kpi.personal.repo.UserDao;
import ua.kpi.personal.service.BudgetAccessService;
import ua.kpi.personal.state.ApplicationSession;
import ua.kpi.personal.util.Alerts; // Використання класу сповіщень

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import java.util.List;

public class BudgetManagementController {

    // FXML-елементи
    @FXML private ChoiceBox<SharedBudget> budgetsChoiceBox;
    @FXML private TextField budgetNameField;
    @FXML private ListView<BudgetAccess> membersListView;
    @FXML private TextField memberIdentifierField;
    @FXML private ChoiceBox<String> memberRoleChoiceBox;
    @FXML private Label statusLabel;
    @FXML private Button addMemberButton;
    @FXML private Button updateRoleButton;
    @FXML private Button removeMemberButton;

    // Сервіси та DAO
    private final ApplicationSession session = ApplicationSession.getInstance();
    private final BudgetAccessService budgetAccessService = session.getBudgetAccessService();
    // Припускаємо, що UserDao.findById(Long) існує
    private final UserDao userDao = new UserDao(); 
    
    // Дані
    private final ObservableList<BudgetAccess> membersList = FXCollections.observableArrayList();
    private final ObservableList<String> rolesList = FXCollections.observableArrayList(
            BudgetAccess.ROLE_OWNER, BudgetAccess.ROLE_EDITOR, BudgetAccess.ROLE_VIEWER
    );

    @FXML
    public void initialize() {
        // 1. Ініціалізація вибору ролей
        memberRoleChoiceBox.setItems(rolesList);
        memberRoleChoiceBox.getSelectionModel().select(BudgetAccess.ROLE_VIEWER); // Дефолтна роль
        
        // 2. Налаштування ListView для членів
        membersListView.setItems(membersList);
        membersListView.setCellFactory(lv -> new ListCell<BudgetAccess>() {
            @Override
            protected void updateItem(BudgetAccess item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                } else {
                    // Знаходимо ім'я користувача
                    User user = userDao.findById(item.getUserId()); 
                    String userName = (user != null) ? user.getUsername() : "Невідомий користувач";
                    setText(String.format("%s (%s)", userName, item.getAccessRole()));
                }
            }
        });

        // 3. Завантаження бюджетів
        loadBudgets();
        
        // 4. Обробник зміни обраного бюджету
        budgetsChoiceBox.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal != null) {
                handleBudgetSelection(newVal);
            }
        });
        
        // 5. Обробник вибору члена списку (для кнопок редагування)
        membersListView.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> {
            boolean isSelected = newVal != null;
            // Перевіряємо, чи поточний користувач може керувати доступом
            boolean canManage = session.getCurrentBudgetAccessState().canManageUsers();

            updateRoleButton.setDisable(!isSelected || !canManage);
            removeMemberButton.setDisable(!isSelected || !canManage);

            // Якщо вибраний член є власником, не дозволяємо видаляти
            if (isSelected && newVal.getAccessRole().equals(BudgetAccess.ROLE_OWNER)) {
                SharedBudget activeBudget = budgetsChoiceBox.getSelectionModel().getSelectedItem();
                if (activeBudget != null && activeBudget.getOwnerId().equals(newVal.getUserId())) {
                    removeMemberButton.setDisable(true);
                }
            }
        });
    }

   
    private void loadBudgets() {
        Long userId = session.getCurrentUser().getId();
        if (userId == null) return;
        
        List<SharedBudget> budgets = session.getSharedBudgetDao().findBudgetsByUserId(userId);
        budgetsChoiceBox.setItems(FXCollections.observableArrayList(budgets));
        
        // Знаходимо і встановлюємо поточний активний бюджет
        SharedBudget currentActive = budgets.stream()
                .filter(b -> b.getId().equals(session.getCurrentBudgetId()))
                .findFirst()
                .orElse(null);
        
        if (currentActive != null) {
            budgetsChoiceBox.getSelectionModel().select(currentActive);
        } else if (!budgets.isEmpty()) {
            // Якщо активний бюджет не знайдено (наприклад, після логіна), обираємо перший
            budgetsChoiceBox.getSelectionModel().select(0);
        }
    }

    
    private void handleBudgetSelection(SharedBudget selectedBudget) {
        // 1. Оновлюємо сесію, що запускає визначення прав
        budgetAccessService.switchActiveBudget(selectedBudget.getId());
        
        // 2. Оновлюємо список членів
        updateMembersList(selectedBudget.getId());
        
        // 3. Оновлюємо стан елементів керування відповідно до прав
        updateControlsState();
        
        statusLabel.setText("Обрано: " + selectedBudget.getName() + 
                            " (Ваша роль: " + session.getCurrentBudgetAccessState().getDisplayRole() + ")");
    }
    
 
    private void updateMembersList(Long budgetId) {
        membersList.clear();
        
        // Приватний бюджет (ID користувача) має лише одного "члена" - власника
        if (budgetId.equals(session.getCurrentUser().getId())) {
            // Створюємо віртуальний запис доступу
            BudgetAccess selfAccess = new BudgetAccess();
            selfAccess.setBudgetId(budgetId);
            selfAccess.setUserId(budgetId);
            selfAccess.setAccessRole(BudgetAccess.ROLE_OWNER);
            membersList.add(selfAccess);
        } else {
            // ? ВИПРАВЛЕНО: Викликаємо метод findMembersByBudgetId безпосередньо в сервісі.
            membersList.addAll(budgetAccessService.findMembersByBudgetId(budgetId));
        }
    }
    
    /**
     * Оновлює доступність кнопок відповідно до прав поточного користувача.
     */
    private void updateControlsState() {
        boolean canManage = session.getCurrentBudgetAccessState().canManageUsers();
        
        // Ці елементи потрібні лише для управління спільним бюджетом
        boolean isSharedBudget = !session.getCurrentBudgetId().equals(session.getCurrentUser().getId());

        memberIdentifierField.setDisable(!canManage || !isSharedBudget);
        memberRoleChoiceBox.setDisable(!canManage || !isSharedBudget);
        addMemberButton.setDisable(!canManage || !isSharedBudget);
        
        // Кнопки редагування членів за замовчуванням вимкнені, доки не буде обрано члена
        updateRoleButton.setDisable(true);
        removeMemberButton.setDisable(true);
    }


    // =======================================================
    // FXML Обробники Подій
    // =======================================================

    @FXML
    private void createBudget() {
        String name = budgetNameField.getText().trim();
        if (name.isEmpty()) {
            Alerts.showError("Помилка", "Назва бюджету не може бути порожньою.");
            return;
        }

        SharedBudget newBudget = budgetAccessService.createSharedBudget(name, session.getCurrentUser().getId());
        
        if (newBudget != null) {
            budgetNameField.clear();
            statusLabel.setText("Спільний бюджет '" + newBudget.getName() + "' успішно створено!");
            loadBudgets(); 
        } else {
            Alerts.showError("Помилка", "Не вдалося створити бюджет.");
        }
    }

    @FXML
    private void addMember() {
        Long currentBudgetId = session.getCurrentBudgetId();
        String identifier = memberIdentifierField.getText().trim();
        String role = memberRoleChoiceBox.getSelectionModel().getSelectedItem();
        
        if (currentBudgetId == null || identifier.isEmpty() || role == null) {
            Alerts.showWarning("Попередження", "Оберіть бюджет та введіть логін або email.");
            return;
        }
        
        // 1. Знаходимо користувача за логіном або email
        User targetUser = userDao.findByUsernameOrEmail(identifier);
        
        if (targetUser == null) {
            Alerts.showError("Помилка", "Користувача з таким логіном/email не знайдено.");
            return;
        }
        
        if (targetUser.getId().equals(session.getCurrentUser().getId())) {
             Alerts.showWarning("Попередження", "Ви вже є членом цього бюджету (власником).");
            return;
        }

        // 2. Додаємо/Оновлюємо доступ
        if (budgetAccessService.addOrUpdateMember(currentBudgetId, targetUser.getId(), role)) {
            memberIdentifierField.clear();
            statusLabel.setText("Користувач " + targetUser.getUsername() + " успішно доданий/оновлений як " + role + ".");
            updateMembersList(currentBudgetId);
        } else {
            Alerts.showError("Помилка", "Не вдалося додати члена. Перевірте ваші права доступу.");
        }
    }

    @FXML
    private void removeMember() {
        BudgetAccess selectedAccess = membersListView.getSelectionModel().getSelectedItem();
        Long currentBudgetId = session.getCurrentBudgetId();
        
        if (selectedAccess == null || currentBudgetId == null) {
            return;
        }
        
        Long targetUserId = selectedAccess.getUserId();

        // Додаткова перевірка, щоб не видалити себе (власника)
        if (currentBudgetId.equals(targetUserId)) {
            Alerts.showError("Помилка", "Ви не можете видалити власника бюджету.");
            return;
        }
        
        if (Alerts.showConfirmation("Підтвердження", 
                                    "Ви впевнені, що хочете видалити цього члена з бюджету?")) {
            
            if (budgetAccessService.removeMember(currentBudgetId, targetUserId)) {
                statusLabel.setText("Член успішно видалений.");
                updateMembersList(currentBudgetId);
            } else {
                Alerts.showError("Помилка", "Не вдалося видалити члена.");
            }
        }
    }

    @FXML
    private void updateRole() {
        BudgetAccess selectedAccess = membersListView.getSelectionModel().getSelectedItem();
        Long currentBudgetId = session.getCurrentBudgetId();
        String newRole = memberRoleChoiceBox.getSelectionModel().getSelectedItem();

        if (selectedAccess == null || currentBudgetId == null || newRole == null) {
            Alerts.showWarning("Попередження", "Оберіть члена та нову роль.");
            return;
        }
        
        Long targetUserId = selectedAccess.getUserId();

        // Оновлення ролі
        if (budgetAccessService.addOrUpdateMember(currentBudgetId, targetUserId, newRole)) {
            statusLabel.setText("Роль члена успішно оновлена на " + newRole + ".");
            updateMembersList(currentBudgetId);
        } else {
            Alerts.showError("Помилка", "Не вдалося оновити роль.");
        }
    }
}