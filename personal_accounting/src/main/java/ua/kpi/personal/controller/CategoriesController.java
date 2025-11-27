package ua.kpi.personal.controller;

import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import ua.kpi.personal.model.Category;
import ua.kpi.personal.model.User;
import ua.kpi.personal.repo.CategoryDao;
import ua.kpi.personal.state.ApplicationSession;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class CategoriesController {
    @FXML private ListView<Category> listView;
    @FXML private TextField nameField;
    @FXML private ChoiceBox<String> typeChoice;
    @FXML private ChoiceBox<Category> parentChoice;
    @FXML private Label messageLabel;
    @FXML private Button backBtn;
    @FXML private Button addButton;
    @FXML private Button editButton;
    @FXML private Button deleteButton;
    @FXML private Button cancelEditBtn;

    private Category editingCategory = null;
    private final CategoryDao categoryDao = new CategoryDao();
    private User user;
    private Map<Long, Category> categoryMap = new HashMap<>();

    @FXML
    private void initialize() {
        this.user = ApplicationSession.getInstance().getCurrentUser();
        typeChoice.getItems().addAll("EXPENSE", "INCOME");
        typeChoice.setValue("EXPENSE");

        listView.setCellFactory(this::createCategoryCellFactory);
        refresh();

        listView.getSelectionModel().selectedItemProperty().addListener((obs, oldV, newV) -> {
            if (newV == null) {
                if (editingCategory == null) toggleEditButtons(false);
            } else {
                boolean isUserCategory = newV.getUserId() != null;
                toggleEditButtons(isUserCategory);
                if (!isUserCategory) {
                    editButton.setDisable(true);
                    deleteButton.setDisable(true);
                }
            }
        });
    }

    private void toggleEditButtons(boolean selected) {
        editButton.setDisable(!selected);
        deleteButton.setDisable(!selected);
        cancelEditBtn.setDisable(editingCategory == null);
    }

    private void refresh() {
        if (user != null) {
            List<Category> allCategories = categoryDao.findByUserId(user.getId());
            categoryMap = allCategories.stream()
                    .collect(Collectors.toMap(Category::getId, c -> c));

            List<Category> sortedList = allCategories.stream()
                    .sorted((c1, c2) -> {
                        boolean isSystem1 = c1.getUserId() == null;
                        boolean isSystem2 = c2.getUserId() == null;
                        if (isSystem1 && !isSystem2) return -1;
                        if (!isSystem1 && isSystem2) return 1;
                        Long rootId1 = c1.getParentId() != null ? c1.getParentId() : c1.getId();
                        Long rootId2 = c2.getParentId() != null ? c2.getParentId() : c2.getId();
                        int rootCompare = rootId1.compareTo(rootId2);
                        if (rootCompare != 0) return rootCompare;
                        if (c1.getParentId() == null && c2.getParentId() != null) return -1;
                        if (c1.getParentId() != null && c2.getParentId() == null) return 1;
                        return c1.getName().compareTo(c2.getName());
                    })
                    .collect(Collectors.toList());

            listView.setItems(FXCollections.observableArrayList(sortedList));

            List<Category> parentOptions = allCategories.stream()
                    .filter(c -> c.getParentId() == null)
                    .collect(Collectors.toList());

            Category selectedParent = parentChoice.getValue();
            parentChoice.getItems().clear();
            parentChoice.getItems().add(0, null);
            parentChoice.getItems().addAll(parentOptions);
            if (selectedParent != null && parentChoice.getItems().contains(selectedParent)) {
                parentChoice.setValue(selectedParent);
            } else parentChoice.getSelectionModel().select(0);

        } else {
            listView.setItems(FXCollections.emptyObservableList());
            System.err.println("User object is null in CategoriesController. Cannot refresh.");
        }
        onCancelEdit();
    }

    @FXML
    private void onAdd() {
        String name = nameField.getText();
        String type = typeChoice.getValue();
        Category parent = parentChoice.getValue();
        if (name == null || name.isBlank()) { messageLabel.setText("Назва обов'язкова"); return; }
        if (type == null) { messageLabel.setText("Тип обов'язковий"); return; }
        Long parentId = (parent != null) ? parent.getId() : null;

        if (editingCategory != null) {
            if (editingCategory.getUserId() == null) {
                messageLabel.setText("Системні категорії не можна оновлювати.");
                onCancelEdit();
                return;
            }
            if (parent != null && !parent.getType().equals(type)) {
                messageLabel.setText("Тип підкатегорії має збігатися з типом батьківської категорії.");
                return;
            }
            Category updatedCategory = editingCategory.withUpdate(name, type, parentId);
            if (categoryDao.update(updatedCategory)) {
                messageLabel.setText("Категорія оновлена: " + updatedCategory.getName());
                refresh();
            } else {
                messageLabel.setText("Помилка оновлення категорії.");
            }
        } else {
            if (parent != null) {
                type = parent.getType();
                typeChoice.setValue(type);
            }
            Category newCategory = new Category(user.getId(), name, type, parentId);
            Category created = categoryDao.create(newCategory);
            if (created != null) {
                messageLabel.setText("Додана категорія: " + created.getName());
                refresh();
            } else messageLabel.setText("Помилка збереження в базі даних.");
        }
    }

    @FXML
    private void onEdit() {
        Category selectedCategory = listView.getSelectionModel().getSelectedItem();
        if (selectedCategory == null) { messageLabel.setText("Виберіть категорію для редагування."); return; }
        if (selectedCategory.getUserId() == null) { messageLabel.setText("Системні категорії не можна редагувати."); return; }
        editingCategory = selectedCategory;
        nameField.setText(selectedCategory.getName());
        typeChoice.setValue(selectedCategory.getType());
        boolean hasChildren = listView.getItems().stream().anyMatch(c -> selectedCategory.getId().equals(c.getParentId()));
        typeChoice.setDisable(selectedCategory.getParentId() != null || hasChildren);
        if (selectedCategory.getParentId() != null) {
            Category parent = categoryMap.get(selectedCategory.getParentId());
            if (parent != null) parentChoice.setValue(parent);
        } else parentChoice.getSelectionModel().select(0);
        parentChoice.setDisable(hasChildren);
        addButton.setText("Оновити");
        messageLabel.setText("Редагування: " + selectedCategory.getName() + ". Натисніть 'Оновити' для збереження.");
        cancelEditBtn.setDisable(false);
    }

    @FXML
    private void onCancelEdit() {
        editingCategory = null;
        nameField.clear();
        typeChoice.setValue("EXPENSE");
        typeChoice.setDisable(false);
        parentChoice.getSelectionModel().select(0);
        parentChoice.setDisable(false);
        addButton.setText("Додати нову");
        messageLabel.setText("Готовий до додавання нової категорії.");
        if (listView != null) listView.getSelectionModel().clearSelection();
        toggleEditButtons(false);
    }

    @FXML
    private void onDelete() {
        Category selectedCategory = listView.getSelectionModel().getSelectedItem();
        if (selectedCategory == null) { messageLabel.setText("Виберіть категорію для видалення."); return; }
        if (selectedCategory.getUserId() == null) { messageLabel.setText("Системні категорії не можна видаляти."); return; }
        boolean hasChildren = listView.getItems().stream().anyMatch(c -> selectedCategory.getId().equals(c.getParentId()));
        if (hasChildren) { messageLabel.setText("Спочатку видаліть усі підкатегорії."); return; }
        if (categoryDao.delete(selectedCategory.getId())) {
            messageLabel.setText("Категорія '" + selectedCategory.getName() + "' видалена.");
            refresh();
        } else messageLabel.setText("Помилка видалення категорії.");
    }

    @FXML
    private void onBack() throws IOException {
        ApplicationSession.getInstance().login(user);
    }

    private String getIconForSystemCategory(Long categoryId) {
        return switch (categoryId.intValue()) {
            case 1 -> "\uD83C\uDF7D"; // Їжа та напої 
            case 2 -> "\uD83D\uDE97"; // Транспорт 
            case 3 -> "\uD83D\uDCB0"; // Комунальні платежі 
            case 4 -> "\u260E";       // Зв'язок 
            case 5 -> "\uD83C\uDFB5"; // Розваги 
            case 6 -> "\uD83D\uDED2"; // Покупки 
            case 7 -> "\uD83C\uDFE0"; // Житло 
            case 8 -> "\uD83D\uDCB3"; // Фінансові витрати 
            case 9 -> "\uD83D\uDCB5"; // Дохід 
            default -> "\u25CF";       // Коло 
        };
    }

    private ListCell<Category> createCategoryCellFactory(ListView<Category> listView) {
        return new ListCell<>() {
            @Override
            protected void updateItem(Category category, boolean empty) {
                super.updateItem(category, empty);
                setText(null);
                setGraphic(null);
                if (empty || category == null) return;

                HBox box = new HBox(10);
                box.setPadding(new Insets(5, 5, 5, 5));
                boolean isSystemRoot = category.getUserId() == null && category.getParentId() == null;
                boolean isSubCategory = category.getParentId() != null;

                Label iconLabel = new Label(isSystemRoot ? getIconForSystemCategory(category.getId()) : (isSubCategory ? "\u25B8" : "\u25CF"));
                iconLabel.setStyle(category.getType().equals("INCOME") ? "-fx-text-fill: #48B95A;" : "-fx-text-fill: #D74646;");

                Label nameLabel = new Label(category.getName());
                HBox.setHgrow(nameLabel, Priority.ALWAYS);

                Label typeLabel = new Label("[" + category.getType() + "]");
                typeLabel.getStyleClass().add("category-type-label");

                if (isSystemRoot) box.getStyleClass().add("system-category-box");
                else if (isSubCategory) {
                    box.setPadding(new Insets(5, 5, 5, 30));
                    nameLabel.getStyleClass().add("subcategory-name-label");
                }

                box.getChildren().addAll(iconLabel, nameLabel, typeLabel);
                setGraphic(box);
            }
        };
    }
}
