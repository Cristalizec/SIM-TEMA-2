import tkinter as tk
from tkinter import ttk, messagebox
import random
import math
import database  # Importamos nuestro módulo de BD

# ==========================================
# PALETA DE COLORES Y ESTILOS UI (Dark Climate)
# ==========================================
COLOR_BG = "#0f172a"          # Azul noche
COLOR_PANEL = "#1e293b"       # Panel lateral
COLOR_TEXT = "#f8fafc"        # Texto principal
COLOR_MUTED = "#94a3b8"       # Texto secundario

COLOR_TEMP_LOW = "#38bdf8"    # Azul (<1.5°C)
COLOR_TEMP_MID = "#f59e0b"    # Naranja (1.5°C - 2.5°C)
COLOR_TEMP_HIGH = "#ef4444"   # Rojo (>2.5°C Peligroso)

class CambioClimaticoApp:
    def __init__(self, root):
        self.root = root
        self.root.title("Simulador Probabilístico Cambio Climático (IPCC) + PostgreSQL")
        self.root.geometry("1200x780")
        self.root.configure(bg=COLOR_BG)

        # Inicializar BD al arrancar
        database.inicializar_base_datos()

        # Variables de Simulación
        self.muestras_resultados = []  # Temperaturas finales al 2100
        self.trayectorias = []         # Proyecciones año por año
        self.temp_promedio_2100 = 0.0
        self.temp_p95_2100 = 0.0
        self.prob_superar_2c = 0.0

        self.setup_ui()
        self.ejecutar_simulacion_montecarlo()

    def setup_ui(self):
        # Layout Principal
        self.main_container = tk.Frame(self.root, bg=COLOR_BG)
        self.main_container.pack(fill=tk.BOTH, expand=True, padx=12, pady=12)

        # Panel Izquierdo: Gráficas (Trayectorias + Distribución Probabilística)
        self.left_frame = tk.Frame(self.main_container, bg=COLOR_BG)
        self.left_frame.pack(side=tk.LEFT, fill=tk.BOTH, expand=True)

        # Canvas 1: Proyección Temporal (2020 - 2100)
        tk.Label(self.left_frame, text="📈 Proyecciones de Calentamiento Global (°C sobre nivel preindustrial)", font=("Segoe UI", 9, "bold"), fg=COLOR_TEXT, bg=COLOR_BG).pack(anchor="w")
        self.canvas_traj = tk.Canvas(
            self.left_frame, bg="#020617", height=350,
            highlightthickness=1, highlightbackground="#334155"
        )
        self.canvas_traj.pack(fill=tk.BOTH, expand=True, pady=(2, 10))

        # Canvas 2: Distribución de Temperatura al 2100 (Histograma Probabilístico)
        tk.Label(self.left_frame, text="📊 Distribución de Probabilidad de Temperatura en 2100 (Monte Carlo)", font=("Segoe UI", 9, "bold"), fg=COLOR_TEXT, bg=COLOR_BG).pack(anchor="w")
        self.canvas_dist = tk.Canvas(
            self.left_frame, bg="#020617", height=230,
            highlightthickness=1, highlightbackground="#334155"
        )
        self.canvas_dist.pack(fill=tk.X)

        # Panel Lateral Derecho (Controles IPCC)
        self.panel = tk.Frame(self.main_container, bg=COLOR_PANEL, width=350, padx=15, pady=15)
        self.panel.pack(side=tk.RIGHT, fill=tk.Y, padx=(12, 0))
        self.panel.pack_propagate(False)

        # Título
        tk.Label(self.panel, text="🌍 Modelo IPCC Monte Carlo", font=("Segoe UI", 15, "bold"), fg=COLOR_TEXT, bg=COLOR_PANEL).pack(anchor="w")
        tk.Label(self.panel, text="Muestreo de parámetros de sensibilidad y emisiones", font=("Segoe UI", 8), fg=COLOR_MUTED, bg=COLOR_PANEL).pack(anchor="w", pady=(0, 10))

        # Frame Parámetros
        frame_params = tk.LabelFrame(self.panel, text=" ⚙️ Configuración del Escenario ", fg="#38bdf8", bg=COLOR_PANEL, font=("Segoe UI", 9, "bold"), padx=10, pady=8)
        frame_params.pack(fill=tk.X, pady=(0, 10))

        # Escenario SSP (Shared Socioeconomic Pathways)
        tk.Label(frame_params, text="Escenario Socioeconómico (SSP):", font=("Segoe UI", 8), fg=COLOR_TEXT, bg=COLOR_PANEL).pack(anchor="w")
        self.combo_ssp = ttk.Combobox(frame_params, values=["SSP1-2.6 (Sostenible / París)", "SSP2-4.5 (Intermedio)", "SSP5-8.5 (Altas Emisiones)"], state="readonly")
        self.combo_ssp.current(1)
        self.combo_ssp.pack(fill=tk.X, pady=(2, 8))
        self.combo_ssp.bind("<<ComboboxSelected>>", lambda e: self.ejecutar_simulacion_montecarlo())

        # Sensibilidad Climática (ECS)
        tk.Label(frame_params, text="Sensibilidad Climática ECS (°C por 2x CO2):", font=("Segoe UI", 8), fg=COLOR_TEXT, bg=COLOR_PANEL).pack(anchor="w")
        self.slider_ecs = tk.Scale(frame_params, from_=1.5, to=5.5, resolution=0.1, orient=tk.HORIZONTAL, bg=COLOR_PANEL, fg=COLOR_TEXT, highlightthickness=0)
        self.slider_ecs.set(3.0)
        self.slider_ecs.pack(fill=tk.X)

        # Factor de Retroalimentación (Feedbacks)
        tk.Label(frame_params, text="Retroalimentación (Permafrost/Nubes):", font=("Segoe UI", 8), fg=COLOR_TEXT, bg=COLOR_PANEL).pack(anchor="w", pady=(5, 0))
        self.slider_feedback = tk.Scale(frame_params, from_=0.8, to=1.6, resolution=0.05, orient=tk.HORIZONTAL, bg=COLOR_PANEL, fg=COLOR_TEXT, highlightthickness=0)
        self.slider_feedback.set(1.15)
        self.slider_feedback.pack(fill=tk.X)

        # Número de Muestras Monte Carlo
        tk.Label(frame_params, text="Muestras Monte Carlo:", font=("Segoe UI", 8), fg=COLOR_TEXT, bg=COLOR_PANEL).pack(anchor="w", pady=(5, 0))
        self.slider_muestras = tk.Scale(frame_params, from_=100, to=1000, resolution=100, orient=tk.HORIZONTAL, bg=COLOR_PANEL, fg=COLOR_TEXT, highlightthickness=0)
        self.slider_muestras.set(300)
        self.slider_muestras.pack(fill=tk.X)

        # Botones
        self.btn_recalcular = tk.Button(
            self.panel, text="🔄 Recalcular Muestreo", font=("Segoe UI", 9, "bold"),
            bg="#16a34a", fg="#ffffff", bd=0, pady=7, cursor="hand2", command=self.ejecutar_simulacion_montecarlo
        )
        self.btn_recalcular.pack(fill=tk.X, pady=(8, 5))

        self.btn_guardar = tk.Button(
            self.panel, text="💾 Guardar en Base de Datos", font=("Segoe UI", 8, "bold"),
            bg="#2563eb", fg="#ffffff", bd=0, pady=6, cursor="hand2", command=self.guardar_en_bd
        )
        self.btn_guardar.pack(fill=tk.X, pady=(0, 10))

        # Tarjeta de Métricas
        frame_stats = tk.Frame(self.panel, bg="#0f172a", padx=10, pady=10)
        frame_stats.pack(fill=tk.X)

        self.lbl_promedio = self.crear_metrica(frame_stats, "🌡️ Temp. Promedio 2100:", "0.0 °C", COLOR_TEXT)
        self.lbl_p95 = self.crear_metrica(frame_stats, "⚠️ Caso Crítico (P95):", "0.0 °C", COLOR_TEMP_HIGH)
        self.lbl_prob = self.crear_metrica(frame_stats, "🚨 Prob. Superar +2.0°C:", "0 %", COLOR_TEMP_MID)
        
        self.lbl_alerta = tk.Label(frame_stats, text="EVALUANDO RIESGO CLIMÁTICO", font=("Segoe UI", 8, "bold"), fg=COLOR_TEMP_LOW, bg="#0f172a")
        self.lbl_alerta.pack(pady=(8, 0))

    def crear_metrica(self, parent, titulo, val_init, color):
        row = tk.Frame(parent, bg="#0f172a")
        row.pack(fill=tk.X, pady=2)
        tk.Label(row, text=titulo, font=("Segoe UI", 8), fg=COLOR_MUTED, bg="#0f172a").pack(side=tk.LEFT)
        lbl = tk.Label(row, text=val_init, font=("Segoe UI", 9, "bold"), fg=color, bg="#0f172a")
        lbl.pack(side=tk.RIGHT)
        return lbl

    def ejecutar_simulacion_montecarlo(self):
        """Aplica muestreo estocástico para proyectar trayectorias climáticas."""
        n_muestras = int(self.slider_muestras.get())
        ecs_base = self.slider_ecs.get()
        feedback_base = self.slider_feedback.get()
        ssp_str = self.combo_ssp.get()

        # Determinar nivel de CO2 estimado según SSP
        if "SSP1" in ssp_str:
            co2_2100 = 450
        elif "SSP2" in ssp_str:
            co2_2100 = 600
        else:
            co2_2100 = 900

        self.muestras_resultados.clear()
        self.trayectorias.clear()

        # Motor Monte Carlo
        for _ in range(n_muestras):
            # Muestrear sensibilidad (Distribución Gaussiana alrededor de ECS)
            ecs_sim = max(1.0, random.gauss(ecs_base, 0.6))
            feedback_sim = max(0.7, random.gauss(feedback_base, 0.12))

            # Trayectoria de temperatura año a año (2020 a 2100)
            puntos = []
            temp_actual = 1.1  # Calentamiento aproximado actual (1.1°C)
            
            # Forzamiento radiativo aproximado por CO2: F = 5.35 * ln(C / C0)
            for anio in range(2020, 2101, 5):
                fraccion_tiempo = (anio - 2020) / 80.0
                co2_anio = 415 + fraccion_tiempo * (co2_2100 - 415)
                
                # Calentamiento en el año
                forzamiento = 5.35 * math.log(co2_anio / 280.0)
                temp_equilibrio = (forzamiento / 3.7) * ecs_sim * feedback_sim
                
                # Inercia térmica del océano (efecto retardo)
                temp_actual += (temp_equilibrio - temp_actual) * 0.15
                puntos.append(temp_actual)

            self.trayectorias.append(puntos)
            self.muestras_resultados.append(puntos[-1])

        # Calcular Estadísticas
        self.muestras_resultados.sort()
        self.temp_promedio_2100 = sum(self.muestras_resultados) / n_muestras
        idx_p95 = int(n_muestras * 0.95)
        self.temp_p95_2100 = self.muestras_resultados[min(idx_p95, n_muestras - 1)]

        superan_2c = sum(1 for t in self.muestras_resultados if t >= 2.0)
        self.prob_superar_2c = (superan_2c / n_muestras) * 100.0

        # Renderizar Visualizaciones
        self.dibujar_trayectorias()
        self.dibujar_histograma()
        self.actualizar_metricas()

    def dibujar_trayectorias(self):
        self.canvas_traj.delete("all")
        w = self.canvas_traj.winfo_width()
        h = self.canvas_traj.winfo_height()
        if w < 10: return

        # Líneas de Referencia de Temperatura
        for temp_ref in [1.5, 2.0, 3.0, 4.0]:
            y_ref = h - ((temp_ref / 5.0) * (h - 40) + 20)
            color_ref = COLOR_TEMP_LOW if temp_ref <= 1.5 else (COLOR_TEMP_MID if temp_ref <= 2.0 else COLOR_TEMP_HIGH)
            self.canvas_traj.create_line(40, y_ref, w - 20, y_ref, fill=color_ref, dash=(3, 3), width=1)
            self.canvas_traj.create_text(25, y_ref, text=f"+{temp_ref}°C", fill=color_ref, font=("Segoe UI", 7))

        # Eje Temporal (Años)
        anios = list(range(2020, 2101, 20))
        for i, anio in enumerate(anios):
            x = 40 + (i / (len(anios) - 1)) * (w - 60)
            self.canvas_traj.create_text(x, h - 10, text=str(anio), fill=COLOR_MUTED, font=("Segoe UI", 7))

        # Dibujar las Nubes de Trayectorias Monte Carlo
        for tray in self.trayectorias:
            pts = []
            for i, val in enumerate(tray):
                x = 40 + (i / (len(tray) - 1)) * (w - 60)
                y = h - ((val / 5.0) * (h - 40) + 20)
                pts.extend([x, y])

            # Color según impacto al 2100
            val_final = tray[-1]
            color_linea = COLOR_TEMP_LOW if val_final < 1.5 else (COLOR_TEMP_MID if val_final < 2.5 else COLOR_TEMP_HIGH)
            self.canvas_traj.create_line(pts, fill=color_linea, width=1)

    def dibujar_histograma(self):
        self.canvas_dist.delete("all")
        w = self.canvas_dist.winfo_width()
        h = self.canvas_dist.winfo_height()
        if w < 10: return

        num_bins = 20
        min_t, max_t = 0.5, 5.0
        bin_width = (max_t - min_t) / num_bins
        counts = [0] * num_bins

        for val in self.muestras_resultados:
            idx = int((val - min_t) / bin_width)
            if 0 <= idx < num_bins:
                counts[idx] += 1

        max_count = max(counts) if max(counts) > 0 else 1

        # Dibujar Barras de la Distribución
        bar_w = (w - 60) / num_bins
        for i in range(num_bins):
            t_bin = min_t + i * bin_width
            height = (counts[i] / max_count) * (h - 40)
            x0 = 40 + i * bar_w
            y0 = h - 20 - height
            x1 = x0 + bar_w - 2
            y1 = h - 20

            color_bar = COLOR_TEMP_LOW if t_bin < 1.5 else (COLOR_TEMP_MID if t_bin < 2.0 else COLOR_TEMP_HIGH)
            self.canvas_dist.create_rectangle(x0, y0, x1, y1, fill=color_bar, outline="")

            # Marcas de escala
            if i % 4 == 0:
                self.canvas_dist.create_text(x0, h - 8, text=f"{t_bin:.1f}°C", fill=COLOR_MUTED, font=("Segoe UI", 7))

    def actualizar_metricas(self):
        self.lbl_promedio.config(text=f"+{self.temp_promedio_2100:.2f} °C")
        self.lbl_p95.config(text=f"+{self.temp_p95_2100:.2f} °C")
        self.lbl_prob.config(text=f"{self.prob_superar_2c:.1f} %")

        if self.prob_superar_2c > 75.0:
            self.lbl_alerta.config(text="⚠️ RIESGO CLIMÁTICO CRÍTICO", fg=COLOR_TEMP_HIGH)
        elif self.prob_superar_2c > 30.0:
            self.lbl_alerta.config(text="⚡ RIESGO CLIMÁTICO MODERADO", fg=COLOR_TEMP_MID)
        else:
            self.lbl_alerta.config(text="✅ OBJETIVO PARÍS ALCANZABLE", fg=COLOR_TEMP_LOW)

    def guardar_en_bd(self):
        ssp_str = self.combo_ssp.get()
        co2_val = 450 if "SSP1" in ssp_str else (600 if "SSP2" in ssp_str else 900)

        exito, msj = database.guardar_simulacion(
            int(self.slider_muestras.get()),
            ssp_str,
            co2_val,
            self.slider_ecs.get(),
            self.slider_feedback.get(),
            round(self.temp_promedio_2100, 2),
            round(self.temp_p95_2100, 2),
            round(self.prob_superar_2c, 1)
        )

        if exito:
            messagebox.showinfo("Éxito BD", msj)
        else:
            messagebox.showerror("Error BD", msj)

if __name__ == "__main__":
    root = tk.Tk()
    app = CambioClimaticoApp(root)
    root.mainloop()