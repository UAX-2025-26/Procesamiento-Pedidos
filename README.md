# Simulación concurrente de procesamiento de pedidos con AOP

https://github.com/UAX-2025-26/Procesamiento-Pedidos.git

## Miembros del grupo

- Javier Yustres
- Mario Blanco

## Contexto de la práctica

Trabajo para la asignatura de Programación Concurrente. El objetivo es mostrar cómo separar la lógica de negocio de las tareas transversales (auditoría, métricas y errores) usando Programación Orientada a Aspectos (AOP) en una aplicación Spring Boot que procesa pedidos en paralelo.

## Qué implementamos

- Arranque de Spring Boot (`AppPedidosApplication`) con `@EnableAsync` y un `CommandLineRunner` que lanza 10 pedidos simultáneos.
- Procesamiento asíncrono (`@Async`, `CompletableFuture`) que simula verificación de stock, pago y envío con pausas aleatorias.
- AOP con anotaciones personalizadas (`@Auditable`, `@TimedProcess`) para auditar inicio/fin, medir tiempos y capturar excepciones sin ensuciar la lógica de negocio.
- Dominio simple (`orders/Order`) con `id`, `total` y `customerName`.
- Simulación de fallos aleatorios (pago rechazado, error de stock) para observar el manejo de errores y la trazabilidad.
- Resumen final con pedidos completados, pedidos con error y el tiempo total de la simulación.

## Tecnologías

- Spring Boot 3
- AOP con AspectJ (starter de Spring)
- Ejecución asíncrona con `@Async` y `CompletableFuture`
- Java 17

## Cómo ejecutarlo

Requisitos: Java 17 y Maven 3.x.

```cmd
mvnw.cmd spring-boot:run
```

Si prefieres usar Maven instalado localmente:

```cmd
mvn spring-boot:run
```

## Qué verás en la consola

- `[INFO]` al recibir cada pedido.
- Auditoría de inicio y fin de proceso por pedido.
- `[PERFORMANCE]` con el tiempo de ejecución, tanto en éxito como en fallo.
- `[ERROR]` con el motivo del fallo cuando se simula una excepción.
- Resumen final de la simulación.
