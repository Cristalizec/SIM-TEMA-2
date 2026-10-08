package com.example.agroflysimulator.controller;

import com.example.agroflysimulator.DatabaseConnection;
import com.example.agroflysimulator.entity.DronFumigador;
import com.example.agroflysimulator.entity.Terreno;
import javafx.animation.AnimationTimer;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.*;
import javafx.scene.paint.Color;

import java.util.ArrayList;
import java.util.List;

public class AgroController {

    @FXML private Canvas canvasCampo;
    @FXML private ComboBox<String> cbNumDrones;
    @FXML private Slider sliderVientoVel;
    @FXML private Slider sliderVientoDir;
    @FXML private Label lblVientoVel;
    @FXML private Label lblVientoDir;
    @FXML private Label lblCobertura;
    @FXML private Label lblBateria;
    @FXML private Label lblTiempo;
    @FXML private ProgressBar progressCobertura;
    @FXML private ProgressBar progressBateria;
    @FXML private Button btnIniciar;

    private GraphicsContext gc;
    private Terreno terreno;
    private List<DronFumigador> drones = new ArrayList<>();
    private AnimationTimer timer;
    private boolean corriendo = false;
    private double tiempoTotalSeg = 0;
    private long ultimoTiempo = 0;

    @FXML
    public void initialize() {
        gc = canvasCampo.getGraphicsContext2D();

        // Opciones de 1 a 4 drones
        cbNumDrones.setItems(FXCollections.observableArrayList("1 Dron", "2 Drones", "3 Drones", "4 Drones"));
        cbNumDrones.setValue("2 Drones");

        sliderVientoVel.valueProperty().addListener((obs, oldVal, newVal) ->
                lblVientoVel.setText(String.format("%.0f km/h", newVal.doubleValue())));

        sliderVientoDir.valueProperty().addListener((obs, oldVal, newVal) ->
                lblVientoDir.setText(String.format("%.0f°", newVal.doubleValue())));

        prepararTerreno();
        iniciarBucleSimulacion();
    }

    private void prepararTerreno() {
        int celdasX = 85; // 850px / 10px celda
        int celdasY = 65; // 650px / 10px celda
        terreno = new Terreno(celdasX, celdasY, 10.0);
        drones.clear();
        tiempoTotalSeg = 0;
        lblTiempo.setText("Tiempo: 00:00");
        progressCobertura.setProgress(0);
        lblCobertura.setText("0.0%");
        progressBateria.setProgress(1.0);
        lblBateria.setText("100.0%");
        dibujar();
    }

    @FXML
    private void iniciarSimulacion() {
        if (corriendo) return;

        prepararTerreno();
        String seleccion = cbNumDrones.getValue();
        int cantDrones = 1;
        if (seleccion.contains("2")) cantDrones = 2;
        else if (seleccion.contains("3")) cantDrones = 3;
        else if (seleccion.contains("4")) cantDrones = 4;

        double w = canvasCampo.getWidth();
        double h = canvasCampo.getHeight();

        // Asignación de zonas según el número de drones
        switch (cantDrones) {
            case 1:
                drones.add(new DronFumigador(1, 10, 10, 10, w - 10, 10, h - 10));
                break;
            case 2:
                // 2 franjas horizontales (Norte y Sur)
                drones.add(new DronFumigador(1, 10, 10, 10, w - 10, 10, h / 2 - 10));
                drones.add(new DronFumigador(2, 10, h / 2 + 10, 10, w - 10, h / 2 + 10, h - 10));
                break;
            case 3:
                // 3 franjas horizontales
                double h3 = h / 3;
                drones.add(new DronFumigador(1, 10, 10, 10, w - 10, 10, h3 - 10));
                drones.add(new DronFumigador(2, 10, h3 + 10, 10, w - 10, h3 + 10, 2 * h3 - 10));
                drones.add(new DronFumigador(3, 10, 2 * h3 + 10, 10, w - 10, 2 * h3 + 10, h - 10));
                break;
            case 4:
                // Cuadrantes 2x2
                double midX = w / 2;
                double midY = h / 2;
                drones.add(new DronFumigador(1, 10, 10, 10, midX - 10, 10, midY - 10));             // NO
                drones.add(new DronFumigador(2, midX + 10, 10, midX + 10, w - 10, 10, midY - 10));  // NE
                drones.add(new DronFumigador(3, 10, midY + 10, 10, midX - 10, midY + 10, h - 10));  // SO
                drones.add(new DronFumigador(4, midX + 10, midY + 10, midX + 10, w - 10, midY + 10, h - 10)); // SE
                break;
        }

        corriendo = true;
        ultimoTiempo = System.nanoTime();
        btnIniciar.setDisable(true);
    }

    @FXML
    private void reiniciarSimulacion() {
        corriendo = false;
        btnIniciar.setDisable(false);
        prepararTerreno();
    }

    private void iniciarBucleSimulacion() {
        timer = new AnimationTimer() {
            @Override
            public void handle(long now) {
                if (!corriendo) {
                    dibujar();
                    return;
                }

                double delta = (now - ultimoTiempo) / 1e9;
                ultimoTiempo = now;
                tiempoTotalSeg += delta;

                double velViento = sliderVientoVel.getValue();
                int dirViento = (int) sliderVientoDir.getValue();

                boolean algunoEnMision = false;
                double bateriaSum = 0;

                for (DronFumigador dron : drones) {
                    dron.actualizar(delta, velViento, dirViento, terreno);
                    bateriaSum += dron.getBateria();
                    if (dron.isEnMision() && dron.getBateria() > 0) {
                        algunoEnMision = true;
                    }
                }

                // Actualizar Métricas
                double cobertura = terreno.getPorcentajeCobertura();
                double bateriaProm = drones.isEmpty() ? 100 : (bateriaSum / drones.size());

                progressCobertura.setProgress(cobertura / 100.0);
                lblCobertura.setText(String.format("%.1f%%", cobertura));

                progressBateria.setProgress(bateriaProm / 100.0);
                lblBateria.setText(String.format("%.1f%%", bateriaProm));

                int min = (int) (tiempoTotalSeg / 60);
                int seg = (int) (tiempoTotalSeg % 60);
                lblTiempo.setText(String.format("Tiempo: %02d:%02d", min, seg));

                dibujar();

                // Terminar simulación y guardar reporte en PostgreSQL
                if (!algunoEnMision || cobertura >= 99.0 || bateriaProm <= 0) {
                    corriendo = false;
                    btnIniciar.setDisable(false);

                    DatabaseConnection.guardarReporte(
                            drones.size(),
                            canvasCampo.getWidth() * canvasCampo.getHeight(),
                            velViento,
                            dirViento,
                            cobertura,
                            tiempoTotalSeg,
                            bateriaProm
                    );
                }
            }
        };
        timer.start();
    }

    private void dibujar() {
        // 1. Dibujar Campo de Maíz e Irregularidad del Terreno
        double sz = terreno.getCeldaTamaño();
        for (int x = 0; x < terreno.getColumnas(); x++) {
            for (int y = 0; y < terreno.getFilas(); y++) {
                double elev = terreno.getElevacion(x, y);

                if (terreno.isFumigado(x, y)) {
                    // Verde resplandeciente / Humectado por fumigación
                    gc.setFill(Color.rgb(34, 197, 94, 0.85));
                } else {
                    // Color Maizal base con sombreado de pendiente/terreno irregular
                    int g = (int) (100 + elev * 60);
                    int r = (int) (40 + elev * 30);
                    gc.setFill(Color.rgb(r, g, 20));
                }
                gc.fillRect(x * sz, y * sz, sz, sz);
            }
        }

        // 2. Dibujar Rosa de Viento / Vector
        double velV = sliderVientoVel.getValue();
        if (velV > 0) {
            double radV = Math.toRadians(sliderVientoDir.getValue());
            gc.setStroke(Color.web("#38BDF8"));
            gc.setLineWidth(2);
            gc.strokeLine(50, 50, 50 + Math.cos(radV) * 30, 50 + Math.sin(radV) * 30);
            gc.setFill(Color.web("#38BDF8"));
            gc.fillText("Viento", 35, 35);
        }

        // 3. Dibujar Drones y Asperjado
        Color[] coloresDron = {Color.CYAN, Color.MAGENTA, Color.YELLOW, Color.ORANGE};

        for (int i = 0; i < drones.size(); i++) {
            DronFumigador dron = drones.get(i);
            Color colorDron = coloresDron[i % coloresDron.length];

            // Chorro de aspersión con deriva de viento
            double rad = Math.toRadians(sliderVientoDir.getValue());
            double sprayX = dron.getX() + Math.cos(rad) * (velV * 0.4);
            double sprayY = dron.getY() + Math.sin(rad) * (velV * 0.4);

            gc.setFill(Color.rgb(56, 189, 248, 0.35));
            gc.fillOval(sprayX - dron.getRadioAspercion(), sprayY - dron.getRadioAspercion(),
                    dron.getRadioAspercion() * 2, dron.getRadioAspercion() * 2);

            // Cuerpo del Dron con identificador
            gc.setFill(colorDron);
            gc.fillOval(dron.getX() - 8, dron.getY() - 8, 16, 16);
            gc.setStroke(Color.WHITE);
            gc.strokeOval(dron.getX() - 12, dron.getY() - 12, 24, 24);

            // Etiqueta ID
            gc.setFill(Color.WHITE);
            gc.fillText("D" + dron.getId(), dron.getX() - 6, dron.getY() - 14);
        }
    }
}