module com.example.trafficsmartsimulator {
    requires javafx.controls;
    requires javafx.fxml;
    requires java.sql;

    opens com.example.trafficsmartsimulator to javafx.fxml;
    opens com.example.trafficsmartsimulator.controller to javafx.fxml;

    exports com.example.trafficsmartsimulator;
    exports com.example.trafficsmartsimulator.controller;
    exports com.example.trafficsmartsimulator.entity;
}