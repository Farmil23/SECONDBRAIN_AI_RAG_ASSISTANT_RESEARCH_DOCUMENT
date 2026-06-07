package com.secondbrain.controller;

import com.secondbrain.persistence.UserEntity;
import com.secondbrain.service.UserService;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.input.MouseEvent;
import javafx.stage.Stage;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class UpgradeController {

    // -> HUBUNGAN BACKEND: Injeksi Dependensi. Menarik file 'UserService.java' untuk dipakai di kelas ini.
    @Autowired
    private UserService userService;

    private UserEntity currentUser;
    private MainController mainController;

    public void initData(UserEntity user, MainController mainController) {
        this.currentUser = user;
        this.mainController = mainController;
    }

    @FXML
    public void selectMonthly(ActionEvent event) {
        processUpgrade((Node) event.getSource(), "PRO-Monthly", 1000);
    }
    
    @FXML
    public void selectMonthlyMouse(MouseEvent event) {
        processUpgrade((Node) event.getSource(), "PRO-Monthly", 1000);
    }

    @FXML
    public void selectAnnual(ActionEvent event) {
        processUpgrade((Node) event.getSource(), "PRO-Annual", 15000);
    }
    
    @FXML
    public void selectAnnualMouse(MouseEvent event) {
        processUpgrade((Node) event.getSource(), "PRO-Annual", 15000);
    }

    private void processUpgrade(Node sourceNode, String tier, int tokens) {
        if (currentUser != null) {
            // -> HUBUNGAN BACKEND: Memanggil file 'UserService.java' -> method 'upgradeSubscription()'
            // Method tersebut akan melakukan operasi UPDATE ke MySQL untuk menimpa Tier/Token User saat ini.
            UserEntity updatedUser = userService.upgradeSubscription(currentUser, tier, tokens);
            
            mainController.onUpgradeSuccess(updatedUser);
            closeStage(sourceNode);
        }
    }

    @FXML
    public void handleCancel(ActionEvent event) {
        closeStage((Node) event.getSource());
    }

    private void closeStage(Node sourceNode) {
        Stage stage = (Stage) sourceNode.getScene().getWindow();
        stage.close();
    }
}
