package ua.kpi.personal.state;

import ua.kpi.personal.model.User;

public class LoggedOutState implements SessionState {
    
    @Override
    public void handleLogin(ApplicationSession session, User user) {
        if (user != null) {
            session.setCurrentUser(user);
            session.changeState(new LoggedInState());
        }
    }

    @Override
    public void handleLogout(ApplicationSession session) {
        
    }

    @Override
    public String getFxmlView() {
        return "/fxml/login.fxml"; 
    }
}