# SplitBill

Aplicación móvil nativa para Android que administra los gastos compartidos de un grupo: registra quién
pagó qué, calcula el saldo de cada integrante y determina el **conjunto mínimo de transferencias**
necesarias para saldar todas las deudas.

Proyecto de la asignatura Aplicaciones Móviles — Uniempresarial.
Acta de Constitución `SPLITBILL-v1`.

**Equipo:** Julián Corredor · Diomar Arias · Juan Rojano · Sofía Reyes

---

## Estado

**Entrega 1 completada.** Funciona de punta a punta y 100 % sin conexión, sobre SQLite local.
El backend, la sincronización, la cámara y los contactos llegan en las entregas siguientes.

| | |
|---|---|
| Lenguaje | Java 11 |
| `minSdk` / `targetSdk` | 26 / 36 |
| Persistencia | Room (capa sobre SQLite) |
| Pruebas | 43 unitarias + 6 instrumentadas |

## Pantallas

| Pantalla | Qué hace |
|---|---|
| `MainActivity` | Total del grupo y lista de gastos |
| `MembersActivity` | Alta, listado y baja de integrantes |
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
├── di/         ServiceLocator y AppExecutors
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

## Cómo compilar

Requiere Android Studio y un dispositivo o emulador con API 26 o superior.

```bash
./gradlew :app:assembleDebug        # compilar
./gradlew :app:installDebug         # instalar
./gradlew :app:testDebugUnitTest    # pruebas del dominio, sin emulador
./gradlew :app:connectedDebugAndroidTest   # pruebas de SQLite, con emulador
```

> Si el proyecto está dentro de una carpeta sincronizada con OneDrive, compilar desde la terminal puede
> fallar con `Unable to delete directory ...\app\build\...`. Es un bloqueo de archivos de OneDrive, no un
> error del código: desde Android Studio compila sin problema.

## Pendiente

- **Entrega 2** — API REST en Spring Boot + PostgreSQL con Spring Security, BCrypt y JWT.
- **Entrega 3** — Retrofit y sincronización offline-first.
- **Entrega 4** — Cámara con OCR de facturas (ML Kit), lectura de contactos y varios grupos.
