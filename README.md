# Banco XYZ — Suite BFF (Backend for Frontend)

**PBY2203 — Desarrollo Backend III — Semana 7 (Exp3)**


## 1. Objetivo del proyecto

Implementar el patrón **Backend for Frontend (BFF)** para optimizar la
comunicación entre los distintos frontends del Banco XYZ, integrando y
agregando información desde servicios backend independientes. Se
implementa **un BFF independiente por canal**, y **un microservicio
independiente por dominio de datos**:

| BFF | Puerto | Cliente | Enfoque |
|---|---|---|---|
| `bff-web` | 8081 | Navegador / back-office | Datos completos, paginación y filtros |
| `bff-mobile` | 8082 | App móvil | Respuestas livianas, mínimo consumo de ancho de banda |
| `bff-atm` | 8083 | Cajero automático | Superficie mínima, máxima seguridad, operaciones críticas |

| Microservicio | Puerto | Dueño de |
|---|---|---|
| `ms-cuentas` | 8090 | Saldo e interés de cada cuenta |
| `ms-movimientos` | 8091 | Historial de retiros/depósitos por cuenta |
| `ms-transacciones` | 8092 | Feed global de transacciones (detección de anomalías) |

Ningún BFF accede a la base de datos directamente: todos llaman a estos
tres microservicios por HTTP (con `RestClient`, protegido con
Resilience4j), y combinan su información según lo que necesita cada
canal — por ejemplo, un retiro en `bff-atm` llama a `ms-cuentas` para
descontar el saldo **y** a `ms-movimientos` para registrar el
movimiento correspondiente.

Desde la Semana 7 (Exp3), `ms-movimientos` además **publica un evento
en Kafka** (`movimiento-registrado`) por cada movimiento nuevo, que
`ms-transacciones` consume de forma asíncrona para alimentar su feed
global y aplicar detección de anomalías — sin que el BFF ni
`ms-movimientos` esperen esa respuesta. Ver el detalle completo en
[`docs/ADR-02-arquitectura-eventos.md`](docs/ADR-02-arquitectura-eventos.md).

El análisis completo de las decisiones de arquitectura de la Semana 4/5
(BFF como servicios independientes, y microservicios de dominio detrás
de ellos) está en
[`docs/ADR-01-estrategia-bff.md`](docs/ADR-01-estrategia-bff.md).


## 2. Estructura del código
Exp3_S7_Grupo20/
├── pom.xml # POM padre (multi-módulo Maven)
├── docker-compose.yml # Postgres + Kafka (KRaft, un solo nodo)
├── docs/
│ ├── ADR-01-estrategia-bff.md
│ └── ADR-02-arquitectura-eventos.md # Decisión Kafka + Resilience4j (Semana 7)
├── evidencia/ # Capturas / salidas de consola (ver sección 6)
├── bff-core/ # Librería compartida: JWT/seguridad + excepciones
│ └── .../core/
│ ├── security/ # JwtTokenProvider, JwtAuthenticationFilter
│ └── exception/ # Excepciones de dominio compartidas (incluye ServicioNoDisponibleException)
├── bff-web/ # Spring Boot app — canal Web (puerto 8081) — Resilience4j
├── bff-mobile/ # Spring Boot app — canal Móvil (puerto 8082) — Resilience4j
├── bff-atm/ # Spring Boot app — canal Cajero (puerto 8083) — Resilience4j
├── ms-cuentas/ # Microservicio — cuentas/saldo (puerto 8090)
├── ms-movimientos/ # Microservicio — movimientos (puerto 8091) — productor Kafka
│ └── .../msmovimientos/event/ # MovimientoRegistradoEvent, MovimientoEventPublisher
└── ms-transacciones/ # Microservicio — feed global (puerto 8092) — consumidor Kafka
  └── .../mstransacciones/
    ├── event/ # MovimientoRegistradoEvent (contrato espejo)
    └── listener/ # MovimientoEventListener (@KafkaListener)

Cada `bff-*` sigue la misma forma interna: `config` (seguridad + clientes
HTTP), `client` (llamadas a los microservicios), `controller`, `service`
(ahora con `@CircuitBreaker`/`@Retry` de Resilience4j), `dto`. Cada
`ms-*` sigue la misma forma: `entity`, `repository`, `controller`,
`dto` — son los únicos módulos que tienen acceso JPA a la base de datos.

## 3. Requisitos previos

- Java 17
- Maven 3.9+
- Docker (para levantar PostgreSQL y Kafka) — o un PostgreSQL/Kafka locales equivalentes

## 4. Cómo ejecutar

### Modo de prueba (H2 en memoria — el usado para la evidencia de este entregable)

Los tres microservicios (`ms-cuentas`, `ms-movimientos`, `ms-transacciones`)
están configurados para arrancar con una base de datos H2 en memoria,
que se autogenera y se llena automáticamente al iniciar leyendo los CSV
oficiales (`bank_legacy_data`) desde `src/main/resources/data/`. No
requiere Docker ni PostgreSQL para este modo.

```bash
# 1) Levantar Kafka (necesario para el flujo de eventos de la Semana 7)
docker compose up -d kafka

# 2) Compilar e instalar todo el multi-módulo (una sola vez, o cada vez que cambie el código)
mvn clean install

# 3) Levantar los microservicios primero (los BFF dependen de ellos), cada uno en su propia terminal
mvn -pl ms-cuentas       spring-boot:run
mvn -pl ms-movimientos   spring-boot:run
mvn -pl ms-transacciones spring-boot:run

# 4) Levantar cada BFF, cada uno en su propia terminal
mvn -pl bff-web    spring-boot:run
mvn -pl bff-mobile spring-boot:run
mvn -pl bff-atm    spring-boot:run
```

### Verificar el flujo de eventos (Semana 7)

1. Haz un retiro por cualquier canal, por ejemplo:
   `POST /api/atm/cuentas/101/retiro` con `{ "monto": 10000 }` (ver
   sección 5 para el login previo).
2. En la consola de `ms-movimientos` deberías ver el log
   `Evento movimiento-registrado publicado: ...`.
3. En la consola de `ms-transacciones` deberías ver, casi de inmediato,
   `Transaccion generada desde evento: ...` — sin que ningún BFF haya
   llamado directamente a `ms-transacciones`.
4. Confirma que aparece en el feed global:
   `GET /api/web/transacciones` (bff-web) o directamente
   `GET http://localhost:8092/transacciones/recientes`.
5. Para probar la tolerancia a fallos: detén `ms-cuentas` y vuelve a
   pedir un saldo por cualquier canal — tras el `Retry`, deberías
   recibir `503` con el mensaje de `ServicioNoDisponibleException`, en
   vez de un error 500 crudo. Revisa `/actuator/circuitbreakers` en el
   BFF correspondiente para ver el circuito abrirse.

Cada microservicio imprime en su log cuántos registros cargó y cuántos
rechazó por datos inválidos (por ejemplo:
`ms-cuentas: 17 cuentas cargadas, 983 filas rechazadas por datos invalidos`),
ya que el dataset oficial incluye intencionalmente filas con datos
inconsistentes (edades fuera de rango, montos inválidos, tipos de cuenta
inexistentes, duplicados).

Cuentas de ejemplo ya cargadas y listas para probar: `101, 105, 106, 108,
109, 117, 118, 122, 124, 127, 128, 130, 132, 133, 143, 144, 147`.

Puedes inspeccionar los datos cargados desde el navegador en la consola
de H2 de cada microservicio, por ejemplo:
`http://localhost:8090/h2-console` (JDBC URL: `jdbc:h2:mem:mscuentas`,
usuario `sa`, sin contraseña).

### Modo productivo (PostgreSQL vía Docker)

Para un despliegue más cercano a producción, cada microservicio también
puede apuntar a PostgreSQL en vez de H2 (basta con cambiar la
dependencia `h2` por `postgresql` en su `pom.xml` y ajustar su
`application.yml` con las credenciales de abajo):

```bash
docker compose up -d
```

Esto levanta un Postgres en `localhost:5432`, base `bank_batch`,
usuario/clave `bank_batch`/`bank_batch`. En este modo, los datos deben
poblarse mediante el proyecto de migración batch de la Semana 3 (Spring
Batch), no con los cargadores CSV descritos arriba.

## 5. Autenticación y autorización por canal

Cada BFF firma y valida **su propio JWT** (clave/issuer distintos), de
forma que un token de un canal no sirve en otro.

### BFF Web (`/api/web/**`)
POST /api/web/auth/login
{ "usuario": "admin.web", "password": "Admin#2026" }
Usuarios demo: `admin.web`/`Admin#2026` (rol `WEB_ADMIN`+`WEB_USER`),
`operador.web`/`Oper#2026` (rol `WEB_USER`). Token válido 60 min.

### BFF Móvil (`/api/mobile/**`)
POST /api/mobile/auth/login
{ "usuario": "cliente.app", "password": "Cliente#2026" }
Token válido solo 15 min (política más estricta por ser dispositivo
personal).

### BFF Cajero (`/api/atm/**`)
Requiere **dos factores de acceso**:
1. Header `X-Atm-Device-Key: atm-device-key-demo-cambiar` en **toda**
   petición (identifica al cajero físico como dispositivo autorizado).
2. Login con tarjeta + PIN, token válido solo 3 minutos:
POST /api/atm/auth/login
Header: X-Atm-Device-Key: atm-device-key-demo-cambiar
Body: { "numeroTarjeta": "4551000000000001", "pin": "1234" }
Tarjetas demo: `4551000000000001`/PIN `1234` → cuenta 1001;
`4551000000000002`/PIN `5678` → cuenta 1002. El token queda ligado a esa
cuenta: no se puede usar para consultar/retirar de otra (ver
`AtmAccessGuard`).

Los tres microservicios (`ms-cuentas`, `ms-movimientos`,
`ms-transacciones`) no exponen autenticación propia — son servicios
internos, solo alcanzables por los BFF dentro de la misma red.

## 6. Endpoints por BFF

### Web
- `GET /api/web/cuentas?tipo=&page=&size=` — listado paginado, datos completos
- `GET /api/web/cuentas/{cuentaOrigenId}` — detalle completo de una cuenta
- `GET /api/web/cuentas/{cuentaOrigenId}/historial-anual` — historial anual
- `GET /api/web/transacciones?desde=&hasta=&tipo=&page=&size=` — feed global paginado

### Móvil
- `GET /api/mobile/cuentas/{cuentaOrigenId}/resumen` — `{cuentaOrigenId, nombre, tipo, saldo}`
- `GET /api/mobile/movimientos/recientes` — últimos 10 movimientos, campos mínimos

### Cajero
- `GET /api/atm/cuentas/{cuentaOrigenId}/saldo` — solo saldo disponible
- `POST /api/atm/cuentas/{cuentaOrigenId}/retiro` `{ "monto": 50000 }` — retiro con validación de saldo; internamente descuenta el saldo en `ms-cuentas` y registra el movimiento en `ms-movimientos`

### Microservicios (uso interno de los BFF)
- `ms-cuentas`: `GET /cuentas`, `GET /cuentas/{id}`, `PATCH /cuentas/{id}/saldo`
- `ms-movimientos`: `GET /movimientos/cuenta/{id}`, `POST /movimientos`
- `ms-transacciones`: `GET /transacciones`, `GET /transacciones/recientes`

Todas las respuestas de error siguen el mismo formato
`{ "timestamp", "status", "error" }`.