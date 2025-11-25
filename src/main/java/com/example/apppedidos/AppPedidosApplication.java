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

@SpringBootApplication
@EnableAsync
public class AppPedidosApplication {

    public static void main(String[] args) {
        SpringApplication.run(AppPedidosApplication.class, args);
    }

    @Bean
    CommandLineRunner runSimulation(OrderProcessingService service) {
        return args -> {
            System.out.println("=== INICIO DE SIMULACIÓN DE PEDIDOS ===");
            Instant start = Instant.now();

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

            orders.forEach(order ->
                    System.out.printf("[INFO] Pedido %d recibido para el cliente: %s%n", order.id(), order.customerName())
            );

            List<CompletableFuture<Boolean>> futures = orders.stream()
                    .map(order -> service.processOrder(order)
                            .thenApply(result -> Boolean.TRUE.equals(result))
                            .exceptionally(ex -> false))
                    .toList();

            CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();

            long success = futures.stream().filter(CompletableFuture::join).count();
            long failed = orders.size() - success;

            long totalMs = Duration.between(start, Instant.now()).toMillis();

            System.out.println("=== PROCESAMIENTO FINALIZADO ===");
            System.out.printf("Pedidos completados exitosamente: %d%n", success);
            System.out.printf("Pedidos con error: %d%n", failed);
            System.out.printf("Tiempo total de simulación: %d ms aprox.%n", totalMs);
        };
    }
}
