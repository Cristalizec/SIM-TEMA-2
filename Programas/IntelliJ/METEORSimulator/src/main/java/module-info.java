module com.example.meteorsimulator {
    requires javafx.controls;
    requires javafx.fxml;
    requires java.sql; // <-- Agregado para PostgreSQL

    opens com.example.meteorsimulator to javafx.fxml;
    opens com.example.meteorsimulator.controller to javafx.fxml;

    exports com.example.meteorsimulator;
    exports com.example.meteorsimulator.controller;
    exports com.example.meteorsimulator.entity;
}