# plazoleta-plazoleta

Microservicio de **plazoleta** de la plazoleta de comidas (Spring Boot 3 + arquitectura hexagonal).
Responsable de restaurantes y platos (y en próximas HU: pedidos).

## Historias de usuario implementadas

| HU | Descripción | Endpoint | Rol |
|----|-------------|----------|-----|
| HU-2 | Crear restaurante | `POST /restaurants` | ADMIN |
| HU-3 | Crear plato | `POST /dishes` | OWNER (propietario del restaurante) |
| HU-4 | Editar plato (precio y descripción) | `PATCH /dishes/{id}` | OWNER (propietario del restaurante) |
| HU-5 | Autenticación: validación de token y rol en todos los endpoints | — | — |
| HU-6 | Consultar restaurante por id (usado por usuarios al crear empleados) | `GET /restaurants/{id}` | Autenticado |
| HU-7 | Activar / desactivar plato | `PATCH /dishes/{id}/status` | OWNER (propietario del restaurante) |

### Reglas de la HU-2

- Campos obligatorios: nombre, NIT, dirección, teléfono, urlLogo e id del propietario.
- El id del propietario debe corresponder a un usuario con rol **propietario** (se consulta al microservicio de usuarios).
- NIT solo numérico y **único**.
- Teléfono: máximo 13 caracteres, solo dígitos y `+` opcional al inicio (ej. `+573005698325`).
- El nombre puede contener números, pero no puede ser solo números.

### Reglas de la HU-3

- Solo el **propietario del restaurante** puede crear platos: el token debe tener rol `OWNER` y su id
  (claim `id` del JWT) debe coincidir con el `ownerId` del restaurante (si no, 403).
- Campos obligatorios: nombre, precio, descripción, urlImagen, categoría e id del restaurante.
- Precio: número entero positivo mayor a 0 (se rechazan `0`, negativos y decimales).
- El plato se asocia a un restaurante y a una categoría existentes (si no existen, 404).
- Todo plato nuevo queda con `active = true`, aunque se envíe otro valor.

### Reglas de la HU-4

- Solo se pueden editar el **precio** y la **descripción**; cualquier otro campo enviado se ignora.
- Hay que enviar al menos uno de los dos (si no, 400).
- Precio: entero positivo mayor a 0; descripción: no puede quedar vacía (400).
- Solo el propietario del restaurante al que pertenece el plato puede editarlo (403); plato inexistente: 404.

### Reglas de la HU-7

- `PATCH /dishes/{id}/status` con `{"active": false}` deshabilita el plato y `{"active": true}` lo habilita.
- Solo el propietario puede hacerlo (rol `OWNER`, 403 en otro caso) y solo sobre platos de **su** restaurante (403).
- `active` es obligatorio (400); plato inexistente: 404. Enviar el mismo estado que ya tiene responde 200.
- Es un endpoint separado de editar plato (HU-4), que sigue permitiendo solo precio y descripción.

### Reglas de la HU-5

El login vive en `plazoleta-usuarios`. Este servicio valida en cada petición el JWT (firma y expiración) y el rol:

| Endpoint | Rol permitido | Sin token / token inválido | Rol incorrecto |
|----------|---------------|----------------------------|----------------|
| `POST /restaurants` | ADMIN | 401 | 403 |
| `POST /dishes` | OWNER, dueño del restaurante | 401 | 403 |
| `PATCH /dishes/{id}` | OWNER, dueño del restaurante | 401 | 403 |
| `GET /restaurants/{id}` | Cualquier usuario autenticado | 401 | — |
| `PATCH /dishes/{id}/status` | OWNER, dueño del restaurante | 401 | 403 |

`EndpointSecurityTest` verifica esta matriz con tokens reales; la regla "dueño del restaurante" se prueba en `DishUseCaseTest`.

Categorías precargadas al iniciar el servicio:

| id | Categoría |
|----|-----------|
| 1 | Entradas |
| 2 | Platos fuertes |
| 3 | Postres |
| 4 | Bebidas |
| 5 | Acompanamientos |

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

# 3. Crear plato con el token del PROPIETARIO de ese restaurante (login en usuarios con su correo)
curl -X POST http://localhost:8082/dishes \
  -H "Authorization: Bearer <TOKEN_PROPIETARIO>" -H "Content-Type: application/json" \
  -d '{"name":"Bandeja paisa","price":25000,"description":"Frijoles, arroz y carne","urlImage":"https://misitio.com/bandeja.png","categoryId":2,"restaurantId":1}'

# 4. Editar precio y/o descripcion del plato (mismo token del PROPIETARIO)
curl -X PATCH http://localhost:8082/dishes/1 \
  -H "Authorization: Bearer <TOKEN_PROPIETARIO>" -H "Content-Type: application/json" \
  -d '{"price":28000,"description":"Frijoles, arroz, carne y arepa"}'

# 5. Desactivar (o activar con true) el plato
curl -X PATCH http://localhost:8082/dishes/1/status \
  -H "Authorization: Bearer <TOKEN_PROPIETARIO>" -H "Content-Type: application/json" \
  -d '{"active":false}'
```

## Pruebas con Postman

En la carpeta `postman/`:

- `plazoleta-plazoleta.postman_collection.json`: 55 peticiones y 129 aserciones.
- `plazoleta-local.postman_environment.json`: `baseUrl` (8082), `usuariosUrl` (8081) y credenciales del admin.

1. En Postman: **Import** → seleccionar ambos archivos.
2. Seleccionar el entorno **Plazoleta - Local (plazoleta)**.
3. Clic derecho sobre la colección → **Run collection** → **Run**.

La carpeta `00 - Preparacion` obtiene el token del ADMIN y crea dos propietarios en `plazoleta-usuarios`;
luego `01 - HU-2` y `02 - Seguridad` prueban la creación de restaurantes, y `03 - HU-3` crea platos en el
restaurante del primer propietario (el segundo se usa para comprobar el 403). `04 - HU-4` edita ese plato y
`05 - HU-6` consulta el restaurante por id y `06 - HU-7` desactiva y vuelve a activar el plato.
Los NIT se generan únicos en cada ejecución.

## Arquitectura

```
domain/            Java puro: modelos Restaurant y Dish, puertos (api / spi), casos de uso, excepciones
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
