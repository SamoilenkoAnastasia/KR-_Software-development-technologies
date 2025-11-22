/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
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
        // Екран входу/реєстрації
        return "/fxml/login.fxml"; 
    }
}