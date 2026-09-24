# SplitBill API (backend)

Servidor de SplitBill: cuentas con login seguro, grupos con sus integrantes y gastos con sus partes.
Es la **entrega 2** del proyecto. La app Android (`../app`) todavía no lo usa; la conexión llega en la
entrega 3.

| | |
|---|---|
| Lenguaje | Java 17 |
| Framework | Spring Boot 4.1 (Web MVC, Data JPA, Security, Validation) |
| Base de datos | PostgreSQL 17, esquema versionado con Flyway |
| Autenticación | BCrypt para las contraseñas + JWT (HS256) |
| Documentación | Swagger UI en `/swagger-ui.html` |
| Pruebas | 25 de integración con MockMvc + Testcontainers (Postgres real) |

## Cómo levantarlo

Se necesita **Java 17** y **Docker Desktop encendido**. No hace falta instalar Maven ni PostgreSQL.

```bash
cd backend
cp .env.example .env          # opcional: cambiar credenciales y JWT_SECRET
docker compose up -d          # PostgreSQL en el puerto 5432
./mvnw spring-boot:run        # API en http://localhost:8080
```

En Windows (PowerShell o CMD) se usa `mvnw.cmd spring-boot:run`.

Luego se abre **http://localhost:8080/swagger-ui.html**:

1. `POST /api/auth/register` → copiar el `token` de la respuesta.
2. Botón **Authorize** → pegar el token.
3. Ya se pueden probar todos los endpoints desde el navegador.

## Pruebas

```bash
./mvnw test
```

Cada ejecución levanta un PostgreSQL temporal en Docker, aplica las migraciones de Flyway y corre las
pruebas contra él. Incluyen el mismo escenario de la prueba manual de la entrega 1 (Almuerzo, Gasolina
y Mercado; total $180.000).

## Endpoints

Todo exige `Authorization: Bearer <token>` salvo registro, login y Swagger. **Los montos van en
centavos**: $60.000 = `6000000`.

| Método | Ruta | Qué hace |
|---|---|---|
| POST | `/api/auth/register` | Crear cuenta y recibir token |
| POST | `/api/auth/login` | Iniciar sesión (el token dura 24 h) |
| GET / PUT | `/api/users/me` | Ver / editar mi perfil |
| GET / POST | `/api/groups` | Mis grupos / crear grupo |
| GET / PUT / DELETE | `/api/groups/{id}` | Ver / editar / borrar (editar y borrar: solo el dueño) |
| GET / POST | `/api/groups/{id}/members` | Integrantes / agregar por nombre o por email |
| DELETE | `/api/groups/{id}/members/{userId}` | Retirar integrante (solo el dueño) |
| GET / POST | `/api/groups/{id}/expenses` | Gastos del grupo / registrar gasto con sus partes |
| GET / PUT / DELETE | `/api/groups/{id}/expenses/{expenseId}` | Ver / editar / borrar gasto |

Los errores siempre tienen la misma forma (`ProblemDetail`, RFC 9457):

```json
{ "status": 400, "title": "Bad Request", "detail": "Las partes suman 99 centavos pero el gasto es de 100 centavos" }
```

## Decisiones de diseño

- **Mismo esquema que la app.** Las tablas y columnas se llaman igual que en `DatabaseContract` de
  Android (`use_`, `grp_`, `exp_`, `shr_`), con UUID como llave y montos en centavos (`BIGINT`).
  La tabla nueva es `group_members`.
- **Integrante ≠ cuenta.** Un integrante agregado por nombre es un `users` sin email ni contraseña,
  igual que en la app. Quien se registra sí los tiene.
- **El servidor no reparte, verifica.** La app calcula las partes con su `SplitStrategy`; el servidor
  comprueba que sumen **exactamente** el monto y que todos los participantes sean del grupo.
- **Reintentos seguros.** La app puede mandar su propio UUID. Si un `POST` se repite con el mismo id,
  no se duplica nada: es la base para la sincronización sin conexión de la entrega 3.
- **Permisos.** Quien no pertenece a un grupo recibe `404`, no `403`, para no revelar que existe.
- **Borrado lógico** con `status` 0/1 en todas las tablas, como en la app.
- **Nada de secretos en git.** Credenciales y clave JWT van en variables de entorno (`.env` local,
  ignorado por git). En producción (`SPRING_PROFILES_ACTIVE=prod`) la app no arranca si falta alguna.

## Estructura

```
ue.edu.co.splitbill
├── config/      SecurityConfig, JwtConfig, JwtProperties, OpenApiConfig
├── security/    JwtService (emite tokens), CurrentUser (lee el usuario del token)
├── entity/      User, Group, GroupMember, Expense, ExpenseShare, DatabaseContract
├── repository/  Spring Data JPA
├── service/     AuthService, UserService, GroupService, ExpenseService  (reglas y permisos)
├── controller/  AuthController, UserController, GroupController, ExpenseController
├── dto/         Records de entrada y salida (nunca se devuelve una entidad)
└── exception/   GlobalExceptionHandler y excepciones propias
```

## Despliegue

El `Dockerfile` construye una imagen con el perfil `prod`. Variables requeridas:

| Variable | Ejemplo |
|---|---|
| `DB_URL` | `jdbc:postgresql://host:5432/splitbill` |
| `DB_USER` / `DB_PASSWORD` | credenciales de la base |
| `JWT_SECRET` | mínimo 32 caracteres (`openssl rand -base64 48`) |
| `PORT` | opcional; lo fijan plataformas como Render o Railway |

```bash
docker build -t splitbill-api .
docker run -p 8080:8080 --env-file .env splitbill-api
```

**Fuera de alcance en esta entrega:** la liquidación se sigue calculando en la app
(`DebtSimplifier`); no hay refresh tokens, recuperación de contraseña ni invitaciones por correo.
