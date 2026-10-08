package com.example.trafficsmartsimulator.controller;

import com.example.trafficsmartsimulator.DatabaseConnection;
import com.example.trafficsmartsimulator.entity.Vehiculo;
import com.example.trafficsmartsimulator.entity.Vehiculo.Direccion;
import javafx.animation.AnimationTimer;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.*;
import javafx.scene.paint.Color;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class TrafficController {

    @FXML private Canvas canvasTrafico;
    @FXML private ComboBox<String> cbModo;
    @FXML private Spinner<Integer> spnTiempoSemaforo;
    @FXML private Label lblTiempoRestanteSemaforo;
    @FXML private Label lblCongestion;
    @FXML private Label lblVehiculos;
    @FXML private Label lblAmbulancias;
    @FXML private Label lblEspera;
    @FXML private Label lblFlujo;
    @FXML private Label lblTiempo;
    @FXML private ProgressBar progressCongestion;
    @FXML private Button btnIniciar;

    private GraphicsContext gc;
    private List<Vehiculo> vehiculos = new ArrayList<>();
    private AnimationTimer timer;
    private boolean corriendo = false;

    private double tiempoTotalSeg = 0;
    private long ultimoTiempo = 0;

    // Semáforos: true = Verde para Horizontal, false = Verde para Vertical
    private boolean luzHorizontalVerde = true;
    private double timerSemaforo = 0;

    private int vehiculosProcesados = 0;
    private int ambulanciasPriorizadas = 0;
    private double tiempoEsperaTotal = 0;

    private Color[] coloresAutos = {
            Color.web("#38BDF8"), Color.web("#22C55E"), Color.web("#EAB308"),
            Color.web("#EC4899"), Color.web("#8B5CF6"), Color.web("#F97316")
    };

    @FXML
    public void initialize() {
        gc = canvasTrafico.getGraphicsContext2D();

        cbModo.setItems(FXCollections.observableArrayList(
                "Adaptativo IA (Detecta Carga y Emergencias)",
                "Tiempo Fijo (Personalizado)"
        ));
        cbModo.setValue("Adaptativo IA (Detecta Carga y Emergencias)");

        SpinnerValueFactory<Integer> valueFactory = new SpinnerValueFactory.IntegerSpinnerValueFactory(1, 60, 10);
        spnTiempoSemaforo.setValueFactory(valueFactory);

        prepararCruce();
        iniciarBucleSimulacion();
    }

    private void prepararCruce() {
        vehiculos.clear();
        vehiculosProcesados = 0;
        ambulanciasPriorizadas = 0;
        tiempoTotalSeg = 0;
        tiempoEsperaTotal = 0;
        luzHorizontalVerde = true;
        timerSemaforo = 0;

        lblTiempo.setText("Tiempo Transcurrido: 00:00");
        lblVehiculos.setText("Vehículos Procesados: 0");
        lblAmbulancias.setText("Ambulancias Priorizadas: 0");
        lblEspera.setText("Espera Promedio: 0.0s");
        lblFlujo.setText("Flujo: 0 veh/min");
        lblTiempoRestanteSemaforo.setText(spnTiempoSemaforo.getValue() + ".0 s");
        progressCongestion.setProgress(0);
        lblCongestion.setText("0.0%");

        dibujar();
    }

    @FXML
    private void iniciarSimulacion() {
        if (corriendo) return;
        prepararCruce();
        corriendo = true;
        ultimoTiempo = System.nanoTime();
        btnIniciar.setDisable(true);
    }

    @FXML
    private void reiniciarSimulacion() {
        if (corriendo) {
            double esperaProm = vehiculosProcesados > 0 ? (tiempoEsperaTotal / vehiculosProcesados) : 0;
            double vehMin = tiempoTotalSeg > 0 ? (vehiculosProcesados / (tiempoTotalSeg / 60.0)) : 0;

            DatabaseConnection.guardarReporteTrafico(
                    cbModo.getValue() + " (" + spnTiempoSemaforo.getValue() + "s)",
                    vehiculosProcesados, ambulanciasPriorizadas,
                    tiempoTotalSeg, esperaProm, vehMin
            );
        }

        corriendo = false;
        btnIniciar.setDisable(false);
        prepararCruce();
    }

    @FXML
    private void generarAmbulancia() {
        if (!corriendo) return;
        boolean horizontal = Math.random() > 0.5;
        if (horizontal) {
            vehiculos.add(new Vehiculo(-60, 345, Direccion.HORIZONTAL, true, Color.WHITE));
        } else {
            vehiculos.add(new Vehiculo(445, -60, Direccion.VERTICAL, true, Color.WHITE));
        }
        ambulanciasPriorizadas++;
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
                timerSemaforo += delta;

                double limiteTiempoSem = spnTiempoSemaforo.getValue();

                // 1. Generación aleatoria de tráfico normal
                if (Math.random() < 0.035) {
                    boolean horiz = Math.random() > 0.5;
                    Color color = coloresAutos[(int) (Math.random() * coloresAutos.length)];

                    if (horiz) {
                        vehiculos.add(new Vehiculo(-50, 345, Direccion.HORIZONTAL, false, color));
                    } else {
                        vehiculos.add(new Vehiculo(445, -50, Direccion.VERTICAL, false, color));
                    }
                }

                // Detectar si hay ambulancias acercándose al cruce
                boolean hayAmbulanciaHorizCercana = vehiculos.stream().anyMatch(v -> v.isEsAmbulancia() && v.getDireccion() == Direccion.HORIZONTAL && v.getX() < 420);
                boolean hayAmbulanciaVertCercana = vehiculos.stream().anyMatch(v -> v.isEsAmbulancia() && v.getDireccion() == Direccion.VERTICAL && v.getY() < 320);

                // 2. Control del Semáforo
                if (hayAmbulanciaHorizCercana && !luzHorizontalVerde) {
                    luzHorizontalVerde = true;
                    timerSemaforo = 0;
                } else if (hayAmbulanciaVertCercana && luzHorizontalVerde) {
                    luzHorizontalVerde = false;
                    timerSemaforo = 0;
                } else if (!hayAmbulanciaHorizCercana && !hayAmbulanciaVertCercana && timerSemaforo >= limiteTiempoSem) {
                    luzHorizontalVerde = !luzHorizontalVerde;
                    timerSemaforo = 0;
                }

                // 3. Etiqueta de tiempo restante
                double tiempoRestante = Math.max(0.0, limiteTiempoSem - timerSemaforo);
                lblTiempoRestanteSemaforo.setText(String.format("%.1f s", tiempoRestante));

                // 4. Detectar ambulancia cercana para orillar autos
                for (Vehiculo amb : vehiculos) {
                    if (amb.isEsAmbulancia()) {
                        for (Vehiculo v : vehiculos) {
                            if (!v.isEsAmbulancia() && v.getDireccion() == amb.getDireccion()) {
                                double distancia = (v.getDireccion() == Direccion.HORIZONTAL) ? (v.getX() - amb.getX()) : (v.getY() - amb.getY());

                                if (distancia > 0 && distancia < 280) {
                                    v.setOrillado(true);
                                } else if (distancia <= -50) {
                                    v.setOrillado(false);
                                }
                            }
                        }
                    }
                }

                // 5. Física y avance de vehículos
                Iterator<Vehiculo> iter = vehiculos.iterator();
                while (iter.hasNext()) {
                    Vehiculo v = iter.next();
                    boolean debeParar = false;

                    if (!v.isEsAmbulancia()) {
                        // REGLA: Si hay una ambulancia activa en esta dirección, los autos normales NO pasan el semáforo
                        boolean emergenciaEnEstaVia = (v.getDireccion() == Direccion.HORIZONTAL && hayAmbulanciaHorizCercana) ||
                                (v.getDireccion() == Direccion.VERTICAL && hayAmbulanciaVertCercana);

                        if (v.getDireccion() == Direccion.HORIZONTAL) {
                            if (!luzHorizontalVerde && v.getX() >= 290 && v.getX() <= 330) {
                                debeParar = true;
                            } else if (emergenciaEnEstaVia && v.getX() >= 290 && v.getX() <= 330) {
                                // Se quedan detenidos en la línea aunque esté en verde para abrirle espacio exclusivo a la ambulancia
                                debeParar = true;
                            }
                        } else {
                            if (luzHorizontalVerde && v.getY() >= 190 && v.getY() <= 230) {
                                debeParar = true;
                            } else if (emergenciaEnEstaVia && v.getY() >= 190 && v.getY() <= 230) {
                                debeParar = true;
                            }
                        }
                    }

                    if (debeParar) {
                        v.setVelocidad(0);
                    } else {
                        v.setVelocidad(v.getVelocidadBase());
                    }

                    v.mover(delta);

                    if (v.getX() > 900 || v.getY() > 700) {
                        tiempoEsperaTotal += v.getTiempoEspera();
                        vehiculosProcesados++;
                        iter.remove();
                    }
                }

                // Métricas
                double esperaProm = vehiculosProcesados > 0 ? (tiempoEsperaTotal / vehiculosProcesados) : 0;
                double vehMin = tiempoTotalSeg > 0 ? (vehiculosProcesados / (tiempoTotalSeg / 60.0)) : 0;
                double congestion = Math.min(100.0, (vehiculos.size() / 15.0) * 100.0);

                progressCongestion.setProgress(congestion / 100.0);
                lblCongestion.setText(String.format("%.1f%%", congestion));
                lblVehiculos.setText("Vehículos Procesados: " + vehiculosProcesados);
                lblAmbulancias.setText("Ambulancias Priorizadas: " + ambulanciasPriorizadas);
                lblEspera.setText(String.format("Espera Promedio: %.1fs", esperaProm));
                lblFlujo.setText(String.format("Flujo: %.0f veh/min", vehMin));

                int min = (int) (tiempoTotalSeg / 60);
                int seg = (int) (tiempoTotalSeg % 60);
                lblTiempo.setText(String.format("Tiempo Transcurrido: %02d:%02d", min, seg));

                dibujar();
            }
        };
        timer.start();
    }

    private void dibujar() {
        double w = canvasTrafico.getWidth();
        double h = canvasTrafico.getHeight();

        gc.setFill(Color.web("#064E3B"));
        gc.fillRect(0, 0, w, h);

        gc.setFill(Color.web("#047857"));
        gc.fillOval(50, 50, 60, 60);
        gc.fillOval(700, 80, 70, 70);
        gc.fillOval(80, 500, 75, 75);
        gc.fillOval(720, 480, 65, 65);

        gc.setFill(Color.web("#1E293B"));
        gc.fillRect(0, 300, w, 100);
        gc.fillRect(400, 0, 100, h);
        gc.fillRect(400, 300, 100, 100);

        gc.setFill(Color.WHITE);
        for (int i = 0; i < 100; i += 15) {
            gc.fillRect(380, 305 + i, 12, 8);
            gc.fillRect(508, 305 + i, 12, 8);
            gc.fillRect(405 + i, 280, 8, 12);
            gc.fillRect(405 + i, 408, 8, 12);
        }

        gc.setStroke(Color.web("#F59E0B"));
        gc.setLineWidth(3);
        gc.setLineDashes(15);
        gc.strokeLine(0, 350, 380, 350);
        gc.strokeLine(520, 350, w, 350);
        gc.strokeLine(450, 0, 450, 280);
        gc.strokeLine(450, 420, 450, h);
        gc.setLineDashes(null);

        dibujarSemaforo(360, 250, luzHorizontalVerde);
        dibujarSemaforo(520, 420, !luzHorizontalVerde);

        for (Vehiculo v : vehiculos) {
            double rx = v.getRenderX();
            double ry = v.getRenderY();

            if (v.isEsAmbulancia()) {
                gc.setFill(Color.WHITE);
                if (v.getDireccion() == Direccion.HORIZONTAL) {
                    gc.fillRect(rx, ry - 10, 45, 22);
                    gc.setFill(Color.web("#DC2626"));
                    gc.fillRect(rx + 18, ry - 10, 8, 22);
                    gc.fillRect(rx + 11, ry - 3, 22, 8);

                    boolean destello = (System.currentTimeMillis() / 150) % 2 == 0;
                    gc.setFill(destello ? Color.web("#EF4444") : Color.web("#38BDF8"));
                    gc.fillOval(rx + 20, ry - 12, 6, 6);
                } else {
                    gc.fillRect(rx - 10, ry, 22, 45);
                    gc.setFill(Color.web("#DC2626"));
                    gc.fillRect(rx - 10, ry + 18, 22, 8);
                    gc.fillRect(rx - 3, ry + 11, 8, 22);

                    boolean destello = (System.currentTimeMillis() / 150) % 2 == 0;
                    gc.setFill(destello ? Color.web("#EF4444") : Color.web("#38BDF8"));
                    gc.fillOval(rx - 12, ry + 20, 6, 6);
                }
            } else {
                gc.setFill(v.getColor());
                if (v.getDireccion() == Direccion.HORIZONTAL) {
                    gc.fillRect(rx, ry - 8, 38, 18);
                    gc.setFill(Color.web("#0F172A"));
                    gc.fillRect(rx + 10, ry - 6, 16, 14);
                    gc.setFill(Color.web("#FEF08A"));
                    gc.fillRect(rx + 36, ry - 7, 3, 4);
                    gc.fillRect(rx + 36, ry + 5, 3, 4);
                } else {
                    gc.fillRect(rx - 8, ry, 18, 38);
                    gc.setFill(Color.web("#0F172A"));
                    gc.fillRect(rx - 6, ry + 10, 14, 16);
                    gc.setFill(Color.web("#FEF08A"));
                    gc.fillRect(rx - 7, ry + 36, 4, 3);
                    gc.fillRect(rx + 5, ry + 36, 4, 3);
                }
            }
        }
    }

    private void dibujarSemaforo(double x, double y, boolean enVerde) {
        gc.setFill(Color.web("#0F172A"));
        gc.fillRect(x, y, 24, 50);
        gc.setStroke(Color.web("#475569"));
        gc.setLineWidth(1.5);
        gc.strokeRect(x, y, 24, 50);

        gc.setFill(enVerde ? Color.web("#7F1D1D") : Color.web("#EF4444"));
        gc.fillOval(x + 4, y + 5, 16, 16);

        gc.setFill(enVerde ? Color.web("#22C55E") : Color.web("#14532D"));
        gc.fillOval(x + 4, y + 28, 16, 16);
    }
}