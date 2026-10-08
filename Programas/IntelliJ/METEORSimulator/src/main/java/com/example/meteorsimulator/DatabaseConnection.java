package com.example.meteorsimulator;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Statement;

public class DatabaseConnection {


    private static final String URL = "jdbc:postgresql://localhost:5432/meteor_db";
    private static final String USER = "postgres";
    private static final String PASSWORD = "";

    // 1. Obtener la conexión JDBC
    public static Connection getConnection() throws SQLException {
        return DriverManager.getConnection(URL, USER, PASSWORD);
    }

    // 2. Crear la tabla automáticamente al iniciar
    public static void inicializarBaseDeDatos() {
        String sql = """
            CREATE TABLE IF NOT EXISTS reportes_meteorito (
                id SERIAL PRIMARY KEY,
                poblacion_total INT NOT NULL,
                evacuados INT NOT NULL,
                caidos INT NOT NULL,
                porcentaje_supervivencia NUMERIC(5,2) NOT NULL,
                tiempo_simulacion_seg NUMERIC(4,1) NOT NULL,
                fecha_registro TIMESTAMP DEFAULT CURRENT_TIMESTAMP
            );
            """;

        new Thread(() -> {
            try (Connection conn = getConnection();
                 Statement stmt = conn.createStatement()) {

                stmt.execute(sql);
                System.out.println("✅ Base de datos 'meteor_db' conectada y tabla 'reportes_meteorito' lista.");

            } catch (SQLException e) {
                System.err.println("❌ Error al conectar o crear la tabla en PostgreSQL: " + e.getMessage());
            }
        }).start();
    }

    // 3. Insertar el reporte en segundo plano al terminar la simulación
    public static void guardarReporte(int total, int evacuados, int caidos, double pctSupervivencia, double tiempo) {
        String sql = """
            INSERT INTO reportes_meteorito 
            (poblacion_total, evacuados, caidos, porcentaje_supervivencia, tiempo_simulacion_seg) 
            VALUES (?, ?, ?, ?, ?);
            """;

        new Thread(() -> {
            try (Connection conn = getConnection();
                 PreparedStatement pstmt = conn.prepareStatement(sql)) {

                pstmt.setInt(1, total);
                pstmt.setInt(2, evacuados);
                pstmt.setInt(3, caidos);
                pstmt.setDouble(4, pctSupervivencia);
                pstmt.setDouble(5, tiempo);

                pstmt.executeUpdate();
                System.out.println("🚀 Reporte guardado exitosamente en 'meteor_db'.");

            } catch (SQLException e) {
                System.err.println("❌ Error al insertar datos en PostgreSQL: " + e.getMessage());
            }
        }).start();
    }
}