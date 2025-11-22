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
        loadMasterList(); 
        refreshList();   
    }

    
    private void loadMasterList() {
        List<TransactionTemplate> templates = templateDao.findByUserId(parentController.getUser().getId());
        this.masterList = FXCollections.observableArrayList(templates);
    }

   
    private void refreshList() {
        String searchText = searchField.getText().toLowerCase();

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
        
        
        templateListView.setCellFactory(lv -> new ListCell<TransactionTemplate>() {
            @Override
            protected void updateItem(TransactionTemplate template, boolean empty) {
                super.updateItem(template, empty);
                if (empty || template == null) {
                    setText(null);
                    setGraphic(null);
                } else {
                    String amount = template.getDefaultAmount() != null && template.getDefaultAmount() != 0.0 ? 
                                    String.format(" (%.2f)", template.getDefaultAmount()) : "";
                    String type = template.getType().equals("EXPENSE") ? "???????" : "?????";
                    
                    setText(String.format("?? %s %s%s\n  ?????????: %s, ???????: %s",
                                          template.getName(), 
                                          amount,
                                          type,
                                          template.getCategory() != null ? template.getCategory().getName() : "?? ??????",
                                          template.getAccount() != null ? template.getAccount().getName() : "?? ??????"));
                }
            }
        });
    }

    @FXML
    private void onSelectTemplate() {
        TransactionTemplate selected = templateListView.getSelectionModel().getSelectedItem();
        if (selected != null) {
            parentController.fillFormWithTemplate(selected); 
            ((Stage) templateListView.getScene().getWindow()).close(); 
        } else {
            showAlert("???????", "???????? ?????? ??? ????????????.");
        }
    }

    @FXML
    private void onDeleteTemplate() {
        TransactionTemplate selected = templateListView.getSelectionModel().getSelectedItem();
        if (selected != null) {
            Optional<ButtonType> result = showConfirmationDialog(
                "?????????????", 
                "?? ????????, ?? ?????? ???????? ?????? '" + selected.getName() + "'?? ?? ????? ?????????."
            );
            
            if (result.isPresent() && result.get() == ButtonType.OK) {
                if (templateDao.delete(selected.getId())) {
                    showAlert("?????", "?????? ????????.");
                    loadMasterList(); 
                    refreshList(); 
                    parentController.refresh(); 
                } else {
                    showAlert("???????", "?? ??????? ???????? ??????.");
                }
            }
        } else {
            showAlert("???????", "???????? ?????? ??? ?????????.");
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