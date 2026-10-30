## Análisis de Impacto (Qué se descartó por sobreingeniería)

Durante el desarrollo, se consideraron y descartaron las siguientes alternativas aplicando el principio YAGNI (You Aren't Gonna Need It) y la Regla de Tres:

1. **Spring ApplicationEventPublisher en vez de Observer manual:** Se consideró publicar eventos de dominio (`PedidoProcesadoEvent`) con `@EventListener`. Se descartó porque, con exactamente tres observadores fijos y conocidos, la indirección adicional no aporta desacoplamiento real y dificulta rastrear la ejecución.
2. **Visitor o Abstract Factory para exportar reportes:** Se consideró preparar el sistema para exportar a PDF y Excel construyendo una jerarquía de exportadores. Se descartó porque actualmente no existe ninguna solicitud real para esos formatos (no cumple la Rule of Three).
3. **Jerarquía de Excepciones para Validadores (Aporte propio):** Se consideró crear excepciones específicas (`MontoExcedidoException`, `DocumentoFaltanteException`, `DuplicadoException`) para cada regla en lugar de usar un simple record `ResultadoValidacion`. Se descartó porque el controlador REST actual solo necesita devolver un HTTP 400 genérico con el motivo en texto; no existe lógica de recuperación diferenciada por error. Se reconsideraría si un cliente externo pidiera códigos de error estrictos para manejar flujos de UI distintos.

# Proyecto Integrador - Sistema de Reembolsos Corporativos

## Descripción

Repositorio correspondiente al post-contenido de la Unidad 12 de la asignatura Patrones de Diseño de Software.

Este proyecto integrador consolida la aplicación de patrones de diseño de comportamiento para resolver diferentes requisitos de variabilidad dentro de un sistema de solicitudes de reembolso corporativo.

Se implementan tres patrones principales:

- **Chain of Responsibility:** Para gestionar las validaciones de las solicitudes mediante una cadena de verificaciones independientes.
- **Strategy:** Para encapsular las diferentes fórmulas de cálculo del reembolso según la categoría del gasto, complementado con un mecanismo Factory para seleccionar la estrategia correspondiente.
- **Observer:** Para notificar simultáneamente a diferentes interesados cuando ocurre un cambio relevante en el sistema.

Adicionalmente, el proyecto incorpora una evaluación arquitectónica mediante **ArchUnit**, integración continua con **GitHub Actions** y un análisis de impacto enfocado en evitar la sobreingeniería mediante los principios **YAGNI** (*You Aren't Gonna Need It*) y **Rule of Three**.

---

## Estructura del proyecto

La organización del código fuente se distribuye en módulos que separan las responsabilidades del dominio, procesamiento, aplicación e infraestructura.

```text
reembolsos-integrado/
├── .github/
│   └── workflows/
│       └── arquitectura.yml
│
├── docs/
│   └── adr/
│       └── ADR-001.md
│
├── src/
│   ├── main/
│   │   └── java/com/empresa/reembolsos/
│   │       ├── dominio/
│   │       ├── procesamiento/
│   │       │   ├── validacion/
│   │       │   ├── calculo/
│   │       │   └── notificacion/
│   │       ├── aplicacion/
│   │       ├── infraestructura/
│   │       └── adaptadores/
│   │           └── rest/
│   │
│   └── test/
│       └── java/com/empresa/reembolsos/
│           └── ReglasArquitectura.java
```

### Descripción de los módulos

| **Módulo / Componente** | **Responsabilidad** |
|:---|:---|
| `dominio/` | Contiene las entidades y reglas de negocio principales, manteniéndose independiente de los detalles de infraestructura. |
| `procesamiento/validacion/` | Implementa la cadena de validadores mediante Chain of Responsibility. |
| `procesamiento/calculo/` | Contiene las estrategias de cálculo de reembolsos y el mecanismo de selección mediante Factory. |
| `procesamiento/notificacion/` | Implementa el patrón Observer para gestionar las notificaciones a los diferentes interesados. |
| `aplicacion/` | Contiene los servicios y casos de uso que coordinan las operaciones del sistema. |
| `infraestructura/` | Gestiona los detalles técnicos de persistencia mediante JPA. |
| `adaptadores/rest/` | Expone las funcionalidades del sistema mediante controladores REST. |
| `ReglasArquitectura.java` | Contiene las pruebas de arquitectura implementadas con ArchUnit. |
| `arquitectura.yml` | Define el pipeline de integración continua mediante GitHub Actions. |
| `ADR-001.md` | Documenta las decisiones arquitectónicas y sus respectivas justificaciones. |

---

## Parte 1: Diseño propuesto (Pre-Implementación)

Antes de iniciar la implementación, se realizó un análisis de los requisitos funcionales para identificar sus puntos de variabilidad y seleccionar los patrones de diseño más adecuados.

### Análisis de requisitos funcionales

| **Requisito funcional** | **Punto de variabilidad** | **Patrón propuesto** | **Justificación** |
|:---|:---|:---|:---|
| **RF1: Validación en varias etapas** | Las reglas de validación (monto, documentos y duplicados), así como la posibilidad de incorporar nuevas reglas. | **Chain of Responsibility** | Permite encadenar verificaciones independientes donde cada eslabón decide si aprueba o rechaza la solicitud. Facilita agregar o quitar validadores sin modificar el código central. |
| **RF2: Cálculo por categoría de gasto** | La fórmula matemática del reembolso varía según la categoría del gasto y pueden incorporarse nuevas categorías. | **Strategy + Factory** | Encapsula cada fórmula de cálculo en su propia clase y permite seleccionar la estrategia correspondiente en tiempo de ejecución, evitando grandes bloques condicionales `switch`. |
| **RF3: Notificación simultánea a varios interesados** | La cantidad de partes interesadas (contabilidad, auditoría y empleado) que reaccionan ante los cambios del sistema. | **Observer** | Desacopla el servicio principal de los interesados. El servicio notifica a suscriptores mediante una abstracción, permitiendo incorporar nuevos observadores sin modificar la lógica de negocio. |

---

## Parte 2: Decisiones de diseño

Durante la implementación se evaluaron diferentes alternativas para garantizar una arquitectura mantenible, evitando introducir abstracciones innecesarias.

| **Decisión** | **Elección** | **Justificación** |
|:---|:---|:---|
| **1. Orden de validadores** | Documentos → Monto → Duplicados | El validador de duplicados requiere consultar la base de datos. Por eficiencia, se ubica al final para ejecutar primero las validaciones de menor costo computacional y evitar consultas innecesarias. |
| **2. Selección de estrategia** | Inyección automática de `List<CalculadoraReembolso>` | Mediante la inyección de una lista de estrategias y su organización en un mapa (Factory), incorporar una nueva categoría de gasto requiere únicamente crear una nueva implementación con `@Component`. Esto favorece el principio abierto/cerrado (OCP) y evita modificar bloques condicionales existentes. |
| **3. Arquitectura (ADR-001)** | Capas simples, sin puertos de persistencia | Actualmente no existen planes de cambiar el motor de base de datos. Introducir interfaces y puertos de persistencia supondría una abstracción especulativa. Además, el cambio previsto en el módulo de contabilidad ya está contemplado mediante el patrón Observer. |
| **4. Límites en ArchUnit** | Excluir JPA y Web, permitir estereotipos de Spring | Aislar completamente el dominio de anotaciones como `@Component` introduciría fricción innecesaria. Por ello, se prohíbe el acoplamiento a componentes de persistencia y controladores, pero se permite la inyección de dependencias como una decisión pragmática. |

---

## Parte 3: Análisis de impacto (Evitando la sobreingeniería)

Durante el desarrollo se evaluaron diferentes alternativas arquitectónicas y de implementación. Algunas fueron descartadas aplicando los principios **YAGNI** (*You Aren't Gonna Need It*) y **Rule of Three**, priorizando soluciones justificadas por las necesidades actuales del sistema.

### Alternativas evaluadas y descartadas

| **Técnica evaluada** | **Decisión** | **Justificación** |
|:---|:---|:---|
| **Eventos de dominio** (`@EventListener` / `ApplicationEventPublisher`) | Descartado | Con exactamente tres observadores definidos, introducir un bus de eventos agregaría indirección sin aportar un beneficio significativo de desacoplamiento frente a una implementación explícita del patrón Observer. Además, dificultaría el seguimiento del flujo de ejecución. |
| **Abstract Factory** (Exportación de reportes PDF/Excel) | Descartado | Actualmente no existe un requisito real para exportar reportes en diferentes formatos. Implementar esta abstracción anticipadamente supondría diseñar para necesidades hipotéticas, incumpliendo el principio YAGNI y la regla de tres (*Rule of Three*). |
| **Jerarquía de excepciones** (Validadores) | Descartado | Se evaluó crear excepciones específicas como `MontoExcedidoException` en lugar de utilizar un `record` para representar los resultados de validación. Se descartó porque la API REST devuelve actualmente una respuesta HTTP 400 genérica. Se reconsiderará si en el futuro el frontend requiere flujos de error diferenciados. |

---

## Ejecución y pruebas

Para verificar el funcionamiento del sistema y validar las reglas arquitectónicas establecidas, ejecuta el siguiente comando desde la terminal de Visual Studio Code.

### Ejecutar pruebas y validación del proyecto

```bash
mvn clean verify
```

Este comando realiza el ciclo completo de validación del proyecto, incluyendo:

- Compilación del código fuente.
- Ejecución de las pruebas unitarias y de negocio.
- Ejecución de las pruebas arquitectónicas mediante ArchUnit.
- Verificación de las reglas de arquitectura establecidas.

**Resultado esperado:** Construcción exitosa del proyecto y cumplimiento de las pruebas automatizadas.

### Integración continua (GitHub Actions)

El proyecto incorpora un pipeline de integración continua mediante GitHub Actions, definido en:

```text
.github/workflows/arquitectura.yml
```

Este flujo permite automatizar la verificación del proyecto y comprobar que los cambios introducidos mantengan las reglas arquitectónicas establecidas.

---

## Herramientas y tecnologías utilizadas

| **Herramienta / Tecnología** | **Uso** |
|:---|:---|
| Java 17 | Lenguaje de programación. |
| Apache Maven | Gestión de dependencias y construcción del proyecto. |
| Spring Boot | Framework para el desarrollo de la aplicación y gestión de dependencias. |
| JPA | Tecnología de persistencia de datos. |
| JUnit 5 | Implementación y ejecución de pruebas automatizadas. |
| ArchUnit | Validación automatizada de reglas arquitectónicas. |
| GitHub Actions | Integración continua y automatización de verificaciones. |
| Visual Studio Code | Entorno de desarrollo. |
| Git | Control de versiones. |
| GitHub | Alojamiento y gestión del repositorio. |

---

## Decisiones arquitectónicas (ADR)

Las decisiones arquitectónicas relevantes del proyecto se documentan mediante registros ADR (*Architecture Decision Records*).

**ADR-001:** Documenta la elección de una arquitectura por capas simples, justificando la decisión de no introducir puertos de persistencia y evitando abstracciones que no responden a necesidades actuales.

Ubicación del documento:

```text
docs/adr/ADR-001.md
```

---

## Conclusiones

Este proyecto integrador permitió aplicar de manera práctica tres patrones de diseño de comportamiento para resolver diferentes necesidades de variabilidad dentro de un sistema de reembolsos corporativos.

La implementación de **Chain of Responsibility** permitió estructurar las validaciones en etapas independientes; **Strategy**, complementado con Factory, facilitó la incorporación de nuevas fórmulas de cálculo sin modificar la lógica central; y **Observer** permitió desacoplar el proceso principal de los diferentes interesados que reciben notificaciones.

Por otra parte, la incorporación de **ArchUnit** y **GitHub Actions** permitió establecer mecanismos automatizados para verificar las reglas arquitectónicas y mantener la calidad estructural del proyecto durante su evolución.

Finalmente, el análisis de impacto demostró la importancia de aplicar criterios como **YAGNI** y **Rule of Three**, evitando introducir abstracciones prematuras y priorizando soluciones proporcionales a las necesidades reales del sistema.

En conjunto, el proyecto demuestra que una buena arquitectura no consiste en utilizar la mayor cantidad posible de patrones, sino en seleccionar e implementar aquellos que resuelven problemas concretos, manteniendo un equilibrio entre extensibilidad, simplicidad y mantenibilidad.