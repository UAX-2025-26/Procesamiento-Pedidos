# Teoría aplicada al proyecto **Procesamiento de Pedidos** (Tema 3: AOP)

Este documento conecta la teoría del archivo `Teoria.md` (apartado 3: AOP y modularización de
lógica transversal) con la práctica del proyecto `procesamiento-pedidos`.

El objetivo es que puedas explicar el proyecto como un ejemplo claro de **programación orientada
a aspectos (AOP)** usando Spring, donde se separa la lógica de negocio del procesamiento de
pedidos de aspectos transversales como la **auditoría** y la **medición de tiempos**.

---

## 1. Visión general del proyecto

El proyecto `procesamiento-pedidos` es una aplicación de consola con Spring Boot que simula el
procesamiento de varios pedidos y utiliza AOP para:

- Registrar auditoría (inicio, fin y errores) de cada pedido.
- Medir el tiempo total de procesamiento de cada pedido.

Elementos clave:

- **Lógica de negocio**: `OrderProcessingService` y el modelo `Order`.
- **Anotaciones personalizadas**: `@Auditable` y `@TimedProcess`.
- **Aspecto**: `OrderProcessingAspect`, que contiene los advices.
- **Arranque y simulación**: `AppPedidosApplication`.

Esta estructura coincide con lo visto en `Teoria.md`:
- Un **servicio funcional** que no sabe nada de logging/auditoría.
- Un **aspecto** que intercepta métodos anotados para aplicar lógica transversal.
- Anotaciones que expresan **intención** y permiten evitar código repetido.

---

## 2. Componentes y su rol en la arquitectura AOP

### 2.1. `Order` (modelo de dominio)

- Ubicación: `com.example.apppedidos.orders.Order`.
- Tipo: `record` de Java con tres campos:
  - `id`: identificador único del pedido.
  - `total`: importe total del pedido.
  - `customerName`: nombre del cliente.
- Rol: representar de forma mínima la información necesaria de un pedido.

**Relación con la teoría:**
- Es el **modelo de dominio** sobre el que se aplica la lógica de negocio y las
  preocupaciones transversales (auditoría/métricas).
- El método `id()` generado por el record es aprovechado por el aspecto vía reflexión
  (`extractOrderId`) para no acoplar el aspecto a una clase específica.

### 2.2. `OrderProcessingService` (servicio de negocio)

- Ubicación: `com.example.apppedidos.service.OrderProcessingService`.
- Anotado con `@Service` (bean de Spring).
- Método principal: `processOrder(Order order)`.
  - Anotado con:
    - `@Async`: se ejecuta en un hilo separado.
    - `@Auditable`: marca el método para auditoría.
    - `@TimedProcess`: marca el método para medición de tiempo.
  - Devuelve `CompletableFuture<Boolean>` indicando éxito o fallo.

**Responsabilidades:**

1. Simular el flujo de procesamiento de un pedido:
   - Verificación de stock (`simulateStep`).
   - Procesamiento de pago (`simulateStep`).
   - Posible fallo (`maybeFail`).
   - Preparación del envío (`simulateStep`).
2. Traducir cualquier fallo simulado (excepciones `RuntimeException`) a un `false` en el
   `CompletableFuture`, de manera que la simulación pueda contar cuántos pedidos
   se procesaron correctamente.

**Relación con la teoría AOP:**

- En `Teoria.md` se señala que la lógica transversal (auditoría, logging, métricas) se debe
  extraer de la clase de negocio para evitar **código duplicado y acoplamiento fuerte**.
- `OrderProcessingService` se centra **solo en el flujo funcional del pedido** y la simulación
  de errores; no hay `System.out.println` de auditoría ni cálculo manual de tiempos dentro.
- La presencia de las anotaciones `@Auditable` y `@TimedProcess` indica la **intención** de que
  otros módulos (el aspecto) añadan esa funcionalidad de forma declarativa.

### 2.3. Anotaciones `@Auditable` y `@TimedProcess`

- Ubicación: `com.example.apppedidos.annotations`.
- Ambas son anotaciones muy sencillas:
  - `@Target(ElementType.METHOD)`: sólo aplicables a métodos.
  - `@Retention(RetentionPolicy.RUNTIME)`: conservadas en tiempo de ejecución para que el
    motor AOP de Spring pueda detectarlas.

**`@Auditable`:**
- Marca que el método debe ser **auditado**.
- El aspecto `OrderProcessingAspect` busca esta anotación en los pointcuts y aplica
  advices `@Before`, `@AfterReturning` y `@AfterThrowing`.

**`@TimedProcess`:**
- Marca que el método debe ser **medido en tiempo**.
- El aspecto define un advice `@Around` sobre los métodos anotados con `@TimedProcess`.

**Relación con la teoría:**
- Estas anotaciones son los **"marcadores"** (metadata) que permiten definir Pointcuts
  limpios y declarativos, tal como se explica en el Tema 3:
  - En lugar de referirse a nombres de clases o paquetes, se seleccionan los métodos
    en función de la anotación.
- Permiten que el servicio de negocio no tenga referencia directa al aspecto ni a la
  infraestructura de AOP.

### 2.4. `OrderProcessingAspect` (aspecto AOP)

- Ubicación: `com.example.apppedidos.aspects.OrderProcessingAspect`.
- Anotaciones:
  - `@Aspect`: indica que esta clase contiene advices y pointcuts.
  - `@Component`: para que Spring la registre como bean y pueda aplicar AOP sobre ella.

Contiene cuatro advices:

1. `@Before("@annotation(auditable) && args(order,..)")` → `auditStart(...)`
   - Se ejecuta **antes** de cualquier método anotado con `@Auditable` cuyo primer
     argumento sea `order`.
   - Extrae `id` mediante `extractOrderId(order)`.
   - Imprime un mensaje de auditoría de inicio.

2. `@AfterReturning(pointcut = "@annotation(auditable) && args(order,..)")` → `auditEnd(...)`
   - Se ejecuta **después** de que un método `@Auditable` termine sin lanzar excepción.
   - Vuelve a extraer el id y muestra un mensaje de fin de proceso.

3. `@Around("@annotation(timedProcess) && args(order,..)")` → `measureTime(...)`
   - Rodea la ejecución de métodos con `@TimedProcess`.
   - Captura el tiempo con `System.currentTimeMillis()` antes y después.
   - Si el método se ejecuta correctamente, muestra el tiempo de éxito.
   - Si lanza una excepción, muestra el tiempo hasta el fallo y vuelve a lanzar la excepción.

4. `@AfterThrowing(pointcut = "@annotation(auditable) && args(order,..)", throwing = "ex")` → `logError(...)`
   - Se ejecuta cuando un método `@Auditable` lanza una excepción.
   - Extrae el id y registra un mensaje de error con el motivo.

Método auxiliar clave:

- `extractOrderId(Object order)`:
  - Usa **reflexión** para llamar al método `id()` del objeto recibido.
  - Permite aplicar el aspecto también a otros tipos de pedidos que tengan un método `id()`.
  - Si algo falla, devuelve `-1L` como valor por defecto.

**Relación con la teoría de AOP (`Teoria.md`):**

- `OrderProcessingAspect` es la **implementación directa de los conceptos**:
  - **Aspecto**: módulo que contiene la lógica transversal.
  - **Advice**: los métodos anotados con `@Before`, `@AfterReturning`, `@AfterThrowing`, `@Around`.
  - **Pointcut**: las expresiones `"@annotation(...) && args(order,..)"` que seleccionan
    qué métodos se interceptan.
  - **Joinpoint**: la ejecución de los métodos `processOrder` (o cualquier otro método
    anotado) donde se aplica la lógica transversal.

- Muestra claramente el uso de distintos tipos de advices, tal y como se describe en el Tema 3:
  - `@Before`, `@AfterReturning`, `@AfterThrowing`, `@Around`.
- Gracias a este aspecto, la clase de servicio no contiene ni una sola línea de código
  de auditoría ni de medición de tiempo: se ha logrado **separación de preocupaciones**.

### 2.5. `AppPedidosApplication` (arranque y simulación)

- Ubicación: `com.example.apppedidos.AppPedidosApplication`.
- Anotaciones:
  - `@SpringBootApplication`: configuración y arranque de Spring Boot.
  - `@EnableAsync`: habilita métodos `@Async` (que se usan en `OrderProcessingService`).
- Define un `CommandLineRunner runSimulation(OrderProcessingService service)` que:
  - Crea una lista de pedidos de ejemplo.
  - Imprime un mensaje de recepción de pedido por consola.
  - Lanza el procesamiento con `service.processOrder(order)` para cada uno.
  - Espera a que terminen todos los `CompletableFuture` con `CompletableFuture.allOf(...).join()`.
  - Cuenta cuántos han tenido éxito y cuántos han fallado.
  - Mide el tiempo total de la simulación con `Instant`/`Duration`.

**Relación con la teoría:**
- Provee el **escenario de ejecución real** sobre el que se ve AOP en acción:
  - Por consola verás mensajes de auditoría, tiempos y errores que **no están
    programados directamente en el servicio** sino en el aspecto.
- Permite demostrar la diferencia entre:
  - Lógica de negocio (flujo de pedido, simulación de errores).
  - Lógica transversal (auditoría/métricas) aplicada desde fuera.

---

## 3. Mapeo directo a los conceptos del Tema 3 (AOP)

En `Teoria.md` se habla de:

- Problema de la lógica transversal repetida (logging, auditoría, métricas).
- Conceptos fundamentales de AOP: aspecto, joinpoint, pointcut, advice, weaving.
- Tipos de advice (`@Before`, `@AfterReturning`, `@AfterThrowing`, `@After`, `@Around`).
- Implementación de AOP en Spring con proxies dinámicos.

Este proyecto es un ejemplo de libro:

1. **Aspecto (`OrderProcessingAspect`)**
   - Encapsula toda la lógica de auditoría y métricas.

2. **Joinpoints**
   - Cada ejecución del método `processOrder` (y potencialmente otros métodos anotados
     con `@Auditable` o `@TimedProcess`).

3. **Pointcuts**
   - Usan expresiones basadas en anotaciones para seleccionar joinpoints:
     - `"@annotation(auditable) && args(order,..)"` → métodos auditables que reciben un pedido.
     - `"@annotation(timedProcess) && args(order,..)"` → métodos cronometrables.

4. **Advices**
   - `@Before` / `@AfterReturning` / `@AfterThrowing` / `@Around` implementan las
     distintas fases alrededor de la ejecución del método.

5. **Weaving**
   - Lo hace Spring en tiempo de ejecución mediante proxies (no tienes que escribir código
     para ello). Simplemente al tener `@Aspect` + `@Component` + dependencias AOP,
     Spring genera proxies alrededor de los beans objetivo.

**Conclusión teórica:** este proyecto muestra cómo AOP permite **sacar fuera** del servicio
código que, de otro modo, estaría mezclado y repetido en todos los métodos de negocio
que quisieras auditar o cronometrar.

---

## 4. Cómo explicar el flujo completo en una defensa

Puedes exponer el proyecto siguiendo este guion:

1. **Modelo y servicio de negocio**
   - `Order` como entidad sencilla.
   - `OrderProcessingService.processOrder` como simulación de un pipeline de pedido
     (stock, pago, envío, fallos aleatorios).

2. **Anotaciones como contrato**
   - `@Auditable` y `@TimedProcess` señalan qué métodos deben ser interceptados.
   - El servicio no sabe quién hace la auditoría ni quién mide tiempos; solo declara
     que desea esas funcionalidades.

3. **Aspecto que implementa la lógica transversal**
   - `OrderProcessingAspect` aplica:
     - Mensaje de inicio y fin.
     - Mensajes de error.
     - Medición de tiempo en éxito y en fallo.
   - Explica brevemente cada tipo de advice.

4. **Ejecución real**
   - `AppPedidosApplication` arranca, genera pedidos y lanza su procesamiento asíncrono.
   - Mostrarías (o comentarías) la traza de consola, indicando qué mensajes vienen del
     servicio y cuáles del aspecto.

5. **Ventajas que aporta AOP (desde la teoría)**
   - Código de negocio más limpio y fácil de mantener.
   - Cambios en la auditoría o métricas se hacen en **un solo sitio** (el aspecto).
   - Posibilidad de reutilizar el mismo aspecto en otros servicios con métodos
     anotados igual.

---

## 5. Posibles extensiones para lucirte

Si quieres ir un poco más allá cuando defiendas el proyecto, puedes comentar ideas como:

- Añadir una persistencia sencilla de auditoría (por ejemplo, escribir en un archivo o
  en una base de datos) sin tocar el servicio, solo cambiando el aspecto.
- Añadir un nuevo aspecto, por ejemplo, para **reintentos automáticos** en ciertos fallos
  concretos (
  usando otra anotación como `@RetryOnFailure`).
- Combinar este enfoque AOP con métricas exportadas (por ejemplo, a Prometheus) usando
  un aspecto que registre contadores y tiempos.

Con todo esto, puedes defender `procesamiento-pedidos` como un ejemplo claro de cómo se
aplica el Tema 3 (AOP) en un proyecto práctico, explicando las decisiones de diseño y
cómo se apoyan en los conceptos teóricos de la asignatura.

