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
| `ExpenseDetailActivity` | Detalle de un gasto: quién pagó, cuándo, cómo se dividió y cuánto le toca a cada uno (con porcentaje si se dividió así). Desde aquí se puede eliminar |
| `AddExpenseActivity` | Registrar o editar un gasto: descripción, monto (o escanearlo), pagador, tipo de división y participantes |
| `ScanReceiptActivity` | Foto (cámara o galería) de una factura → propone el total y deja escoger otro valor |
| `SettlementActivity` | Saldo de cada integrante y transferencias mínimas para saldar |
| `QuickSplitActivity` | **Cuenta rápida**: divide una cuenta al momento con propina, sin registrar integrantes ni pagador; el total se puede escanear |

## Arquitectura

```
ue.edu.co.splitbill
├── domain/     Java puro, sin un solo import de Android. Aquí vive toda la lógica de negocio.
│   ├── Money                 Objeto de valor inmutable sobre centavos enteros
│   ├── split/                Interfaz SplitStrategy + 3 implementaciones + fábrica
│   ├── BalanceCalculator     Saldo neto = lo pagado − lo adeudado
│   ├── DebtSimplifier        Algoritmo voraz de liquidación
│   └── ReceiptParser         Encuentra el total en el texto de una factura
├── entity/     Entidades de Room
├── manager/    SplitBillDatabase, DatabaseContract (todo el SQL), Converters
├── dao/        @Dao con las consultas y sus proyecciones
├── model/      Repositorios sobre BaseRepository; ReceiptScanner (ML Kit)
├── permission/ PermissionManager: permisos peligrosos desde cualquier pantalla
├── di/         ServiceLocator y AppExecutors (io, network, mainThread)
├── network/    ApiService (Retrofit), AuthInterceptor, DTO y ApiMapper
├── session/    SessionManager y KeystoreTokenStore (token cifrado con AES-GCM)
├── sync/       SyncManager (push + pull) y NetworkMonitor
└── ui/         Activities sobre BaseActivity (auth, group, expense, settle, quick, contacts, scan)
```

Tres decisiones que vale la pena conocer antes de tocar el código:

**El dinero nunca es `double`.** `Money` guarda una cantidad entera de centavos. En punto flotante
`0.1 + 0.2` no da `0.3`, y en una app que reparte plata entre personas ese error se acumula hasta ser
visible. El reparto de los centavos sobrantes usa el método del residuo mayor, de modo que la suma de
las partes es **exactamente** igual al total.

**La forma de dividir es polimórfica.** `AddExpenseActivity` no conoce ninguna de las tres estrategias:
le pide una a `SplitStrategyFactory` según lo que el usuario escogió en el Spinner. Agregar una cuarta
forma de dividir no obliga a tocar ninguna pantalla.

**Las llaves primarias son UUID generados en el dispositivo**, no enteros autoincrementales. Es lo que
hará viable el funcionamiento sin conexión: una fila creada en el celular ya nace con su identificador
definitivo y el servidor la aceptará tal cual, sin reconciliar ids locales contra remotos.

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

Las pantallas **siempre** leen y escriben en Room. Cada fila guarda su `sync_status`
(`PENDING_CREATE`, `PENDING_DELETE`, `SYNCED`), y esa es la cola de cambios por enviar. El
`SyncManager` trabaja por detrás:

1. **Push**: sube la cola en orden (grupo → integrantes → gastos). Cada fila viaja con su UUID, así
   que reintentar nunca duplica nada en el servidor.
2. **Pull**: trae lo que otros integrantes cambiaron. Nunca pisa un cambio local pendiente.

Se sincroniza al abrir la pantalla principal, después de cada cambio, al tocar el botón de
sincronizar y **cada vez que vuelve la conexión** (`NetworkMonitor`). Además, cada cambio deja
programado un `SyncWorker` con **WorkManager**: si no hay red o el servidor no responde, Android lo
reintenta solo y lo sube **aunque la app esté cerrada**.

La primera sincronización trae todos los gastos del grupo; las siguientes piden solo los que
cambiaron (`?updatedSince=`, con la hora del servidor y un minuto de margen), incluidos los que
otro integrante borró. Si el servidor rechaza un
cambio (por ejemplo, partes que no suman el total), el cambio se descarta y se le avisa al usuario.
Si el token vence, la app vuelve al login sin cerrarse.

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

## Cómo compilar

Requiere Android Studio y un dispositivo o emulador con API 26 o superior.

```bash
./gradlew :app:assembleDebug        # compilar
./gradlew :app:installDebug         # instalar
./gradlew :app:testDebugUnitTest    # pruebas del dominio, sin emulador
./gradlew :app:connectedDebugAndroidTest   # SQLite, migraciones, sincronización y Espresso, con emulador
```

Para usar la app hay que tener el backend corriendo en el mismo computador (ver
[`backend/README.md`](backend/README.md)). Por defecto la app lo busca en `10.0.2.2:8080`, que es
el computador visto **desde el emulador**. Esa dirección no existe en un celular real.

**En un celular físico** (o para usar la misma dirección en todos los dispositivos), agregar a
`local.properties` (no se sube a git):

```
splitbill.apiBaseUrl=http://localhost:8080/
splitbill.adbReversePort=8080
```

La primera línea hace que la app busque el backend en `localhost`. La segunda hace que Gradle ejecute
`adb reverse tcp:8080 tcp:8080` en **todos los dispositivos conectados** antes de cada compilación
(también al darle Run en Android Studio). Así el `localhost:8080` del celular o del emulador llega al
backend del PC por la conexión de depuración, sin IP ni firewall. El celular tiene que estar conectado
por USB o por depuración inalámbrica.

> **Proyecto dentro de OneDrive:** la sincronización bloquea los archivos de `app\build` mientras Gradle
> compila, y falla con `Unable to delete directory` o en `dexBuilderDebug`. No es un error del código.
> Solución: agregar a `local.properties` la línea `splitbill.buildDir=C:/Temp/splitbill-build`,
> sincronizar Gradle (**Sync Now**) y borrar la carpeta `app\build` vieja. Las salidas quedan fuera de OneDrive.

Las pruebas de interfaz (Espresso) arrancan la app con `SplitBillTestRunner`: base de datos y token en
memoria y un servidor falso, así no borran los datos del emulador ni necesitan el backend. Conviene
desactivar las animaciones del emulador (Opciones de desarrollador → escalas de animación en 0).

## Documentación

- [Manual técnico](docs/manual-tecnico.md): arquitectura, modelo de datos, sincronización, seguridad, API y pruebas.
- [Manual de usuario](docs/manual-usuario.md): cada pantalla y sus flujos.

## Pendiente

- Desplegar el backend en un servicio público (Render, plan gratuito) y apuntar el `buildType`
  release a su URL HTTPS.
