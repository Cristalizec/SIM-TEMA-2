package com.example.meteorsimulator;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.io.IOException;

public class MainApplication extends Application {
    @Override
    public void start(Stage stage) throws IOException {
        // Inicializa/Crea la tabla en PostgreSQL de forma transparente
        DatabaseConnection.inicializarBaseDeDatos();

        FXMLLoader fxmlLoader = new FXMLLoader(MainApplication.class.getResource("main-view.fxml"));
        Scene scene = new Scene(fxmlLoader.load(), 940, 540);
        stage.setTitle("METEOR IMPACT - Monte Carlo Evacuation Simulator");
        stage.setResizable(false);
        stage.setScene(scene);
        stage.show();
    }

    public static void main(String[] args) {
        launch();
    }
}