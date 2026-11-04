# plazoleta-plazoleta

Microservicio de **plazoleta** de la plazoleta de comidas (Spring Boot 3 + arquitectura hexagonal).
Responsable de restaurantes, platos y pedidos.

## Historias de usuario implementadas

| HU | Descripción | Endpoint | Rol |
|----|-------------|----------|-----|
| HU-2 | Crear restaurante | `POST /restaurants` | ADMIN |
| HU-3 | Crear plato | `POST /dishes` | OWNER (propietario del restaurante) |
| HU-4 | Editar plato (precio y descripción) | `PATCH /dishes/{id}` | OWNER (propietario del restaurante) |
| HU-5 | Autenticación: validación de token y rol en todos los endpoints | — | — |
| HU-6 | Consultar restaurante por id (usado por usuarios al crear empleados) | `GET /restaurants/{id}` | Autenticado |
| HU-7 | Activar / desactivar plato | `PATCH /dishes/{id}/status` | OWNER (propietario del restaurante) |
| HU-9 | Listar restaurantes | `GET /restaurants?page=0&size=10` | CLIENT |
| HU-10 | Platos por restaurante (menú) | `GET /restaurants/{id}/dishes?categoryId=2&page=0&size=10` | CLIENT |
| HU-11 | Realizar pedido | `POST /orders` | CLIENT |
| HU-12 | Pedidos del restaurante con filtro por estado | `GET /orders?status=PENDIENTE&page=0&size=10` | EMPLOYEE |
| HU-13 | Pedido "en preparación": asignarse un pedido | `PATCH /orders/{id}/assign` (+ listado `GET /orders`) | EMPLOYEE |
| HU-14 | Pedido listo: notificar al cliente por SMS con un PIN | `PATCH /orders/{id}/ready` | EMPLOYEE (asignado al pedido) |
| HU-15 | Pedido entregado validando el PIN del cliente | `PATCH /orders/{id}/deliver` | EMPLOYEE |
| HU-16 | Cancelar pedido | `PATCH /orders/{id}/cancel` | CLIENT (dueño del pedido) |
| HU-17 | Trazabilidad: registrar cada cambio de estado del pedido | (en cada cambio de estado → `plazoleta-trazabilidad`) | — |

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

### Reglas de la HU-17

- **Cada cambio de estado de un pedido se registra** en el microservicio `plazoleta-trazabilidad`
  (`POST /traces`), con el pedido, cliente, restaurante, empleado asignado, estado anterior y estado nuevo:

  | Acción | Cambio registrado |
  |--------|-------------------|
  | Realizar pedido (HU-11) | — → `PENDIENTE` |
  | Asignarse el pedido (HU-13) | `PENDIENTE` → `EN_PREPARACION` |
  | Pedido listo (HU-14) | `EN_PREPARACION` → `LISTO` |
  | Pedido entregado (HU-15) | `LISTO` → `ENTREGADO` |
  | Cancelar pedido (HU-16) | `PENDIENTE` → `CANCELADO` |

- Plazoleta reenvía el token de quien hizo el cambio (cliente o empleado).
- El pedido y su registro se guardan en la misma transacción: si `plazoleta-trazabilidad` no está disponible
  se responde **503** y el cambio de estado **no se aplica**, para que el historial nunca tenga huecos.
  Los intentos rechazados (400, 403, 404, 409) no generan registros.
- La consulta del historial (`GET /traces/orders/{id}`, solo el cliente dueño del pedido) está en
  `plazoleta-trazabilidad`.

### Reglas de la HU-16

- **Cancelar un pedido** con `PATCH /orders/{id}/cancel` (solo rol `CLIENT`, sin body): el pedido pasa a
  **`CANCELADO`**.
- Solo se pueden cancelar pedidos en estado **`PENDIENTE`**. En cualquier otro estado se responde **409** con el
  mensaje de la HU: `Lo sentimos, tu pedido ya está en preparación y no se puede cancelar`.
- El cliente sale del token: solo puede cancelar **sus propios pedidos** (403 si es de otro cliente).
  Pedido inexistente: 404.
- La notificación al usuario es el mensaje de la respuesta (la HU no pide SMS).
- Un pedido `CANCELADO` ya no cuenta como "en proceso": el cliente puede hacer un pedido nuevo (HU-11) y ningún
  empleado puede asignárselo (409).

### Reglas de la HU-15

- **Entregar un pedido** con `PATCH /orders/{id}/deliver` (solo rol `EMPLOYEE`) enviando el PIN de seguridad
  que el cliente recibió por SMS en la HU-14: `{"pin": "482913"}`.
- Solo los pedidos en estado **`LISTO`** pueden pasar a **`ENTREGADO`** (409 en cualquier otro estado).
- **`ENTREGADO` es un estado final**: ningún endpoint permite cambiarlo (asignarse 409, marcar listo 409,
  entregar otra vez 409), porque cada cambio de estado exige un estado anterior distinto de `ENTREGADO`.
- PIN obligatorio y de 6 dígitos (400); si no coincide con el del pedido: 400 `El PIN de seguridad es incorrecto`
  y el pedido sigue `LISTO`.
- Solo pedidos **del restaurante del empleado** (403). Pedido inexistente: 404. La HU no exige que lo entregue
  el mismo empleado asignado, así que puede hacerlo cualquier empleado del restaurante.

### Reglas de la HU-14

- **Marcar pedido como listo** con `PATCH /orders/{id}/ready` (solo rol `EMPLOYEE`, sin body).
- Solo el **empleado asignado** al pedido (HU-13) puede marcarlo (403 si es otro empleado), solo pedidos
  **del restaurante del empleado** (403) y en estado **`EN_PREPARACION`** (409). Pedido inexistente: 404.
- Se genera un **PIN de seguridad de 6 dígitos** (aleatorio con `SecureRandom`) y se envía por **SMS al
  celular del cliente**: plazoleta consulta el celular en `plazoleta-usuarios` (`GET /users/{id}`) y envía
  el SMS con `plazoleta-mensajeria` (`POST /sms`). Mensaje:
  `Tu pedido #10 esta listo. Presenta este PIN para reclamarlo: 074891`.
- El PIN se guarda en el pedido (columna `pin`) para validarlo al entregar, pero **no se devuelve** en la
  respuesta: el cliente solo lo recibe por SMS.
- Primero se envía el SMS y después se guarda el pedido como **`LISTO`**: si el SMS no se puede enviar
  (mensajería caída), el pedido **sigue `EN_PREPARACION`** y se responde 503 para reintentar.

### Reglas de la HU-13

- Los criterios de **listado** (filtro por estado, paginado, todos los campos y solo los pedidos del
  restaurante del empleado) son los mismos de la HU-12 y se cumplen con `GET /orders`.
- **Asignarse un pedido** con `PATCH /orders/{id}/assign` (solo rol `EMPLOYEE`, sin body):
  el campo `employeeId` queda con el **id de quien se asigna** (sale de su token) y el estado pasa a
  **`EN_PREPARACION`**.
- Solo pedidos **del restaurante del empleado** (403 si es de otro) y en estado **`PENDIENTE`**
  (409 si ya fue asignado o avanzó). Pedido inexistente: 404.

### Reglas de la HU-12

- Solo el rol `EMPLOYEE` puede listar pedidos (403 con otro rol, 401 sin token).
- **Solo los pedidos del restaurante al que pertenece el empleado:** el restaurante sale del claim
  `restaurantId` de su token (lo agrega plazoleta-usuarios al iniciar sesión); el empleado no lo envía.
  Si el token no trae restaurante, responde 403.
- **Filtro por estado** con `status` (opcional): `PENDIENTE`, `EN_PREPARACION`, `LISTO`, `ENTREGADO` o
  `CANCELADO`; un valor distinto responde 400. Sin `status` se listan todos.
- **Paginado** con `size` (pedidos por página) y `page` (desde 0); mismas reglas que la HU-9.
- Del más antiguo al más reciente, con **todos los campos** del pedido: `id`, `clientId`, `restaurantId`,
  `date`, `status`, `employeeId` y `dishes` (`dishId`, `quantity`).

### Reglas de la HU-11

- Solo el rol `CLIENT` puede realizar pedidos (403 con otro rol, 401 sin token).
- El pedido indica el **restaurante** y la lista de **platos** con su **cantidad**:
  ```json
  { "restaurantId": 1, "dishes": [ { "dishId": 12, "quantity": 2 }, { "dishId": 13, "quantity": 1 } ] }
  ```
- Todos los platos deben ser **del mismo restaurante** indicado (400); restaurante o plato inexistentes: 404.
- Restaurante y platos obligatorios, cantidad entera mayor a 0, sin platos repetidos en la lista (400).
- No se pueden pedir platos desactivados (400).
- El pedido queda en estado **PENDIENTE**, con la fecha y el cliente del token; sin empleado asignado.
- **Un solo pedido en proceso por cliente:** si tiene uno `PENDIENTE`, `EN_PREPARACION` o `LISTO`, responde 409.

Ciclo de vida del pedido: `PENDIENTE` → `EN_PREPARACION` → `LISTO` → `ENTREGADO` (o `CANCELADO`).
Tablas: `orders` (pedido) y `order_dishes` (platos del pedido con su cantidad).

### Reglas de la HU-10

- Solo el rol `CLIENT` puede ver el menú (403 con otro rol, 401 sin token).
- Lista los platos **del restaurante** indicado en la URL (404 si el restaurante no existe).
- **Filtro por categoría** opcional con `categoryId` (404 si la categoría no existe; lista vacía si el
  restaurante no tiene platos de esa categoría).
- **Paginado** con `size` (platos por página) y `page` (desde 0); mismas reglas que la HU-9.
- Solo se muestran platos **activos** (los desactivados en la HU-7 no aparecen), ordenados por nombre.
- Cada plato devuelve `id`, `name`, `price`, `description`, `urlImage` y `categoryId`.

### Reglas de la HU-9

- Solo el rol `CLIENT` puede listar restaurantes (403 con otro rol, 401 sin token).
- Orden **alfabético** por nombre, sin distinguir mayúsculas de minúsculas.
- Paginado: `size` indica cuántos restaurantes por página y `page` qué página (desde 0).
  Por defecto `page=0` y `size=10`; `page` negativo o `size` menor o igual a 0 responden 400.
- Cada restaurante devuelve **solo** `name` y `urlLogo`. La respuesta incluye además `page`, `size`,
  `totalElements` y `totalPages`.

```json
{
  "content": [ { "name": "Burger House", "urlLogo": "https://misitio.com/logo1.png" } ],
  "page": 0, "size": 10, "totalElements": 1, "totalPages": 1
}
```

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
| `GET /restaurants` | CLIENT | 401 | 403 |
| `GET /restaurants/{id}/dishes` | CLIENT | 401 | 403 |
| `POST /orders` | CLIENT | 401 | 403 |
| `GET /orders` | EMPLOYEE (pedidos de su restaurante) | 401 | 403 |
| `PATCH /orders/{id}/assign` | EMPLOYEE (pedidos de su restaurante) | 401 | 403 |

`EndpointSecurityTest` verifica esta matriz con tokens reales; la regla "dueño del restaurante" se prueba en `DishUseCaseTest`.

Categorías precargadas al iniciar el servicio:

| id | Categoría |
|----|-----------|
| 1 | Entradas |
| 2 | Platos fuertes |
| 3 | Postres |
| 4 | Bebidas |
| 5 | Acompanamientos |

## Dependencia con plazoleta-usuarios, plazoleta-mensajeria y plazoleta-trazabilidad

Este servicio **no emite tokens**: valida los JWT emitidos por `plazoleta-usuarios` (misma `JWT_SECRET`)
y consulta `GET /users/{id}` en ese servicio, reenviando el token, para validar el rol del propietario
(HU-2) y obtener el celular del cliente (HU-14). Para notificar que un pedido está listo llama a
`POST /sms` de `plazoleta-mensajeria`, también reenviando el token del empleado. En cada cambio de estado de
un pedido llama a `POST /traces` de `plazoleta-trazabilidad` (HU-17) con el token de quien hizo el cambio.

```
Admin ────POST /restaurants (JWT)────► plazoleta ──GET /users/{id} (JWT)──► usuarios
Empleado ─PATCH /orders/{id}/ready──► plazoleta ──GET /users/{id} (JWT)──► usuarios
                                                ├─POST /sms (JWT)───────► mensajeria
                                                └─POST /traces (JWT)────► trazabilidad
```

## Requisitos

- Java 21+
- Maven 3.9+
- Docker (para PostgreSQL)
- `plazoleta-usuarios` en ejecución (puerto 8081)
- `plazoleta-mensajeria` en ejecución (puerto 8083) para la HU-14
- `plazoleta-trazabilidad` en ejecución (puerto 8084) para cualquier cambio de estado de un pedido (HU-17)

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
| `MENSAJERIA_URL` | `http://localhost:8083` |
| `TRAZABILIDAD_URL` | `http://localhost:8084` |

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

# 6. Listar restaurantes como CLIENTE (registro en POST /users/client y login en usuarios)
curl "http://localhost:8082/restaurants?page=0&size=10" -H "Authorization: Bearer <TOKEN_CLIENTE>"

# 7. Ver el menu de un restaurante (opcional: filtrar por categoria)
curl "http://localhost:8082/restaurants/1/dishes?categoryId=2&page=0&size=10" -H "Authorization: Bearer <TOKEN_CLIENTE>"

# 8. Realizar un pedido como CLIENTE
curl -X POST http://localhost:8082/orders \
  -H "Authorization: Bearer <TOKEN_CLIENTE>" -H "Content-Type: application/json" \
  -d '{"restaurantId":1,"dishes":[{"dishId":1,"quantity":2}]}'

# 9. Listar los pedidos del restaurante como EMPLEADO (filtro opcional por estado)
curl "http://localhost:8082/orders?status=PENDIENTE&page=0&size=10" -H "Authorization: Bearer <TOKEN_EMPLEADO>"

# 10. Asignarse un pedido como EMPLEADO (pasa a EN_PREPARACION)
curl -X PATCH http://localhost:8082/orders/1/assign -H "Authorization: Bearer <TOKEN_EMPLEADO>"

# 11. Marcar el pedido como listo (pasa a LISTO y el cliente recibe el PIN por SMS)
curl -X PATCH http://localhost:8082/orders/1/ready -H "Authorization: Bearer <TOKEN_EMPLEADO>"

# 12. Entregar el pedido con el PIN que el cliente recibio por SMS (pasa a ENTREGADO)
curl -X PATCH http://localhost:8082/orders/1/deliver \
  -H "Authorization: Bearer <TOKEN_EMPLEADO>" -H "Content-Type: application/json" \
  -d '{"pin":"482913"}'

# 13. Cancelar un pedido PENDIENTE como CLIENTE (pasa a CANCELADO)
curl -X PATCH http://localhost:8082/orders/1/cancel -H "Authorization: Bearer <TOKEN_CLIENTE>"
```

## Pruebas con Postman

En la carpeta `postman/`:

- `plazoleta-plazoleta.postman_collection.json`: 176 peticiones y 369 aserciones.
- `plazoleta-local.postman_environment.json`: `baseUrl` (8082), `usuariosUrl` (8081), `trazabilidadUrl` (8084) y
  credenciales del admin.

1. En Postman: **Import** → seleccionar ambos archivos.
2. Seleccionar el entorno **Plazoleta - Local (plazoleta)**.
3. Clic derecho sobre la colección → **Run collection** → **Run**.

La carpeta `00 - Preparacion` obtiene el token del ADMIN y crea dos propietarios en `plazoleta-usuarios`;
luego `01 - HU-2` y `02 - Seguridad` prueban la creación de restaurantes, y `03 - HU-3` crea platos en el
restaurante del primer propietario (el segundo se usa para comprobar el 403). `04 - HU-4` edita ese plato y
`05 - HU-6` consulta el restaurante por id y `06 - HU-7` desactiva y vuelve a activar el plato.
`07 - HU-9` registra un cliente en usuarios y lista los restaurantes paginados.
`08 - HU-10` crea un postre y un plato desactivado, y consulta el menú con y sin filtro de categoría.
`09 - HU-11` registra su propio cliente, prueba los pedidos inválidos, realiza un pedido y comprueba que no
se pueda hacer otro mientras el primero esté en proceso.
`10 - HU-12` crea un empleado del restaurante y un pedido en otro restaurante, y lista los pedidos con y sin
filtro de estado (el pedido del otro restaurante no aparece).
`11 - HU-13` el empleado se asigna el pedido de la carpeta 09, que pasa a EN_PREPARACION con su id como
empleado asignado; también prueba reasignar (409) y asignarse un pedido de otro restaurante (403).
`12 - HU-14` (requiere `plazoleta-mensajeria` levantado) crea un segundo empleado, un cliente y un pedido
PENDIENTE; prueba los errores (pedido PENDIENTE 409, otro empleado 403, otro restaurante 403, 404, 400,
roles y token) y marca como LISTO el pedido de la carpeta 09. El SMS con el PIN se ve en la consola de
`plazoleta-mensajeria` (`[SMS SIMULADO] Para: ... | Mensaje: ...`).
`13 - HU-15` entrega ese pedido con el PIN. **Antes de ejecutarla**, copia el PIN del SMS de la consola de
mensajería en la variable de colección `orderPin` (clic en la colección → **Variables**), porque el PIN no se
devuelve en ninguna respuesta. Prueba PIN incorrecto, sin PIN y con formato inválido (400), pedido PENDIENTE
(409), otro restaurante (403), roles y token; entrega el pedido (ENTREGADO) y comprueba que ya no se pueda
entregar, asignar ni marcar listo otra vez (409).
`14 - HU-16` registra un cliente con un pedido PENDIENTE y lo cancela (CANCELADO); prueba el mensaje de la HU
al cancelar un pedido que ya no está PENDIENTE (409), otro cliente (403), roles y token, que no se pueda
cancelar dos veces ni asignar un pedido cancelado (409) y que el cliente pueda hacer un pedido nuevo.
`15 - HU-17` consulta en `plazoleta-trazabilidad` el historial de los pedidos de las carpetas anteriores: el pedido
de la carpeta 09 (PENDIENTE → EN_PREPARACION → LISTO → ENTREGADO) y el cancelado de la carpeta 14
(PENDIENTE → CANCELADO, sin registros de los intentos rechazados); prueba también otro cliente (403), pedido sin
registros (404), roles y token.
Los NIT se generan únicos en cada ejecución.

## Arquitectura

```
domain/            Java puro: modelos (Restaurant, Dish, Order...), puertos (api / spi), casos de uso, excepciones
application/       DTOs, mappers (MapStruct) y handlers
infrastructure/    Controller REST, adaptador JPA, clientes HTTP a usuarios, mensajeria y trazabilidad, seguridad JWT,
                   manejo de errores y configuración
```

El caso de uso valida el rol del propietario a través del puerto `IUserClientPort`;
su implementación (`UserRestClientAdapter`) usa `RestClient` para llamar al microservicio de usuarios.
El envío del SMS de la HU-14 pasa por el puerto `ISmsClientPort`, implementado por `SmsRestClientAdapter`.

Los pedidos tienen dos casos de uso: `OrderUseCase` (crear y listar) y `OrderStatusUseCase` (asignarse, listo,
entregado y cancelar). Ambos registran cada cambio de estado por el puerto `ITraceClientPort`, implementado por
`TraceRestClientAdapter` (HU-17).

## Pruebas

```bash
mvn test                      # Reporte de cobertura en target/site/jacoco/index.html
```
