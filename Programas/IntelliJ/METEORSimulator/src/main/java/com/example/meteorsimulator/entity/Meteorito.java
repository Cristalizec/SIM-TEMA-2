package com.example.meteorsimulator.entity;

import java.util.ArrayList;
import java.util.List;

public class Meteorito {
    private double x, y;
    private double targetX, targetY;
    private double startX, startY;
    private double velocidad;
    private double tamano;
    private boolean impactado;
    private List<double[]> fragmentos = new ArrayList<>(); // [x, y, tamano]

    public Meteorito(double startX, double startY, double targetX, double targetY, double velocidad, double tamano) {
        this.startX = startX;
        this.startY = startY;
        this.x = startX;
        this.y = startY;
        this.targetX = targetX;
        this.targetY = targetY;
        this.velocidad = velocidad;
        this.tamano = tamano;
        this.impactado = false;
    }

    public void actualizar(double delta) {
        if (impactado) return;

        double dx = targetX - x;
        double dy = targetY - y;
        double distancia = Math.hypot(dx, dy);

        if (distancia < 10) {
            x = targetX;
            y = targetY;
            impactado = true;
            generarFragmentosEstocasticos();
        } else {
            x += (dx / distancia) * velocidad * delta;
            y += (dy / distancia) * velocidad * delta;
        }
    }

    private void generarFragmentosEstocasticos() {
        // Generar entre 2 y 5 fragmentos secundarios alrededor del cráter principal
        int numFragmentos = (int) (Math.random() * 4) + 2;
        for (int i = 0; i < numFragmentos; i++) {
            double angulo = Math.random() * Math.PI * 2;
            double dist = Math.random() * 80 + 20;
            double fx = x + Math.cos(angulo) * dist;
            double fy = y + Math.sin(angulo) * dist;
            double fTam = tamano * (Math.random() * 0.3 + 0.15);
            fragmentos.add(new double[]{fx, fy, fTam});
        }
    }

    public double getX() { return x; }
    public double getY() { return y; }
    public double getStartX() { return startX; }
    public double getStartY() { return startY; }
    public double getTargetX() { return targetX; }
    public double getTargetY() { return targetY; }
    public double getTamano() { return tamano; }
    public boolean isImpactado() { return impactado; }
    public List<double[]> getFragmentos() { return fragmentos; }
}