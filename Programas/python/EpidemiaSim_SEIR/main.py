import tkinter as tk
from tkinter import ttk, messagebox
import random
import math
import database  # Importamos nuestro módulo de BD

# ==========================================
# PALETA DE COLORES Y ESTILOS UI
# ==========================================
COLOR_BG = "#0f172a"          # Fondo azul noche
COLOR_PANEL = "#1e293b"       # Panel lateral
COLOR_TEXT = "#f8fafc"        # Texto principal
COLOR_MUTED = "#94a3b8"       # Texto secundario

COLOR_S = "#38bdf8"           # Susceptible (Azul claro)
COLOR_I = "#ef4444"           # Infectado (Rojo)
COLOR_R = "#22c55e"           # Recuperado / Inmune (Verde)

class EpidemiaApp:
    def __init__(self, root):
        self.root = root
        self.root.title("Simulador de Brote Epidémico (SEIR) + PostgreSQL")
        self.root.geometry("1180x760")
        self.root.configure(bg=COLOR_BG)

        # Inicializar/Verificar BD al arrancar
        database.inicializar_base_datos()

        # Variables del Modelo
        self.simulacion_activa = False
        self.dia = 0
        self.poblacion_total = 200
        self.pico_infectados = 0
        self.colapso_sanitario_ocurrio = False
        self.personas = []
        
        # Historial para la gráfica
        self.historial_S = []
        self.historial_I = []
        self.historial_R = []

        self.setup_ui()
        self.reiniciar_simulacion()
        self.bucle_simulacion()

    def setup_ui(self):
        # Layout Principal
        self.main_container = tk.Frame(self.root, bg=COLOR_BG)
        self.main_container.pack(fill=tk.BOTH, expand=True, padx=12, pady=12)

        # Panel Izquierdo: Canvas 2D + Gráfica
        self.left_frame = tk.Frame(self.main_container, bg=COLOR_BG)
        self.left_frame.pack(side=tk.LEFT, fill=tk.BOTH, expand=True)

        # Canvas de Partículas (Población)
        self.canvas = tk.Canvas(
            self.left_frame, bg="#020617", height=420,
            highlightthickness=1, highlightbackground="#334155"
        )
        self.canvas.pack(fill=tk.BOTH, expand=True, pady=(0, 10))

        # Canvas de la Gráfica de Curva Epidémica
        self.canvas_graph = tk.Canvas(
            self.left_frame, bg="#020617", height=200,
            highlightthickness=1, highlightbackground="#334155"
        )
        self.canvas_graph.pack(fill=tk.X)

        # Panel Lateral Derecho (Controles y Parámetros)
        self.panel = tk.Frame(self.main_container, bg=COLOR_PANEL, width=340, padx=15, pady=15)
        self.panel.pack(side=tk.RIGHT, fill=tk.Y, padx=(12, 0))
        self.panel.pack_propagate(False)

        # Título
        tk.Label(self.panel, text="🦠 Simulación SEIR", font=("Segoe UI", 16, "bold"), fg=COLOR_TEXT, bg=COLOR_PANEL).pack(anchor="w")
        tk.Label(self.panel, text="Modelado de propagación y capacidad hospitalaria", font=("Segoe UI", 8), fg=COLOR_MUTED, bg=COLOR_PANEL).pack(anchor="w", pady=(0, 10))

        # Controles
        frame_params = tk.LabelFrame(self.panel, text=" ⚙️ Parámetros del Virus ", fg="#38bdf8", bg=COLOR_PANEL, font=("Segoe UI", 9, "bold"), padx=10, pady=8)
        frame_params.pack(fill=tk.X, pady=(0, 10))

        # Tasa de Contagio
        tk.Label(frame_params, text="Probabilidad de Contagio (%):", font=("Segoe UI", 8), fg=COLOR_TEXT, bg=COLOR_PANEL).pack(anchor="w")
        self.slider_contagio = tk.Scale(frame_params, from_=10, to=100, orient=tk.HORIZONTAL, bg=COLOR_PANEL, fg=COLOR_TEXT, highlightthickness=0)
        self.slider_contagio.set(70)
        self.slider_contagio.pack(fill=tk.X)

        # Distanciamiento Social
        tk.Label(frame_params, text="Distanciamiento Social (% Inmóviles):", font=("Segoe UI", 8), fg=COLOR_TEXT, bg=COLOR_PANEL).pack(anchor="w", pady=(5, 0))
        self.slider_distanciamiento = tk.Scale(frame_params, from_=0, to=90, orient=tk.HORIZONTAL, bg=COLOR_PANEL, fg=COLOR_TEXT, highlightthickness=0)
        self.slider_distanciamiento.set(30)
        self.slider_distanciamiento.pack(fill=tk.X)

        # Capacidad Hospitalaria
        tk.Label(frame_params, text="Capacidad Hospitalaria (% Camas):", font=("Segoe UI", 8), fg=COLOR_TEXT, bg=COLOR_PANEL).pack(anchor="w", pady=(5, 0))
        self.slider_hospital = tk.Scale(frame_params, from_=10, to=50, orient=tk.HORIZONTAL, bg=COLOR_PANEL, fg=COLOR_TEXT, highlightthickness=0)
        self.slider_hospital.set(25)
        self.slider_hospital.pack(fill=tk.X)

        # Botones de Acción
        self.btn_toggle = tk.Button(
            self.panel, text="▶ Iniciar Brote", font=("Segoe UI", 9, "bold"),
            bg="#16a34a", fg="#ffffff", bd=0, pady=8, cursor="hand2", command=self.toggle_simulacion
        )
        self.btn_toggle.pack(fill=tk.X, pady=(5, 5))

        self.btn_reset = tk.Button(
            self.panel, text="🔄 Reiniciar Población", font=("Segoe UI", 8),
            bg="#334155", fg=COLOR_TEXT, bd=0, pady=5, cursor="hand2", command=self.reiniciar_simulacion
        )
        self.btn_reset.pack(fill=tk.X, pady=(0, 5))

        self.btn_guardar = tk.Button(
            self.panel, text="💾 Guardar en Base de Datos", font=("Segoe UI", 8, "bold"),
            bg="#2563eb", fg="#ffffff", bd=0, pady=6, cursor="hand2", command=self.guardar_en_bd
        )
        self.btn_guardar.pack(fill=tk.X, pady=(0, 10))

        # Tarjeta de Métricas
        frame_stats = tk.Frame(self.panel, bg="#0f172a", padx=10, pady=10)
        frame_stats.pack(fill=tk.X)

        self.lbl_susceptibles = self.crear_metrica(frame_stats, "🔵 Susceptibles:", "0", COLOR_S)
        self.lbl_infectados = self.crear_metrica(frame_stats, "🔴 Infectados:", "0", COLOR_I)
        self.lbl_recuperados = self.crear_metrica(frame_stats, "🟢 Inmunes/Recup:", "0", COLOR_R)
        self.lbl_pico = self.crear_metrica(frame_stats, "📈 Pico Máximo:", "0", "#f59e0b")
        self.lbl_alerta = tk.Label(frame_stats, text="SISTEMA SANITARIO OK", font=("Segoe UI", 8, "bold"), fg=COLOR_R, bg="#0f172a")
        self.lbl_alerta.pack(pady=(8, 0))

    def crear_metrica(self, parent, titulo, val_init, color):
        row = tk.Frame(parent, bg="#0f172a")
        row.pack(fill=tk.X, pady=2)
        tk.Label(row, text=titulo, font=("Segoe UI", 8), fg=COLOR_MUTED, bg="#0f172a").pack(side=tk.LEFT)
        lbl = tk.Label(row, text=val_init, font=("Segoe UI", 9, "bold"), fg=color, bg="#0f172a")
        lbl.pack(side=tk.RIGHT)
        return lbl

    def toggle_simulacion(self):
        self.simulacion_activa = not self.simulacion_activa
        if self.simulacion_activa:
            self.btn_toggle.config(text="⏸ Pausar", bg="#dc2626")
        else:
            self.btn_toggle.config(text="▶ Reanudar", bg="#16a34a")

    def reiniciar_simulacion(self):
        self.simulacion_activa = False
        self.btn_toggle.config(text="▶ Iniciar Brote", bg="#16a34a")
        self.dia = 0
        self.pico_infectados = 0
        self.colapso_sanitario_ocurrio = False
        self.historial_S.clear()
        self.historial_I.clear()
        self.historial_R.clear()

        w = self.canvas.winfo_width() if self.canvas.winfo_width() > 10 else 700
        h = self.canvas.winfo_height() if self.canvas.winfo_height() > 10 else 400
        
        dist_pct = self.slider_distanciamiento.get() / 100.0

        self.personas = []
        for i in range(self.poblacion_total):
            inmovil = random.random() < dist_pct
            vx = 0 if inmovil else random.choice([-2, -1, 1, 2])
            vy = 0 if inmovil else random.choice([-2, -1, 1, 2])

            estado = 'I' if i == 0 else 'S'
            tiempo_infeccion = 150 if i == 0 else 0

            self.personas.append({
                'x': random.randint(20, w - 20),
                'y': random.randint(20, h - 20),
                'vx': vx,
                'vy': vy,
                'estado': estado,
                'tiempo_infeccion': tiempo_infeccion
            })

    def bucle_simulacion(self):
        if self.simulacion_activa:
            self.dia += 1
            self.actualizar_posiciones()
            self.evaluar_contagios()

        self.dibujar_poblacion()
        self.dibujar_grafica()
        self.actualizar_metricas()

        self.root.after(40, self.bucle_simulacion)

    def actualizar_posiciones(self):
        w = self.canvas.winfo_width()
        h = self.canvas.winfo_height()

        for p in self.personas:
            p['x'] += p['vx']
            p['y'] += p['vy']

            if p['x'] <= 10 or p['x'] >= w - 10: p['vx'] *= -1
            if p['y'] <= 10 or p['y'] >= h - 10: p['vy'] *= -1

            if p['estado'] == 'I':
                p['tiempo_infeccion'] -= 1
                if p['tiempo_infeccion'] <= 0:
                    p['estado'] = 'R'

    def evaluar_contagios(self):
        prob_contagio = self.slider_contagio.get() / 100.0
        radio_contagio = 12

        infectados = [p for p in self.personas if p['estado'] == 'I']
        susceptibles = [p for p in self.personas if p['estado'] == 'S']

        for inf in infectados:
            for sus in susceptibles:
                dist = math.hypot(inf['x'] - sus['x'], inf['y'] - sus['y'])
                if dist < radio_contagio:
                    if random.random() < prob_contagio:
                        sus['estado'] = 'I'
                        sus['tiempo_infeccion'] = random.randint(120, 180)

    def dibujar_poblacion(self):
        self.canvas.delete("all")
        r = 5
        for p in self.personas:
            color = COLOR_S
            if p['estado'] == 'I': color = COLOR_I
            elif p['estado'] == 'R': color = COLOR_R

            self.canvas.create_oval(p['x']-r, p['y']-r, p['x']+r, p['y']+r, fill=color, outline="")

    def dibujar_grafica(self):
        self.canvas_graph.delete("all")
        w = self.canvas_graph.winfo_width()
        h = self.canvas_graph.winfo_height()
        if w < 10: return

        n_S = sum(1 for p in self.personas if p['estado'] == 'S')
        n_I = sum(1 for p in self.personas if p['estado'] == 'I')
        n_R = sum(1 for p in self.personas if p['estado'] == 'R')

        if self.simulacion_activa or len(self.historial_I) == 0:
            self.historial_S.append(n_S)
            self.historial_I.append(n_I)
            self.historial_R.append(n_R)

            if len(self.historial_I) > w - 40:
                self.historial_S.pop(0)
                self.historial_I.pop(0)
                self.historial_R.pop(0)

        cap_hosp_pct = self.slider_hospital.get() / 100.0
        y_hospital = h - (cap_hosp_pct * h)
        self.canvas_graph.create_line(0, y_hospital, w, y_hospital, fill="#f59e0b", dash=(4, 4), width=1.5)
        self.canvas_graph.create_text(80, y_hospital - 8, text=f"Límite Hospitalario ({int(cap_hosp_pct*100)}%)", fill="#f59e0b", font=("Segoe UI", 7))

        pts_I = []
        for i, val in enumerate(self.historial_I):
            x = i + 20
            y = h - (val / self.poblacion_total * h)
            pts_I.extend([x, y])

        if len(pts_I) >= 4:
            self.canvas_graph.create_line(pts_I, fill=COLOR_I, width=2)

    def actualizar_metricas(self):
        n_S = sum(1 for p in self.personas if p['estado'] == 'S')
        n_I = sum(1 for p in self.personas if p['estado'] == 'I')
        n_R = sum(1 for p in self.personas if p['estado'] == 'R')

        if n_I > self.pico_infectados:
            self.pico_infectados = n_I

        self.lbl_susceptibles.config(text=str(n_S))
        self.lbl_infectados.config(text=str(n_I))
        self.lbl_recuperados.config(text=str(n_R))
        self.lbl_pico.config(text=f"{self.pico_infectados} personas")

        limite_camas = (self.slider_hospital.get() / 100.0) * self.poblacion_total
        if n_I > limite_camas:
            self.colapso_sanitario_ocurrio = True
            self.lbl_alerta.config(text="⚠️ COLAPSO HOSPITALARIO", fg="#ef4444")
        else:
            self.lbl_alerta.config(text="SISTEMA SANITARIO OK", fg=COLOR_R)

    def guardar_en_bd(self):
        n_R = sum(1 for p in self.personas if p['estado'] == 'R')

        exito, msj = database.guardar_simulacion(
            self.poblacion_total,
            self.slider_contagio.get(),
            self.slider_distanciamiento.get(),
            self.slider_hospital.get(),
            self.dia,
            self.pico_infectados,
            n_R,
            self.colapso_sanitario_ocurrio
        )

        if exito:
            messagebox.showinfo("Éxito BD", msj)
        else:
            messagebox.showerror("Error BD", msj)

if __name__ == "__main__":
    root = tk.Tk()
    app = EpidemiaApp(root)
    root.mainloop()