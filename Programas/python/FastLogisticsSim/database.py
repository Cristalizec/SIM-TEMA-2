import psycopg2
from datetime import datetime

# Credenciales de conexión
DB_CONFIG = {
    "dbname": "fastlogistics_db",
    "user": "postgres",       # Tu usuario de PostgreSQL
    "password": "",    # Cambia por tu contraseña de PostgreSQL
    "host": "localhost",
    "port": "5432"
}

def obtener_conexion():
    try:
        conn = psycopg2.connect(**DB_CONFIG)
        return conn
    except Exception as e:
        print(f"❌ Error al conectar a PostgreSQL: {e}")
        return None

def inicializar_base_de_datos(clientes_dataset):
    """Crea la tabla automáticamente e inserta/reinicia el dataset inicial"""
    conn = obtener_conexion()
    if not conn:
        return

    cursor = conn.cursor()

    # 1. Crear la tabla automáticamente si no existe en la BD
    create_table_sql = """
    CREATE TABLE IF NOT EXISTS entregas (
        id SERIAL PRIMARY KEY,
        cliente_id INT NOT NULL,
        nombre_cliente VARCHAR(100),
        paquete VARCHAR(100),
        posicion_x INT,
        posicion_y INT,
        estado VARCHAR(20) DEFAULT 'Pendiente',
        repartidor_asignado VARCHAR(100),
        fecha_entrega TIMESTAMP
    );
    """
    cursor.execute(create_table_sql)

    # 2. Limpiar datos de simulaciones pasadas para reiniciar desde cero
    cursor.execute("TRUNCATE TABLE entregas;")

    # 3. Poblar la tabla desde el dataset de Python
    for c in clientes_dataset:
        cursor.execute("""
            INSERT INTO entregas (cliente_id, nombre_cliente, paquete, posicion_x, posicion_y, estado)
            VALUES (%s, %s, %s, %s, %s, 'Pendiente');
        """, (c["id"], c["nombre"], c["paquete"], c["x"], c["y"]))

    conn.commit()
    cursor.close()
    conn.close()
    print("✅ Tabla 'entregas' verificada y dataset poblado en PostgreSQL.")

def registrar_entrega_en_bd(cliente_id, nombre_repartidor):
    """Actualiza en PostgreSQL cuando un vehículo entrega un paquete en Pygame"""
    conn = obtener_conexion()
    if not conn:
        return

    cursor = conn.cursor()
    cursor.execute("""
        UPDATE entregas 
        SET estado = 'Entregado',
            repartidor_asignado = %s,
            fecha_entrega = %s
        WHERE cliente_id = %s;
    """, (nombre_repartidor, datetime.now(), cliente_id))

    conn.commit()
    cursor.close()
    conn.close()
    print(f"💾 BD Actualizada: Cliente ID {cliente_id} entregado por {nombre_repartidor}.")