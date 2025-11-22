/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package ua.kpi.personal.state;

import ua.kpi.personal.model.User;

public interface SessionState {
    void handleLogin(ApplicationSession session, User user);
    void handleLogout(ApplicationSession session);
    String getFxmlView();
}