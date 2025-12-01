# TEMA 3 - procesamiento-pedidos

## 1. Visión general de la arquitectura

Este proyecto es una pequeña aplicación de consola con Spring Boot que demuestra cómo aplicar
**Programación Orientada a Aspectos (AOP)** para separar la lógica de negocio de la lógica
transversal de **auditoría** y **medición de tiempos**.

Elementos clave:

- **Dominio de negocio**: modelo `Order` y servicio `OrderProcessingService`.
- **Anotaciones personalizadas**: `@Auditable` y `@TimedProcess`.
- **Aspecto AOP**: `OrderProcessingAspect`, que implementa auditoría y métricas.
- **Arranque y simulación**: `AppPedidosApplication`, que crea pedidos y lanza el flujo.

El objetivo didáctico es mostrar los conceptos del Tema 3 de `Teoria.md`:

- Aspectos, joinpoints, pointcuts y advices.
- Tipos de advice: `@Before`, `@AfterReturning`, `@AfterThrowing`, `@Around`.
- Uso de anotaciones como contrato para lógica transversal.
- Weaving mediante proxies en Spring.

## 2. Componentes principales y su rol

### 2.1. Modelo de dominio (`orders.Order`)

`Order` es un `record` Java que representa un pedido mínimo:

- Campos:
  - `Long id`: identificador del pedido.
  - `double total`: importe del pedido.
  - `String customerName`: nombre del cliente.

Rol en la arquitectura:

- Es la entidad de negocio sobre la que se aplican tanto el procesamiento funcional como
  la auditoría y el timing.
- Su método `id()` es usado por el aspecto `OrderProcessingAspect` mediante reflexión para
  poder registrar información del pedido sin acoplarse a una implementación concreta.

### 2.2. Servicio de negocio (`service.OrderProcessingService`)

`OrderProcessingService` es un `@Service` que contiene la **lógica principal de negocio**:

- Método `CompletableFuture<Boolean> processOrder(Order order)`:
  - Anotado con:
    - `@Async`: se ejecuta en un hilo distinto, permitiendo procesar varios pedidos en paralelo.
    - `@Auditable`: indica que debe registrarse auditoría de inicio/fin y errores.
    - `@TimedProcess`: indica que debe medirse el tiempo total de procesamiento.
  - Flujo interno:
    1. Llama a `simulateStep("Verificando stock", order)`.
    2. Llama a `simulateStep("Procesando pago", order)`.
    3. Llama a `maybeFail(order)` donde se introducen fallos aleatorios.
    4. Llama a `simulateStep("Preparando envío", order)`.
    5. Si todo va bien, devuelve `CompletableFuture.completedFuture(true)`.
    6. Si hay alguna excepción (`RuntimeException`), captura el error y devuelve
       `CompletableFuture.completedFuture(false)`.

Métodos auxiliares:

- `simulateStep(String step, Order order)`:
  - Simula una etapa del proceso durmiendo el hilo entre 500 y 2000 ms.
  - Representa tareas bloqueantes como consultas a BD, servicios externos, etc.

- `maybeFail(Order order)`:
  - Introduce fallos con una probabilidad del 20% en pago y 20% en verificación de stock.
  - Lanza `RuntimeException` con mensajes de error simulados.

Relación con la teoría AOP:

- La clase se enfoca solo en **la lógica funcional** del proceso de pedido.
- No contiene `System.out.println` de auditoría ni cálculos de tiempos; esos aspectos
  se implementan fuera, en `OrderProcessingAspect`.
- Las anotaciones `@Auditable` y `@TimedProcess` expresan la **intención** de que haya
  auditoría y métricas, sin acoplarse al cómo.

### 2.3. Anotaciones personalizadas (`annotations.Auditable`, `annotations.TimedProcess`)

Ambas se encuentran en `com.example.apppedidos.annotations` y comparten estructura similar:

- `@Target(ElementType.METHOD)`: solo se pueden aplicar a métodos.
- `@Retention(RetentionPolicy.RUNTIME)`: están disponibles en tiempo de ejecución para
  que el motor AOP de Spring pueda detectarlas.

`@Auditable`:

- Es una **anotación de marcador** que indica que el método debe ser sujeto a auditoría.
- `OrderProcessingAspect` define pointcuts basados en esta anotación para aplicar
  advices de inicio, fin y error.

`@TimedProcess`:

- Es otra anotación de marcador para indicar que el método debe medirse en tiempo.
- El aspecto la usa para el advice `@Around` que cronometra la ejecución.

Relación con la teoría:

- En `Teoria.md` se menciona que en Spring AOP es muy común seleccionar joinpoints según
  **anotaciones personalizadas**, porque expresan reglas de negocio de alto nivel.
- Estas anotaciones permiten que el servicio no dependa en absoluto del aspecto, logrando
  un fuerte **bajo acoplamiento**.

### 2.4. Aspecto AOP (`aspects.OrderProcessingAspect`)

`OrderProcessingAspect` es el corazón del Tema 3 aplicado:

- Anotado con `@Aspect` y `@Component` para que Spring lo registre y aplique AOP.

Contiene varios **advices** que implementan lógica transversal alrededor de métodos anotados:

1. `@Before("@annotation(auditable) && args(order,..)")` → `auditStart(...)`:
   - Se ejecuta antes de métodos anotados con `@Auditable` cuyo primer parámetro sea `order`.
   - Usa `extractOrderId(order)` para obtener el id.
   - Imprime un mensaje `--- Auditoria: Inicio de proceso para Pedido X ---`.

2. `@AfterReturning(pointcut = "@annotation(auditable) && args(order,..)")` → `auditEnd(...)`:
   - Se ejecuta después de que el método termine con éxito.
   - Vuelve a obtener el id e imprime `--- Auditoria: Fin de proceso para Pedido X ---`.

3. `@Around("@annotation(timedProcess) && args(order,..)")` → `measureTime(...)`:
   - Rodea la ejecución de métodos anotados con `@TimedProcess`.
   - Lógica:
     - Toma el tiempo inicial con `System.currentTimeMillis()`.
     - Llama a `pjp.proceed()` para ejecutar el método real.
     - Calcula el tiempo transcurrido.
     - Si no hay error: imprime `[PERFORMANCE] Pedido X procesado en Y ms`.
     - Si hay error: también imprime `[PERFORMANCE] Pedido X falló en Y ms` y relanza
       la excepción.

4. `@AfterThrowing(pointcut = "@annotation(auditable) && args(order,..)", throwing = "ex")` → `logError(...)`:
   - Se ejecuta cuando un método anotado con `@Auditable` lanza una excepción.
   - Obtiene el id del pedido y registra `[ERROR] Pedido X falló: mensaje`.

Método auxiliar: `extractOrderId(Object order)`

- Usa **reflexión** para invocar el método `id()` del parámetro `order`.
- Devuelve el id del pedido como `Long` o `-1L` si algo falla.

Relación con la teoría de AOP:

- El aspecto ejemplifica todos los conceptos clave del Tema 3:
  - **Aspecto**: clase que agrupa la lógica transversal.
  - **Joinpoint**: ejecuciones de métodos anotados con `@Auditable` y `@TimedProcess`.
  - **Pointcut**: expresiones `"@annotation(...) && args(order,..)"` que seleccionan
    esos joinpoints.
  - **Advice**: `@Before`, `@AfterReturning`, `@AfterThrowing`, `@Around`.
- La lógica transversal de auditoría y tiempos está completamente separada de la clase
  de servicio.

### 2.5. Aplicación principal y simulación (`AppPedidosApplication`)

`AppPedidosApplication` es la clase que arranca la aplicación y lanza una simulación de pedidos:

- Anotaciones:
  - `@SpringBootApplication`: aplicación Spring Boot estándar.
  - `@EnableAsync`: activa soporte para métodos `@Async` (usados en `OrderProcessingService`).

- Define un `@Bean CommandLineRunner runSimulation(OrderProcessingService service)` que:
  1. Imprime `=== INICIO DE SIMULACIÓN DE PEDIDOS ===`.
  2. Registra el tiempo de inicio (`Instant start`).
  3. Crea una lista de 10 pedidos de ejemplo con distintos clientes e importes.
  4. Imprime por consola que cada pedido ha sido recibido.
  5. Llama a `service.processOrder(order)` para cada pedido, obteniendo una lista de
     `CompletableFuture<Boolean>`.
  6. Usa `CompletableFuture.allOf(...).join()` para esperar a que se procesen todos.
  7. Cuenta cuántos pedidos han tenido éxito (futuros que devuelven `true`) y cuántos
     han fallado.
  8. Calcula el tiempo total de simulación con `Duration.between(start, Instant.now())`.
  9. Imprime un resumen final con número de éxitos, fallos y tiempo total.

Relación con la teoría:

- Proporciona el escenario donde se ve **AOP en acción**: al ejecutar la simulación aparecen
  en consola los mensajes de auditoría y rendimiento generados por el aspecto.
- También muestra el uso de `@Async` y `CompletableFuture` para procesar varios pedidos
  de forma concurrente (conceptos conectados con el Tema 2, aunque aquí el foco es AOP).

## 3. Flujo completo de procesamiento de un pedido

1. **Creación de pedidos**:
   - `AppPedidosApplication` crea una lista de objetos `Order` con id, total y cliente.

2. **Lanzamiento del procesamiento asíncrono**:
   - Por cada `Order`, se llama a `OrderProcessingService.processOrder(order)`.
   - Debido a `@Async`, cada procesamiento se ejecuta en un hilo separado gestionado por Spring.

3. **Aplicación de aspectos (weaving en tiempo de ejecución)**:
   - Spring crea un proxy alrededor de `OrderProcessingService` porque hay un `@Aspect`
     que apunta a métodos anotados.
   - Cuando se llama a `processOrder`, realmente se invocan los advices de `OrderProcessingAspect`:
     - `@Before` → log de inicio.
     - `@Around` → medida de tiempo.
     - Si hay error → `@AfterThrowing` y mensaje de error.
     - Si termina bien → `@AfterReturning` y mensaje de fin.

4. **Flujo interno del servicio**:
   - Dentro de `processOrder` se simulan las etapas del pipeline de un pedido con
     `simulateStep` y `maybeFail`.
   - Puede lanzar excepciones simuladas para representar errores.

5. **Finalización y recogida de resultados**:
   - `AppPedidosApplication` espera a todas las tareas con `CompletableFuture.allOf(...).join()`.
   - Calcula cuántos pedidos han sido procesados correctamente (`true`) y cuántos no (`false`).
   - Imprime las estadísticas finales.

6. **Mensajes en consola**:
   - Mientras tanto, los advices del aspecto van escribiendo mensajes de:
     - Inicio/fin de cada pedido.
     - Errores y tiempos de procesamiento.

Este flujo muestra claramente la separación entre:

- **Lógica de negocio** (qué significa procesar un pedido).
- **Lógica transversal** (auditoría, métricas) que se aplica sin modificar la clase de negocio.

## 4. Decisiones de diseño y relación con la teoría (Tema 3)

1. **Uso de anotaciones como contrato de AOP**:
   - En vez de basar los pointcuts en nombres de clases/paquetes, se usan `@Auditable` y
     `@TimedProcess`.
   - Esto hace el diseño más declarativo y alineado con la práctica recomendada en Spring AOP.

2. **Separación completa de preocupaciones**:
   - `OrderProcessingService` no contiene código de auditoría ni tiempos.
   - Cualquier cambio en la auditoría o métricas se hace modificando sólo el aspecto.

3. **Aplicación de todos los tipos de advice más comunes**:
   - `@Before`, `@AfterReturning`, `@AfterThrowing`, `@Around` se ilustran en un ejemplo sencillo.
   - Esto te permite explicar en la defensa qué hace cada tipo y cuándo se ejecuta.

4. **Uso de reflexión para desacoplar el aspecto del modelo**:
   - `extractOrderId` no depende de la clase concreta `Order`, solo de que el objeto tenga
     un método `id()`.
   - Esto ejemplifica cómo un aspecto puede ser **reutilizable** si el contrato es genérico.

5. **Integración con `@Async`**:
   - Aunque el foco es AOP, el uso de `@Async` muestra cómo aspectos y concurrencia pueden
     convivir: el aspecto intercepta llamadas a métodos que se ejecutan en otros hilos.

## 5. Cómo defender este proyecto en una exposición

Un guion razonable para explicarlo podría ser:

1. **Contexto**: "Este proyecto demuestra cómo usar AOP en Spring para auditar y medir el
   tiempo de procesamiento de pedidos sin mezclar esa lógica con la de negocio".
2. **Arquitectura**: describir paquetes (`orders`, `service`, `annotations`, `aspects`) y
   la clase de arranque.
3. **Flujo de negocio**: explicar cómo `OrderProcessingService` simula el pipeline de
   un pedido (stock, pago, posibles fallos, envío).
4. **AOP**:
   - Presentar `@Auditable` y `@TimedProcess`.
   - Explicar `OrderProcessingAspect` y cada advice.
   - Dejar claro el concepto de joinpoint, pointcut, advice y weaving.
5. **Ejecución y observación**:
   - Contar cómo al lanzar la simulación se ven los mensajes de auditoría y tiempos
     generados por el aspecto.
6. **Relación con la teoría**:
   - Enlazar todo con el Tema 3 de `Teoria.md`: lógica transversal, proxies Spring AOP,
     tipos de advice, anotaciones como contrato.

Con esto tendrás un discurso sólido para defender `procesamiento-pedidos` como aplicación
práctica de la programación orientada a aspectos en Spring.
