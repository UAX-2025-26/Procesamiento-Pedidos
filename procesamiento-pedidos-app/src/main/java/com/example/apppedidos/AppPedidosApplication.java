package com.example.apppedidos;

import com.example.apppedidos.orders.Order;
import com.example.apppedidos.service.OrderProcessingService;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.scheduling.annotation.EnableAsync;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.concurrent.CompletableFuture;

// Aplicación principal de Spring Boot que lanza una simulación de procesamiento de pedidos
@SpringBootApplication // Anotación combinada que habilita la autoconfiguración de Spring Boot, el escaneo de componentes y permite definir configuración adicional
@EnableAsync // Habilita el soporte de métodos @Async en la aplicación - Permite que métodos anotados con @Async se ejecuten de forma asíncrona en hilos separados
public class AppPedidosApplication {

    public static void main(String[] args) {
        // Arranca el contexto de Spring y ejecuta los beans CommandLineRunner
        SpringApplication.run(AppPedidosApplication.class, args);
    }

    // Bean que se ejecuta al iniciar la aplicación y simula la llegada y procesamiento de pedidos
    @Bean // Registra este método como un bean de Spring que será gestionado por el contenedor
    CommandLineRunner runSimulation(OrderProcessingService service) {
        return args -> {
            System.out.println("=== INICIO DE SIMULACIÓN DE PEDIDOS ===");
            // Marca de tiempo de inicio para medir la duración total de la simulación
            Instant start = Instant.now();

            // Lista de pedidos de ejemplo con id, importe total y nombre del cliente
            List<Order> orders = List.of(
                    new Order(1L, 120.50, "Ana López"),
                    new Order(2L, 89.99, "Carlos Gómez"),
                    new Order(3L, 45.00, "Marta Ruiz"),
                    new Order(4L, 300.10, "Diego Torres"),
                    new Order(5L, 15.75, "Laura Fernández"),
                    new Order(6L, 220.00, "Pedro Ramírez"),
                    new Order(7L, 75.30, "Sofía Medina"),
                    new Order(8L, 50.00, "Juan Pérez"),
                    new Order(9L, 199.99, "Lucía Vargas"),
                    new Order(10L, 130.00, "Jorge Castillo")
            );

            // Traza simple indicando que los pedidos han sido recibidos por el sistema
            orders.forEach(order ->
                    System.out.printf("[INFO] Pedido %d recibido para el cliente: %s%n", order.id(), order.customerName())
            );

            // Lanza el procesamiento asíncrono de cada pedido y recoge los futuros
            List<CompletableFuture<Boolean>> futures = orders.stream()
                    .map(order -> service.processOrder(order)
                            // Normaliza posibles nulls a false
                            .thenApply(result -> Boolean.TRUE.equals(result))
                            // Si la llamada asíncrona lanza excepción, se considera fallo
                            .exceptionally(ex -> false))
                    .toList();

            // Espera a que todos los procesos asíncronos se completen
            CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();

            // Cuenta cuántos pedidos se han completado correctamente
            long success = futures.stream().filter(CompletableFuture::join).count();
            long failed = orders.size() - success;

            // Calcula el tiempo total invertido en procesar todos los pedidos
            long totalMs = Duration.between(start, Instant.now()).toMillis();

            System.out.println("=== PROCESAMIENTO FINALIZADO ===");
            System.out.printf("Pedidos completados exitosamente: %d%n", success);
            System.out.printf("Pedidos con error: %d%n", failed);
            System.out.printf("Tiempo total de simulación: %d ms aprox.%n", totalMs);
        };
    }
}
