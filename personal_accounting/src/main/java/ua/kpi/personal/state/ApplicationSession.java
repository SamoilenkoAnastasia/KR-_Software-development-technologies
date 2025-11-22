package ua.kpi.personal.state;

import ua.kpi.personal.model.User;
import javafx.stage.Stage;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import ua.kpi.personal.controller.MainController; 
import java.io.IOException;

public class ApplicationSession {
    
    private static ApplicationSession instance;
    
    private SessionState currentState;
    private User currentUser;
    private Stage primaryStage; 
    
    // ? ДОДАНО: Поле для зберігання активного MainController для навігації
    private MainController mainController; 

    
    private ApplicationSession(Stage primaryStage) {
        this.primaryStage = primaryStage;    
        this.currentState = new LoggedOutState(); 
        loadView(); 
    }
    
    
    public static void initialize(Stage stage) {
        if (instance != null) {
            throw new IllegalStateException("ApplicationSession вже ініціалізовано.");
        }
        instance = new ApplicationSession(stage);
    }
    
    
    public static ApplicationSession getInstance() {
        if (instance == null) {
            throw new IllegalStateException("ApplicationSession не ініціалізовано. Викличте initialize(Stage) у методі start().");
        }
        return instance;
    }

   

    public void changeState(SessionState newState) {
        this.currentState = newState;
        loadView(); 
    }

    public void login(User user) {
        // У цьому місці LoggedOutState має викликати changeState(new LoggedInState())
        currentState.handleLogin(this, user);
    }
    
    public void logout() {
        
        currentState.handleLogout(this);
    }
    

    private void loadView() {
    String fxmlPath = null;
    try {
        fxmlPath = currentState.getFxmlView();
        
       
        this.mainController = null;

        FXMLLoader loader = new FXMLLoader(getClass().getResource(fxmlPath));
        Scene scene = new Scene(loader.load());

        if (fxmlPath.equals("/fxml/login.fxml")) {
            primaryStage.setTitle("Вхід / Реєстрація");
            primaryStage.setResizable(false);
            primaryStage.setMaximized(false);
        } else if (fxmlPath.equals("/fxml/root_view.fxml")) {
            primaryStage.setTitle("Персональний фінансовий облік");
            
           
            primaryStage.setResizable(true); 
            primaryStage.setMaximized(true); 

            MainController newMainController = loader.getController();
            if (newMainController != null) {
                
                this.mainController = newMainController;
                newMainController.showInitialView();
            }
        }

        primaryStage.setScene(scene);
        primaryStage.show();

    } catch (IOException e) {
        System.err.println("Помилка завантаження FXML: " + fxmlPath);
        e.printStackTrace();
    }
}

   
    
    public User getCurrentUser() {
        return currentUser;
    }

    public void setCurrentUser(User currentUser) {
        this.currentUser = currentUser;
    }
    
    public Stage getPrimaryStage() {
        return primaryStage;
    }

    
    public MainController getController() {
        return this.mainController;
    }
}
