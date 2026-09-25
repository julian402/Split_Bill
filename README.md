# SplitBill

**Divide gastos en grupo sin pelear por las cuentas.**

SplitBill es una aplicación Android nativa para llevar los gastos compartidos de un viaje, una casa o
una salida con amigos. Registras quién pagó qué y la app calcula cuánto debe cada persona y cuál es el
**menor número de transferencias** para quedar a paz y salvo.

Funciona **sin conexión**: todo se guarda primero en el celular y se sincroniza solo con el servidor
cuando vuelve la red.

---

## Contenido

- [Características](#características)
- [Tecnologías](#tecnologías)
- [Requisitos](#requisitos)
- [Puesta en marcha](#puesta-en-marcha)
- [Configuración](#configuración)
- [Pruebas](#pruebas)
- [Despliegue en Render](#despliegue-en-render)
- [Arquitectura](#arquitectura)
- [Estructura del repositorio](#estructura-del-repositorio)
- [Solución de problemas](#solución-de-problemas)
- [Documentación](#documentación)
- [Hoja de ruta](#hoja-de-ruta)
- [Autor](#autor)

## Características

- **Grupos compartidos**: crea varios grupos y agrega gente a mano, desde los contactos o por email.
  Quien tiene cuenta ve el grupo en su propio celular, con los mismos gastos y saldos; quien no, puede
  estar en el grupo solo con su nombre y vincular su cuenta después.
- **Gastos**: tres formas de dividir (partes iguales, montos exactos o porcentajes), con categoría,
  fecha y una vista previa de cuánto le toca a cada uno mientras escribes.
- **Liquidación mínima**: un algoritmo voraz reduce las deudas a un máximo de *n − 1* transferencias.
  Cada transferencia se puede compartir por WhatsApp y marcar como pagada.
- **Escáner de facturas**: toma una foto o elige una de la galería y la app encuentra el total con
  reconocimiento de texto en el propio dispositivo (ML Kit). La foto no sale del celular.
- **Chat del grupo**: los integrantes con cuenta se escriben dentro de cada grupo. Funciona sin
  conexión: el mensaje sale cuando vuelve la red.
- **Cuenta rápida**: divide una cuenta al instante, con propina, sin registrar a nadie. Después puedes
  pasarla a un grupo o guardarla por aparte.
- **Inicio con resumen**: total gastado, gasto del mes, tu parte y tu balance en todos tus grupos.
- **Offline-first**: puedes crear, editar y borrar sin internet; los cambios se suben solos, incluso con
  la app cerrada.
- **Sesión segura**: autenticación con JWT y token cifrado con el Android Keystore.

## Tecnologías

| Capa | Tecnologías |
|---|---|
| App Android | Java 11 · `minSdk` 26 / `targetSdk` 36 · Material 3 · Room · Retrofit + Gson · WorkManager · CameraX · ML Kit Text Recognition |
| Backend | Java 17 · Spring Boot 4 · Spring Security (JWT + BCrypt) · Spring Data JPA · Flyway · springdoc-openapi |
| Base de datos | SQLite (Room) en el dispositivo · PostgreSQL 17 en el servidor |
| Pruebas | JUnit 4 · Espresso · MockWebServer · JUnit 5 + MockMvc + Testcontainers |

## Requisitos

- **JDK 17**, para el backend. Android Studio trae su propio JDK para compilar la app.
- **Docker Desktop**, para PostgreSQL. No hace falta instalar PostgreSQL ni Maven: el backend incluye
  `docker-compose.yml` y el wrapper `mvnw`.
- **Android Studio** (versión reciente) con un emulador de API 26 o superior, o un teléfono Android con
  la depuración USB activada.

## Puesta en marcha

### 1. Clonar el repositorio

```bash
git clone https://github.com/julian402/Split_Bill.git
cd Split_Bill
```

### 2. Levantar el backend

Con Docker Desktop abierto:

```bash
cd backend
docker compose up -d        # PostgreSQL en localhost:5432
./mvnw spring-boot:run      # API en http://localhost:8080
```

En Windows (PowerShell o CMD) usa `.\mvnw.cmd spring-boot:run`.

El servidor está listo cuando la consola muestra `Started SplitBillApiApplication`. Flyway crea las
tablas en el primer arranque. La documentación interactiva de la API queda en
<http://localhost:8080/swagger-ui.html>.

### 3. Configurar la app

Abre la carpeta raíz del proyecto en Android Studio y espera a que termine el *Gradle Sync*. Luego
agrega estas líneas a `local.properties`, que Android Studio crea con la ruta del SDK:

```properties
splitbill.apiBaseUrl=http://localhost:8080/
splitbill.adbReversePort=8080
```

Con esta configuración, cada vez que compilas, Gradle ejecuta `adb reverse tcp:8080 tcp:8080`, y la app
llega a tu backend tanto en el emulador como en un teléfono conectado por cable, sin configurar IP ni
firewall. Vuelve a sincronizar Gradle (**Sync Now**).

### 4. Ejecutar

Con el backend encendido, elige el dispositivo y pulsa **Run ▶**. En la app, toca **Crear cuenta** con
tu nombre, email y una contraseña de al menos 8 caracteres.

## Configuración

### App (`local.properties`)

Este archivo es local y no se sube al repositorio.

| Propiedad | Por defecto | Descripción |
|---|---|---|
| `splitbill.apiBaseUrl` | `http://10.0.2.2:8080/` | URL del backend. El valor por defecto solo funciona en el emulador (`10.0.2.2` es el computador visto desde él) |
| `splitbill.adbReversePort` | — | Si se define, Gradle ejecuta `adb reverse` con ese puerto antes de instalar la app |
| `splitbill.buildDir` | — | Carpeta de compilación alternativa. Útil si el proyecto está dentro de OneDrive u otra carpeta sincronizada |

### Backend (variables de entorno o `backend/.env`)

Para desarrollo no hace falta configurar nada. Para cambiar valores, copia `backend/.env.example` como
`backend/.env`; ese archivo tampoco se sube al repositorio.

| Variable | Por defecto | Descripción |
|---|---|---|
| `DB_URL` | `jdbc:postgresql://localhost:5432/splitbill` | Conexión a PostgreSQL |
| `DB_USER` / `DB_PASSWORD` | `splitbill` / `splitbill` | Credenciales de la base de datos |
| `JWT_SECRET` | clave de desarrollo | Clave para firmar los tokens (mínimo 32 caracteres). **Obligatoria en producción** (perfil `prod`) |

Para generar una clave segura: `openssl rand -base64 48`.

## Pruebas

```bash
# App, desde la raíz del proyecto
./gradlew :app:testDebugUnitTest            # unitarias del dominio (sin emulador)
./gradlew :app:connectedDebugAndroidTest    # Room, migraciones, sincronización e interfaz (Espresso)

# Backend, desde backend/ y con Docker abierto
./mvnw test                                 # integración contra un PostgreSQL temporal (Testcontainers)
```

| Conjunto | Cantidad | Qué cubre |
|---|---|---|
| Unitarias | 63 | Dinero, estrategias de división, saldos, liquidación, lectura de facturas y conversión con la API |
| Instrumentadas | 49 | Consultas de Room, migraciones, sincronización contra un servidor simulado y 19 flujos de interfaz |
| Backend | 46 | Todos los endpoints contra PostgreSQL real, incluidos un grupo compartido entre dos cuentas y su chat |

Antes de las pruebas instrumentadas, desactiva las animaciones del dispositivo (*Opciones de
desarrollador → escalas de animación*). Estas pruebas usan una base de datos en memoria y un servidor
falso, así que no tocan tus datos, pero desinstalan la app al terminar.

## Despliegue en Render

El repositorio incluye un [`render.yaml`](render.yaml) que crea el backend y su base de datos en
[Render](https://render.com) con el plan gratuito. Con el servidor en internet, cada persona usa la app
desde su propio teléfono y todos comparten grupos, gastos, pagos y chat.

1. En Render, entra a **Blueprints → New Blueprint Instance** y conecta este repositorio de GitHub.
2. Render lee `render.yaml` y propone dos recursos: el servicio `splitbill-api` (Docker) y la base
   `splitbill-db` (PostgreSQL 17). Toca **Apply**. La clave de los tokens (`JWT_SECRET`) la genera
   Render y nunca queda en el repositorio.
3. Espera a que el servicio diga **Live** (la primera compilación tarda unos minutos). Su dirección es
   del tipo `https://splitbill-api.onrender.com`. Comprueba que responde en
   `https://<tu-servicio>.onrender.com/api/health`.
4. En `local.properties` de cada computador, apunta la app a esa dirección y quita
   `splitbill.adbReversePort`:

   ```properties
   splitbill.apiBaseUrl=https://<tu-servicio>.onrender.com/
   ```

5. Sincroniza Gradle, instala la app y crea una cuenta. Para compartir un grupo, invita a la otra
   persona por el email de su cuenta.

Cada `git push` que cambie `backend/` vuelve a desplegar el servidor, y Flyway aplica las migraciones
nuevas al arrancar.

**Límites del plan gratuito:**

- El servidor se duerme tras unos 15 minutos sin uso y tarda hasta un minuto en despertar. La app
  espera ese tiempo; antes de una demostración conviene abrir `/api/health` para despertarlo.
- La base de datos gratuita de Render vence al cabo de un tiempo (revisa las condiciones de su plan).
  Para algo más duradero, cambia `DATABASE_URL`, `DB_USER` y `DB_PASSWORD` por los de otra base
  PostgreSQL (Neon, Supabase…).

## Arquitectura

```
┌────────────── App Android ──────────────┐            ┌──────────── Backend ────────────┐
│                                         │            │                                 │
│  Activities ─► Repositorios ─► Room     │   HTTP     │  Controllers ─► Services ─► JPA │
│                                 ▲       │   + JWT    │                              │  │
│                        SyncManager ─────┼───────────►│  Spring Security   PostgreSQL ◄┘ │
│                        (WorkManager)    │            │  Flyway · Swagger               │
└─────────────────────────────────────────┘            └─────────────────────────────────┘
```

**Decisiones de diseño principales:**

- **Offline-first.** Las pantallas solo leen y escriben en Room. Cada fila lleva un estado de
  sincronización (`PENDING_CREATE`, `PENDING_UPDATE`, `PENDING_DELETE`, `SYNCED`) que funciona como
  bandeja de salida; `SyncManager` sube los cambios en orden de dependencias y luego trae solo lo que
  cambió desde la última vez (`?updatedSince=`).
- **Identificadores UUID generados en el dispositivo.** Una fila creada sin conexión ya tiene su id
  definitivo, y reenviarla no la duplica en el servidor.
- **El dinero nunca es `double`.** `Money` trabaja con centavos enteros y reparte los sobrantes por el
  método del residuo mayor, de modo que las partes suman exactamente el total.
- **Dominio en Java puro.** Dinero, división, saldos, liquidación y lectura de facturas no dependen de
  Android y se prueban con JUnit.
- **División polimórfica.** Cada forma de dividir es una `SplitStrategy` creada por una fábrica; agregar
  una nueva no obliga a tocar las pantallas.
- **Migraciones escritas a mano** y probadas, en Room y en Flyway. Actualizar la app nunca borra datos.
- **El servidor valida, la app calcula.** El backend verifica que las partes de cada gasto sumen el
  monto y que los participantes pertenezcan al grupo.

### Liquidación

`DebtSimplifier` empareja siempre al mayor acreedor con el mayor deudor usando dos colas de prioridad.
Como cada transferencia deja al menos a una persona en cero, un grupo de *n* personas nunca necesita más
de *n − 1* transferencias.

```
Saldos      Diomar  +$33.000    Julián  +$9.000
            Juan     −$3.000    Sofía  −$39.000

Resultado   3 transferencias en lugar de 9
            Sofía → Diomar   $33.000
            Sofía → Julián    $6.000
            Juan  → Julián    $3.000
```

El detalle de la arquitectura, el modelo de datos, la sincronización y la seguridad está en el
[manual técnico](docs/manual-tecnico.md).

## Estructura del repositorio

```
Split_Bill/
├── app/                        Aplicación Android
│   └── src/main/java/ue/edu/co/splitbill/
│       ├── domain/             Lógica de negocio en Java puro (Money, estrategias, liquidación)
│       ├── entity/  dao/       Entidades y consultas de Room
│       ├── manager/            Base de datos, contrato del esquema y migraciones
│       ├── model/              Repositorios
│       ├── network/            Cliente Retrofit, DTO y mapeo
│       ├── sync/               Sincronización y trabajo en segundo plano
│       ├── session/            Sesión y almacenamiento cifrado del token
│       ├── permission/         Permisos en tiempo de ejecución
│       └── ui/                 Pantallas
├── backend/                    API REST en Spring Boot (ver backend/README.md)
└── docs/                       Manual técnico y manual de usuario
```

## Solución de problemas

| Síntoma | Solución |
|---|---|
| La app muestra "Sin conexión" | Verifica que el backend esté corriendo. En un teléfono físico, confirma `splitbill.adbReversePort` o ejecuta `adb reverse tcp:8080 tcp:8080` |
| El backend falla con `Connection refused` al puerto 5432 | Abre Docker Desktop y ejecuta `docker compose up -d` en `backend/` |
| El puerto 8080 o 5432 ya está en uso | Cierra la otra instancia o cambia el puerto |
| `Unable to delete directory` al compilar | El proyecto está en una carpeta sincronizada (OneDrive): define `splitbill.buildDir` fuera de ella |
| Se negó un permiso y ya no se vuelve a pedir | Desde la app, toca **Abrir ajustes**, o ve a *Ajustes → Apps → SplitBill → Permisos* |

## Documentación

- [Manual de usuario](docs/manual-usuario.md): cada pantalla y cómo usarla.
- [Manual técnico](docs/manual-tecnico.md): arquitectura, modelo de datos, sincronización, seguridad y
  pruebas.
- [Backend](backend/README.md): endpoints, formato de errores y despliegue con Docker.
- API interactiva: `/swagger-ui.html` con el backend en marcha.

## Hoja de ruta

- Generar una versión *release* firmada que apunte al backend desplegado.
- Notificaciones push (Firebase Cloud Messaging) para el chat y los gastos nuevos.

## Autores

Proyecto desarrollado para la asignatura Aplicaciones Móviles de Uniempresarial,
Julian Corredor, Diomar Arias, Juan Rojano y Sofía Reyes.
