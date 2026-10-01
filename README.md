# ms-articulos

Microservicio de articulos con Java 17, Spring Boot y PostgreSQL.
Puerto predeterminado: 8082. Base separada: bodega_articulos.

## Preparacion de PostgreSQL

La base debe existir antes de iniciar la aplicacion. Su creacion queda pendiente
de autorizacion. Desde una conexion administrativa se puede ejecutar:

```sql
CREATE DATABASE bodega_articulos;
```

La configuracion local usa postgres como usuario y clave, igual que ms-usuarios.
Se puede sobrescribir con ARTICULOS_DB_USERNAME y ARTICULOS_DB_PASSWORD.
ARTICULOS_DB_URL permite cambiar la URL JDBC y ARTICULOS_PORT el puerto HTTP.
Hibernate crea o actualiza la tabla articulos al arrancar.

## Ejecutar desde esta carpeta

```powershell
.\mvnw.cmd clean test
.\mvnw.cmd spring-boot:run
```

Las pruebas HTTP usan MockMvc, el servicio real y un repositorio simulado.
No requieren PostgreSQL ni verifican persistencia o el arranque completo.

## API

| Metodo | Ruta | Resultado |
|---|---|---|
| GET | /api/articulos | 200, lista |
| GET | /api/articulos/{id} | 200, articulo; 404 si no existe |
| POST | /api/articulos | 201 y cabecera Location |
| PUT | /api/articulos/{id} | 200; 404 si no existe |
| DELETE | /api/articulos/{id} | 204; 404 si no existe |

POST y PUT reciben todos los campos siguientes:

```json
{
  "nombre": "Caja",
  "descripcion": "Caja mediana",
  "precio": 2500.50,
  "stock": 10
}
```

Nombre y descripcion son obligatorios. Precio y stock no pueden ser negativos.
El precio admite hasta dos decimales. Los datos invalidos devuelven 400.
El identificador lo genera la base; PUT conserva el identificador de la ruta.
