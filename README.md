# ms-articulos

Microservicio de gestión de artículos de bodega del sistema BodegaExpress.
Expone una API REST para crear, consultar, actualizar y eliminar artículos y su
stock, con persistencia en PostgreSQL. Cada operación de escritura queda
registrada en un historial.

## Tecnologías

- Java 17
- Spring Boot 4.1.1 (Spring Web MVC, Spring Data JPA, Jakarta Validation)
- PostgreSQL
- Lombok
- Maven (mediante Maven Wrapper)

## Requisitos previos

| Herramienta | Versión | Notas |
|---|---|---|
| JDK | 17 o superior | La variable `JAVA_HOME` debe apuntar al JDK |
| PostgreSQL | 14 o superior | Servicio en ejecución y accesible |
| Git | Cualquiera reciente | Para clonar el repositorio |
| Postman | Opcional | Para probar la API manualmente |

No es necesario instalar Maven: el repositorio incluye Maven Wrapper (`mvnw` / `mvnw.cmd`).

## Instalación y ejecución local

Los comandos se muestran para Windows (PowerShell). En Linux o macOS, reemplazar
`.\mvnw.cmd` por `./mvnw` y `copy` por `cp`.

### 1. Clonar el repositorio

```powershell
git clone https://github.com/BodegaExpressDuoc/ms-articulos.git
cd ms-articulos
```

### 2. Crear la base de datos

Desde pgAdmin o psql, con un usuario con permisos de creación:

```sql
CREATE DATABASE bodega_articulos;
```

Las tablas `articulos` e `historial_articulos` no se crean manualmente:
Hibernate las crea o actualiza al iniciar la aplicación
(`spring.jpa.hibernate.ddl-auto: update`).

### 3. Configurar las variables de entorno

Las credenciales no se escriben en `application.yaml`; se leen desde variables de
entorno. Para desarrollo local se usa un archivo `.env`, excluido de Git:

```powershell
copy .env.example .env
```

Editar `.env` y completar al menos `ARTICULOS_DB_PASSWORD`:

| Variable | Obligatoria | Valor por defecto | Descripción |
|---|---|---|---|
| `ARTICULOS_DB_URL` | No | `jdbc:postgresql://localhost:5432/bodega_articulos` | URL JDBC de la base de datos |
| `ARTICULOS_DB_USERNAME` | No | `postgres` | Usuario de PostgreSQL |
| `ARTICULOS_DB_PASSWORD` | **Sí** | — | Contraseña de PostgreSQL |
| `ARTICULOS_PORT` | No | `8082` | Puerto HTTP del servicio |

El archivo se escribe con el formato `NOMBRE=valor`, sin comillas. Spring Boot lo
carga mediante `spring.config.import`, sin dependencias adicionales, y debe
ubicarse en la carpeta desde donde se ejecuta la aplicación (la raíz del
proyecto). Las variables definidas en el sistema operativo tienen prioridad
sobre el archivo.

### 4. Compilar, probar y empaquetar

```powershell
.\mvnw.cmd clean install
```

Ejecuta las pruebas y genera el archivo `target/ms-articulos-0.0.1-SNAPSHOT.jar`.
Las pruebas usan MockMvc y repositorios simulados, por lo que no requieren
PostgreSQL.

### 5. Ejecutar

Con Maven:

```powershell
.\mvnw.cmd spring-boot:run
```

O con el `.jar` generado:

```powershell
java -jar target/ms-articulos-0.0.1-SNAPSHOT.jar
```

El servicio queda disponible en `http://localhost:8082/api/articulos`.
Para detenerlo, usar `Ctrl+C`.

## Estructura del proyecto

```text
src/main/java/cl/duoc/bodegaexpress/articulos/
├── MsArticulosApplication.java   Punto de entrada de la aplicación
├── controller/                   Endpoints REST
├── service/                      Lógica de negocio y transacciones
├── repository/                   Acceso a datos con Spring Data JPA
└── model/                        Entidades JPA (Articulo, HistorialArticulo)
```

## API

URL base: `http://localhost:8082/api/articulos`

| Método | Ruta | Descripción | Respuesta |
|---|---|---|---|
| GET | `/api/articulos` | Lista todos los artículos | `200` |
| GET | `/api/articulos/{id}` | Obtiene un artículo | `200`; `404` si no existe |
| POST | `/api/articulos` | Crea un artículo | `201` con cabecera `Location` |
| PUT | `/api/articulos/{id}` | Reemplaza los datos de un artículo | `200`; `404` si no existe |
| DELETE | `/api/articulos/{id}` | Elimina un artículo | `204`; `404` si no existe |

POST y PUT requieren `Content-Type: application/json`:

```json
{
  "nombre": "Caja",
  "descripcion": "Caja mediana",
  "precio": 2500.50,
  "stock": 10
}
```

- Todos los campos son obligatorios.
- El precio y el stock no pueden ser negativos; el precio admite hasta dos decimales.
- El `id` lo genera la base de datos.
- Los datos inválidos devuelven `400 Bad Request`.

### Historial

Cada creación, actualización y eliminación guarda una copia del artículo en la
tabla `historial_articulos`, con el tipo de operación (`CREACION`,
`ACTUALIZACION`, `ELIMINACION`) y la fecha. El historial se conserva aunque el
artículo se elimine.

## Pruebas manuales

1. Iniciar el servicio (paso 5).
2. Enviar un POST a `http://localhost:8082/api/articulos` con el JSON de ejemplo
   (en Postman: **Body → raw → JSON**) y guardar el `id` de la respuesta.
3. Consultar con GET `/api/articulos` y GET `/api/articulos/{id}`.
4. Modificar con PUT `/api/articulos/{id}`.
5. Verificar los datos en PostgreSQL:

   ```sql
   SELECT * FROM articulos ORDER BY id;
   SELECT * FROM historial_articulos ORDER BY id;
   ```

6. Eliminar con DELETE `/api/articulos/{id}`; un GET posterior devuelve `404`.
7. Enviar un POST con el nombre vacío o un stock negativo para obtener `400`.

## Servicios relacionados

| Servicio | Puerto | Ruta base |
|---|---|---|
| ms-usuarios | 8081 | `/api/usuarios` |
| ms-articulos | 8082 | `/api/articulos` |
| ms-busqueda | 8083 | `/api/busqueda` |

ms-busqueda consulta este servicio para resolver las búsquedas de artículos.
