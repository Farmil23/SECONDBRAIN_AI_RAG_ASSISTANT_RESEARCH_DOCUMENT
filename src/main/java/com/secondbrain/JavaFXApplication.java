package com.secondbrain;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.stage.Stage;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.ConfigurableApplicationContext;

public class JavaFXApplication extends Application {

    private ConfigurableApplicationContext context;

    @Override
    public void init() {
        // Langkah pertama integrasi. Saat aplikasi UI JavaFX mulai jalan, ia menyalakan mesin Spring Boot terlebih dahulu di latar belakang.
        this.context = new SpringApplicationBuilder(SecondBrainApplication.class).run();
    }

    @Override
    public void start(Stage primaryStage) {
        // Setelah Spring Boot nyala dan aplikasi UI (Stage) disiapkan oleh JavaFX, ia membunyikan "alarm" berupa StageReadyEvent ke Spring Boot.
        context.publishEvent(new StageReadyEvent(primaryStage));
    }

    @Override
    public void stop() {
        this.context.close();
        Platform.exit();
    }
}