# Documentación - Procesamiento de Pedidos con AOP

Esta carpeta contiene toda la documentación técnica del proyecto de procesamiento asíncrono de pedidos con Programación Orientada a Aspectos.

## Contenido

### Documentos principales

- **EXPLICACION.md**: Descripción detallada de cada componente de la arquitectura, decisiones de diseño y funcionamiento del sistema.
- **TEORIA_APLICADA.md**: Conceptos teóricos de programación concurrente y AOP aplicados en este proyecto (Tema 3).

### Diagramas UML

#### 1. Diagrama de Clases (`clases-procesamiento-pedidos.mmd`)
Muestra la estructura del proyecto:
- **Model**: Order (record con id, total, customerName)
- **Annotations**: @Auditable, @TimedProcess (anotaciones custom)
- **Service**: OrderProcessingService (lógica de procesamiento asíncrono)
- **Aspects**: OrderProcessingAspect (intercepta y añade funcionalidad transversal)
- **Application**: AppPedidosApplication (demo)

#### 2. Diagrama de Secuencia (`secuencia-procesamiento-pedidos.mmd`)
Ilustra el flujo de un pedido:
1. Llamada a processOrder (anotado con @Async)
2. Delegación al thread pool de Spring
3. Interceptación por aspectos (@Before, @Around)
4. Ejecución de pasos (verificar stock, procesar pago, preparar envío)
5. Manejo de fallos simulados (20% probabilidad)
6. Interceptación de finalización (@AfterReturning o @AfterThrowing)
7. Registro de métricas y auditoría

#### 3. Diagrama de Actividad (`actividad-procesamiento-pedidos.mmd`)
Representa el flujo de control:
- Creación de múltiples pedidos
- Ejecución asíncrona con @Async
- Interceptación por aspectos AOP
- Simulación de pasos con delays aleatorios
- Fallos simulados aleatorios
- Logging de auditoría y performance
- Procesamiento concurrente de múltiples pedidos

## Visualización de diagramas

### GitHub
Los archivos `.mmd` se renderizan automáticamente.

### Editores locales
- **VS Code**: Extensión "Markdown Preview Mermaid Support"
- **IntelliJ IDEA**: Plugin "Mermaid"

### Online
[Mermaid Live Editor](https://mermaid.live/)

## Conceptos ilustrados

### Programación Concurrente
1. **@Async**: Ejecución asíncrona en thread pool de Spring
2. **CompletableFuture**: Promesas para resultados asíncronos
3. **Procesamiento paralelo**: Múltiples pedidos concurrentemente
4. **Thread pools**: Gestión automática por Spring

### Programación Orientada a Aspectos (AOP)
1. **@Aspect**: Definición de aspectos
2. **@Before**: Advice que se ejecuta antes del método
3. **@AfterReturning**: Advice tras retorno exitoso
4. **@AfterThrowing**: Advice tras excepción
5. **@Around**: Advice que rodea la ejecución (permite medir tiempo)
6. **ProceedingJoinPoint**: Control de la ejecución del método objetivo
7. **Pointcuts**: Selección de métodos a interceptar
8. **Cross-cutting concerns**: Auditoría y performance como funcionalidad transversal

## Arquitectura

```
AppPedidosApplication
    ↓ (crea múltiples pedidos)
OrderProcessingService.processOrder(@Async)
    ↓ (interceptado por)
OrderProcessingAspect
    ├─→ @Before: Auditoría inicio
    ├─→ @Around: Medición de tiempo
    │   └─→ Ejecución real del método
    │       ├─→ Verificar stock (delay aleatorio)
    │       ├─→ Procesar pago (delay + posible fallo)
    │       └─→ Preparar envío (delay)
    ├─→ @AfterReturning: Auditoría fin exitoso
    └─→ @AfterThrowing: Log de errores
```

## Simulación de fallos

El método `maybeFail()` introduce:
- 20% probabilidad de "Pago rechazado"
- 20% probabilidad de "Error al verificar stock"
- Total: ~40% de fallos en el procesamiento

Esto permite demostrar el manejo de excepciones en contextos asíncronos y la interceptación de errores por aspectos.

## Logs generados

Cada pedido genera:
```
--- Auditoría: Inicio de proceso para Pedido X ---
[PERFORMANCE] Pedido X procesado en Y ms
--- Auditoría: Fin de proceso para Pedido X ---
```

O en caso de error:
```
--- Auditoría: Inicio de proceso para Pedido X ---
[PERFORMANCE] Pedido X falló en Y ms
[ERROR] Pedido X falló: Pago rechazado (Error simulado)
```

## Actualización

Si modificas aspectos o servicios, actualiza los diagramas y documenta las razones del cambio.

