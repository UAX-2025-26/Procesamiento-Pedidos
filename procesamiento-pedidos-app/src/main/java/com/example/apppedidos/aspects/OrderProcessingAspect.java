package com.example.apppedidos.aspects;

import com.example.apppedidos.annotations.Auditable;
import com.example.apppedidos.annotations.TimedProcess;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.AfterReturning;
import org.aspectj.lang.annotation.AfterThrowing;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.springframework.stereotype.Component;

// Aspecto que añade auditoría y medición de tiempos al procesamiento de pedidos
@Aspect
@Component
public class OrderProcessingAspect {

    // Advice que se ejecuta ANTES de métodos anotados con @Auditable
    // y que reciben un objeto order como primer argumento
    @Before("@annotation(auditable) && args(order,..)")
    public void auditStart(Auditable auditable, Object order) {
        Long id = extractOrderId(order);
        System.out.printf("--- Auditoría: Inicio de proceso para Pedido %d ---%n", id);
    }

    // Advice que se ejecuta DESPUÉS de que un método @Auditable termine correctamente
    @AfterReturning(pointcut = "@annotation(auditable) && args(order,..)") // Define un advice que se ejecuta después de que el método objetivo retorne exitosamente (sin lanzar excepciones)
    public void auditEnd(Auditable auditable, Object order) {
        Long id = extractOrderId(order);
        System.out.printf("--- Auditoría: Fin de proceso para Pedido %d ---%n", id);
    }

    // Advice @Around que rodea la ejecución de métodos anotados con @TimedProcess
    // Se usa para medir el tiempo total de procesamiento, tanto en éxito como en fallo
    @Around("@annotation(timedProcess) && args(order,..)") // Define un advice que rodea completamente la ejecución del método objetivo, permitiendo ejecutar código antes y después, o incluso decidir si invocar el método
    public Object measureTime(ProceedingJoinPoint pjp, TimedProcess timedProcess, Object order) throws Throwable {
        long start = System.currentTimeMillis();
        try {
            // Continúa con la ejecución del método objetivo
            Object result = pjp.proceed();
            long time = System.currentTimeMillis() - start;
            Long id = extractOrderId(order);
            System.out.printf("[PERFORMANCE] Pedido %d procesado en %d ms%n", id, time);
            return result;
        } catch (Throwable ex) {
            // Si el método lanza una excepción, también se registra el tiempo hasta el fallo
            long time = System.currentTimeMillis() - start;
            Long id = extractOrderId(order);
            System.out.printf("[PERFORMANCE] Pedido %d falló en %d ms%n", id, time);
            throw ex;
        }
    }

    // Advice que se ejecuta cuando un método @Auditable lanza una excepción
    @AfterThrowing(pointcut = "@annotation(auditable) && args(order,..)", throwing = "ex") // Define un advice que se ejecuta cuando el método objetivo lanza una excepción, permitiendo capturar y procesar el error
    public void logError(Auditable auditable, Object order, Throwable ex) {
        Long id = extractOrderId(order);
        System.out.printf("[ERROR] Pedido %d falló: %s%n", id, ex.getMessage());
    }

    // Método auxiliar que intenta obtener el id del pedido usando reflexión
    private Long extractOrderId(Object order) {
        try {
            // Asume que el objeto tiene un método "id()" (como el record Order)
            return (Long) order.getClass().getMethod("id").invoke(order);
        } catch (Exception e) {
            // Si no se puede obtener, devuelve -1 como valor por defecto
            return -1L;
        }
    }

}
