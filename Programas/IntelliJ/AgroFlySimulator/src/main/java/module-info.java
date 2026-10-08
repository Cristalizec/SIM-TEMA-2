module com.example.agroflysimulator {
    requires javafx.controls;
    requires javafx.fxml;
    requires java.sql;

    opens com.example.agroflysimulator to javafx.fxml;
    opens com.example.agroflysimulator.controller to javafx.fxml;

    exports com.example.agroflysimulator;
    exports com.example.agroflysimulator.controller;
    exports com.example.agroflysimulator.entity;
}