package ua.kpi.personal.state;

import ua.kpi.personal.model.User;

public interface SessionState {
    void handleLogin(ApplicationSession session, User user);
    void handleLogout(ApplicationSession session);
    String getFxmlView();
}