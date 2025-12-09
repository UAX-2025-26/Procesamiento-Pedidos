package com.example.apppedidos.service;

import com.example.apppedidos.annotations.Auditable;
import com.example.apppedidos.annotations.TimedProcess;
import com.example.apppedidos.orders.Order;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.Random;
import java.util.concurrent.CompletableFuture;

// Servicio que simula el procesamiento de pedidos paso a paso
@Service // Marca esta clase como un componente de servicio de Spring para que sea detectado automáticamente y registrado en el contexto de Spring
public class OrderProcessingService {

    // Generador de números aleatorios para tiempos y fallos simulados
    private final Random random = new Random();

    // Método principal de negocio que procesa un pedido
    // @Async: se ejecuta en un hilo separado gestionado por Spring
    // @Auditable: el aspecto registrará inicio/fin y errores del proceso
    // @TimedProcess: el aspecto medirá el tiempo total de procesamiento
    @Async // Indica que este método se ejecutará de forma asíncrona en un hilo separado del pool de hilos configurado por Spring
    @Auditable // Anotación personalizada que activa la auditoría mediante AOP (intercepta el método para registrar inicio, fin y errores)
    @TimedProcess // Anotación personalizada que activa la medición de tiempo mediante AOP (intercepta el método para medir cuánto tarda en ejecutarse)
    public CompletableFuture<Boolean> processOrder(Order order) {
        // Simula la fase de verificación de stock
        simulateStep("Verificando stock");
        // Simula la fase de procesamiento de pago
        simulateStep("Procesando pago");
        // Punto donde, de forma aleatoria, el pedido puede fallar
        maybeFail();
        // Simula la fase de preparación del envío
        simulateStep("Preparando envío");
        // Si se completan todas las fases sin excepción, se marca el pedido como correcto
        return CompletableFuture.completedFuture(true);
    }

    // Simula una etapa del flujo de pedido introduciendo una pausa aleatoria
    private void simulateStep(String step) {
        try {
            // Duerme entre 500 y 2000 ms para imitar trabajo bloqueante (BD, servicios externos, etc.)
            Thread.sleep(500 + random.nextInt(1500));
        } catch (InterruptedException e) {
            // Restablece el estado de interrupción si el hilo es interrumpido
            Thread.currentThread().interrupt();
        }
    }

    // Introduce fallos simulados en el procesamiento del pedido
    private void maybeFail() {
        // 20% de probabilidad de fallo en el pago
        if (random.nextDouble() < 0.2) {
            throw new RuntimeException("Pago rechazado (Error simulado)");
        }
        // 20% de probabilidad de fallo en la verificación de stock
        if (random.nextDouble() < 0.2) {
            throw new RuntimeException("Error al verificar stock (Error simulado)");
        }
    }

}
