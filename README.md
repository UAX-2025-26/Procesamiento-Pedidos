# Simulación concurrente de procesamiento de pedidos con AOP

https://github.com/UAX-2025-26/Procesamiento-Pedidos.git

## Miembros del grupo

- Javier Yustres
- Mario Blanco

## Descripción

Aplicación de consola basada en Spring Boot que simula el procesamiento de pedidos de una tienda online. Cada pedido se procesa en un hilo independiente usando `@Async`, mientras que la auditoría, el control de rendimiento y el manejo de errores se implementan con Programación Orientada a Aspectos (AOP).

## Lógica de la solución

- `AppPedidosApplication`: clase principal de Spring Boot. Habilita `@EnableAsync` y define un `CommandLineRunner` que crea y lanza concurrentemente 10 pedidos usando el servicio `OrderProcessingService`. Resume la simulación al final (éxitos, errores y tiempo total).
- `orders/Order`: clase sencilla de dominio que representa un pedido con `id`, `total` y `customerName`.
- `annotations/Auditable`: anotación personalizada para marcar métodos cuyo inicio/fin deben ser auditados.
- `annotations/TimedProcess`: anotación personalizada para marcar métodos cuyo tiempo de ejecución debe ser medido.
- `service/OrderProcessingService`: servicio donde se implementa la lógica de negocio del procesamiento de un pedido. El método `processOrder` es `@Async`, `@Auditable` y `@TimedProcess`. Simula pasos como verificación de stock, pago y envío con `Thread.sleep()` y lanza fallos aleatorios.
- `aspects/OrderProcessingAspect`: aspecto AOP que:
  - Con `@Before` registra el inicio de cada proceso de pedido para métodos anotados con `@Auditable`.
  - Con `@Around` (para `@TimedProcess`) calcula el tiempo de procesamiento de cada pedido y lo registra, tanto en éxito como en error.
  - Con `@AfterThrowing` captura excepciones de métodos `@Auditable` y registra un mensaje de error por pedido.

## Cómo ejecutar

Desde la carpeta del proyecto:

```cmd
mvnw.cmd spring-boot:run
```

La salida en consola mostrará:

- Recepción de cada pedido.
- Mensajes de auditoría de inicio y fin de proceso.
- Tiempo de procesamiento de cada pedido (`[PERFORMANCE]`).
- Errores simulados (`[ERROR]`) para algunos pedidos.
- Resumen final con número de pedidos exitosos, fallidos y tiempo total aproximado.
