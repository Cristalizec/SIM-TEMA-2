import psycopg2
from psycopg2.extensions import ISOLATION_LEVEL_AUTOCOMMIT

# Configuración de credenciales de PostgreSQL
DB_HOST = "localhost"
DB_PORT = "5432"
DB_USER = "postgres"       # Cambia por tu usuario de PostgreSQL
DB_PASS = "tu_password"    # Cambia por tu contraseña de PostgreSQL
DB_NAME = "clima_db"

def obtener_conexion():
    """Devuelve una conexión activa a la base de datos clima_db."""
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
        # 1. Conexión al servidor global
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
        CREATE TABLE IF NOT EXISTS reportes_climaticos (
            id SERIAL PRIMARY KEY,
            fecha_registro TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
            num_muestras INT NOT NULL,
            escenario_ssp VARCHAR(50) NOT NULL,
            emisiones_co2_ppm INT NOT NULL,
            sensibilidad_promedio FLOAT NOT NULL,
            retroalimentacion_val FLOAT NOT NULL,
            temp_promedio_2100 FLOAT NOT NULL,
            temp_p95_critica FLOAT NOT NULL,
            probabilidad_superar_2c FLOAT NOT NULL
        );
        """
        cursor.execute(sql_tabla)
        conn.commit()
        cursor.close()
        conn.close()
        print("✅ Tabla 'reportes_climaticos' lista y verificada.")

    except Exception as e:
        print(f"❌ Error al inicializar la base de datos: {e}")

def guardar_simulacion(muestras, ssp, co2, sens, retro, t_prom, t_p95, prob_2c):
    """Inserta los resultados del muestreo Monte Carlo en PostgreSQL."""
    try:
        conn = obtener_conexion()
        cursor = conn.cursor()

        query = """
        INSERT INTO reportes_climaticos 
        (num_muestras, escenario_ssp, emisiones_co2_ppm, sensibilidad_promedio, retroalimentacion_val, temp_promedio_2100, temp_p95_critica, probabilidad_superar_2c)
        VALUES (%s, %s, %s, %s, %s, %s, %s, %s);
        """
        valores = (muestras, ssp, co2, sens, retro, t_prom, t_p95, prob_2c)

        cursor.execute(query, valores)
        conn.commit()
        cursor.close()
        conn.close()
        return True, "¡Simulación climática guardada con éxito en PostgreSQL!"
    except Exception as e:
        return False, f"Error al guardar en BD:\n{e}"