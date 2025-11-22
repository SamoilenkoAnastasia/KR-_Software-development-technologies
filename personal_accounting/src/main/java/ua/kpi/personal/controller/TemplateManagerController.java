package ua.kpi.personal.controller;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.stage.Stage;
import ua.kpi.personal.model.TransactionTemplate;
import ua.kpi.personal.repo.TemplateDao;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

public class TemplateManagerController {
    @FXML private ListView<TransactionTemplate> templateListView;
    @FXML private TextField searchField; 
    
    private final TemplateDao templateDao = new TemplateDao();
    private TransactionsController parentController; 
    private ObservableList<TransactionTemplate> masterList; 

    public void setParentController(TransactionsController controller) {
        this.parentController = controller;
        // Завантаження списку тут, щоб мати доступ до parentController
        loadMasterList(); 
        refreshList();   
    }

    
    private void loadMasterList() {
        System.out.println("DEBUG (TM): Завантаження Master List шаблонів...");
        if (parentController.getUser() != null) {
            // Викликаємо findByUserId. Припускаємо, що він завантажує "легкі" об'єкти з ID
            List<TransactionTemplate> templates = templateDao.findByUserId(parentController.getUser().getId());
            this.masterList = FXCollections.observableArrayList(templates);
            System.out.printf("DEBUG (TM): Завантажено %d шаблонів.\n", templates.size());
        }
    }

    
    private void refreshList() {
        String searchText = searchField.getText() != null ? searchField.getText().toLowerCase() : "";

        if (masterList == null) return;

        if (searchText.isEmpty()) {
            templateListView.getItems().setAll(masterList);
        } else {
            List<TransactionTemplate> filtered = masterList.stream()
                .filter(t -> t.getName().toLowerCase().contains(searchText) || 
                              (t.getDescription() != null && t.getDescription().toLowerCase().contains(searchText)))
                .collect(Collectors.toList());
            templateListView.getItems().setAll(filtered);
        }
    }
    
    @FXML
    private void initialize() {

        templateListView.setOnMouseClicked(event -> {
            if (event.getClickCount() == 2 && !templateListView.getSelectionModel().isEmpty()) {
                onSelectTemplate();
            }
        });

        
        searchField.textProperty().addListener((obs, oldText, newText) -> refreshList());
        
        // ? ListCellFactory: коректний та детальний вивід даних шаблону
        templateListView.setCellFactory(lv -> new ListCell<TransactionTemplate>() {
            @Override
            protected void updateItem(TransactionTemplate template, boolean empty) {
                super.updateItem(template, empty);
                if (empty || template == null) {
                    setText(null);
                } else {
                    String amount = template.getDefaultAmount() != null && template.getDefaultAmount() != 0.0 ? 
                                             String.format(" (%.2f %s)", template.getDefaultAmount(), template.getCurrency()) : "";
                                             
                    String typeName = template.getType() != null && template.getType().equals("EXPENSE") ? "Витрата" : "Дохід";
                    
                    // Перевірка на null для об'єктів
                    // ПРИМІТКА: Для ListCell достатньо "легких" об'єктів з ID та Name
                    String categoryName = (template.getCategory() != null && template.getCategory().getName() != null) ? template.getCategory().getName() : "Без категорії";
                    String accountName = (template.getAccount() != null && template.getAccount().getName() != null) ? template.getAccount().getName() : "Без рахунку";
                    
                    String recurringInfo = "";
                    if (template.getRecurringType() != null && template.getRecurringType() != TransactionTemplate.RecurringType.NONE) {
                        recurringInfo = String.format(" | Періодичність: %s (Інтервал: %d)",
                            template.getRecurringType().name(),
                            template.getRecurrenceInterval() != null ? template.getRecurrenceInterval() : 1
                        );
                    }
                    
                    String descriptionLine = (template.getDescription() != null && !template.getDescription().isEmpty()) 
                                            ? String.format("\n  Опис: %s", template.getDescription()) 
                                            : "";

                    // Багаторядковий вивід для кращої читабельності
                    setText(String.format("? %s %s (%s)%s\n  Кат: %s, Рах: %s %s",
                                              template.getName(), 
                                              amount,
                                              typeName,
                                              descriptionLine,
                                              categoryName,
                                              accountName,
                                              recurringInfo));
                }
            }
        });
    }

    @FXML
    private void onSelectTemplate() {
        TransactionTemplate selectedLight = templateListView.getSelectionModel().getSelectedItem();
        
        if (selectedLight != null) {
            System.out.printf("DEBUG (TM): Вибрано шаблон (Light): %s (ID: %d)\n", selectedLight.getName(), selectedLight.getId());
            
            // ? КЛЮЧОВИЙ МОМЕНТ: Завантаження повного об'єкта
            TransactionTemplate selectedFull = templateDao.findById(selectedLight.getId());

            if (selectedFull != null) {
                System.out.printf("DEBUG (TM): Завантажено повний шаблон: %s (Кат: %s, Рах: %s, Сума: %.2f)\n",
                                    selectedFull.getName(),
                                    selectedFull.getCategory() != null ? selectedFull.getCategory().getName() : "NULL",
                                    selectedFull.getAccount() != null ? selectedFull.getAccount().getName() : "NULL",
                                    selectedFull.getDefaultAmount() != null ? selectedFull.getDefaultAmount() : 0.0);
                
                // Перевірка на NULL: Якщо Категорія або Рахунок все ще NULL, проблема в TemplateDao.findById
                if (selectedFull.getCategory() == null || selectedFull.getAccount() == null) {
                     System.out.println("ERROR (TM): Повний об'єкт має NULL Category або Account. Проблема в DAO.");
                }

                // КЛЮЧОВИЙ ВИКЛИК: Заповнення форми в батьківському контролері
                parentController.fillFormWithTemplate(selectedFull); 
                System.out.println("DEBUG (TM): Викликано fillFormWithTemplate у батьківському контролері.");
                
                // Закриття вікна
                ((Stage) templateListView.getScene().getWindow()).close(); 
            } else {
                showAlert("Помилка", "Не вдалося завантажити повні дані шаблону (ID не знайдено).");
                System.out.println("ERROR (TM): templateDao.findById повернув NULL.");
            }
        } else {
            showAlert("Помилка", "Виберіть шаблон для застосування.");
        }
    }

    @FXML
    private void onDeleteTemplate() {
        // ... (Без змін) ...
        TransactionTemplate selected = templateListView.getSelectionModel().getSelectedItem();
        if (selected != null) {
            Optional<ButtonType> result = showConfirmationDialog(
                "Підтвердження", 
                "Ви впевнені, що хочете видалити шаблон '" + selected.getName() + "'? Його неможливо відновити."
            );
            
            if (result.isPresent() && result.get() == ButtonType.OK) {
                if (templateDao.delete(selected.getId())) {
                    showAlert("Успіх", "Шаблон видалено.");
                    loadMasterList(); 
                    refreshList(); 
                    parentController.refresh(); 
                } else {
                    showAlert("Помилка", "Не вдалося видалити шаблон.");
                }
            }
        } else {
            showAlert("Помилка", "Виберіть шаблон для видалення.");
        }
    }

    
    private void showAlert(String title, String content) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        alert.showAndWait();
    }
    
    private Optional<ButtonType> showConfirmationDialog(String title, String content) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        return alert.showAndWait();
    }
    
    @FXML
    private void onCancel() {
        ((Stage) templateListView.getScene().getWindow()).close(); 
    }
}