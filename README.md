# Banco XYZ — Suite BFF (Backend for Frontend)

**PBY2203 — Desarrollo Backend III — Experiencia 3 (Semana 8)**

Esta entrega agrega a la suite BFF de la Semana 7: **seguridad OAuth2 (`client_credentials`)** entre los BFF y los microservicios, **imágenes Docker** por módulo, un **`docker-compose.yaml`** que orquesta todo el proyecto y **Kafka desplegado en una instancia EC2**. Resilience4j y Kafka ya existían desde la Semana 7 y se verificaron dentro del nuevo entorno.

---

## 1. Objetivo del proyecto

Implementar el patrón **Backend for Frontend (BFF)**: un BFF independiente por canal y un microservicio independiente por dominio de datos.

| BFF | Puerto | Cliente | Enfoque |
|---|---|---|---|
| `bff-web` | 8081 | Navegador / back-office | Datos completos, paginación y filtros |
| `bff-mobile` | 8082 | App móvil | Respuestas livianas |
| `bff-atm` | 8083 | Cajero automático | Superficie mínima, operaciones críticas |

| Microservicio | Puerto | Dueño de |
|---|---|---|
| `ms-cuentas` | 8090 | Saldo e interés de cada cuenta |
| `ms-movimientos` | 8091 | Historial de retiros/depósitos (productor Kafka) |
| `ms-transacciones` | 8092 | Feed global de transacciones y anomalías (consumidor Kafka) |

| Servicio de infraestructura | Puerto | Función |
|---|---|---|
| `config-server` | 8888 | Configuración centralizada (`ms-cuentas` toma de aquí su puerto y su datasource) |
| `auth-server` | 9000 | Servidor de autorización OAuth2 (Spring Authorization Server) |

Los BFF no acceden a datos directamente: llaman a los tres microservicios por HTTP (`RestClient`), protegidos con **OAuth2** y **Resilience4j**. Un retiro en `bff-atm` descuenta el saldo en `ms-cuentas` y registra el movimiento en `ms-movimientos`, que publica el evento `movimiento-registrado` en **Kafka**; `ms-transacciones` lo consume y genera la transacción.

## 2. Arquitectura

```
                       ┌──────────────┐
                       │ auth-server  │  emite tokens OAuth2
                       │   :9000      │  (client_credentials)
                       └──────▲───────┘
                              │ token (1 por BFF, se reutiliza ~5 min)
 Web ──► bff-web    :8081 ────┤
 App ──► bff-mobile :8082 ────┼──► ms-cuentas        :8090  (Resource Server)
 ATM ──► bff-atm    :8083 ────┤──► ms-movimientos    :8091  (Resource Server) ──┐ publica evento
                              └──► ms-transacciones  :8092  (Resource Server) ◄─┘ consume evento
                                                                  ▲
                                          Kafka (EC2) ────────────┘  topic: movimiento-registrado
```

## 3. Estructura del código

```
Exp3_S7_Grupo20/
├── pom.xml                      # POM padre (multi-módulo Maven)
├── docker-compose.yaml          # Orquesta los 8 servicios (sin Kafka: va en la EC2)
├── docker-compose.kafka-local.yml  # Compose antiguo (Kafka local para desarrollo)
├── .env                         # KAFKA_BOOTSTRAP_SERVERS=<IP_EC2>:9092 (ver sección 5)
├── evidencia/                   # Capturas y documento de evidencia (sección 10)
├── bff-core/                    # Librería compartida: JWT, excepciones, clientes HTTP y OAuth2
│   └── .../core/config/         # BackendClientsConfig, OAuth2TokenInterceptor
├── bff-web/  bff-mobile/  bff-atm/        # Un Dockerfile cada uno
├── ms-cuentas/  ms-movimientos/  ms-transacciones/   # Un Dockerfile cada uno
├── config-server/               # Dockerfile + config-repo/ms-cuentas.yml
├── auth-server/                 # Dockerfile + clientes OAuth2 en application.yml
├── eureka-server/               # Existe, pero no se usa en esta entrega
└── data/                        # CSV oficiales
```

## 4. Requisitos previos

- Java 17 o superior (los módulos compilan para 17; las imágenes Docker usan `eclipse-temurin:21-jre`)
- Maven 3.9+
- Docker Desktop (con Docker Compose v2)
- Una instancia con Kafka accesible (ver sección 9) o el Kafka local del compose antiguo

## 5. Cómo ejecutar (Docker Compose)

```bash
# 1) Compilar y empaquetar los jars ejecutables (una vez, o cada vez que cambie el código)
mvn clean package -DskipTests

# 2) Indicar dónde está Kafka (archivo .env junto al docker-compose.yaml)
echo KAFKA_BOOTSTRAP_SERVERS=<IP_ELASTICA_EC2>:9092 > .env

# 3) Construir las imágenes y levantar los 8 servicios
docker compose up -d --build

# 4) Comprobar que todos están en "Up"
docker compose ps
```

Servicios que levanta el compose: `config-server`, `auth-server`, `ms-cuentas`, `ms-movimientos`, `ms-transacciones`, `bff-web`, `bff-mobile` y `bff-atm`. No incluye base de datos externa: los microservicios usan **H2 en memoria** y cargan los CSV oficiales al iniciar.

Para detener todo: `docker compose down`.

**Puertos que deben estar libres:** 8081, 8082, 8083, 8090, 8091, 8092, 8888 y 9000.

### Variables de entorno que usa el compose

Dentro de Docker, `localhost` es el propio contenedor, así que el compose reemplaza las URLs de los `application.yml` por los nombres de servicio:

| Variable | Valor | Quién la usa |
|---|---|---|
| `BACKEND_OAUTH_TOKENURL` | `http://auth-server:9000/oauth2/token` | los 3 BFF |
| `BACKEND_MSCUENTAS_BASEURL` (y `MSMOVIMIENTOS`, `MSTRANSACCIONES`) | `http://ms-...:puerto` | los 3 BFF |
| `SPRING_SECURITY_OAUTH2_RESOURCESERVER_JWT_ISSUERURI` | `http://auth-server:9000` | los 3 microservicios |
| `SPRING_CONFIG_IMPORT` | `configserver:http://config-server:8888` | `ms-cuentas` |
| `KAFKA_BOOTSTRAP_SERVERS` | desde `.env` | `ms-movimientos`, `ms-transacciones` |

### Ejecución sin Docker (desarrollo)

Cada servicio puede levantarse con `mvn -pl <modulo> spring-boot:run`, en este orden: `config-server`, `auth-server`, los tres `ms-*` y los tres `bff-*`, cada uno en su propia terminal. En ese modo se usan los `localhost` de los `application.yml`.

## 6. Seguridad

### 6.1 OAuth2 entre BFF y microservicios (nuevo en esta entrega)

Los microservicios son **Resource Servers**: rechazan con **401** cualquier petición sin token válido y con **403** si el token no trae el *scope* requerido. Cada BFF obtiene su token del `auth-server` con el flujo **`client_credentials`**; `OAuth2TokenInterceptor` (en `bff-core`) lo pide automáticamente, lo guarda hasta unos 30 segundos antes de que venza (dura 299 s) y lo agrega como `Authorization: Bearer ...` a cada llamada interna. Las credenciales de cada BFF están en su propio `application.yml` (bloque `backend.oauth`), de modo que el mismo código usa credenciales distintas según el canal.

| Cliente OAuth2 | Usado por | Scopes registrados en el `auth-server` |
|---|---|---|
| `bff-web-client` | `bff-web` | `cuentas.read`, `movimientos.read`, `transacciones.read` |
| `bff-mobile-client` | `bff-mobile` | `cuentas.read`, `movimientos.read`, `transacciones.read` |
| `bff-atm-client` | `bff-atm` | `cuentas.read`, `cuentas.write`, `movimientos.write` |

| Microservicio | Regla de autorización |
|---|---|
| `ms-cuentas` | `GET` → `SCOPE_cuentas.read`; `PATCH` → `SCOPE_cuentas.write` |
| `ms-movimientos` | `GET` → `SCOPE_movimientos.read`; `POST` → `SCOPE_movimientos.write` |
| `ms-transacciones` | todo → `SCOPE_transacciones.read` |

Probar el `auth-server` directamente (Postman o curl):

```
POST http://localhost:9000/oauth2/token
Authorization: Basic  bff-web-client / web-secret-2026
Body (x-www-form-urlencoded): grant_type=client_credentials
                              scope=cuentas.read movimientos.read transacciones.read
```

> Los tokens emitidos por el `auth-server` dentro de Docker tienen como emisor `http://auth-server:9000`, que coincide con el `issuer-uri` de los microservicios. Por eso las pruebas se hacen **a través de los BFF**; un token pedido a `localhost:9000` no es válido para llamar directamente a un microservicio del compose.

### 6.2 JWT propio de cada canal (sin cambios)

Cada BFF firma y valida su propio JWT (clave e issuer distintos), así que un token de un canal no sirve en otro. Este JWT autentica a las personas y dispositivos que llaman al BFF, y es independiente de OAuth2, que solo protege la comunicación interna.

| Canal | Login | Credenciales demo | Vigencia |
|---|---|---|---|
| Web | `POST /api/web/auth/login` | `admin.web` / `Admin#2026` | 60 min |
| Móvil | `POST /api/mobile/auth/login` | `cliente.app` / `Cliente#2026` | 15 min |
| Cajero | `POST /api/atm/auth/login` | tarjeta `4551000000000001`, PIN `1234` | 15 min |

Los usuarios demo están definidos en el código de cada BFF; no hay base de datos de usuarios.

El **cajero** exige además el header `X-Atm-Device-Key: atm-device-key-demo-cambiar` en **todas** las rutas, y el token queda ligado a la cuenta de la tarjeta (`AtmAccessGuard`). Cuentas de prueba: tarjeta `...0001` → cuenta `101`; tarjeta `...0002` → cuenta `105`. Cuerpo del login ATM: `{ "numeroTarjeta": "...", "pin": "..." }`.

Los BFF responden **403** (cuerpo vacío) a peticiones sin JWT: es el comportamiento por defecto de Spring Security. Los microservicios responden **401**.

## 7. Endpoints por BFF

**Web**
- `GET /api/web/cuentas?tipo=&page=&size=` — listado paginado
- `GET /api/web/cuentas/{id}` — detalle de una cuenta
- `GET /api/web/cuentas/{id}/historial-anual` — historial (usa `ms-movimientos`)
- `GET /api/web/transacciones?desde=&hasta=&tipo=&page=&size=` — feed global

**Móvil**
- `GET /api/mobile/cuentas/{id}/resumen`
- `GET /api/mobile/movimientos/recientes`

**Cajero**
- `GET /api/atm/cuentas/{id}/saldo`
- `POST /api/atm/cuentas/{id}/retiro` con `{ "monto": 1000 }`

**Microservicios (uso interno)**
- `ms-cuentas`: `GET /cuentas`, `GET /cuentas/{id}`, `PATCH /cuentas/{id}/saldo`
- `ms-movimientos`: `GET /movimientos/cuenta/{id}`, `POST /movimientos`
- `ms-transacciones`: `GET /transacciones`, `GET /transacciones/recientes`

Los errores siguen el formato `{ "timestamp", "status", "error" }`.

## 8. Resilience4j (Circuit Breaker + Retry)

Los servicios de los BFF usan `@CircuitBreaker` y `@Retry` con *fallback* hacia `ServicioNoDisponibleException`, que devuelve un **503** controlado en vez de un error 500. Configuración de `bff-atm` (instancia `msCuentas`):

| Parámetro | Valor |
|---|---|
| `sliding-window-size` | 5 llamadas |
| `minimum-number-of-calls` | 3 |
| `failure-rate-threshold` | 50 % |
| `wait-duration-in-open-state` | 10 s |
| `permitted-number-of-calls-in-half-open-state` | 2 |
| Retry: `max-attempts` / `wait-duration` | 3 / 500 ms |

**Cómo reproducir la apertura del circuito** (con el compose levantado y un JWT de `bff-atm`):

1. `GET http://localhost:8083/actuator/circuitbreakers` → `msCuentas` en `CLOSED` (requiere el JWT y el header del dispositivo).
2. `docker compose stop ms-cuentas`
3. Repetir `GET /api/atm/cuentas/101/saldo` 3 o 4 veces: primero fallan con reintentos cada 500 ms y luego responde 503.
4. `GET /actuator/circuitbreakers` → `OPEN` (3 fallas de 5 llamadas = 60 % > 50 %). En el log de `bff-atm` aparece `CallNotPermittedException: CircuitBreaker 'msCuentas' is OPEN and does not permit further calls`.
5. Pasados 10 s el circuito queda en `HALF_OPEN`.
6. `docker compose start ms-cuentas` y repetir el saldo: vuelve a `CLOSED`.

## 9. Kafka en EC2

Kafka corre en una instancia **EC2 (Ubuntu) del Learner Lab**, con IP elástica y los puertos 22 (SSH) y 9092 (Kafka) abiertos en el Security Group. Es un **único nodo en modo KRaft** (sin Zookeeper), con la imagen `apache/kafka:3.7.0`, más **Kafka UI** (puerto 8080) para inspeccionar topics y consumidores.

Instalación de Docker en la instancia:

```bash
curl -fsSL https://get.docker.com -o get-docker.sh
sudo sh get-docker.sh
sudo usermod -aG docker ubuntu
```

`docker-compose.yaml` de la EC2 (reemplazar `<IP_ELASTICA_EC2>` por la IP elástica de la instancia):

```yaml
services:
  kafka:
    image: apache/kafka:3.7.0
    container_name: kafka
    restart: unless-stopped
    ports:
      - "9092:9092"
    environment:
      KAFKA_NODE_ID: 1
      KAFKA_PROCESS_ROLES: broker,controller
      KAFKA_LISTENERS: PLAINTEXT://0.0.0.0:19092,CONTROLLER://0.0.0.0:19093,EXTERNAL://0.0.0.0:9092
      KAFKA_ADVERTISED_LISTENERS: PLAINTEXT://kafka:19092,EXTERNAL://<IP_ELASTICA_EC2>:9092
      KAFKA_LISTENER_SECURITY_PROTOCOL_MAP: CONTROLLER:PLAINTEXT,PLAINTEXT:PLAINTEXT,EXTERNAL:PLAINTEXT
      KAFKA_CONTROLLER_LISTENER_NAMES: CONTROLLER
      KAFKA_CONTROLLER_QUORUM_VOTERS: 1@kafka:19093
      KAFKA_INTER_BROKER_LISTENER_NAME: PLAINTEXT
      KAFKA_OFFSETS_TOPIC_REPLICATION_FACTOR: 1
      KAFKA_TRANSACTION_STATE_LOG_REPLICATION_FACTOR: 1
      KAFKA_TRANSACTION_STATE_LOG_MIN_ISR: 1
      KAFKA_AUTO_CREATE_TOPICS_ENABLE: "true"
    volumes:
      - kafkadata:/var/lib/kafka/data

  kafka-ui:
    image: provectuslabs/kafka-ui:latest
    container_name: kafka-ui
    restart: unless-stopped
    ports:
      - "8080:8080"
    environment:
      KAFKA_CLUSTERS_0_NAME: ec2-kafka
      KAFKA_CLUSTERS_0_BOOTSTRAPSERVERS: kafka:19092
    depends_on:
      - kafka

volumes:
  kafkadata:
```

Se levanta con `sudo docker compose up -d`. El listener `EXTERNAL` anuncia la IP pública para que los contenedores del proyecto (en la máquina local) puedan conectarse; el listener `PLAINTEXT` es el interno entre contenedores de la EC2.

**Flujo de eventos**

1. `POST /api/atm/cuentas/{id}/retiro` → `bff-atm` descuenta el saldo en `ms-cuentas` y registra el movimiento en `ms-movimientos`.
2. `ms-movimientos` publica `movimiento-registrado` (clave = cuenta) en Kafka; el log dice `Evento movimiento-registrado publicado: ...`.
3. `ms-transacciones` (`MovimientoEventListener`, grupo `ms-transacciones`, `auto-offset-reset: earliest`) consume el evento, detecta anomalías (monto mayor a 3.000.000), evita procesar dos veces el mismo evento y guarda la transacción; el log dice `Transaccion generada desde evento: movimientoId=..., cuenta=..., monto=...`.
4. En Kafka UI (`http://<IP_ELASTICA_EC2>:8080`): el topic `movimiento-registrado` muestra los mensajes y el grupo `ms-transacciones` aparece `STABLE` con **lag 0**, es decir, todos los mensajes fueron leídos.


## 10. Evidencia
La evidencia se entregará en un documento a parte en formato word. 

