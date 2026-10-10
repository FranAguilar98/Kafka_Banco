# Banco XYZ — Modernización de sistemas legacy

**PBY2203 — Desarrollo Backend Avanzado: Spring Cloud y Batch — Evaluación Final Transversal**

---

## 1. Descripción

Banco XYZ cuenta con un sistema legacy obsoleto, cuyos procesos nocturnos y datos en archivos planos ya no responden a las necesidades actuales. Este proyecto lo moderniza con una arquitectura de microservicios que busca tres objetivos:

1. **Modernizar el legacy:** migrar el procesamiento masivo de archivos a Spring Batch.
2. **Desacoplar a los clientes:** un backend dedicado (BFF) por cada canal: web, móvil y cajero automático.
3. **Dar resiliencia y alta disponibilidad:** seguridad distribuida con OAuth 2.0, tolerancia a fallos con Resilience4j, mensajería asíncrona con Kafka en AWS y despliegue con Docker.

## 2. Los cinco procesos clave

| Proceso | Tecnología | Dónde está |
|---|---|---|
| Procesamiento masivo y migración legacy | Spring Batch + MySQL | `batch-service` |
| Adaptación multicanal | Backend for Frontend (BFF) | `bff-web`, `bff-mobile`, `bff-atm`, `bff-core` |
| Microservicios y resiliencia | Spring Boot, Spring Cloud Config, Resilience4j | `ms-cuentas`, `ms-movimientos`, `ms-transacciones`, `config-server` |
| Seguridad distribuida | OAuth 2.0 (`client_credentials`) + JWT | `auth-server` y los Resource Server |
| Mensajería asíncrona | Apache Kafka (EC2 de AWS) | `ms-movimientos` → `ms-transacciones` |

## 3. Arquitectura

```
Web ──► bff-web    :8081 ─┐
App ──► bff-mobile :8082 ─┼─ 1) piden token ──► auth-server :9000
ATM ──► bff-atm    :8083 ─┘
                          └─ 2) HTTP + JWT (Circuit Breaker + Retry) ──►
        ms-cuentas :8090 · ms-movimientos :8091 · ms-transacciones :8092
                                   │ publica                ▲ consume
                                   └──────► Kafka (EC2) ────┘
                                        tópico: movimiento-registrado

config-server :8888 → configuración centralizada (ms-cuentas)

Ejecución local, fuera de Docker:
batch-service (3 Jobs Spring Batch) ──► MySQL banco_xyz_batch
```

El diagrama completo se encuentra en el informe técnico (Figura 1).

## 4. Componentes

| Módulo | Puerto | Tecnología base | Función |
|---|---|---|---|
| `batch-service` | 8080 (local) | Spring Batch, MySQL | Procesa los 3 archivos CSV del legacy |
| `bff-web` | 8081 | Spring Boot, Spring Security | Backend del canal web: datos completos |
| `bff-mobile` | 8082 | Spring Boot, Spring Security | Backend del canal móvil: respuestas livianas |
| `bff-atm` | 8083 | Spring Boot, Spring Security | Backend del cajero: saldo y retiro |
| `bff-core` | — | Librería compartida | Clientes HTTP, interceptor OAuth2, JWT y excepciones |
| `ms-cuentas` | 8090 | Spring Boot, JPA, H2 | Cuentas y saldos |
| `ms-movimientos` | 8091 | Spring Boot, JPA, H2, Kafka | Movimientos (productor de eventos) |
| `ms-transacciones` | 8092 | Spring Boot, JPA, H2, Kafka | Transacciones y anomalías (consumidor de eventos) |
| `config-server` | 8888 | Spring Cloud Config (modo native) | Configuración centralizada |
| `auth-server` | 9000 | Spring Authorization Server | Emite los tokens OAuth 2.0 |
| `eureka-server` | 8761 | Spring Cloud Netflix Eureka | Existe, pero no forma parte del despliegue en Docker |

## 5. Spring Batch

El módulo `batch-service` define tres Jobs independientes, uno por cada archivo oficial del legacy:

| Job | Archivo de entrada | Endpoint para ejecutarlo |
|---|---|---|
| `reporteTransaccionesDiariasJob` | `movimientos_financieros_diarios.csv` | `POST /api/batch/transacciones-diarias` |
| `calculoInteresesMensualesJob` | `intereses_trimestrales.csv` | `POST /api/batch/intereses-mensuales` |
| `generacionEstadosCuentaAnualesJob` | `estados_financieros_anuales.csv` | `POST /api/batch/estados-cuenta-anuales` |

Características principales:
- **Ciclo ETL** por Step: `ItemReader` → `ItemProcessor` → `ItemWriter`.
- **Procesamiento por chunk:** bloques de 5 registros (`app.batch.chunk-size`), con `ChunkCompletionPolicy`, que también cierra el bloque a los 2 segundos.
- **Particionamiento y ejecución en paralelo:** `SimpleGridPartitioner` (3 particiones) y `ThreadPoolTaskExecutor` de 3 hilos.
- **Tolerancia a fallos:** `GenericSkipPolicy` (omite registros inválidos, hasta 500), `GenericRetryPolicy` con `ExponentialBackOffPolicy` (errores transitorios de la base de datos) y `GenericSkipListener` (registra los descartados).
- **Persistencia del estado:** el `JobRepository` guarda el progreso en MySQL.

Los Jobs no se ejecutan al iniciar el servicio, sino con una llamada POST a `BatchController`.

## 6. Cómo ejecutar

El detalle paso a paso está en [`instrucciones.md`](instrucciones.md) y el despliegue de Kafka en AWS en [`despliegue.md`](despliegue.md). Resumen:

**Requisitos:** Java 17 o superior, Maven 3.9+, Docker Desktop (Compose v2), MySQL 8 y una instancia con Kafka accesible.

**Microservicios, BFF y servicios de infraestructura (Docker):**

```bash
mvn clean package -DskipTests
echo KAFKA_BOOTSTRAP_SERVERS=<IP_ELASTICA_EC2>:9092 > .env
docker compose up -d --build
docker compose ps
```

Para demostrar la escalabilidad horizontal del canal web:

```bash
docker compose up -d --scale bff-web=2
```

Cada réplica queda publicada en un puerto distinto del rango `8181-8189`.

**Batch (local):**

1. Tener MySQL encendido. La base `banco_xyz_batch` se crea sola.
2. Crear el archivo `batch-service/src/main/resources/application-secrets.yml` con la contraseña (no se sube al repositorio):

```yaml
spring:
  datasource:
    password: <tu_contraseña_de_mysql>
```

3. Levantar el servicio y disparar los Jobs:

```bash
mvn -pl batch-service spring-boot:run
```

```
POST http://localhost:8080/api/batch/transacciones-diarias
POST http://localhost:8080/api/batch/intereses-mensuales
POST http://localhost:8080/api/batch/estados-cuenta-anuales
```

## 7. Seguridad

- **BFF → microservicios (OAuth 2.0):** cada BFF obtiene su token con el flujo `client_credentials` mediante `OAuth2TokenInterceptor`, y los microservicios lo validan como Resource Server. Cada canal tiene scopes distintos: solo el cajero puede escribir (`cuentas.write`, `movimientos.write`).
- **Usuarios → BFF (JWT):** cada canal tiene su propio login y su propio JWT. El cajero exige además el header `X-Atm-Device-Key`.
- Sin token válido, los microservicios responden `401`, y con un token sin el scope requerido, `403`.

## 8. Resiliencia

Los servicios de los tres BFF usan **Circuit Breaker** y **Retry** (Resilience4j) con un método *fallback* que devuelve un `503` controlado cuando el microservicio no está disponible. Configuración: ventana de 5 llamadas, mínimo 3, umbral de fallos del 50 %, 10 s en estado abierto y 3 reintentos con 500 ms de espera.

## 9. Kafka

Kafka corre en una instancia EC2 de AWS (Learner Lab), como un único nodo en modo KRaft. Al registrarse un movimiento, `ms-movimientos` publica el evento en el tópico `movimiento-registrado` y `ms-transacciones` lo consume (`MovimientoEventListener`), genera la transacción, marca como anomalía los montos superiores a 3.000.000 y descarta los eventos que ya procesó.

## 10. Limitaciones conocidas

- Las réplicas de `bff-web` no tienen un balanceador de carga al frente.
- `batch-service` se ejecuta de forma local, fuera de Docker.
- Los microservicios usan H2 en memoria, por lo que los datos se recargan desde los CSV en cada inicio.
- `docker-compose` usa `depends_on` sin *healthchecks*, y se apoya en `restart: on-failure`.
- Solo `ms-cuentas` consume su configuración desde el Config Server, y Eureka no forma parte del despliegue en Docker.
- El partitioner del batch calcula las particiones usando solo el archivo de estados anuales.
- Al reejecutar un Job del batch, los registros se acumulan en las tablas.
- El retiro del cajero realiza dos llamadas sin una transacción que las agrupe, y la publicación del evento a Kafka no está protegida con un patrón Outbox.

## 11. Entregables

| Entregable | Archivo |
|---|---|
| Informe técnico | `informe_tecnico.pdf` |
| Instrucciones de ejecución | `instrucciones.md` |
| Despliegue en AWS | `despliegue.md` |
| Video de la demostración | `video.mp4` |