package ua.kpi.personal.controller;

import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import ua.kpi.personal.model.Category;
import ua.kpi.personal.repo.CategoryDao;
import ua.kpi.personal.model.User;
import ua.kpi.personal.state.ApplicationSession;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.stream.Collectors;

public class CategoriesController {
    // FXML поля залишаються без змін
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
    private void initialize(){
        this.user = ApplicationSession.getInstance().getCurrentUser();
        typeChoice.getItems().addAll("EXPENSE", "INCOME");
        typeChoice.setValue("EXPENSE");

        listView.setCellFactory(this::createCategoryCellFactory);

        refresh();

        listView.getSelectionModel().selectedItemProperty().addListener((obs, oldV, newV) -> {
            if (newV == null) {
                if (editingCategory == null) {
                    toggleEditButtons(false);
                }
            } else {
                // Дозволяємо редагування/видалення лише користувацьких категорій
                // Системні категорії мають user_id = null
                boolean isUserCategory = newV.getUserId() != null; 
                toggleEditButtons(isUserCategory);
                
                // Якщо обрана системна категорія, вимикаємо кнопки
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

    /**
     * Оновлює список категорій, завантажуючи їх з бази даних та сортуючи 
     * для відображення ієрархічної структури.
     */
    private void refresh(){
        if (user != null) {
            List<Category> allCategories = categoryDao.findByUserId(user.getId());

            categoryMap = allCategories.stream()
                .collect(Collectors.toMap(Category::getId, category -> category));

            // ? ВИПРАВЛЕНО: НОВА ЛОГІКА СОРТУВАННЯ ДЛЯ ІЄРАРХІЧНОГО ВІДОБРАЖЕННЯ
            List<Category> sortedList = allCategories.stream()
                .sorted((c1, c2) -> {
                    // 1. СОРТУВАННЯ ЗА СИСТЕМНІСТЮ (System: user_id == null)
                    boolean isSystem1 = c1.getUserId() == null;
                    boolean isSystem2 = c2.getUserId() == null;

                    if (isSystem1 && !isSystem2) return -1;
                    if (!isSystem1 && isSystem2) return 1;

                    // 2. ГРУПУВАННЯ ЗА КОРЕНЕВОЮ КАТЕГОРІЄЮ (для ієрархії)
                    // Визначаємо ID кореневої (батьківської або своєї) категорії
                    Long rootId1 = c1.getParentId() != null ? c1.getParentId() : c1.getId();
                    Long rootId2 = c2.getParentId() != null ? c2.getParentId() : c2.getId();
                    
                    // Сортуємо за ID кореневої категорії (щоб "Їжа" (ID=1) йшла перед "Житло" (ID=4))
                    int rootCompare = rootId1.compareTo(rootId2);
                    if (rootCompare != 0) return rootCompare; 

                    // 3. ВНУТРІШНЄ СОРТУВАННЯ (Батьківська перед підкатегоріями)
                    // Кореневі (ParentId == null) завжди йдуть перед своїми підкатегоріями
                    if (c1.getParentId() == null && c2.getParentId() != null) return -1;
                    if (c1.getParentId() != null && c2.getParentId() == null) return 1;

                    // 4. Сортування за іменем (для підкатегорій в межах групи)
                    return c1.getName().compareTo(c2.getName());
                })
                .collect(Collectors.toList());

            listView.setItems(FXCollections.observableArrayList(sortedList));

            // Дозволяємо ЛИШЕ КОРЕНЕВІ КАТЕГОРІЇ (ParentId == null) бути батьківськими
            List<Category> parentOptions = allCategories.stream()
                .filter(c -> c.getParentId() == null) // Тільки кореневі
                .collect(Collectors.toList());

            Category selectedParent = parentChoice.getValue();
            parentChoice.getItems().clear();
            parentChoice.getItems().add(0, null); // Опція "Без батьківської"
            parentChoice.getItems().addAll(parentOptions);

            if (selectedParent != null && parentChoice.getItems().contains(selectedParent)) {
                parentChoice.setValue(selectedParent);
            } else {
                parentChoice.getSelectionModel().select(0);
            }

        } else {
            listView.setItems(FXCollections.emptyObservableList());
            System.err.println("User object is null in CategoriesController. Cannot refresh.");
        }

        onCancelEdit();
    }

    @FXML
    private void onAdd(){
        String name = nameField.getText();
        String type = typeChoice.getValue();
        Category parent = parentChoice.getValue();

        if(name==null || name.isBlank()){ messageLabel.setText("Назва обов'язкова"); return; }
        if(type==null){ messageLabel.setText("Тип обов'язковий"); return; }
        
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
                messageLabel.setText("Помилка оновлення категорії. Можливо, конфлікт даних.");
            }
        } else {
            // При створенні нової: якщо є батьківська, тип успадковується
            if (parent != null) {
                type = parent.getType();
                typeChoice.setValue(type); // Встановлюємо в формі
            }
            
            Category newCategory = new Category(user.getId(), name, type, parentId);
            
            Category created = categoryDao.create(newCategory);
            
            if(created!=null){
                messageLabel.setText("Додана категорія: " + created.getName());
                refresh();
            } else {
                messageLabel.setText("Помилка збереження в базі даних.");
            }
        }
    }

    @FXML
    private void onEdit() {
        Category selectedCategory = listView.getSelectionModel().getSelectedItem();
        if (selectedCategory == null) {
            messageLabel.setText("Виберіть категорію для редагування.");
            return;
        }

        if (selectedCategory.getUserId() == null) {
            messageLabel.setText("Системні категорії не можна редагувати.");
            return;
        }

        editingCategory = selectedCategory;

        nameField.setText(selectedCategory.getName());
        typeChoice.setValue(selectedCategory.getType());
        
        boolean hasChildren = listView.getItems().stream().anyMatch(c -> selectedCategory.getId().equals(c.getParentId()));

        // Вимикаємо вибір типу, якщо категорія має дітей або є підкатегорією
        typeChoice.setDisable(selectedCategory.getParentId() != null || hasChildren);

        if (selectedCategory.getParentId() != null) {
            Category parent = categoryMap.get(selectedCategory.getParentId());
            if (parent != null) {
                parentChoice.setValue(parent);
            }
        } else {
            parentChoice.getSelectionModel().select(0); // Опція "Без батьківської"
        }
        
        // Блокуємо вибір батьківської категорії, якщо категорія має підкатегорії
        parentChoice.setDisable(hasChildren); 

        addButton.setText("Оновити");
        messageLabel.setText("Редагування: " + selectedCategory.getName() + ". Натисніть 'Оновити' для збереження.");
        cancelEditBtn.setDisable(false);
    }

    @FXML
    private void onCancelEdit() {
        editingCategory = null;
        nameField.clear();

        if (typeChoice != null) {
            typeChoice.setValue("EXPENSE");
            typeChoice.setDisable(false);
        }

        if (parentChoice != null) {
            parentChoice.getSelectionModel().select(0);
            parentChoice.setDisable(false);
        }

        addButton.setText("Додати нову");
        messageLabel.setText("Готовий до додавання нової категорії.");
        if (listView != null) {
            listView.getSelectionModel().clearSelection();
        }
        toggleEditButtons(false);
    }

    @FXML
    private void onDelete() {
        Category selectedCategory = listView.getSelectionModel().getSelectedItem();
        if (selectedCategory == null) {
            messageLabel.setText("Виберіть категорію для видалення.");
            return;
        }

        if (selectedCategory.getUserId() == null) {
            messageLabel.setText("Системні категорії не можна видаляти.");
            return;
        }

        // Перевірка на наявність підкатегорій
        boolean hasChildren = listView.getItems().stream()
                                     .anyMatch(c -> selectedCategory.getId().equals(c.getParentId()));
        if (hasChildren) {
             messageLabel.setText("Спочатку видаліть усі підкатегорії.");
             return;
        }
        

        if (categoryDao.delete(selectedCategory.getId())) {
            messageLabel.setText("Категорія '" + selectedCategory.getName() + "' видалена.");
            refresh();
        } else {
            // Це зазвичай означає, що категорія пов'язана з транзакціями
            messageLabel.setText("Помилка видалення категорії. Перевірте, чи немає пов'язаних транзакцій.");
        }
    }


    @FXML
    private void onBack() throws IOException {
        ApplicationSession.getInstance().login(user); 
    }
    
    // --- Логіка відображення іконок (Коди FontAwesome) ---
    // Ці коди відповідають символам у FontAwesome Solid (900)
    private String getIconForSystemCategory(Long categoryId) {
        return switch (categoryId.intValue()) {
            case 1 -> "\uf0d6"; // Gift/Price Tag - тег (ваші "Подарунки")
            case 2 -> "\uf2e7"; // Restaurant / Utensils - Їжа
            case 3 -> "\uf1b9"; // Car - Авто
            case 4 -> "\uf015"; // Home - Дім/Житло
            case 5 -> "\uf155"; // Dollar Sign / Money - Зарплата/Гроші
            case 6 -> "\uf291"; // Shopping Bag - Покупки
            case 7 -> "\uf19c"; // Building/Bank - Офіс/Банк
            case 8 -> "\uf09d"; // Credit Card - Картка
            case 9 -> "\uf53a"; // Hand-holding dollar - Гроші
            default -> "\uf111"; // Circle
        };
    }
    
    // --- ФАБРИКА КЛІТИНОК ---
    private ListCell<Category> createCategoryCellFactory(ListView<Category> listView) {
        return new ListCell<Category>() {
            @Override
            protected void updateItem(Category category, boolean empty) {
                super.updateItem(category, empty);
                setText(null);
                setGraphic(null);

                if (empty || category == null) {
                    return;
                }

                HBox box = new HBox(10);
                box.setPadding(new Insets(5, 5, 5, 5));
                
                // Визначаємо тип категорії
                // Використовуємо category.getUserId() == null для системних категорій
                boolean isSystemRoot = category.getUserId() == null && category.getParentId() == null; 
                boolean isSubCategory = category.getParentId() != null;
                
                // 1. ІКОНКА (Код FontAwesome або простий символ)
                String icon;
                if (isSystemRoot) {
                    icon = getIconForSystemCategory(category.getId());
                } else {
                    // Використовуємо прості символи для користувацьких підкатегорій
                    icon = isSubCategory ? "\u25B8" : "\u25CF"; // ? або ?
                }
                
                Label iconLabel = new Label(icon);
                
                // --- СТИЛІЗАЦІЯ ІКОНКИ ЧЕРЕЗ CSS ---
                iconLabel.getStyleClass().add("category-icon");
                
                // ? ДОДАНО: Клас для FontAwesome для коректного відображення іконок
                iconLabel.getStyleClass().add("fa-icon"); 

                if (category.getType().equals("INCOME")) {
                    iconLabel.getStyleClass().add("income-icon");
                } else {
                    iconLabel.getStyleClass().add("expense-icon");
                }
                
                // 2. НАЗВА
                Label nameLabel = new Label(category.getName());
                HBox.setHgrow(nameLabel, Priority.ALWAYS);

                // 3. ТИП (EXPENSE/INCOME)
                Label typeLabel = new Label("[" + category.getType() + "]");
                typeLabel.getStyleClass().add("category-type-label");
                
                // 4. СТИЛІЗАЦІЯ ТА ВІДСТУПИ (КОНТЕЙНЕРИ)
                if (isSystemRoot) {
                    box.getStyleClass().add("system-category-box");
                } else if (isSubCategory) {
                    // Збільшуємо відступ для підкатегорії
                    box.setPadding(new Insets(5, 5, 5, 30)); 
                    nameLabel.getStyleClass().add("subcategory-name-label");
                }
                
                box.getChildren().addAll(iconLabel, nameLabel, typeLabel);
                setGraphic(box);
                
                // Блокуємо вибір системних кореневих категорій для редагування/видалення
                setDisable(isSystemRoot && editingCategory == null); 
            }
        };
    }
}
