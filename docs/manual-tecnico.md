# SplitBill — Manual técnico

Proyecto de Aplicaciones Móviles (Uniempresarial), Acta `SPLITBILL-v1`.
Equipo: Julián Corredor · Diomar Arias · Juan Rojano · Sofía Reyes.

Este documento explica cómo está construido SplitBill y por qué. Para instalarlo y correrlo, ver el
[README](../README.md) y el [README del backend](../backend/README.md).

---

## 1. Visión general

```
┌──────────────── Android (Java 11) ────────────────┐        ┌──────── Backend (Java 17) ────────┐
│ Activities ─► Repositorios ─► Room (SQLite)       │        │ Spring Boot 4.1 · Spring Security │
│                    │              ▲               │  HTTPS │ JWT HS256 · BCrypt · Flyway       │
│                    └─► SyncManager ┴─ Retrofit ───┼───────►│ PostgreSQL 17                     │
│  CameraX + ML Kit · Contactos · PermissionManager │  JSON  │ Swagger UI · ProblemDetail        │
└───────────────────────────────────────────────────┘        └───────────────────────────────────┘
```

- **Offline-first.** Las pantallas nunca esperan al servidor: leen y escriben en Room. El
  `SyncManager` sube y baja los cambios por detrás.
- **El dominio es Java puro.** Dinero, estrategias de división, saldos, liquidación y lectura de
  facturas no importan nada de Android, y se prueban con JUnit en la JVM.
- **El servidor valida, pero no calcula.** El backend comprueba que las partes de un gasto sumen el
  monto y que todos sean integrantes. Repartir y liquidar lo hace la app.

## 2. Estructura de la app

| Paquete | Contenido |
|---|---|
| `domain` | `Money`, `SplitStrategy` (+ `EqualSplitStrategy`, `ExactAmountSplitStrategy`, `PercentageSplitStrategy`, `SplitStrategyFactory`), `BalanceCalculator`, `DebtSimplifier`, `ReceiptParser` |
| `entity` | Entidades de Room: `User`, `Group`, `GroupMember`, `Expense`, `ExpenseShare`, `SyncStatus` |
| `manager` | `SplitBillDatabase` (versión 3, migraciones escritas a mano), `DatabaseContract` (todo el SQL), `Converters` |
| `dao` | `@Dao` y proyecciones (`ExpenseListItem`, `GroupListItem`, `UserAmount`…) |
| `model` | Repositorios sobre `BaseRepository`, más `ReceiptScanner` (ML Kit) |
| `network` | `ApiService` (Retrofit), `ApiClient`, `AuthInterceptor`, `ApiMapper`, DTO |
| `session` | `SessionManager`, `TokenStore` / `KeystoreTokenStore` |
| `sync` | `SyncManager`, `SyncWorker` + `SyncScheduler` (WorkManager), `NetworkMonitor` |
| `permission` | `PermissionManager` |
| `di` | `ServiceLocator`, `AppExecutors` |
| `ui` | `BaseActivity` y las pantallas (`auth`, `group`, `expense`, `settle`, `quick`, `contacts`, `scan`) |

### Convenciones del curso

Todo el código sigue el estilo de los proyectos de la asignatura:

- `initObjects()` va al final de cada Activity.
- Listeners por referencia a método (`this.btnGuardar.setOnClickListener(this::addExpenseDB)`).
- `getData()` / `clearFields()` / `showToast()`.
- POJOs con `toString()` hecho con `StringBuilder`.
- Columnas con prefijo de tres letras y borrado lógico con `status`.
- Constantes SQL en `DatabaseContract`.
- `validar()` en las entidades.
- Código en inglés y comentarios en español.

### Patrones

- **Método plantilla.**
  - `BaseActivity` fija el arranque: layout, márgenes, `initObjects()` y luego `initListeners()`.
  - `BaseRepository` fija el esqueleto de cada operación: otro hilo, resultado en el hilo principal y
    errores al Log.
- **Estrategia + fábrica.** `AddExpenseActivity` no conoce las formas de dividir: se las pide a
  `SplitStrategyFactory`.
- **Inyección de dependencias manual.** `ServiceLocator` crea cada pieza una sola vez. Las pruebas de
  interfaz usan su segundo constructor para inyectar la base de datos en memoria, el token en memoria y
  el servidor falso.
- **Callbacks en vez de LiveData.** `DataCallback<T>` + `UiCallback<T>`, que no toca la pantalla si ya
  se cerró.

## 3. Modelo de datos

La base de datos local (Room) y la del servidor (PostgreSQL) usan los mismos nombres de tablas y
columnas.

| Tabla | Columnas principales | Notas |
|---|---|---|
| `users` | `use_id`, `use_names`, `use_email`, `use_phone` | Directorio de personas. Sin email = integrante sin cuenta |
| `groups` | `grp_id`, `grp_name`, `grp_currency`, `grp_owner_id`, `grp_status`, `grp_sync_status` | |
| `group_members` | `gmb_group_id`, `gmb_user_id`, `gmb_status`, `gmb_sync_status` | Llave compuesta. Quién está en cada grupo (v3) |
| `expenses` | `exp_id`, `exp_group_id`, `exp_payer_id`, `exp_description`, `exp_amount_cents`, `exp_split_type`, `exp_date`, `exp_status`, `exp_sync_status` | Monto en **centavos** (`long`) |
| `expense_shares` | `shr_expense_id`, `shr_user_id`, `shr_amount_cents` | Parte de cada participante. Suman exactamente el monto |

- Las llaves son **UUID generados en el celular**. Una fila creada sin conexión ya tiene su id
  definitivo, y reenviarla no duplica nada, porque el servidor reconoce el id.
- **Borrado lógico** en todo: un integrante retirado sigue apareciendo en los gastos viejos, y los
  saldos cuadran.
- Migraciones:
  - `1→2` (entrega 3): estado de sincronización y dueño del grupo.
  - `2→3` (entrega 4): tabla `group_members`, llenada con el único grupo que existía.

  Se prueban con `MigrationTest`. No se usa `fallbackToDestructiveMigration`: perder los datos del
  usuario no es una opción.

## 4. Algoritmos

**Dinero.**
- `Money` guarda centavos enteros. Nunca se usa `double`, porque `0.1 + 0.2 ≠ 0.3`.
- Los centavos que sobran al dividir se reparten por **residuo mayor**, así la suma de las partes es
  exactamente el total.

**División.** Tres estrategias:
- Partes iguales.
- Montos exactos, que deben sumar el total.
- Porcentajes, que deben sumar 100.

Todas devuelven `List<Share>`, y agregar una cuarta no toca ninguna pantalla.

**Saldos.** `BalanceCalculator`: saldo = lo pagado − lo que le correspondía. La suma de todos los
saldos es cero.

**Liquidación.** `DebtSimplifier` es voraz, con dos colas de prioridad: el mayor acreedor contra el
mayor deudor. Cada transferencia deja a alguien en cero, así que nunca pasa de *n − 1*
transferencias. La pantalla lo compara con las transferencias "directas", gasto por gasto; por ejemplo,
"3 transferencias en lugar de 9".

**Factura.** `ReceiptParser` recibe las líneas que reconoció ML Kit, con su posición, y:
1. Busca las líneas con `TOTAL`, sin contar `SUBTOTAL`, `TOTAL IVA` ni `TOTAL ARTÍCULOS`. Si el valor
   está en otra columna, lo junta con la palabra porque están a la misma altura. Entre varios totales
   gana el mayor, por ejemplo "TOTAL CON PROPINA".
2. Si no hay `TOTAL`, propone el valor más grande. No cuenta lo que el cliente entregó (`EFECTIVO`,
   `CAMBIO`).
3. Ignora NIT, fechas, teléfonos y porcentajes.
4. Entiende `$ 45.900`, `45.900,00`, `45,900.00` y `45900`: con un solo separador seguido de tres
   dígitos, es separador de miles.

El usuario siempre confirma el total, y puede escoger otro de los valores encontrados.

## 5. Sincronización

Cada fila lleva `sync_status` (`SYNCED`, `PENDING_CREATE`, `PENDING_UPDATE`, `PENDING_DELETE`). Esa es
la cola de salida, y el `SyncManager` hace:

1. **Push**, en orden de dependencias y de **todos** los grupos:
   1. Grupos (`POST`, o `PUT` si se renombraron).
   2. Integrantes, cada uno a su grupo.
   3. Gastos (`POST` / `PUT` / `DELETE`).
2. **Pull**:
   1. La lista de grupos: aparecen los nuevos y se ocultan los que el servidor ya no devuelve.
   2. Los integrantes y gastos del grupo actual. La primera vez trae todo; después solo
      `?updatedSince=<hora del servidor − 60 s>`, que incluye los borrados.

Reglas:
- Un cambio local pendiente **nunca** se pisa con lo que llega del servidor.
- **4xx**: el servidor rechazó el cambio. Se descarta, el pull restaura la versión del servidor y se
  avisa al usuario.
- **5xx o sin red**: el cambio queda en la cola. **401**: la sesión venció y se vuelve al login.

Cuándo se sincroniza:
- Al abrir la pantalla principal o la de grupos, después de cada cambio y con el botón de sincronizar.
- Cuando vuelve la red (`NetworkMonitor`).
- **Con la app cerrada**: cada cambio encola un `SyncWorker` único en WorkManager, con la restricción
  "hay red" y reintento exponencial.

## 6. Seguridad

- Contraseñas con **BCrypt**. El login responde el mismo mensaje si el email no existe o si la clave
  está mal.
- **JWT HS256** de 24 h. La app lo guarda **cifrado con AES-GCM** con una llave del Android Keystore
  (`KeystoreTokenStore`), y `AuthInterceptor` lo agrega a cada petición.
- El servidor responde **404 a quien no es integrante** de un grupo, así no revela que el grupo existe.
  Editar o borrar un grupo y retirar integrantes es solo para el dueño.
- Al cerrar sesión se borra la base de datos local. Si en el mismo celular entra otra persona, no ve los
  datos de la anterior.
- Los secretos (`.env`, `local.properties`) no se suben a git.
- **Permisos peligrosos** solo al usarlos, con explicación. La foto de la factura se procesa en memoria
  y no se guarda; la agenda no se sube, solo los contactos elegidos.

## 7. Hardware

**`PermissionManager`** es el flujo de `App_Permisos` hecho clase reutilizable. En cualquier pantalla:

```java
this.permissionManager = new PermissionManager(this);          // en initObjects()
this.permissionManager.request(PermissionManager.CAMERA, R.string.msgCameraRationale, this::startCamera);
```

1. Si ya está concedido, ejecuta la acción.
2. Si se negó una vez, primero explica para qué sirve el permiso.
3. Si se negó para siempre, ofrece **Abrir ajustes**.

Usa `registerForActivityResult(RequestMultiplePermissions)`, la forma actual de
`requestPermissions` + `onRequestPermissionsResult`.

**Cámara.** `ScanReceiptActivity`:
- CameraX, con `Preview` e `ImageCapture` en memoria, atado al ciclo de vida de la pantalla. La cámara
  se apaga mientras se revisa el resultado.
- Galería con el selector de fotos del sistema, que no necesita permiso.
- Devuelve `EXTRA_AMOUNT_CENTS` y `EXTRA_MERCHANT` con `setResult`. La usan igual la cuenta rápida y el
  formulario de gastos.

**Contactos.** `ContactRepository` consulta `ContactsContract.CommonDataKinds.Phone` con un
`ContentResolver`, fuera del hilo principal. Los contactos que ya son integrantes, por teléfono
normalizado o por nombre, salen marcados.

## 8. API

La documentación completa está en Swagger (`/swagger-ui.html`) y en el
[README del backend](../backend/README.md#endpoints).
- Montos en centavos.
- Errores en formato **ProblemDetail**; la app muestra el campo `detail`.

## 9. Pruebas

| Tipo | Cantidad | Qué cubren |
|---|---|---|
| Unitarias (JVM) | 57 | `Money`, las tres estrategias, saldos, liquidación, escenario completo de la entrega 1, `ReceiptParser`, `ApiMapper` |
| Instrumentadas | 21 | Consultas de Room, migraciones 1→2 y 2→3, `SyncManager` contra `MockWebServer` (push, pull, rechazos, sin red, token vencido, incremental, varios grupos) |
| Interfaz (Espresso) | 10 | Login; gasto sin monto; porcentajes que no suman 100; gasto válido en lista y total; editar desde el detalle; borrar con confirmación; grupo nuevo y cambiar de grupo; liquidación mínima; cuenta rápida → gasto |
| Backend (integración) | 29 | Endpoints con MockMvc + PostgreSQL real (Testcontainers) |

Las pruebas Espresso corren con `SplitBillTestRunner`, que arranca la app con:
- Room en memoria.
- Un `TokenStore` en memoria.
- `FakeBackend`: un `MockWebServer` que acepta el login y responde 503 a todo lo demás, así se prueba
  justamente el trabajo sin conexión.
- Los hilos de `AppExecutors` envueltos en un `CountingIdlingResource`, para que Espresso espere a la
  base de datos.

La cámara y los contactos no se automatizan porque abren diálogos del sistema. Su lógica se prueba
aparte (`ReceiptParser`, repositorios) y el flujo se verificó a mano en el emulador.

```bash
./gradlew :app:testDebugUnitTest
./gradlew :app:connectedDebugAndroidTest      # emulador encendido, animaciones desactivadas
cd backend && ./mvnw test                     # Docker encendido
```
