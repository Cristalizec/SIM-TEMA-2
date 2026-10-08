package com.example.agroflysimulator;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.io.IOException;

public class MainApplication extends Application {
    @Override
    public void start(Stage stage) throws IOException {

        DatabaseConnection.inicializarBaseDeDatos();

        FXMLLoader fxmlLoader = new FXMLLoader(MainApplication.class.getResource("main-view.fxml"));
        Scene scene = new Scene(fxmlLoader.getRoot() != null ? fxmlLoader.getRoot() : fxmlLoader.load(), 1180, 700);

        stage.setTitle("AgroFlySimulator - Control & Fumigación con Drones");
        stage.setScene(scene);
        stage.setResizable(false);
        stage.show();
    }

    public static void main(String[] args) {
        launch();
    }
}