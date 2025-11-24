package ua.kpi.personal.state;

import ua.kpi.personal.model.User;

public class LoggedInState implements SessionState {
    
    @Override
    public void handleLogin(ApplicationSession session, User user) {  
        session.changeState(this); 
    }

    @Override
    public void handleLogout(ApplicationSession session) {
        session.setCurrentUser(null);
        session.changeState(new LoggedOutState());
    }

    @Override
    public String getFxmlView() {
        return "/fxml/root_view.fxml"; 
    }
}