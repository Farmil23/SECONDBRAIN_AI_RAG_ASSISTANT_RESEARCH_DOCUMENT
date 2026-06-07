package com.secondbrain;

import javafx.stage.Stage;
import org.springframework.context.ApplicationEvent;

public class StageReadyEvent extends ApplicationEvent {
    // -> Ini adalah objek "PESAN / SINYAL" yang membawa bingkai UI kosong (Stage) melintasi batas antara thread JavaFX menuju thread Spring Boot.
    public StageReadyEvent(Stage stage) {
        super(stage);
    }

    public Stage getStage() {
        return (Stage) getSource();
    }
}
