package com.example.trafficsmartsimulator;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Statement;

public class DatabaseConnection {

    private static final String URL_SERVER = "jdbc:postgresql://localhost:5432/";
    private static final String DB_NAME = "traffic_smart_db";
    private static final String URL_DB = URL_SERVER + DB_NAME;
    private static final String USER = "postgres";
    private static final String PASSWORD = "";

    public static Connection getConnection() throws SQLException {
        return DriverManager.getConnection(URL_DB, USER, PASSWORD);
    }

    public static void inicializarBaseDeDatos() {
        new Thread(() -> {
            // 1. Crear base de datos si no existe
            try (Connection conn = DriverManager.getConnection(URL_SERVER + "postgres", USER, PASSWORD);
                 Statement stmt = conn.createStatement()) {

                stmt.execute("CREATE DATABASE " + DB_NAME + ";");
                System.out.println("✅ Base de datos '" + DB_NAME + "' creada con éxito.");

            } catch (SQLException e) {
                if (!"42P04".equals(e.getSQLState())) {
                    System.out.println("ℹ️ Conectando a BD existente '" + DB_NAME + "'...");
                }
            }

            // 2. Crear tabla de reportes de tráfico
            String sqlTabla = """
                CREATE TABLE IF NOT EXISTS reportes_trafico_urbano (
                    id SERIAL PRIMARY KEY,
                    modo_semaforo VARCHAR(50) NOT NULL,
                    vehiculos_procesados INT NOT NULL,
                    ambulancias_priorizadas INT NOT NULL,
                    tiempo_simulacion_seg NUMERIC(8,2) NOT NULL,
                    tiempo_promedio_espera_seg NUMERIC(6,2) NOT NULL,
                    promedio_vehiculos_minuto NUMERIC(6,2) NOT NULL,
                    fecha_registro TIMESTAMP DEFAULT CURRENT_TIMESTAMP
                );
                """;

            try (Connection conn = getConnection();
                 Statement stmt = conn.createStatement()) {

                stmt.execute(sqlTabla);
                System.out.println("✅ Tabla 'reportes_trafico_urbano' verificada/creada en PostgreSQL.");

            } catch (SQLException e) {
                System.err.println("❌ Error al crear tabla en BD: " + e.getMessage());
            }
        }).start();
    }

    public static void guardarReporteTrafico(String modo, int vehiculos, int ambulancias, double tiempoSeg, double esperaProm, double vehMin) {
        String sql = """
            INSERT INTO reportes_trafico_urbano 
            (modo_semaforo, vehiculos_procesados, ambulancias_priorizadas, tiempo_simulacion_seg, tiempo_promedio_espera_seg, promedio_vehiculos_minuto) 
            VALUES (?, ?, ?, ?, ?, ?);
            """;

        new Thread(() -> {
            try (Connection conn = getConnection();
                 PreparedStatement pstmt = conn.prepareStatement(sql)) {

                pstmt.setString(1, modo);
                pstmt.setInt(2, vehiculos);
                pstmt.setInt(3, ambulancias);
                pstmt.setDouble(4, tiempoSeg);
                pstmt.setDouble(5, esperaProm);
                pstmt.setDouble(6, vehMin);

                pstmt.executeUpdate();
                System.out.println("🚀 Reporte de tráfico registrado exitosamente en PostgreSQL.");

            } catch (SQLException e) {
                System.err.println("❌ Error al guardar reporte de tráfico: " + e.getMessage());
            }
        }).start();
    }
}