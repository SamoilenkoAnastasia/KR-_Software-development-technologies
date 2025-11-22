package ua.kpi.personal;

import javafx.application.Application;
import javafx.stage.Stage;
import ua.kpi.personal.util.Db;
import ua.kpi.personal.state.ApplicationSession; // <--- 1. Äîäàºìî ³ìïîðò
import java.io.IOException;

public class MainApp extends Application {

    @Override
    public void start(Stage stage) throws IOException {
        Db.init(); // initialize DB (create tables if not exist)
        
        // *** 2. ²Í²Ö²ÀË²ÇÀÖ²ß APPLICATION SESSION ***
        // Ïåðåäàºìî ãîëîâíèé Stage Singleton'ó.
        // Öå ñòâîðþº åêçåìïëÿð, âñòàíîâëþº ïî÷àòêîâèé ñòàí (LoggedOutState) 
        // ³ çàâàíòàæóº login.fxml âñåðåäèí³ êîíñòðóêòîðà ApplicationSession.
        ApplicationSession.initialize(stage); 
        
        // *** 3. ÂÈÄÀËÅÍÎ ÂÑÞ ÐÓ×ÍÓ ËÎÃ²ÊÓ ÇÀÂÀÍÒÀÆÅÍÍß ÑÖÅÍÈ: ***
        // FXMLLoader fxmlLoader = new FXMLLoader(MainApp.class.getResource("/fxml/login.fxml"));
        // Scene scene = new Scene(fxmlLoader.load());
        // stage.setTitle("Îñîáèñòà áóõãàëòåð³ÿ — Login"); // Çàãîëîâîê òàêîæ âñòàíîâëþºòüñÿ â ApplicationSession
        // stage.setScene(scene);
        
        stage.setResizable(false);
        // stage.show(); âèêëèêàºòüñÿ âñåðåäèí³ ApplicationSession.
    }

    public static void main(String[] args) {
        launch();
    }
}