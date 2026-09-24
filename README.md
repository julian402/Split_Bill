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

| | |
|---|---|
| Lenguaje | Java 11 |
| `minSdk` / `targetSdk` | 26 / 36 |
| Persistencia | Room (capa sobre SQLite) |
| Red | Retrofit 3 + Gson, token JWT cifrado con el Android Keystore |
| Pruebas | 50 unitarias + 14 instrumentadas |

## Pantallas

| Pantalla | Qué hace |
|---|---|
| `LoginActivity` | Inicio de sesión. Es la primera pantalla; si ya hay sesión, pasa directo a la principal |
| `RegisterActivity` | Crear cuenta (queda con la sesión iniciada) |
| `MainActivity` | Total del grupo, lista de gastos, estado de la sincronización, sincronizar y cerrar sesión |
| `MembersActivity` | Alta, listado y baja de integrantes |
| `ExpenseDetailActivity` | Detalle de un gasto: quién pagó, cuándo, cómo se dividió y cuánto le toca a cada uno (con porcentaje si se dividió así). Desde aquí se puede eliminar |
| `AddExpenseActivity` | Registrar un gasto: descripción, monto, pagador, tipo de división y participantes |
| `SettlementActivity` | Saldo de cada integrante y transferencias mínimas para saldar |
| `QuickSplitActivity` | **Cuenta rápida**: divide una cuenta al momento con propina, sin registrar integrantes ni pagador |

## Arquitectura

```
ue.edu.co.splitbill
├── domain/     Java puro, sin un solo import de Android. Aquí vive toda la lógica de negocio.
│   ├── Money                 Objeto de valor inmutable sobre centavos enteros
│   ├── split/                Interfaz SplitStrategy + 3 implementaciones + fábrica
│   ├── BalanceCalculator     Saldo neto = lo pagado − lo adeudado
│   └── DebtSimplifier        Algoritmo voraz de liquidación
├── entity/     Entidades de Room
├── manager/    SplitBillDatabase, DatabaseContract (todo el SQL), Converters
├── dao/        @Dao con las consultas y sus proyecciones
├── model/      Repositorios sobre BaseRepository
├── di/         ServiceLocator y AppExecutors (io, network, mainThread)
├── network/    ApiService (Retrofit), AuthInterceptor, DTO y ApiMapper
├── session/    SessionManager y KeystoreTokenStore (token cifrado con AES-GCM)
├── sync/       SyncManager (push + pull) y NetworkMonitor
└── ui/         Activities sobre BaseActivity, y adaptadores de RecyclerView
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
sincronizar y **cada vez que vuelve la conexión** (`NetworkMonitor`). Si el servidor rechaza un
cambio (por ejemplo, partes que no suman el total), el cambio se descarta y se le avisa al usuario.
Si el token vence, la app vuelve al login sin cerrarse.

Al pasar de la versión 1 a la 2 de la base de datos (`MIGRATION_1_2`) no se pierde nada: al iniciar
sesión, los gastos que ya había en el celular se suben a la cuenta.

## Cómo compilar

Requiere Android Studio y un dispositivo o emulador con API 26 o superior.

```bash
./gradlew :app:assembleDebug        # compilar
./gradlew :app:installDebug         # instalar
./gradlew :app:testDebugUnitTest    # pruebas del dominio, sin emulador
./gradlew :app:connectedDebugAndroidTest   # SQLite, migración y sincronización, con emulador
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

## Pendiente

- **Entrega 4** — Cámara con OCR de facturas (ML Kit), lectura de contactos y varios grupos.
