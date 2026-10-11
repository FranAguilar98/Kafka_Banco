# Despliegue de Kafka en AWS — Banco XYZ

Kafka corre en una instancia EC2 del AWS Academy Learner Lab, como un único nodo en modo KRaft (sin Zookeeper). Los microservicios `ms-movimientos` y `ms-transacciones` se conectan a ella desde Docker local.

## 1. Crear la instancia EC2

1. Entrar al Learner Lab, abrir la consola de AWS y lanzar una instancia EC2.
2. Elegir una imagen Ubuntu y un tipo de instancia como por ejemplo c7i-flex.large.
3. Crear (o reutilizar) un par de llaves `.pem` para conectarse por SSH.
4. En el *Security Group*, permitir de entrada:

| Puerto | Uso |
|---|---|
| 22 | SSH |
| 9092 | Kafka |

5. Asociar una **Elastic IP** a la instancia, para que su dirección no cambie cuando el laboratorio se reinicie.

## 2. Instalar Docker en la instancia

```bash
ssh -i <llave>.pem ubuntu@<IP_ELASTICA>
sudo apt-get update
sudo apt-get install -y docker.io
sudo systemctl enable --now docker
sudo usermod -aG docker ubuntu
```

Cerrar la sesión y volver a entrar para que el grupo `docker` tenga efecto. (En Amazon Linux, instalar con `sudo yum install -y docker`.)

## 3. Levantar Kafka

Reemplazar `<IP_ELASTICA>` por la IP elástica de la instancia:

```bash
docker run -d --name kafka --restart unless-stopped -p 9092:9092 \
  -e KAFKA_NODE_ID=1 \
  -e KAFKA_PROCESS_ROLES=broker,controller \
  -e KAFKA_LISTENERS=PLAINTEXT://0.0.0.0:9092,CONTROLLER://0.0.0.0:9093 \
  -e KAFKA_ADVERTISED_LISTENERS=PLAINTEXT://<IP_ELASTICA>:9092 \
  -e KAFKA_LISTENER_SECURITY_PROTOCOL_MAP=CONTROLLER:PLAINTEXT,PLAINTEXT:PLAINTEXT \
  -e KAFKA_CONTROLLER_LISTENER_NAMES=CONTROLLER \
  -e KAFKA_CONTROLLER_QUORUM_VOTERS=1@localhost:9093 \
  -e KAFKA_INTER_BROKER_LISTENER_NAME=PLAINTEXT \
  -e KAFKA_OFFSETS_TOPIC_REPLICATION_FACTOR=1 \
  -e KAFKA_TRANSACTION_STATE_LOG_REPLICATION_FACTOR=1 \
  -e KAFKA_TRANSACTION_STATE_LOG_MIN_ISR=1 \
  -e KAFKA_AUTO_CREATE_TOPICS_ENABLE=true \
  apache/kafka:3.7.0
```

El tópico `movimiento-registrado` se crea solo al publicar el primer evento.

## 4. Conectar los servicios

En la raíz del proyecto, en el computador donde corre Docker:

```bash
echo KAFKA_BOOTSTRAP_SERVERS=<IP_ELASTICA>:9092 > .env
docker compose up -d --build
```

## 5. Verificar

Comprobar que el broker responde y listar los tópicos:

```bash
docker exec kafka /opt/kafka/bin/kafka-topics.sh --bootstrap-server localhost:9092 --list
```

Hacer un retiro en el ATM (ver `instrucciones.md`) y leer el evento:

```bash
docker exec kafka /opt/kafka/bin/kafka-console-consumer.sh \
  --bootstrap-server localhost:9092 --topic movimiento-registrado --from-beginning
```

En los logs de `ms-transacciones` debe aparecer el procesamiento del evento.

## 6. Notas

- Si el Learner Lab se detiene, la instancia también; al reiniciarla, el contenedor arranca solo (`--restart unless-stopped`) y la Elastic IP se mantiene.
- El broker usa `PLAINTEXT` y un solo nodo, suficiente para esta evaluación. En producción convendría cifrar el tráfico (TLS) y usar varios brokers con replicación.
- No dejar abierto el puerto 9092 a todo internet más tiempo del necesario: limitarlo a la IP desde donde se ejecuta Docker.
