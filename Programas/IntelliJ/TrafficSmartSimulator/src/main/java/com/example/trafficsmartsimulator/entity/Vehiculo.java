package com.example.trafficsmartsimulator.entity;

import javafx.scene.paint.Color;

public class Vehiculo {
    public enum Direccion { HORIZONTAL, VERTICAL }

    private double x, y;
    private double velocidad;
    private double velocidadBase;
    private Direccion direccion;
    private boolean esAmbulancia;
    private Color color;
    private double tiempoEspera;

    // Campos nuevos para el efecto de orillarse
    private double offsetLateral = 0; // Desplazamiento en pixels hacia la orilla
    private boolean orillado = false;

    public Vehiculo(double x, double y, Direccion direccion, boolean esAmbulancia, Color color) {
        this.x = x;
        this.y = y;
        this.direccion = direccion;
        this.esAmbulancia = esAmbulancia;
        this.color = color;
        this.velocidadBase = esAmbulancia ? 190.0 : 120.0;
        this.velocidad = velocidadBase;
        this.tiempoEspera = 0;
    }

    public void mover(double delta) {
        // Suavizar el movimiento de orillarse (interpolación)
        double objetivoOffset = orillado ? 20.0 : 0.0;
        if (offsetLateral < objetivoOffset) {
            offsetLateral = Math.min(objetivoOffset, offsetLateral + delta * 30.0);
        } else if (offsetLateral > objetivoOffset) {
            offsetLateral = Math.max(objetivoOffset, offsetLateral - delta * 30.0);
        }

        if (velocidad > 0) {
            if (direccion == Direccion.HORIZONTAL) {
                x += velocidad * delta;
            } else {
                y += velocidad * delta;
            }
        } else {
            tiempoEspera += delta;
        }
    }

    // Retornan la posición real en Canvas con el offset aplicado
    public double getRenderX() {
        if (direccion == Direccion.VERTICAL) {
            return x + offsetLateral; // Se orilla a la derecha en el eje X
        }
        return x;
    }

    public double getRenderY() {
        if (direccion == Direccion.HORIZONTAL) {
            return y + offsetLateral; // Se orilla hacia abajo en el eje Y
        }
        return y;
    }

    public double getX() { return x; }
    public double getY() { return y; }
    public Direccion getDireccion() { return direccion; }
    public boolean isEsAmbulancia() { return esAmbulancia; }
    public Color getColor() { return color; }
    public double getVelocidad() { return velocidad; }
    public void setVelocidad(double velocidad) { this.velocidad = velocidad; }
    public double getVelocidadBase() { return velocidadBase; }
    public double getTiempoEspera() { return tiempoEspera; }

    public boolean isOrillado() { return orillado; }
    public void setOrillado(boolean orillado) { this.orillado = orillado; }
}