package com.example.meteorsimulator.controller;

import com.example.meteorsimulator.DatabaseConnection;
import com.example.meteorsimulator.entity.Ciudadano;
import com.example.meteorsimulator.entity.Meteorito;
import javafx.animation.AnimationTimer;
import javafx.fxml.FXML;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.paint.Color;

import java.util.ArrayList;
import java.util.List;

public class MeteorController {

    @FXML private TextField popInput;
    @FXML private TextField sizeInput;
    @FXML private TextField speedInput;
    @FXML private Label statusLabel;
    @FXML private Canvas simCanvas;

    private List<Ciudadano> poblacion = new ArrayList<>();
    private Meteorito meteorito;
    private AnimationTimer gameLoop;
    private boolean isRunning = false;

    private double radioOndaExpansiva = 0;
    private double tiempoSimulacion = 0;
    private long ultimoTiempo = 0;

    @FXML
    public void initialize() {
        generarEscenarioEstatico();
    }

    @FXML
    protected void onStartSimulation() {
        try {
            int totalPersonas = Integer.parseInt(popInput.getText());
            double tamano = Double.parseDouble(sizeInput.getText());
            double velocidad = Double.parseDouble(speedInput.getText());

            radioOndaExpansiva = 0;
            tiempoSimulacion = 0;
            poblacion.clear();

            // Instanciar el meteorito
            double targetX = Math.random() * (simCanvas.getWidth() - 200) + 100;
            double targetY = Math.random() * (simCanvas.getHeight() - 200) + 100;
            meteorito = new Meteorito(-50, -50, targetX, targetY, velocidad, tamano);

            // Generar la población urbana
            for (int i = 0; i < totalPersonas; i++) {
                double x = Math.random() * (simCanvas.getWidth() - 80) + 40;
                double y = Math.random() * (simCanvas.getHeight() - 80) + 40;
                double velBase = Math.random() * 1.6 + 1.2;
                double reaccion = Math.random() * 2.5;

                poblacion.add(new Ciudadano(x, y, velBase, reaccion));
            }

            if (gameLoop != null) gameLoop.stop();

            ultimoTiempo = System.nanoTime();
            gameLoop = new AnimationTimer() {
                @Override
                public void handle(long now) {
                    double deltaSegundos = (now - ultimoTiempo) / 1e9;
                    ultimoTiempo = now;
                    updateAndDraw(deltaSegundos);
                }
            };

            gameLoop.start();
            isRunning = true;

        } catch (NumberFormatException e) {
            statusLabel.setText("⚠️ Revisa los parámetros ingresados.");
        }
    }

    @FXML
    protected void onTogglePause() {
        if (gameLoop == null) return;
        if (isRunning) {
            gameLoop.stop();
            isRunning = false;
            statusLabel.setText("Estado: ⏸️ Pausado");
        } else {
            ultimoTiempo = System.nanoTime();
            gameLoop.start();
            isRunning = true;
        }
    }

    private void updateAndDraw(double delta) {
        tiempoSimulacion += delta;

        GraphicsContext gc = simCanvas.getGraphicsContext2D();
        double w = simCanvas.getWidth();
        double h = simCanvas.getHeight();

        // 1. Fondo del Mapa Urbano
        gc.setFill(Color.web("#080a0f"));
        gc.fillRect(0, 0, w, h);

        // Rutas de evacuación / Autopistas
        gc.setStroke(Color.web("#1e293b"));
        gc.setLineWidth(14);
        gc.strokeLine(0, h / 2, w, h / 2);
        gc.strokeLine(w / 2, 0, w / 2, h);

        gc.setStroke(Color.web("#38bdf8"));
        gc.setLineWidth(1);
        gc.strokeLine(0, h / 2, w, h / 2);
        gc.strokeLine(w / 2, 0, w / 2, h);

        // 2. Dibujar Meteorito e Impacto
        if (meteorito != null) {
            meteorito.actualizar(delta);

            if (!meteorito.isImpactado()) {
                // Estela de fuego
                gc.setStroke(Color.web("#f97316"));
                gc.setLineWidth(4);
                gc.strokeLine(meteorito.getStartX(), meteorito.getStartY(), meteorito.getX(), meteorito.getY());

                // Núcleo brillante
                gc.setFill(Color.web("#fbbf24"));
                gc.fillOval(meteorito.getX() - 8, meteorito.getY() - 8, 16, 16);
            } else {
                radioOndaExpansiva += 220.0 * delta;

                // Cráter Principal
                gc.setFill(Color.web("#451a03"));
                gc.fillOval(meteorito.getTargetX() - meteorito.getTamano(), meteorito.getTargetY() - meteorito.getTamano(),
                        meteorito.getTamano() * 2, meteorito.getTamano() * 2);

                // Cráteres Secundarios
                for (double[] frag : meteorito.getFragmentos()) {
                    gc.setFill(Color.web("#78350f"));
                    gc.fillOval(frag[0] - frag[2], frag[1] - frag[2], frag[2] * 2, frag[2] * 2);
                }

                // Onda de Choque
                gc.setStroke(Color.web("#ef4444"));
                gc.setLineWidth(3);
                gc.strokeOval(meteorito.getTargetX() - radioOndaExpansiva, meteorito.getTargetY() - radioOndaExpansiva,
                        radioOndaExpansiva * 2, radioOndaExpansiva * 2);
            }
        }

        // 3. Procesar Habitantes
        int evacuados = 0;
        int destruidos = 0;

        for (Ciudadano c : poblacion) {
            c.actualizar(tiempoSimulacion, meteorito, radioOndaExpansiva, w, h, poblacion);

            switch (c.getEstado()) {
                case EVACUADO -> {
                    evacuados++;
                    gc.setFill(Color.web("#10b981"));
                }
                case DESTRUIDO -> {
                    destruidos++;
                    gc.setFill(Color.web("#ef4444"));
                }
                case HUYENDO -> gc.setFill(Color.web("#38bdf8"));
                case ESPERANDO -> gc.setFill(Color.WHITE);
            }

            if (c.getEstado() != Ciudadano.Estado.EVACUADO) {
                gc.fillOval(c.getX(), c.getY(), 5, 5);
            }
        }

        // 4. Métrica en vivo sobre el Canvas
        gc.setFill(Color.rgb(0, 0, 0, 0.75));
        gc.fillRect(10, h - 35, 360, 25);
        gc.setFill(Color.web("#38bdf8"));
        gc.fillText(String.format("T: %.1fs | 🟢 Evacuados: %d | 🔴 Caídos: %d", tiempoSimulacion, evacuados, destruidos), 20, h - 18);

        statusLabel.setText(String.format("Trayectoria activa | T: %.1fs", tiempoSimulacion));

        // Condición de cierre de la simulación
        if (evacuados + destruidos == poblacion.size() || tiempoSimulacion >= 12.0) {
            gameLoop.stop();
            isRunning = false;
            double pct = (evacuados / (double) poblacion.size()) * 100.0;

            statusLabel.setText(String.format("FIN | Evacuación exitosa: %.1f%% (%d/%d)", pct, evacuados, poblacion.size()));

            // 💾 REGISTRO AUTOMÁTICO EN POSTGRESQL EN SEGUNDO PLANO
            DatabaseConnection.guardarReporte(poblacion.size(), evacuados, destruidos, pct, tiempoSimulacion);
        }
    }

    private void generarEscenarioEstatico() {
        GraphicsContext gc = simCanvas.getGraphicsContext2D();
        double w = simCanvas.getWidth();
        double h = simCanvas.getHeight();

        gc.setFill(Color.web("#080a0f"));
        gc.fillRect(0, 0, w, h);

        gc.setStroke(Color.web("#1e293b"));
        gc.setLineWidth(14);
        gc.strokeLine(0, h / 2, w, h / 2);
        gc.strokeLine(w / 2, 0, w / 2, h);
    }
}