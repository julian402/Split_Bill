# SplitBill

Aplicación móvil nativa para Android que administra los gastos compartidos de un grupo: registra quién
pagó qué, calcula el saldo de cada integrante y determina el **conjunto mínimo de transferencias**
necesarias para saldar todas las deudas.

Proyecto de la asignatura Aplicaciones Móviles — Uniempresarial.
Acta de Constitución `SPLITBILL-v1`.

**Equipo:** Julián Corredor · Diomar Arias · Juan Rojano · Sofía Reyes

---

## Estado

**Entrega 1 completada.** La app funciona de punta a punta y 100 % sin conexión, sobre SQLite local.

**Entrega 2 completada.** El backend (Spring Boot + PostgreSQL + JWT) vive en [`backend/`](backend/README.md).

**Entrega 3 completada.** La app se conecta al backend: registro e inicio de sesión reales, y
sincronización **offline-first**. Todo se sigue guardando primero en el celular, así que la app
funciona igual sin conexión, y los cambios se suben solos cuando vuelve la red.

**Entrega 4 completada.** Hardware y cierre: permisos en tiempo de ejecución (`PermissionManager`),
escaneo del total de una factura con la cámara (CameraX + ML Kit), integrantes desde los contactos,
varios grupos y pruebas Espresso. Documentación en [`docs/`](docs/).

> **¿Primera vez?** Sigue la guía [Cómo correr todo](#cómo-correr-todo-guía-para-el-equipo): backend, app,
> cámara, contactos y pruebas, paso a paso.

| | |
|---|---|
| Lenguaje | Java 11 |
| `minSdk` / `targetSdk` | 26 / 36 |
| Persistencia | Room (capa sobre SQLite) |
| Red | Retrofit 3 + Gson, token JWT cifrado con el Android Keystore |
| Hardware | CameraX 1.6 + ML Kit Text Recognition (en el celular), contactos (`ContentResolver`) |
| Pruebas | 57 unitarias + 31 instrumentadas (10 de interfaz con Espresso) · backend: 29 |

## Pantallas

| Pantalla | Qué hace |
|---|---|
| `LoginActivity` | Inicio de sesión. Es la primera pantalla; si ya hay sesión, pasa directo a la principal |
| `RegisterActivity` | Crear cuenta (queda con la sesión iniciada) |
| `MainActivity` | Nombre del grupo (toca para cambiar), total, lista de gastos, estado de la sincronización, sincronizar y cerrar sesión |
| `GroupsActivity` | Tus grupos con su total: crear, renombrar (quien lo creó) y cambiar de grupo |
| `MembersActivity` | Alta, listado y baja de integrantes; "Agregar desde contactos" y "¿Ya estabas en la lista?" |
| `ContactsActivity` | Elegir varios integrantes de la agenda, con buscador; marca los que ya están |
| `ExpenseDetailActivity` | Detalle de un gasto: quién pagó, cuándo, cómo se dividió y cuánto le toca a cada uno (con porcentaje si se dividió así) y si ya se subió al servidor. Desde aquí se edita o elimina |
| `AddExpenseActivity` | Registrar o editar un gasto: descripción, monto (o escanearlo), pagador, tipo de división y participantes |
| `ScanReceiptActivity` | Foto (cámara o galería) de una factura → propone el total y deja escoger otro valor |
| `SettlementActivity` | Saldo de cada integrante y transferencias mínimas para saldar |
| `QuickSplitActivity` | **Cuenta rápida**: divide una cuenta al momento con propina, sin registrar integrantes ni pagador; el total se puede escanear |

## Arquitectura

### Vista general

```
┌──────────────────── App Android (Java 11) ────────────────────┐          ┌────── Backend (Java 17) ──────┐
│                                                               │          │                               │
│  Activities ──► Repositorios ──► Room (SQLite en el celular)  │          │  Spring Boot 4.1 (REST)       │
│  (ui/)          (model/)            ▲                         │  HTTP +  │  Spring Security: JWT + BCrypt│
│                                     │ sync_status             │  JSON    │  JPA + Flyway                 │
│                                  SyncManager ── Retrofit ─────┼─────────►│  PostgreSQL 17 (Docker)       │
│                                  (+ WorkManager)              │  token   │  Swagger UI                   │
│                                                               │          │                               │
│  CameraX + ML Kit · Contactos · PermissionManager             │          └───────────────────────────────┘
└───────────────────────────────────────────────────────────────┘
```

- **Offline-first.** Las pantallas nunca esperan al servidor: leen y escriben en Room, y el
  `SyncManager` sube y baja los cambios por detrás. Sin internet, la app funciona igual.
- **El dominio es Java puro.** El dinero, la división, los saldos, la liquidación y la lectura de
  facturas no dependen de Android, y se prueban con JUnit sin celular.
- **El servidor valida, pero no calcula.** El backend comprueba que las partes de un gasto sumen el
  monto y que todos sean integrantes del grupo. Repartir y liquidar lo hace la app.

### Paquetes de la app

```
ue.edu.co.splitbill
├── domain/     Java puro, sin un solo import de Android. Aquí vive toda la lógica de negocio.
│   ├── Money                 Objeto de valor inmutable sobre centavos enteros
│   ├── split/                Interfaz SplitStrategy + 3 implementaciones + fábrica
│   ├── BalanceCalculator     Saldo neto = lo pagado − lo adeudado
│   ├── DebtSimplifier        Algoritmo voraz de liquidación
│   └── ReceiptParser         Encuentra el total en el texto de una factura
├── entity/     Entidades de Room: User, Group, GroupMember, Expense, ExpenseShare
├── manager/    SplitBillDatabase, DatabaseContract (todo el SQL), Converters
├── dao/        @Dao con las consultas y sus proyecciones
├── model/      Repositorios sobre BaseRepository; ReceiptScanner (ML Kit)
├── permission/ PermissionManager: permisos peligrosos desde cualquier pantalla
├── di/         ServiceLocator y AppExecutors (io, network, mainThread)
├── network/    ApiService (Retrofit), AuthInterceptor, DTO y ApiMapper
├── session/    SessionManager y KeystoreTokenStore (token cifrado con AES-GCM)
├── sync/       SyncManager (push + pull), SyncWorker + SyncScheduler (WorkManager), NetworkMonitor
└── ui/         Activities sobre BaseActivity (auth, group, expense, settle, quick, contacts, scan)
```

El backend (`backend/`) sigue la estructura clásica de Spring: `controller` → `service` → `repository`
→ `entity`, con DTO de entrada y salida y un manejador global de errores (ProblemDetail). Ver
[`backend/README.md`](backend/README.md).

### Patrones que se usan

| Patrón | Dónde | Para qué |
|---|---|---|
| **Método plantilla** | `BaseActivity`, `BaseRepository` | El padre fija los pasos: layout, `initObjects()`, `initListeners()` en las pantallas; otro hilo, resultado y errores en los repositorios. Cada hija llena solo su parte |
| **Estrategia + fábrica** | `SplitStrategy`, `SplitStrategyFactory` | Las tres formas de dividir se intercambian sin que la pantalla las conozca |
| **Repositorio** | `model/` | Las pantallas piden datos sin conocer Room, la red ni los hilos |
| **Inyección de dependencias manual** | `ServiceLocator` | Cada pieza se crea una vez. Las pruebas Espresso inyectan una base de datos en memoria y un servidor falso |
| **Bandeja de salida** | `sync_status` + `SyncManager` | La base local guarda su propia cola de cambios por enviar |
| **Observador** | `SyncListener`, `DataCallback` | El `SyncManager` y los repositorios avisan a la pantalla cuando terminan |

### Modelo de datos

Room (en el celular) y PostgreSQL (en el servidor) usan los mismos nombres de tablas y columnas, con
prefijo de tres letras y borrado lógico:

| Tabla | Guarda |
|---|---|
| `users` | Las personas. Sin email = integrante sin cuenta, agregado por nombre o desde los contactos |
| `groups` | Los grupos, con su dueño |
| `group_members` | Quién está en cada grupo. Una persona puede estar en varios |
| `expenses` | Los gastos: quién pagó, monto **en centavos**, tipo de división y fecha |
| `expense_shares` | Cuánto le toca a cada participante de un gasto. Suman exactamente el monto |

El detalle de cada columna, la seguridad y las pruebas está en el [manual técnico](docs/manual-tecnico.md).

### Decisiones que vale la pena conocer antes de tocar el código

**El dinero nunca es `double`.** `Money` guarda una cantidad entera de centavos. En punto flotante
`0.1 + 0.2` no da `0.3`, y en una app que reparte plata entre personas ese error se acumula hasta ser
visible. El reparto de los centavos sobrantes usa el método del residuo mayor, de modo que la suma de
las partes es **exactamente** igual al total.

**La forma de dividir es polimórfica.** `AddExpenseActivity` no conoce ninguna de las tres estrategias:
le pide una a `SplitStrategyFactory` según lo que el usuario escogió en el Spinner. Agregar una cuarta
forma de dividir no obliga a tocar ninguna pantalla.

**Las llaves primarias son UUID generados en el dispositivo**, no enteros autoincrementales. Es lo que
hace posible trabajar sin conexión: una fila creada en el celular ya nace con su identificador
definitivo y el servidor la acepta tal cual, sin reconciliar ids locales contra remotos. Si un envío
se corta y se repite, el servidor reconoce el id y no duplica el gasto.

**Las migraciones se escriben a mano.** La base local va en la versión 3, y cada cambio de esquema
tiene su `Migration` con una prueba (`MigrationTest`). No se usa `fallbackToDestructiveMigration`,
porque perder los gastos de un usuario al actualizar la app no es una opción.

**El token nunca queda en texto plano.** El JWT se guarda cifrado con AES-GCM, con una llave del
Android Keystore (`KeystoreTokenStore`). Al cerrar sesión se borran los datos del celular.

## El algoritmo de liquidación

Cuando cada integrante le transfiere por separado a cada uno de los demás, un grupo de *n* personas
termina haciendo muchas más transferencias de las necesarias. `DebtSimplifier` empareja siempre al mayor
acreedor con el mayor deudor usando dos colas de prioridad; como cada transferencia deja al menos a una
persona en cero, el resultado nunca supera **n − 1** transferencias.

Ejemplo real de la app, con cuatro personas y tres gastos por $180.000:

```
Saldos      Diomar  +$33.000   Julián  +$9.000
            Juan     −$3.000   Sofía  −$39.000

Resultado   3 transferencias en lugar de 9
            Sofía → Diomar   $33.000
            Sofía → Julián    $6.000
            Juan  → Julián    $3.000
```

## Sincronización sin conexión

Las pantallas **siempre** leen y escriben en Room. Cada fila guarda su `sync_status` (`SYNCED`,
`PENDING_CREATE`, `PENDING_UPDATE`, `PENDING_DELETE`), y esa es la cola de cambios por enviar. El
`SyncManager` trabaja por detrás:

1. **Push**: sube la cola de **todos** los grupos, en orden de dependencias: grupos (crear o
   renombrar) → integrantes (cada uno a su grupo) → gastos (crear, editar o borrar). Cada fila viaja
   con su UUID, así que reintentar nunca duplica nada en el servidor.
2. **Pull**: trae la lista de grupos (aparecen los nuevos y se ocultan los que ya no están), y los
   integrantes y gastos del grupo actual. Nunca pisa un cambio local pendiente.

| Respuesta del servidor | Qué hace la app |
|---|---|
| 2xx | Marca la fila como `SYNCED` |
| 4xx (el servidor rechazó el cambio) | Lo descarta, trae la versión del servidor y le avisa al usuario |
| 5xx o sin red | Deja el cambio en la cola para el próximo intento |
| 401 (token vencido) | Vuelve al login sin perder los cambios pendientes |

Se sincroniza al abrir la pantalla principal o la de grupos, después de cada cambio, al tocar el botón
de sincronizar y **cada vez que vuelve la conexión** (`NetworkMonitor`). Además, cada cambio deja
programado un `SyncWorker` con **WorkManager**: si no hay red o el servidor no responde, Android lo
reintenta solo y lo sube **aunque la app esté cerrada**.

La primera sincronización trae todos los gastos del grupo; las siguientes piden solo los que
cambiaron (`?updatedSince=`, con la hora del servidor y un minuto de margen), incluidos los que
otro integrante borró.

Al pasar de la versión 1 a la 2 de la base de datos (`MIGRATION_1_2`) no se pierde nada: al iniciar
sesión, los gastos que ya había en el celular se suben a la cuenta. La versión 3 (`MIGRATION_2_3`)
agrega `group_members` para los varios grupos y deja a cada persona en el grupo en el que estaba.

## Hardware y permisos

| Función | Permiso | Cómo se pide |
|---|---|---|
| Escanear factura | `CAMERA` | Al abrir el escáner. Si se niega, la **galería** sigue funcionando (el selector de fotos del sistema no necesita permiso) |
| Integrantes desde contactos | `READ_CONTACTS` | Al tocar "Agregar desde contactos" |

`PermissionManager` es el flujo del proyecto `App_Permisos` convertido en una clase reutilizable: revisa
qué falta, pide solo eso, explica para qué sirve si ya se negó una vez y, si se negó para siempre,
ofrece abrir los ajustes de la app.

El texto de la factura lo reconoce **ML Kit en el celular** (el modelo viene dentro del APK): funciona sin
internet y la foto no se guarda. `ReceiptParser` (dominio, con pruebas JUnit) busca la línea `TOTAL`
(no `SUBTOTAL`), junta la palabra y el valor aunque estén en columnas distintas, ignora NIT, fechas,
teléfonos, porcentajes y lo que el cliente entregó (`EFECTIVO`, `CAMBIO`), y entiende `$ 45.900`,
`45.900,00` y `45,900.00`. El escáner solo llena el campo del monto: no toca la división.

## Cómo correr todo (guía para el equipo)

Pasos para probar la app completa en tu computador: backend, app, cámara, contactos y pruebas. Se
hace una sola vez; después basta con los pasos 2 y 4.

### 1. Lo que necesitas instalado

| Programa | Para qué |
|---|---|
| **Git** | Clonar el repositorio |
| **JDK 17** | Correr el backend (Android Studio trae su propio JDK para la app) |
| **Docker Desktop** | La base de datos PostgreSQL del backend. **Tiene que estar abierto** antes de levantar el backend |
| **Android Studio** (reciente) | Compilar y correr la app. Con un emulador de API 26 o superior, o tu celular |

No hace falta instalar Maven ni PostgreSQL: el proyecto trae `mvnw` y la base corre en Docker.

```bash
git clone https://github.com/julian402/Split_Bill.git
cd Split_Bill
```

### 2. Levantar el backend

En una terminal, dentro de `backend/`:

```bash
cd backend
docker compose up -d          # PostgreSQL en el puerto 5432 (la primera vez descarga la imagen)
./mvnw spring-boot:run        # API en http://localhost:8080
```

- En Windows con PowerShell o CMD: `.\mvnw.cmd spring-boot:run` (en Git Bash sirve `./mvnw`).
- Está listo cuando la consola dice `Started SplitBillApiApplication`. **No cierres esa terminal**:
  si la cierras, el backend se apaga y la app muestra "Sin conexión".
- Para comprobarlo, abre http://localhost:8080/swagger-ui.html en el navegador.
- El archivo `.env` es opcional: sin él se usan los valores de desarrollo (usuario y clave `splitbill`).
  Para cambiarlos, copia `.env.example` como `.env`.
- Cada computador tiene **su propia base de datos**. Las cuentas que creó otra persona no existen en
  la tuya: regístrate desde la app.

### 3. Configurar la app (una sola vez)

1. Abre la carpeta `Split_Bill` en Android Studio y espera a que termine el **Gradle Sync**. Crea el
   archivo `local.properties` con la ruta de tu SDK.
2. Agrega estas líneas al final de `local.properties`. El archivo no se sube a git, así que cada quien
   pone las suyas:

```properties
# La app busca el backend en localhost, y Gradle ejecuta "adb reverse" antes de cada compilación
splitbill.apiBaseUrl=http://localhost:8080/
splitbill.adbReversePort=8080

# Solo si el proyecto está dentro de OneDrive: saca las compilaciones fuera de la carpeta sincronizada
splitbill.buildDir=C:/Temp/splitbill-build
```

3. Haz clic en **Sync Now**. Si agregaste `splitbill.buildDir`, borra la carpeta `app\build` vieja.

Qué hace cada línea:
- **`splitbill.apiBaseUrl`**: la dirección del backend que usa la app. Sin ella la app usa
  `10.0.2.2:8080`, que es el computador visto desde el emulador, pero no sirve en un celular real.
- **`splitbill.adbReversePort`**: en cada Run, Gradle ejecuta `adb reverse tcp:8080 tcp:8080` en todos
  los dispositivos conectados. Así el `localhost:8080` del celular o del emulador llega a tu backend,
  sin configurar IP ni firewall.
- **`splitbill.buildDir`**: OneDrive bloquea `app\build` mientras Gradle compila, y el error se ve como
  `Unable to delete directory` o una falla en `dexBuilderDebug`. No es un error del código.

### 4. Correr la app

1. Con el backend encendido (paso 2), elige el emulador o tu celular y dale **Run ▶** en Android Studio.
   - **Celular físico**: activa la depuración USB (o la inalámbrica) y conéctalo. Tiene que aparecer en
     la lista de dispositivos.
2. En la app, **Crear cuenta** con tu nombre, email y una clave de al menos 8 caracteres. Quedas dentro
   de "Mi grupo".
3. Recorrido sugerido para probar todo lo de la entrega 4:
   1. **Grupos**: toca el nombre del grupo (arriba) → *Nuevo grupo* → "Viaje".
   2. **Contactos**: *Integrantes* → *Agregar desde contactos* → permitir → marca 2 o 3 → *Agregar*.
      Si usas el emulador, primero crea unos contactos en la app Contactos.
   3. **Escanear factura**: *Agregar gasto* → ícono de cámara en el monto.
      - En un celular real, toma la foto de una factura.
      - En el emulador la cámara muestra una sala virtual, así que usa **Galería**. Para tener una
        factura ahí, arrastra una imagen a la ventana del emulador, o usa
        `adb push factura.png /sdcard/Pictures/`.
   4. **Porcentajes**: registra otro gasto por porcentajes. Tócalo en la lista para ver el detalle, y
      prueba *Editar*.
   5. **Liquidar**: el plan de pagos con el mínimo de transferencias.
   6. **Sin conexión**: detén el backend (Ctrl+C en su terminal), agrega un gasto (sale "Sin conexión ·
      1 cambio pendiente"), vuelve a levantar el backend y toca ⟳. El gasto se sube solo. El modo avión
      **no** sirve para esta prueba, porque `adb reverse` va por el cable o la depuración y no por la red.

### 5. Correr las pruebas

```bash
# App (desde la raíz del proyecto)
./gradlew :app:testDebugUnitTest            # 57 pruebas del dominio, sin emulador
./gradlew :app:connectedDebugAndroidTest    # 31 pruebas con emulador: Room, migraciones, sincronización y Espresso

# Backend (desde backend/, con Docker abierto)
./mvnw test                                 # 29 pruebas contra un PostgreSQL temporal
```

- **Antes** de `connectedDebugAndroidTest`, desactiva las animaciones del emulador (Opciones de
  desarrollador → las tres "escalas de animación" en *Desactivada*). Al terminar, vuelve a activarlas.
- `connectedDebugAndroidTest` **desinstala la app al terminar**. Después hay que volver a darle Run e
  iniciar sesión. Si tienes el celular y el emulador conectados a la vez, las pruebas corren en los dos:
  desconecta el celular, o exporta `ANDROID_SERIAL=emulator-5554` antes de correrlas.
- Las pruebas Espresso no usan tu backend ni tus datos: arrancan la app con una base de datos en
  memoria y un servidor falso (`SplitBillTestRunner`).
- Si todas las pruebas de interfaz fallan con `RootViewWithoutFocusException` y la pantalla sale en
  blanco, reinicia el emulador (Cold Boot) y vuelve a correrlas.

### Problemas comunes

| Síntoma | Causa y solución |
|---|---|
| La app dice "Sin conexión" o "No hay conexión con el servidor" | El backend no está corriendo (paso 2), o falta `adb reverse`: dale Run de nuevo, o ejecuta `adb reverse tcp:8080 tcp:8080` |
| `./mvnw spring-boot:run` falla con `Connection refused` a 5432 | Docker Desktop cerrado, o faltó `docker compose up -d` |
| El puerto 8080 o el 5432 ya está en uso | Otro backend u otro PostgreSQL abierto: ciérralo, o cambia el puerto |
| `Unable to delete directory` o `dexBuilderDebug` al compilar | Proyecto dentro de OneDrive: agrega `splitbill.buildDir` (paso 3) |
| Android Studio no encuentra el APK después de cambiar `buildDir` | *File → Sync Project with Gradle Files* |
| El celular no aparece en Android Studio | Reconecta el cable, o vuelve a emparejar la depuración inalámbrica (*Pair devices using Wi-Fi*) |
| Se negó el permiso de cámara o contactos y ya no lo pregunta | En la app toca **Abrir ajustes** → Permisos, o en el celular: Ajustes → Apps → SplitBill → Permisos |
| "Email o contraseña incorrectos" con una cuenta de otro compañero | Cada computador tiene su propia base de datos: crea tu cuenta |

## Documentación

- [Manual técnico](docs/manual-tecnico.md): arquitectura, modelo de datos, sincronización, seguridad, API y pruebas.
- [Manual de usuario](docs/manual-usuario.md): cada pantalla y sus flujos.

## Pendiente

- Desplegar el backend en un servicio público (Render, plan gratuito) y apuntar el `buildType`
  release a su URL HTTPS.
