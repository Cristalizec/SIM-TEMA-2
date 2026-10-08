import psycopg2
from psycopg2.extensions import ISOLATION_LEVEL_AUTOCOMMIT

# Configuración de credenciales de PostgreSQL
DB_HOST = "localhost"
DB_PORT = "5432"
DB_USER = "postgres"       # Cambia por tu usuario de PostgreSQL
DB_PASS = "tu_password"    # Cambia por tu contraseña de PostgreSQL
DB_NAME = "epidemia_db"

def obtener_conexion():
    """Devuelve una conexión activa a la base de datos epidemia_db."""
    return psycopg2.connect(
        host=DB_HOST,
        port=DB_PORT,
        user=DB_USER,
        password=DB_PASS,
        dbname=DB_NAME
    )

def inicializar_base_datos():
    """Verifica si la base de datos y la tabla existen, y las crea automáticamente."""
    try:
        # 1. Conexión inicial al servidor PostgreSQL por defecto
        conn_sys = psycopg2.connect(
            host=DB_HOST,
            port=DB_PORT,
            user=DB_USER,
            password=DB_PASS,
            dbname="postgres"
        )
        conn_sys.set_isolation_level(ISOLATION_LEVEL_AUTOCOMMIT)
        cursor_sys = conn_sys.cursor()

        # Crear BD si no existe
        cursor_sys.execute("SELECT 1 FROM pg_catalog.pg_database WHERE datname = %s;", (DB_NAME,))
        exists = cursor_sys.fetchone()
        if not exists:
            cursor_sys.execute(f'CREATE DATABASE "{DB_NAME}";')
            print(f"✅ Base de datos '{DB_NAME}' creada con éxito.")

        cursor_sys.close()
        conn_sys.close()

        # 2. Conexión a la BD del proyecto para crear la tabla
        conn = obtener_conexion()
        cursor = conn.cursor()

        sql_tabla = """
        CREATE TABLE IF NOT EXISTS reportes_simulacion (
            id SERIAL PRIMARY KEY,
            fecha_registro TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
            poblacion_total INT NOT NULL,
            tasa_contagio INT NOT NULL,
            pct_distanciamiento INT NOT NULL,
            capacidad_hospitalaria_pct INT NOT NULL,
            dias_duracion INT NOT NULL,
            pico_maximo_infectados INT NOT NULL,
            total_recuperados INT NOT NULL,
            colapso_sanitario BOOLEAN NOT NULL
        );
        """
        cursor.execute(sql_tabla)
        conn.commit()
        cursor.close()
        conn.close()
        print("✅ Tabla 'reportes_simulacion' lista y verificada.")

    except Exception as e:
        print(f"❌ Error al inicializar la base de datos: {e}")

def guardar_simulacion(poblacion, contagio, distanciamiento, hospital, dias, pico, recuperados, colapso):
    """Inserta los datos de una corrida de simulación en la BD."""
    try:
        conn = obtener_conexion()
        cursor = conn.cursor()

        query = """
        INSERT INTO reportes_simulacion 
        (poblacion_total, tasa_contagio, pct_distanciamiento, capacidad_hospitalaria_pct, dias_duracion, pico_maximo_infectados, total_recuperados, colapso_sanitario)
        VALUES (%s, %s, %s, %s, %s, %s, %s, %s);
        """
        valores = (poblacion, contagio, distanciamiento, hospital, dias, pico, recuperados, colapso)

        cursor.execute(query, valores)
        conn.commit()
        cursor.close()
        conn.close()
        return True, "¡Simulación guardada con éxito en PostgreSQL!"
    except Exception as e:
        return False, f"Error al guardar en BD:\n{e}"