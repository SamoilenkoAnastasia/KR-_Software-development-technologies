package ua.kpi.personal;

import javafx.application.Application;
import javafx.stage.Stage;
import ua.kpi.personal.util.Db;
import ua.kpi.personal.state.ApplicationSession; 
import java.io.IOException;

public class MainApp extends Application {

    @Override
    public void start(Stage stage) throws IOException {
        Db.init(); // initialize DB (create tables if not exist)
        
        // ІНІЦІАЛІЗАЦІЯ APPLICATION SESSION
        // Вся логіка завантаження login.fxml та управління stage знаходиться тут.
        ApplicationSession.initialize(stage);    
        
        stage.setResizable(false);
        // stage.show(); викликається всередині ApplicationSession.
    }

    public static void main(String[] args) {
        launch();
    }
}