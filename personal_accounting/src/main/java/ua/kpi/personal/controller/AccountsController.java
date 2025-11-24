package ua.kpi.personal.controller;

import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.stage.Stage;
import ua.kpi.personal.model.Account;
import ua.kpi.personal.service.AccountService; 
import ua.kpi.personal.model.User;
import ua.kpi.personal.state.ApplicationSession;
import ua.kpi.personal.state.BudgetAccessState;

public class AccountsController { 
    
    @FXML private TextField nameField;
    @FXML private TextField balanceField;
    @FXML private ComboBox<String> currencyChoice; 
    @FXML private ComboBox<String> typeChoice;    
    @FXML private Label messageLabel;
    @FXML private Button saveBtn;
    
    @FXML private CheckBox isSharedCheckbox; 

    private final AccountService accountService;
    private final ApplicationSession session = ApplicationSession.getInstance();
    private User user;
    private DashboardViewController dashboardController; 

    public AccountsController(AccountService accountService) {
        this.accountService = accountService;
    }

    @FXML
    private void initialize(){
        this.user = session.getCurrentUser();
        
        typeChoice.getItems().addAll("Cash", "Card", "Savings", "Other"); 
        currencyChoice.getItems().addAll("UAH", "USD", "EUR");
        
        typeChoice.getSelectionModel().selectFirst();
        currencyChoice.getSelectionModel().selectFirst();
       
        BudgetAccessState accessState = session.getCurrentBudgetAccessState();
        if (isSharedCheckbox != null) {
            if (!accessState.isOwner()) {
         
                isSharedCheckbox.setDisable(true);
                isSharedCheckbox.setText("Зробити рахунок спільним (Доступно лише Власнику)");
            } else {

                isSharedCheckbox.setSelected(false);
            }
        }
    }

    public void setDashboardController(DashboardViewController dashboardController) {
        this.dashboardController = dashboardController;
    }
    
    @FXML
    private void onSave(){
        String name = nameField.getText();
        String currency = currencyChoice.getValue();
        String type = typeChoice.getValue(); 
        
        if(name==null || name.isBlank()){ messageLabel.setText("Введіть назву"); return; }
        if(currency==null){ messageLabel.setText("Оберіть валюту"); return; }
        if(type==null){ messageLabel.setText("Оберіть тип рахунку"); return; }

        double bal = 0;
        try{ 
            bal = Double.parseDouble(balanceField.getText().trim()); 
        } catch(Exception e){ 
            messageLabel.setText("Невірний формат балансу (використовуйте .)"); 
            return;
        }
        
        Account a = new Account();
        a.setName(name);
        a.setBalance(bal);

        a.setCurrency(currency); 
        a.setType(type); 

        boolean isSharedSelected = isSharedCheckbox != null && isSharedCheckbox.isSelected();
 
        if (isSharedSelected && session.getCurrentBudgetAccessState().isOwner()) {
             a.setShared(true);
        } else {
             a.setShared(false); 
        }
        
        try {
            accountService.createAccount(a); 
            messageLabel.setText("Рахунок успішно додано."); 
                
            if (dashboardController != null) {
                dashboardController.refreshData();
            }
            closeDialog();
        } catch (SecurityException se) {
             messageLabel.setText("Помилка доступу: " + se.getMessage());
        } catch (Exception e) {
             messageLabel.setText("Помилка збереження: " + e.getMessage());
        }
    }

    @FXML
    private void onCancel() {
        closeDialog();
    }
    
    private void closeDialog() {
        Stage stage = (Stage) nameField.getScene().getWindow();
        stage.close();
    }
}