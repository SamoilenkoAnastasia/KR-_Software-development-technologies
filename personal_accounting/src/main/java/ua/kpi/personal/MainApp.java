package ua.kpi.personal;

import javafx.application.Application;
import javafx.stage.Stage;
import ua.kpi.personal.util.Db;
import ua.kpi.personal.state.ApplicationSession; 
import java.io.IOException;

public class MainApp extends Application {

    @Override
    public void start(Stage stage) throws IOException {
        Db.init(); 
        ApplicationSession.initialize(stage);    
        stage.setResizable(false);

    }

    public static void main(String[] args) {
        launch();
    }
}