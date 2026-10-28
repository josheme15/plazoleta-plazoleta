# plazoleta-plazoleta

Microservicio de **plazoleta** de la plazoleta de comidas (Spring Boot 3 + arquitectura hexagonal).
Responsable de restaurantes (y en próximas HU: platos y pedidos).

## Historias de usuario implementadas

| HU | Descripción | Endpoint | Rol |
|----|-------------|----------|-----|
| HU-2 | Crear restaurante | `POST /restaurants` | ADMIN |

### Reglas de la HU-2

- Campos obligatorios: nombre, NIT, dirección, teléfono, urlLogo e id del propietario.
- El id del propietario debe corresponder a un usuario con rol **propietario** (se consulta al microservicio de usuarios).
- NIT solo numérico y **único**.
- Teléfono: máximo 13 caracteres, solo dígitos y `+` opcional al inicio (ej. `+573005698325`).
- El nombre puede contener números, pero no puede ser solo números.

## Dependencia con plazoleta-usuarios

Este servicio **no emite tokens**: valida los JWT emitidos por `plazoleta-usuarios` (misma `JWT_SECRET`)
y consulta `GET /users/{id}` en ese servicio, reenviando el token, para validar el rol del propietario.

```
Admin ──POST /restaurants (JWT)──► plazoleta ──GET /users/{id} (JWT)──► usuarios
```

## Requisitos

- Java 21+
- Maven 3.9+
- Docker (para PostgreSQL)
- `plazoleta-usuarios` en ejecución (puerto 8081)

## Cómo levantarlo

```bash
docker compose up -d          # PostgreSQL en localhost:5433
mvn spring-boot:run           # Servicio en http://localhost:8082
```

Swagger UI: http://localhost:8082/swagger-ui.html
OpenAPI JSON: http://localhost:8082/v3/api-docs

### Variables de entorno

| Variable | Default |
|----------|---------|
| `SERVER_PORT` | `8082` |
| `DB_URL` | `jdbc:postgresql://localhost:5433/plazoleta_plazoleta` |
| `DB_USERNAME` / `DB_PASSWORD` | `postgres` / `postgres` |
| `JWT_SECRET` | misma clave que `plazoleta-usuarios` |
| `USUARIOS_URL` | `http://localhost:8081` |

## Uso rápido

```bash
# 1. Token de ADMIN (microservicio de usuarios)
curl -X POST http://localhost:8081/auth/login -H "Content-Type: application/json" \
  -d '{"email":"admin@plazoleta.com","password":"Admin123*"}'

# 2. Crear restaurante con el token obtenido
curl -X POST http://localhost:8082/restaurants \
  -H "Authorization: Bearer <TOKEN>" -H "Content-Type: application/json" \
  -d '{"name":"Restaurante El Sabor","nit":"900123456","address":"Calle 10 # 20-30","phone":"+573005698325","urlLogo":"https://misitio.com/logo.png","ownerId":2}'
```

## Pruebas con Postman

En la carpeta `postman/`:

- `plazoleta-plazoleta.postman_collection.json`
- `plazoleta-local.postman_environment.json`: `baseUrl` (8082), `usuariosUrl` (8081) y credenciales del admin.

1. En Postman: **Import** → seleccionar ambos archivos.
2. Seleccionar el entorno **Plazoleta - Local (plazoleta)**.
3. Clic derecho sobre la colección → **Run collection** → **Run**.

La carpeta `00 - Preparacion` obtiene el token del ADMIN y crea un propietario en `plazoleta-usuarios`;
luego `01 - HU-2` y `02 - Seguridad` prueban la creación de restaurantes. Los NIT se generan únicos en cada ejecución.

## Arquitectura

```
domain/            Java puro: modelo Restaurant, puertos (api / spi), caso de uso, excepciones
application/       DTOs, mappers (MapStruct) y handlers
infrastructure/    Controller REST, adaptador JPA, cliente HTTP a usuarios, seguridad JWT,
                   manejo de errores y configuración
```

El caso de uso valida el rol del propietario a través del puerto `IUserClientPort`;
su implementación (`UserRestClientAdapter`) usa `RestClient` para llamar al microservicio de usuarios.

## Pruebas

```bash
mvn test                      # Reporte de cobertura en target/site/jacoco/index.html
```
