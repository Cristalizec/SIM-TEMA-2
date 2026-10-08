import tkinter as tk
from tkinter import ttk
import math
import random
from database import obtener_conexion, inicializar_base_de_datos
from dataset import CLIENTES_DATASET

# ==========================================
# PALETA DE COLORES Y ESTILOS
# ==========================================
COLOR_BG = "#0f172a"          # Fondo azul noche
COLOR_PANEL = "#1e293b"       # Panel lateral
COLOR_GRID = "#1e293b"        # Cuadrícula
COLOR_TEXT = "#f8fafc"        # Texto blanco
COLOR_MUTED = "#94a3b8"       # Texto secundario
COLOR_HUB = "#6366f1"         # Almacén central
COLOR_PENDING = "#f59e0b"     # Ámbar / Pendiente
COLOR_SUCCESS = "#22c55e"     # Verde / Entregado
COLOR_TRUCK = "#06b6d4"       # Cyan furgoneta

class FastLogisticsApp:
    def __init__(self, root):
        self.root = root
        self.root.title("FastLogisticsSim - Control de Flota 2D")
        self.root.geometry("1100x720")
        self.root.configure(bg=COLOR_BG)

        # Variables de Control de Simulación
        self.simulacion_activa = False
        self.col_entregado_nombre = None
        self.tick_count = 0

        # Inicializar DB con dataset
        try:
            inicializar_base_de_datos(CLIENTES_DATASET)
        except Exception as e:
            print(f"Aviso al inicializar DB: {e}")

        self.setup_ui()
        self.cargar_datos_db()
        self.inicializar_flota()

        # Bucle principal de renderizado (corre siempre para dibujar, pero la lógica avanza solo si activa)
        self.actualizar_simulacion()

    def setup_ui(self):
        # Layout Principal
        self.main_container = tk.Frame(self.root, bg=COLOR_BG)
        self.main_container.pack(fill=tk.BOTH, expand=True, padx=12, pady=12)

        # Canvas para el Mapa
        self.canvas = tk.Canvas(
            self.main_container,
            bg=COLOR_BG,
            highlightthickness=1,
            highlightbackground="#334155"
        )
        self.canvas.pack(side=tk.LEFT, fill=tk.BOTH, expand=True)

        # Panel Lateral Derecho (HUD / Controles)
        self.panel = tk.Frame(self.main_container, bg=COLOR_PANEL, width=320, padx=15, pady=15)
        self.panel.pack(side=tk.RIGHT, fill=tk.Y, padx=(12, 0))
        self.panel.pack_propagate(False)

        # Título Panel
        tk.Label(
            self.panel, text="🚛 FastLogistics", 
            font=("Segoe UI", 16, "bold"), fg=COLOR_TEXT, bg=COLOR_PANEL
        ).pack(anchor="w")
        
        tk.Label(
            self.panel, text="Monitoreo de Flota en Tiempo Real", 
            font=("Segoe UI", 8), fg=COLOR_MUTED, bg=COLOR_PANEL
        ).pack(anchor="w", pady=(0, 10))

        # --- CONTROLES DE SESIÓN Y FLOTA ---
        frame_controles = tk.LabelFrame(self.panel, text=" ⚙ Control de Simulación ", fg=COLOR_ACCENT if 'COLOR_ACCENT' in globals() else "#38bdf8", bg=COLOR_PANEL, font=("Segoe UI", 9, "bold"), padx=10, pady=10)
        frame_controles.pack(fill=tk.X, pady=(0, 12))

        # Selector de número de vehículos (Max 4)
        lbl_vehiculos = tk.Label(frame_controles, text="Número de Vehículos (Máx 4):", font=("Segoe UI", 8), fg=COLOR_TEXT, bg=COLOR_PANEL)
        lbl_vehiculos.pack(anchor="w")
        
        self.combo_vehiculos = ttk.Spinbox(frame_controles, from_=1, to=4, width=5, state="readonly")
        self.combo_vehiculos.set(2)
        self.combo_vehiculos.pack(anchor="w", pady=(2, 8))

        # Botones de Acción
        self.btn_toggle = tk.Button(
            frame_controles, text="▶ Iniciar Sesión", font=("Segoe UI", 9, "bold"),
            bg="#16a34a", fg="#ffffff", activebackground="#15803d", activeforeground="#ffffff",
            bd=0, pady=6, cursor="hand2", command=self.toggle_simulacion
        )
        self.btn_toggle.pack(fill=tk.X, pady=2)

        self.btn_reset = tk.Button(
            frame_controles, text="🔄 Reiniciar Datos", font=("Segoe UI", 8),
            bg="#334155", fg=COLOR_TEXT, activebackground="#475569", activeforeground="#ffffff",
            bd=0, pady=4, cursor="hand2", command=self.reiniciar_simulacion
        )
        self.btn_reset.pack(fill=tk.X, pady=(4, 0))

        # Tarjeta de Métricas
        self.frame_stats = tk.Frame(self.panel, bg="#0f172a", padx=12, pady=10)
        self.frame_stats.pack(fill=tk.X, pady=(0, 10))

        self.lbl_total = self.crear_metrica("Total Paquetes:", "0", COLOR_TEXT)
        self.lbl_entregados = self.crear_metrica("Entregados:", "0", COLOR_SUCCESS)
        self.lbl_pendientes = self.crear_metrica("Pendientes:", "0", COLOR_PENDING)

        # Barra de Progreso
        tk.Label(self.panel, text="Progreso global:", font=("Segoe UI", 8, "bold"), fg=COLOR_TEXT, bg=COLOR_PANEL).pack(anchor="w", pady=(4, 2))
        self.progress_bar = ttk.Progressbar(self.panel, length=200, mode='determinate')
        self.progress_bar.pack(fill=tk.X, pady=(0, 10))

        # Consola de Eventos
        tk.Label(self.panel, text="📋 Registro de Eventos:", font=("Segoe UI", 8, "bold"), fg=COLOR_TEXT, bg=COLOR_PANEL).pack(anchor="w", pady=(2, 2))
        self.log_box = tk.Text(self.panel, height=8, bg="#0f172a", fg=COLOR_TEXT, font=("Consolas", 8), bd=0, padx=6, pady=6)
        self.log_box.pack(fill=tk.BOTH, expand=True)
        self.log_box.config(state=tk.DISABLED)

    def crear_metrica(self, titulo, valor_inicial, color_val):
        row = tk.Frame(self.frame_stats, bg="#0f172a")
        row.pack(fill=tk.X, pady=2)
        tk.Label(row, text=titulo, font=("Segoe UI", 8), fg=COLOR_MUTED, bg="#0f172a").pack(side=tk.LEFT)
        lbl = tk.Label(row, text=valor_inicial, font=("Segoe UI", 9, "bold"), fg=color_val, bg="#0f172a")
        lbl.pack(side=tk.RIGHT)
        return lbl

    def log(self, mensaje):
        self.log_box.config(state=tk.NORMAL)
        self.log_box.insert(tk.END, f"> {mensaje}\n")
        self.log_box.see(tk.END)
        self.log_box.config(state=tk.DISABLED)

    def toggle_simulacion(self):
        self.simulacion_activa = not self.simulacion_activa

        if self.simulacion_activa:
            # Re-inicializar flota con la cantidad seleccionada si aún no arrancan
            num_v = int(self.combo_vehiculos.get())
            if len(self.vehiculos) != num_v:
                self.inicializar_flota()

            self.btn_toggle.config(text="⏸ Pausar Sesión", bg="#dc2626", activebackground="#b91c1c")
            self.combo_vehiculos.config(state="disabled")
            self.log(f"▶ Simulación iniciada con {num_v} vehículos.")
        else:
            self.btn_toggle.config(text="▶ Reanudar Sesión", bg="#16a34a", activebackground="#15803d")
            self.log("⏸ Simulación pausada.")

    def reiniciar_simulacion(self):
        self.simulacion_activa = False
        self.btn_toggle.config(text="▶ Iniciar Sesión", bg="#16a34a", activebackground="#15803d")
        self.combo_vehiculos.config(state="readonly")
        
        # Reiniciar base de datos e interfaz
        try:
            inicializar_base_de_datos(CLIENTES_DATASET)
        except Exception as e:
            print(f"Error reseteando DB: {e}")

        self.cargar_datos_db()
        self.inicializar_flota()
        self.log("🔄 Simulación y datos reiniciados.")

    def cargar_datos_db(self):
        conn = obtener_conexion()
        self.clientes = []
        self.almacen = {"x": 380, "y": 300, "nombre": "ALMACÉN CENTRAL HUB"}

        if conn:
            cursor = conn.cursor()
            cursor.execute("SELECT column_name FROM information_schema.columns WHERE table_name = 'entregas';")
            columnas = [col[0].lower() for col in cursor.fetchall()]

            for col in ["entregado", "completado", "estado"]:
                if col in columnas:
                    self.col_entregado_nombre = col
                    break

            cursor.execute("SELECT * FROM entregas;")
            filas = cursor.fetchall()

            for row in filas:
                data = dict(zip(columnas, row))
                
                c_nom = data.get("cliente") or data.get("cliente_id") or data.get("nombre") or f"Cliente {data.get('id')}"
                p_nom = data.get("producto") or "Paquete"
                px = float(data.get("pos_x") or data.get("x") or random.randint(150, 600))
                py = float(data.get("pos_y") or data.get("y") or random.randint(150, 500))
                
                ent_val = False
                if self.col_entregado_nombre:
                    v = data.get(self.col_entregado_nombre)
                    ent_val = True if v in [True, "TRUE", "true", 1, "entregado", "Entregado"] else False

                self.clientes.append({
                    "id": data.get("id"),
                    "cliente": str(c_nom),
                    "producto": str(p_nom),
                    "x": px,
                    "y": py,
                    "entregado": ent_val
                })
            conn.close()
            self.log(f"Cargados {len(self.clientes)} registros de PostgreSQL.")
        else:
            self.log("⚠ Error leyendo PostgreSQL. Usando modo simulado.")

    def inicializar_flota(self):
        cant = int(self.combo_vehiculos.get()) if hasattr(self, 'combo_vehiculos') else 2
        self.vehiculos = []
        for i in range(1, cant + 1):
            self.vehiculos.append({
                "id": i,
                "x": float(self.almacen["x"]),
                "y": float(self.almacen["y"]),
                "destino": None,
                "estado": "LIBRE",
                "angulo": 0
            })

    def actualizar_simulacion(self):
        self.tick_count += 1
        
        # Redibujar canvas
        self.dibujar_mapa()

        # Avanzar posiciones y lógica solo si la sesión está ACTIVA
        if self.simulacion_activa:
            self.actualizar_logica_flota()

        self.actualizar_metricas()
        self.root.after(40, self.actualizar_simulacion)

    def dibujar_mapa(self):
        self.canvas.delete("all")
        w = self.canvas.winfo_width()
        h = self.canvas.winfo_height()

        if w < 10: w, h = 700, 600

        # Cuadrícula
        for x in range(0, w, 40):
            self.canvas.create_line(x, 0, x, h, fill=COLOR_GRID, dash=(2, 4))
        for y in range(0, h, 40):
            self.canvas.create_line(0, y, w, y, fill=COLOR_GRID, dash=(2, 4))

        # Almacén Central
        ax, ay = self.almacen["x"], self.almacen["y"]
        r_pulse = 25 + math.sin(self.tick_count * 0.1) * 4
        self.canvas.create_oval(ax-r_pulse, ay-r_pulse, ax+r_pulse, ay+r_pulse, outline=COLOR_HUB, width=1)
        self.canvas.create_rectangle(ax-20, ay-20, ax+20, ay+20, fill=COLOR_HUB, outline="#c7d2fe", width=2)
        self.canvas.create_text(ax, ay-28, text="🏭 " + self.almacen["nombre"], fill=COLOR_TEXT, font=("Segoe UI", 9, "bold"))

        # Clientes
        for c in self.clientes:
            cx, cy = c["x"], c["y"]
            color = COLOR_SUCCESS if c["entregado"] else COLOR_PENDING
            
            size = 12
            self.canvas.create_rectangle(cx-size, cy-size, cx+size, cy+size, fill="#1e293b", outline=color, width=2)
            self.canvas.create_polygon(cx-size-2, cy-size, cx, cy-size-8, cx+size+2, cy-size, fill=color)
            
            estado_str = "✓ Entregado" if c["entregado"] else f"📦 {c['producto']}"
            self.canvas.create_text(cx, cy + 20, text=f"{c['cliente']}\n({estado_str})", fill=COLOR_MUTED, font=("Segoe UI", 8), justify="center")

        # Vehículos
        for v in self.vehiculos:
            vx, vy = v["x"], v["y"]

            if v["destino"]:
                dx, dy = v["destino"]["x"], v["destino"]["y"]
                self.canvas.create_line(vx, vy, dx, dy, fill=COLOR_TRUCK, dash=(4, 4), width=2)

            self.dibujar_furgoneta(vx, vy, v["angulo"], v["id"])

    def dibujar_furgoneta(self, x, y, angulo_rad, v_id):
        cos_a = math.cos(angulo_rad)
        sin_a = math.sin(angulo_rad)

        def rotar(px, py):
            rx = px * cos_a - py * sin_a + x
            ry = px * sin_a + py * cos_a + y
            return rx, ry

        # Chasis
        p1 = rotar(-14, -8)
        p2 = rotar(10, -8)
        p3 = rotar(14, 0)
        p4 = rotar(10, 8)
        p5 = rotar(-14, 8)
        self.canvas.create_polygon(p1, p2, p3, p4, p5, fill=COLOR_TRUCK, outline="#ffffff", width=1.5)

        # Cabina
        c1 = rotar(2, -6)
        c2 = rotar(8, -6)
        c3 = rotar(8, 6)
        c4 = rotar(2, 6)
        self.canvas.create_polygon(c1, c2, c3, c4, fill="#0284c7")

        # Faros
        f1 = rotar(14, -4)
        f2 = rotar(28, -10)
        f3 = rotar(28, 10)
        f4 = rotar(14, 4)
        self.canvas.create_polygon(f1, f2, f3, f4, fill="#fef08a", stipple="gray50")

        self.canvas.create_text(x, y - 18, text=f"🚚 U-{v_id}", fill="#ffffff", font=("Segoe UI", 8, "bold"))

    def actualizar_logica_flota(self):
        velocidad = 3.5

        for v in self.vehiculos:
            if v["estado"] == "LIBRE":
                pendientes = [c for c in self.clientes if not c["entregado"] and not any(v2["destino"] == c for v2 in self.vehiculos)]
                if pendientes:
                    destino_elegido = pendientes[0]
                    v["destino"] = destino_elegido
                    v["estado"] = "ENTREGANDO"
                    self.log(f"Unidad #{v['id']} inició ruta a {destino_elegido['cliente']}")

            if v["destino"]:
                target_x = v["destino"]["x"]
                target_y = v["destino"]["y"]

                dx = target_x - v["x"]
                dy = target_y - v["y"]
                distancia = math.hypot(dx, dy)

                if distancia > velocidad:
                    v["angulo"] = math.atan2(dy, dx)
                    v["x"] += (dx / distancia) * velocidad
                    v["y"] += (dy / distancia) * velocidad
                else:
                    v["x"] = target_x
                    v["y"] = target_y

                    if v["estado"] == "ENTREGANDO":
                        cliente = v["destino"]
                        cliente["entregado"] = True
                        self.marcar_entregado_db(cliente["id"])
                        self.log(f"✅ Unidad #{v['id']} entregó a {cliente['cliente']}")

                        v["estado"] = "RETORNANDO"
                        v["destino"] = self.almacen
                    elif v["estado"] == "RETORNANDO":
                        self.log(f"🔄 Unidad #{v['id']} regresó al Almacén")
                        v["estado"] = "LIBRE"
                        v["destino"] = None

    def marcar_entregado_db(self, cliente_id):
        if not cliente_id or not self.col_entregado_nombre:
            return
            
        try:
            conn = obtener_conexion()
            if conn:
                cursor = conn.cursor()
                query = f"UPDATE entregas SET {self.col_entregado_nombre} = TRUE WHERE id = %s;"
                cursor.execute(query, (cliente_id,))
                conn.commit()
                conn.close()
        except Exception:
            pass

    def actualizar_metricas(self):
        total = len(self.clientes)
        entregados = sum(1 for c in self.clientes if c["entregado"])
        pendientes = total - entregados

        self.lbl_total.config(text=str(total))
        self.lbl_entregados.config(text=str(entregados))
        self.lbl_pendientes.config(text=str(pendientes))

        porcentaje = (entregados / total * 100) if total > 0 else 0
        self.progress_bar["value"] = porcentaje

if __name__ == "__main__":
    root = tk.Tk()
    app = FastLogisticsApp(root)
    root.mainloop()