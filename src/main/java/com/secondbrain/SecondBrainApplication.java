package com.secondbrain;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class SecondBrainApplication {

    public static void main(String[] args) {
        javafx.application.Application.launch(JavaFXApplication.class, args);
    }
}
