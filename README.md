1. Clases Reutilizables en evcharging-common
Debes crear dos clases principales en este módulo:

Clase	Función	Detalle de Implementación
KafkaClientConfig	Gestionar la configuración	Una clase estática o singleton que crea el objeto Properties de Kafka. Debe leer la dirección del broker (HOST_IP:29092) de una variable de entorno o argumento de arranque.
KafkaManager	Wrapper para la funcionalidad	Una clase que contenga métodos estáticos o singleton para: send(topic, key, value) y createConsumer(topic, groupId, handler).

2. Implementación del Productor (Ejemplo: ev-driver)
Tu módulo ev-driver solo necesita una línea de código para enviar un evento, sin saber nada sobre IPs, puertos o serializadores.

Java

// Código en el módulo ev-driver:
public void solicitarSuministro(String driverId, int stationId) {
    // Llama al método de la librería. No hay código de productor aquí.
    KafkaManager.send("requests.raw", driverId, new SupplyRequest(stationId));
}
3. Implementación del Consumidor (Ejemplo: ev-central)
Tu módulo ev-central solo necesita un código simple de inicialización para escuchar un tema.

Java

// Código en el módulo ev-central:
public void startCentral() {
    // Define el grupo y el manejador de mensajes (lambda o clase).
    KafkaManager.createConsumer("telemetry.reports", "central-processor-group", this::handleTelemetryMessage);
    // ... más lógica ...
}

private void handleTelemetryMessage(ConsumerRecord<String, String> record) {
    // Lógica para procesar el evento de telemetría
    System.out.println("Telemetría recibida: " + record.value());
}

Estrategia de Arranque Distribuido (La Guía de Despliegue) 🚀
Para cumplir con el requisito de tener los componentes distribuidos en PC 1, PC 2 y PC 3, sigue este plan de arranque.

1. Preparación en PC 2 (El Servidor Central)
PC 2 es el anfitrión de la infraestructura (Kafka) y del componente principal (Ev_Central).

Tarea	Comando / Acción
Definir la IP del Host	Asegúrate de que el archivo .env en la carpeta de Docker contiene tu IP real (ej., HOST_IP=192.168.1.50).
Arrancar Kafka (y ZooKeeper)	Abre una terminal en la carpeta C:\dev\kafka y ejecuta: docker compose up -d
Arrancar Ev_Central	Abre otra terminal (en tu máquina Windows, fuera de Docker) y arranca el JAR, pasándole los parámetros de Sockets y Kafka: java -jar ev-central.jar <Puerto_Socket_Central> <Kafka_Bootstrap_Servers>
Ejemplo de Arranque	java -jar ev-central.jar 8080 192.168.1.50:29092

Exportar a Hojas de cálculo
2. Arranque en PC 3 (El Punto de Recarga)
PC 3 necesita ejecutar dos módulos: Ev_CP_Monitor y Ev_CP_Engine. Ambos deben saber dónde están sus compañeros de Socket y Kafka.

Módulo	Comando / Acción
Arrancar Ev_CP_Engine	El Engine necesita escuchar las peticiones del Monitor (Socket) y la dirección de Kafka: java -jar ev-cp-engine.jar <Puerto_Socket_Engine> <Kafka_Bootstrap_Servers>
Ejemplo de Arranque Engine	java -jar ev-cp-engine.jar 9000 192.168.1.50:29092
Arrancar Ev_CP_Monitor	El Monitor necesita saber dónde está la Central (Socket) y el Engine (Socket) para conectarse: java -jar ev-cp-monitor.jar <IP_Central> <Puerto_Central> <IP_Engine> <Puerto_Engine>
Ejemplo de Arranque Monitor	java -jar ev-cp-monitor.jar 192.168.1.50 8080 192.168.1.50 9000

Exportar a Hojas de cálculo
3. Arranque en PC 1 (La Aplicación Driver)
PC 1 es el cliente más simple; solo necesita la dirección del broker de Kafka para enviar solicitudes.

Módulo	Comando / Acción
Arrancar Ev_Driver	El Driver necesita la dirección de Kafka: java -jar ev-driver.jar <ID_Driver> <Kafka_Bootstrap_Servers>
Ejemplo de Arranque Driver	java -jar ev-driver.jar DRV001 192.168.1.50:29092