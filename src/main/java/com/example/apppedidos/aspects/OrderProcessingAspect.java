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

@Aspect
@Component
public class OrderProcessingAspect {

    @Before("@annotation(auditable) && args(order,..)")
    public void auditStart(Auditable auditable, Object order) {
        Long id = extractOrderId(order);
        System.out.printf("--- Auditoria: Inicio de proceso para Pedido %d ---%n", id);
    }

    @AfterReturning(pointcut = "@annotation(auditable) && args(order,..)")
    public void auditEnd(Auditable auditable, Object order) {
        Long id = extractOrderId(order);
        System.out.printf("--- Auditoria: Fin de proceso para Pedido %d ---%n", id);
    }

    @Around("@annotation(timedProcess) && args(order,..)")
    public Object measureTime(ProceedingJoinPoint pjp, TimedProcess timedProcess, Object order) throws Throwable {
        long start = System.currentTimeMillis();
        try {
            Object result = pjp.proceed();
            long time = System.currentTimeMillis() - start;
            Long id = extractOrderId(order);
            System.out.printf("[PERFORMANCE] Pedido %d procesado en %d ms%n", id, time);
            return result;
        } catch (Throwable ex) {
            long time = System.currentTimeMillis() - start;
            Long id = extractOrderId(order);
            System.out.printf("[PERFORMANCE] Pedido %d falló en %d ms%n", id, time);
            throw ex;
        }
    }

    @AfterThrowing(pointcut = "@annotation(auditable) && args(order,..)", throwing = "ex")
    public void logError(Auditable auditable, Object order, Throwable ex) {
        Long id = extractOrderId(order);
        System.out.printf("[ERROR] Pedido %d falló: %s%n", id, ex.getMessage());
    }

    private Long extractOrderId(Object order) {
        try {
            return (Long) order.getClass().getMethod("id").invoke(order);
        } catch (Exception e) {
            return -1L;
        }
    }
}
