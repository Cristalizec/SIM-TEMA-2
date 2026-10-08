package com.example.meteorsimulator.entity;

import java.util.List;

public class Ciudadano {
    public enum Estado { ESPERANDO, HUYENDO, EVACUADO, DESTRUIDO }

    private double x, y;
    private double velocidadBase;
    private double tiempoReaccion;
    private Estado estado;

    public Ciudadano(double x, double y, double velocidadBase, double tiempoReaccion) {
        this.x = x;
        this.y = y;
        this.velocidadBase = velocidadBase;
        this.tiempoReaccion = tiempoReaccion;
        this.estado = Estado.ESPERANDO;
    }

    public void actualizar(double tiempo, Meteorito meteorito, double radioOnda, double canvasW, double canvasH, List<Ciudadano> todos) {
        if (estado == Estado.DESTRUIDO || estado == Estado.EVACUADO) return;

        // 1. Evaluación de impacto directo o por onda expansiva
        if (meteorito != null && meteorito.isImpactado()) {
            double distImpacto = Math.hypot(x - meteorito.getTargetX(), y - meteorito.getTargetY());

            if (distImpacto <= meteorito.getTamano() * 1.5 || distImpacto <= radioOnda) {
                estado = Estado.DESTRUIDO;
                return;
            }

            // Impactos secundarios por fragmentos
            for (double[] frag : meteorito.getFragmentos()) {
                if (Math.hypot(x - frag[0], y - frag[1]) <= frag[2] * 1.5) {
                    estado = Estado.DESTRUIDO;
                    return;
                }
            }
        }

        // 2. Lógica de huida y embotellamientos en vías de evacuación
        if (tiempo >= tiempoReaccion) {
            estado = Estado.HUYENDO;

            int vecinosCercanos = 0;
            for (Ciudadano c : todos) {
                if (c != this && c.getEstado() == Estado.HUYENDO && Math.hypot(x - c.x, y - c.y) < 12) {
                    vecinosCercanos++;
                }
            }

            // Factor de embotellamiento (reduce velocidad si hay alta densidad)
            double factorTragico = Math.max(0.2, 1.0 - (vecinosCercanos * 0.15));
            double velActual = velocidadBase * factorTragico;

            if (x < canvasW / 2) {
                x -= velActual;
            } else {
                x += velActual;
            }

            y += (Math.random() - 0.5) * 1.2;

            if (x <= 10 || x >= canvasW - 10 || y <= 10 || y >= canvasH - 10) {
                estado = Estado.EVACUADO;
            }
        }
    }

    public double getX() { return x; }
    public double getY() { return y; }
    public Estado getEstado() { return estado; }
}