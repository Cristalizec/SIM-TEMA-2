package com.example.agroflysimulator;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Statement;

public class DatabaseConnection {

    // Configura la URL y credenciales de tu PostgreSQL local
    private static final String URL_SERVER = "jdbc:postgresql://localhost:5432/";
    private static final String DB_NAME = "agrofly_db";
    private static final String URL_DB = URL_SERVER + DB_NAME;
    private static final String USER = "postgres";
    private static final String PASSWORD = "";

    public static Connection getConnection() throws SQLException {
        return DriverManager.getConnection(URL_DB, USER, PASSWORD);
    }

    public static void inicializarBaseDeDatos() {
        new Thread(() -> {
            // 1. Crear la base de datos si no existe
            try (Connection conn = DriverManager.getConnection(URL_SERVER + "postgres", USER, PASSWORD);
                 Statement stmt = conn.createStatement()) {

                stmt.execute("CREATE DATABASE " + DB_NAME + ";");
                System.out.println("✅ Base de datos '" + DB_NAME + "' creada exitosamente.");

            } catch (SQLException e) {
                // Si la base de datos ya existe, ignoramos la excepción (código 42P04)
                if (!"42P04".equals(e.getSQLState())) {
                    System.out.println("ℹ️ Conectando a la base de datos existente '" + DB_NAME + "'...");
                }
            }

            // 2. Crear la tabla 'reportes_fumigacion' si no existe
            String sqlTabla = """
                CREATE TABLE IF NOT EXISTS reportes_fumigacion (
                    id SERIAL PRIMARY KEY,
                    cantidad_drones INT NOT NULL,
                    superficie_m2 NUMERIC(10,2) NOT NULL,
                    velocidad_viento_kmh NUMERIC(5,2) NOT NULL,
                    direccion_viento_deg INT NOT NULL,
                    cobertura_porcentaje NUMERIC(5,2) NOT NULL,
                    tiempo_vuelo_seg NUMERIC(6,2) NOT NULL,
                    bateria_restante_promed NUMERIC(5,2) NOT NULL,
                    fecha_registro TIMESTAMP DEFAULT CURRENT_TIMESTAMP
                );
                """;

            try (Connection conn = getConnection();
                 Statement stmt = conn.createStatement()) {

                stmt.execute(sqlTabla);
                System.out.println("✅ Tabla 'reportes_fumigacion' lista en PostgreSQL.");

            } catch (SQLException e) {
                System.err.println("❌ Error al crear/verificar la tabla: " + e.getMessage());
            }
        }).start();
    }

    public static void guardarReporte(int cantDrones, double superficie, double velViento, int dirViento, double cobertura, double tiempoVuelo, double bateria) {
        String sql = """
            INSERT INTO reportes_fumigacion
            (cantidad_drones, superficie_m2, velocidad_viento_kmh, direccion_viento_deg, cobertura_porcentaje, tiempo_vuelo_seg, bateria_restante_promed)
            VALUES (?, ?, ?, ?, ?, ?, ?);
            """;

        new Thread(() -> {
            try (Connection conn = getConnection();
                 PreparedStatement pstmt = conn.prepareStatement(sql)) {

                pstmt.setInt(1, cantDrones);
                pstmt.setDouble(2, superficie);
                pstmt.setDouble(3, velViento);
                pstmt.setInt(4, dirViento);
                pstmt.setDouble(5, cobertura);
                pstmt.setDouble(6, tiempoVuelo);
                pstmt.setDouble(7, bateria);

                pstmt.executeUpdate();
                System.out.println("🚀 Reporte de fumigación guardado exitosamente en PostgreSQL.");

            } catch (SQLException e) {
                System.err.println("❌ Error al guardar el reporte: " + e.getMessage());
            }
        }).start();
    }
}