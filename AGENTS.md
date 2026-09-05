# Reglas Operativas para Agentes (`AGENTS.md`)

Este archivo contiene las directrices, restricciones inmutables y buenas prácticas que cualquier agente de IA debe acatar al interactuar con esta base de código.

---

## 🛑 Directrices Críticas e Inmutables

1. **Razonamiento Previo Obligatorio**:
   - Antes de realizar cualquier cambio, debes razonar a fondo qué herramientas vas a usar, qué archivos se verán afectados y cuál es la causa raíz de cualquier problema. No respondas ni actúes sin antes pensar.
2. **Entorno del Usuario**:
   - El usuario **no tiene PC**, únicamente cuenta con su **teléfono móvil**.
   - No sugieras comandos que requieran conectar el móvil a un ordenador por USB con `adb devices`.
   - Las soluciones con privilegios deben apoyarse en **Shizuku** (depuración inalámbrica en el propio dispositivo) o en el modo normal sin root como alternativa.
3. **Distribución y Dependencias**:
   - El APK se publicará en **Uptodown** o canales de terceros, **NO en Google Play**. No restrinjas permisos útiles por políticas comerciales de Google Play.
   - **No escatimar en dependencias funcionales**: Si una librería probada resuelve el problema de forma robusta, úsala. No inventes implementaciones manuales frágiles para ahorrar unos pocos kilobytes.
4. **Protección de Nombres y Marcas**:
   - Evita en el nombrado de archivos, clases o recursos cualquier marca registrada protegida por derechos de autor que pueda generar problemas legales al usuario.
5. **Restricción de Optimizaciones del Sistema**:
   - Si en el futuro se implementan módulos de optimización o aceleración de juegos, **NUNCA utilices ni alteres propiedades del tipo `persist.sys.*`**.
6. **Manejo de Mensajes de Commit**:
   - Si existe un archivo `commit_message.txt`, su contenido debe redactarse siempre en **español** y no debe modificarse a menos que el usuario lo solicite expresamente.
7. **Lenguajes Nativos / Compilados**:
   - Si se llega a utilizar C++, Rust, Python u otro lenguaje nativo junto a Kotlin, debe estar correctamente configurado en Gradle y compilar sin saltarse fases. No sustituyas con soluciones simuladas si el usuario solicitó un componente específico.
8. **Eficiencia en Inspección de Código**:
   - No revises ni leas archivos de código que no sean estrictamente necesarios para la consulta o tarea que el usuario está solicitando.

---

## 📐 Estándares de Código y Arquitectura

### 1. Kotlin & Jetpack Compose
- Uso exclusivo de **Kotlin moderno** y **Jetpack Compose**.
- Mantener la arquitectura **MVVM (Model-View-ViewModel)** con flujo unidireccional de datos (`StateFlow` y `collectAsState`).
- Diseñar pensando en ergonomía móvil: componentes con área táctil mínima de 48.dp, soporte completo para `enableEdgeToEdge()` y gestión limpia de `WindowInsets`.

### 2. Concurrencia y Rendimiento
- **Prohibido el I/O en el hilo principal**: Todas las operaciones con archivos, procesos shell o consultas de almacenamiento deben correr en `Dispatchers.IO` o mediante `Flow`.
- Usar `remember` y `derivedStateOf` en Compose para evitar recomposiciones costosas en listas con miles de archivos.

### 3. Ejecución de Scripts Shell y Shizuku
- Todo script `.sh` debe almacenarse en `app/src/main/assets/scripts/`.
- La ejecución con Shizuku debe validar previamente la disponibilidad (`Shizuku.pingBinder()`) y el estado de la autorización (`Shizuku.checkSelfPermission()`).
- Siempre debe proveerse un modo de contingencia que permita escanear el almacenamiento público accesible cuando Shizuku no esté activo.
