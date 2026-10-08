package com.example.agroflysimulator.entity;

public class DronFumigador {
    private int id;
    private double x, y;
    private double velocidad;
    private double radioAspercion; // Radio de cobertura de las boquillas
    private double bateria; // 0 a 100%
    private boolean enMision;

    // Patrón de movimiento (Zig-Zag)
    private double minX, maxX, minY, maxY;
    private boolean moviendoDerecha = true;
    private double pasoVertical = 20.0;

    public DronFumigador(int id, double x, double y, double minX, double maxX, double minY, double maxY) {
        this.id = id;
        this.x = x;
        this.y = y;
        this.minX = minX;
        this.maxX = maxX;
        this.minY = minY;
        this.maxY = maxY;
        this.velocidad = 90.0; // Píxeles por segundo
        this.radioAspercion = 25.0;
        this.bateria = 100.0;
        this.enMision = true;
    }

    public void actualizar(double delta, double velVientoKmh, int dirVientoDeg, Terreno terreno) {
        if (!enMision || bateria <= 0) return;

        // 1. Movimiento en Zig-Zag (Escaneo de campo)
        if (moviendoDerecha) {
            x += velocidad * delta;
            if (x >= maxX) {
                x = maxX;
                y += pasoVertical;
                moviendoDerecha = false;
            }
        } else {
            x -= velocidad * delta;
            if (x <= minX) {
                x = minX;
                y += pasoVertical;
                moviendoDerecha = true;
            }
        }

        if (y >= maxY) {
            enMision = false; // Misión completada en su zona
        }

        // 2. Consumo de Batería según la elevación del terreno (Resistencia del motor)
        int col = (int) (x / terreno.getCeldaTamaño());
        int fila = (int) (y / terreno.getCeldaTamaño());
        double factorElevacion = 1.0;
        if (col >= 0 && col < terreno.getColumnas() && fila >= 0 && fila < terreno.getFilas()) {
            factorElevacion += terreno.getElevacion(col, fila) * 0.8; // Mayor elevación = más gasto
        }

        double gastoViento = 1.0 + (velVientoKmh / 50.0);
        bateria -= (delta * 1.5 * factorElevacion * gastoViento);
        if (bateria < 0) bateria = 0;

        // 3. Aplicar fumigación en el terreno considerando la DERIVA DEL VIENTO
        double rad = Math.toRadians(dirVientoDeg);
        double derivaX = Math.cos(rad) * (velVientoKmh * 0.4);
        double derivaY = Math.sin(rad) * (velVientoKmh * 0.4);

        // Coordenadas reales donde cae el líquido fumigante
        double sprayX = x + derivaX;
        double sprayY = y + derivaY;

        int radioCeldas = (int) (radioAspercion / terreno.getCeldaTamaño());
        int centroCol = (int) (sprayX / terreno.getCeldaTamaño());
        int centroFila = (int) (sprayY / terreno.getCeldaTamaño());

        for (int cx = centroCol - radioCeldas; cx <= centroCol + radioCeldas; cx++) {
            for (int cy = centroFila - radioCeldas; cy <= centroFila + radioCeldas; cy++) {
                terreno.fumigarCelda(cx, cy);
            }
        }
    }

    public int getId() { return id; }
    public double getX() { return x; }
    public double getY() { return y; }
    public double getBateria() { return bateria; }
    public double getRadioAspercion() { return radioAspercion; }
    public boolean isEnMision() { return enMision; }
}