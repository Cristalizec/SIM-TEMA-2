package com.example.agroflysimulator.entity;

public class Terreno {
    private int columnas;
    private int filas;
    private double celdaTamaño; // En píxeles
    private double[][] elevacion; // Matriz de elevación/irregularidad del terreno (0.0 a 1.0)
    private boolean[][] fumigado; // Estado de fumigación por celda

    public Terreno(int columnas, int filas, double celdaTamaño) {
        this.columnas = columnas;
        this.filas = filas;
        this.celdaTamaño = celdaTamaño;
        this.elevacion = new double[columnas][filas];
        this.fumigado = new boolean[columnas][filas];
        generarSuperficieIrregular();
    }

    private void generarSuperficieIrregular() {
        // Genera pendientes y lomas irregulares simulando un campo real de cultivo
        for (int x = 0; x < columnas; x++) {
            for (int y = 0; y < filas; y++) {
                double elev = Math.sin(x * 0.15) * Math.cos(y * 0.15) * 0.5 + 0.5;
                elevacion[x][y] = elev;
                fumigado[x][y] = false;
            }
        }
    }

    public boolean fumigarCelda(int col, int fila) {
        if (col >= 0 && col < columnas && fila >= 0 && fila < filas) {
            if (!fumigado[col][fila]) {
                fumigado[col][fila] = true;
                return true; // Nueva celda fumigada
            }
        }
        return false;
    }

    public double getPorcentajeCobertura() {
        int totalCeldas = columnas * filas;
        int celdasFumigadas = 0;
        for (int x = 0; x < columnas; x++) {
            for (int y = 0; y < filas; y++) {
                if (fumigado[x][y]) celdasFumigadas++;
            }
        }
        return (celdasFumigadas * 100.0) / totalCeldas;
    }

    public int getColumnas() { return columnas; }
    public int getFilas() { return filas; }
    public double getCeldaTamaño() { return celdaTamaño; }
    public double getElevacion(int x, int y) { return elevacion[x][y]; }
    public boolean isFumigado(int x, int y) { return fumigado[x][y]; }
}