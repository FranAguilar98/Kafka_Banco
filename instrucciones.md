# Instrucciones de ejecución — Banco XYZ

Guía para levantar y probar el proyecto en un computador local. Para Kafka en AWS, ver [`despliegue.md`](despliegue.md).

## 1. Requisitos

| Herramienta | Versión |
|---|---|
| Java (JDK) | 17 o superior |
| Maven | 3.9 o superior |
| Docker Desktop | Compose v2 |
| MySQL | 8 (solo para el `batch-service`) |
| Kafka | Una instancia accesible (ver `despliegue.md`) |

## 2. Obtener el código

```bash
git clone https://github.com/FranAguilar98/Kafka_Banco.git
cd Kafka_Banco
```

## 3. Microservicios, BFF e infraestructura (Docker)

1. Compilar todos los módulos (genera los `.jar` ejecutables que usan las imágenes):

```bash
mvn clean package -DskipTests
```

2. Indicar dónde está Kafka (IP elástica de la EC2 de AWS):

```bash
echo KAFKA_BOOTSTRAP_SERVERS=<IP_ELASTICA_EC2>:9092 > .env
```

3. Construir y levantar los servicios:

```bash
docker compose up -d --build
docker compose ps
```

4. Esperar un minuto y comprobar que todos los servicios estén en estado `Up`. Como el compose usa `restart: on-failure`, un servicio que arranca antes que su dependencia se reinicia solo hasta que ésta esté lista.

### Servicios y puertos

| Servicio | Puerto |
|---|---|
| `config-server` | 8888 |
| `auth-server` | 9000 |
| `eureka-server` | 8761 |
| `ms-cuentas` | 8090 |
| `ms-movimientos` | 8091 |
| `ms-transacciones` | 8092 |
| `bff-web` | 8081 (o 8181-8189 al escalar) |
| `bff-mobile` | 8082 |
| `bff-atm` | 8083 |

## 4. Escalabilidad horizontal

```bash
docker compose up -d --scale bff-web=2
docker compose ps
```

Cada réplica de `bff-web` queda publicada en un puerto distinto del rango `8181-8189`.

## 5. Batch (local, fuera de Docker)

1. Tener MySQL encendido. La base `banco_xyz_batch` se crea sola.
2. Crear `batch-service/src/main/resources/application-secrets.yml` con la contraseña (el archivo no se sube al repositorio):

```yaml
spring:
  datasource:
    password: <tu_contraseña_de_mysql>
```

3. Levantar el servicio:

```bash
mvn -pl batch-service spring-boot:run
```

4. Disparar cada Job (los Jobs no se ejecutan al iniciar):

```
POST http://localhost:8080/api/batch/transacciones-diarias
POST http://localhost:8080/api/batch/intereses-mensuales
POST http://localhost:8080/api/batch/estados-cuenta-anuales
```

El progreso queda registrado en las tablas del `JobRepository` en MySQL. Los CSV de entrada están en `batch-service/src/main/resources/data`.

> Al reejecutar un Job, los registros se acumulan en las tablas. Para repetir la prueba desde cero, vaciar las tablas antes.

## 6. Pruebas de los BFF

Los tres BFF primero piden un login y luego usan el JWT recibido como `Authorization: Bearer <token>`.

### Canal web (usuario demo `admin.web`)

```
POST http://localhost:8081/api/web/auth/login
{ "usuario": "admin.web", "password": "Admin#2026" }

GET  http://localhost:8081/api/web/cuentas/109
GET  http://localhost:8081/api/web/cuentas/109/historial-anual
```

### Canal móvil

```
POST http://localhost:8082/api/mobile/auth/login
GET  http://localhost:8082/api/mobile/cuentas/109/resumen
GET  http://localhost:8082/api/mobile/movimientos/recientes
```

El login móvil usa el mismo formato (`usuario`, `password`) con el usuario demo definido en `MobileAuthService`.

### Cajero (ATM)

El login exige tarjeta y PIN, y todas las llamadas llevan además el header `X-Atm-Device-Key` (por defecto `atm-device-key-demo-cambiar`, configurable con `ATM_DEVICE_API_KEY`).

```
POST http://localhost:8083/api/atm/auth/login
{ "numeroTarjeta": "4551000000000001", "pin": "1234" }

GET  http://localhost:8083/api/atm/cuentas/109/saldo
POST http://localhost:8083/api/atm/cuentas/109/retiro
{ "monto": 100 }
```

El retiro actualiza el saldo, registra el movimiento y publica el evento en Kafka.

## 7. Qué verificar

| Prueba | Resultado esperado |
|---|---|
| Llamar a un microservicio sin token | `401` |
| Llamar con un token sin el scope requerido | `403` |
| Retiro en el ATM | `200`, con el nuevo saldo |
| Detener `ms-cuentas` y consultar el saldo en `bff-atm` | Tras varios fallos el Circuit Breaker se abre y se responde `503` controlado |
| Retiro con Kafka disponible | Mensaje en el tópico `movimiento-registrado` (ver `despliegue.md`) |
| Eureka en `http://localhost:8761` | Los tres microservicios en estado `UP` |
| `docker compose up -d --scale bff-web=2` | Dos réplicas de `bff-web` |

## 8. Detener y limpiar

```bash
docker compose down
```
