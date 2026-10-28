# MPAZ PRO 3° — Módulo Jefatura UTP (Sección 007, San Bernardo)

Proyecto Android para el caso de Vinculación con el Medio **"MPAZ PRO 3°: Apoyo didáctico y seguimiento del aprendizaje"** de la Escuela Escritora Marcela Paz. Parte del proyecto base común de la sección, que trae la estructura, las pantallas de cada módulo y perfil, y la navegación. Este equipo implementó el módulo de **Jefatura UTP** (RF-01 y RF-02) con persistencia local en Room. Los demás módulos los implementan otros equipos de la sección.

## Integrantes

| Integrante        | GitHub | Aporte principal |
|-------------------|---|---|
| Fernando Catalán  | fernaanndo | Persistencia con Room (Entity, DAO, Database, datos iniciales) |
| Jean Darius       | Jeanrenel92 | Repository, conexión ViewModel–Room y filtros RF-02 |
| Felipe Cassaretto | FelipeCassaretto | Validaciones RF-01, regla RN-04, AlertDialog y mensajes |
| Bayron Cerda      | B4yr0n-web | Recurso nativo (vibración) y animaciones |

**Repositorio:** https://github.com/fernaanndo/mpaz-ea2

Documentación del caso (carpeta `docs/`):

- `Requerimientos_Seccion007.pdf`: requerimientos funcionales, reglas de negocio, RNF y supuestos.
- `Consultas_Pendientes_Seccion007.pdf`: preguntas abiertas al CITT (Paz Morales). No resolver con supuestos propios.

## Cómo abrir y ejecutar

1. Descomprimir el ZIP en una carpeta limpia.
2. Android Studio → **File > Open…** → seleccionar la carpeta que contiene `settings.gradle.kts`.
3. Esperar el Gradle Sync (la primera vez necesita internet).
4. Ejecutar ▶ **Run 'app'**.
5. En el login, presionar **"Entrar como Jefatura UTP"** (Modo desarrollo). El login real (RF-00) le corresponde a otro equipo.

Si Android Studio sugiere actualizar Gradle o el Android Gradle Plugin, **no aceptar**: las versiones están fijadas para que todos los equipos usen exactamente las mismas.

Usuarios de prueba (en `data/MockData.kt`, para cuando implementen el login):

| Perfil | Usuario | Clave |
|---|---|---|
| Jefatura UTP | utp@mpazpro3.cl | utp123 |
| Docente 3° básico A | docente.3basicoA@mpazpro3.cl | docente123 |
| Estudiante | est001 | 1234 |

La forma en que se autentica el estudiante está pendiente de confirmar con el CITT.

## Requerimientos implementados

**RF-01 · Aprobar o rechazar contenido (Jefatura UTP)**
- Desde el detalle de un contenido, UTP puede aprobarlo o rechazarlo.
- Para rechazar es obligatorio escribir el motivo: si está vacío, el error aparece bajo el campo y no se guarda nada.
- Antes de rechazar se pide confirmación con un diálogo.
- RN-04: no se puede aprobar una segunda unidad de la misma asignatura.
- Se muestra un mensaje con el resultado de la acción.

**RF-02 · Registrar y consultar el estado de aprobación**
- El estado (Pendiente, Aprobado, Rechazado) y la observación de UTP se guardan en Room y se mantienen al cerrar la app.
- La lista de contenidos se puede filtrar por asignatura y por estado; si no hay resultados se indica en pantalla.

**Arquitectura:** UI (Compose) → `MpazProViewModel` (StateFlow) → `ContenidoRepository` → `ContenidoDao` → Room (`AppDatabase`).

**Cómo probarlo:** Entrar como Jefatura UTP → abrir un contenido → aprobar o rechazar → cerrar la app y volver a abrirla para comprobar que el estado se mantiene.

## Recurso nativo: vibración

**Qué hace:** cuando UTP aprueba o rechaza un contenido y la acción se completa, el dispositivo vibra brevemente (100 ms).

**Por qué es pertinente al caso:** confirma de forma física que la decisión quedó registrada, sin depender solo de mirar la pantalla. Respeta las restricciones del caso, que prohíbe cámara, GPS, micrófono y datos biométricos: la vibración no usa ninguno de ellos ni recoge datos del usuario.

**Permiso:** `android.permission.VIBRATE` en `AndroidManifest.xml`. Es un permiso normal, por lo que no se solicita al usuario en tiempo de ejecución.

**Integración:** `DetalleContenidoUtpScreen.kt`. La vibración se dispara con `LaunchedEffect(mensaje)` cuando el ViewModel publica el resultado, por lo que no vibra si la validación falla. Usa `VibrationEffect` en Android 8.0+ y el método anterior en versiones antiguas (minSdk 24).

**Nota:** el emulador no vibra; para verlo se necesita un dispositivo físico.

## Animaciones y componentes interactivos

- `AnimatedVisibility` para mostrar el mensaje de resultado.
- `animateColorAsState` para cambiar el color del estado (gris, verde o rojo).
- Listas desplegables para los filtros y `AlertDialog` para confirmar el rechazo.

## Stack

Kotlin 2.0 · Jetpack Compose · Material 3 · MVVM (ViewModel + StateFlow) · Navigation Compose · Room + KSP y DataStore (ya configurados en Gradle). El caso no define el stack: se aplica el estándar de la asignatura mientras el CITT no indique otra cosa. El caso **no** permite usar cámara, GPS, micrófono ni datos biométricos.

## Flujo de pantallas

```
LOGIN ──(según perfil)──┬─> UTP: Contenidos ──> Revisar contenido (aprobar / rechazar)
                        ├─> DOCENTE: Panel ──> Planificación (objetivos, intentos, asignar)
                        │                 └─> Resultados del curso ──> Informe pedagógico
                        └─> ESTUDIANTE: Mis actividades ──> Material ──> Desafíos
                                        ──> Evaluación final ──> Mis respuestas
Cerrar sesión (cualquier perfil) ──> LOGIN
```

El botón atrás nativo de Android funciona solo (lo maneja el NavHost). La flecha "←" de la barra superior hace lo mismo. Al ingresar, el login sale de la pila; "Cerrar sesión" la limpia completa. Ninguna pantalla del estudiante navega al informe pedagógico (restricción del caso).

## Estructura del código (`app/src/main/java/com/mpazpro3/app/`)

| Carpeta | Contenido |
|---|---|
| `model/` | `Contenido`, `RecursoDidactico`, `Actividad`, `Asignacion`, `Estudiante`, `SeguimientoAvance`, `InformeDocente`, `Usuario`, `Perfil` y sus enums. Corresponden a las entidades del caso. |
| `data/` | `MockData.kt`: datos y textos pedagógicos ficticios de ejemplo. |
| `data/local/` | `ContenidoEntity`, `ContenidoDao`, `AppDatabase` (Room) y `DatosIniciales` (carga los contenidos de ejemplo si la tabla está vacía). |
| `data/repository/` | `ContenidoRepository`: único acceso a los datos de contenidos desde el ViewModel. |
| `viewmodel/` | `MpazProViewModel`: un solo ViewModel compartido. Todas las funciones pendientes están marcadas con `TODO(equipo)`. |
| `navigation/` | `AppNavigation`: rutas, NavHost, ingreso por perfil y cierre de sesión. |
| `ui/components/` | `BarraSuperior` (flecha atrás y cerrar sesión) y `SelectorDesplegable` (lista desplegable reutilizable). |
| `ui/screen/` | `LoginScreen` y una carpeta por perfil: `utp/`, `docente/`, `estudiante/`. |
| `ui/theme/` y `res/` | Tema, colores, logo e ícono **provisionales**. |

## Trazabilidad: requerimiento → pantalla → función a implementar

| Módulo | Requerimiento | Pantalla (ya construida) | Función en el ViewModel (TODO del equipo) |
|---|---|---|---|
| Acceso | Inicio de sesión por perfil | `LoginScreen` | `login()` |
| Autorización (UTP) | Revisar y autorizar o rechazar un contenido | `ContenidosUtpScreen`, `DetalleContenidoUtpScreen` | `aprobarContenido()`, `rechazarContenido()` |
| Autorización (UTP) | Registrar el estado de aprobación | `DetalleContenidoUtpScreen` | `aprobarContenido()`, `rechazarContenido()` |
| Autorización (UTP) | Filtrar contenidos por asignatura y estado | `ContenidosUtpScreen` | `contenidosFiltrados()` |
| Planificación docente | Seleccionar objetivos disponibles (autorizados) | `PlanificacionDocenteScreen` | `contenidosDisponiblesParaAsignar()` |
| Planificación docente | Asignar contenidos y desafíos al curso | `PlanificacionDocenteScreen`, `PanelDocenteScreen` | `asignarAlCurso()`, `asignacionesDelCurso()` |
| Planificación docente | Definir el máximo de intentos de la evaluación final | `PlanificacionDocenteScreen` | `asignarAlCurso()` |
| Planificación docente | Revisar y descargar resultados | `ResultadosCursoScreen` | `progresoGeneral()`, `descargarResultados()` |
| Experiencia del estudiante | Consultar el material asignado | `InicioEstudianteScreen`, `MaterialScreen` | `contenidosDelEstudiante()`, reproducción de audio |
| Experiencia del estudiante | Desafíos de dificultad progresiva | `DesafiosScreen` | `desafiosDe()`, `responderDesafio()` |
| Experiencia del estudiante | Evaluación final dentro de los intentos permitidos | `EvaluacionFinalScreen` | `evaluacionFinalDe()`, `intentosRestantes()`, `enviarEvaluacion()` |
| Experiencia del estudiante | Revisar respuestas correctas e incorrectas | `RevisionRespuestasScreen` | `respuestasDe()` |
| Experiencia del estudiante | Conservar el progreso y confirmar la finalización | `MaterialScreen`, `InicioEstudianteScreen` | `confirmarMaterialRevisado()`, `progreso()` |
| Informe pedagógico | Generar informe (rendimiento, fortalezas, aspectos a reforzar) | `InformeDocenteScreen` | `generarInforme()` |
| Informe pedagógico | Validación docente antes de considerarlo definitivo | `InformeDocenteScreen` | `validarInforme()` |
| Persistencia | Guardar datos y progreso en el dispositivo | — | Room: `ContenidoEntity`, `ContenidoDao`, `AppDatabase` (contenidos UTP) |

**Reglas de negocio que el equipo debe hacer cumplir:**

- El estudiante no puede modificar contenidos ni actividades, ni acceder al informe pedagógico.
- Un contenido no autorizado por UTP no puede ser asignado por el docente.
- El número de intentos de la evaluación final lo define el docente y debe respetarse estrictamente.
- El informe no emite diagnósticos ni aplica consecuencias académicas automáticas.

Para ver todo lo pendiente: **Edit > Find > Find in Files** (Ctrl+Shift+F / Cmd+Shift+F) y buscar `TODO(equipo)`.

## Qué pasa hoy al ejecutar la app (es intencional)

- **Ingresar:** el login siempre rechaza. Hay que entrar con "Modo desarrollo".
- **Listas:** la lista de contenidos de UTP se filtra por asignatura y estado. Las demás listas muestran todos los contenidos y actividades, sin filtrar (por curso o tipo).
- **Acciones:** aprobar y rechazar contenidos (UTP) funcionan y se guardan en Room. Asignar, responder, enviar la evaluación, generar o validar un informe todavía no cambian nada.
- **Avance, retroalimentación e intentos:** el avance aparece en 0 %, la retroalimentación de los desafíos y el conteo de intentos aparecen como pendientes, y la revisión de respuestas aparece vacía.

## Orden sugerido de trabajo

1. **Login:** validar campos vacíos y luego las credenciales por perfil.
2. **UTP:** aprobar y rechazar contenidos, y filtrar la lista.
3. **Docente:**
   - Mostrar solo los contenidos aprobados.
   - Asignarlos al curso con el máximo de intentos.
4. **Estudiante:**
   - Mostrar sus contenidos asignados.
   - Desafíos ordenados con retroalimentación.
   - Evaluación final con control de intentos.
   - Revisión de respuestas.
5. **Docente:**
   - Mostrar el avance de cada estudiante.
   - Generar y validar el informe.
   - Descargar los resultados.
6. **Persistencia:** reemplazar `MockData` por Room (Semana 9). El caso exige conservar el progreso del estudiante.
7. **Cierre:**
   - Quitar el "Modo desarrollo".
   - Revisar que no quede ningún `TODO(equipo)`.
   - Reemplazar el logo, los colores y los textos de ejemplo.

## Contenido pedagógico

Los textos de `MockData.kt` son **de ejemplo**. Cada equipo debe trabajar como máximo una unidad por asignatura (Lenguaje, Matemática, Ciencias Naturales, Historia), con los OA que se definan con el CITT. El material debe ser propio, público o expresamente autorizado: no se puede copiar material protegido. Los audios de apoyo complementan la lectura; no la reemplazan.

## Reglas del proyecto

- **Datos:** solo ficticios (usuarios, cursos, respuestas, resultados e informes).
- **GitHub:**
  - Un integrante sube esta base como commit inicial.
  - Después, una rama por funcionalidad (por ejemplo `feature/login`).
  - Debe haber commits de **ambos** integrantes.
- **Consultas sobre el caso:** van por el canal Estudiante → Docente → CITT → organización.

## Logo e ícono

Son **provisionales** (un libro abierto con estrella). Para usar el logo oficial que entregue la escuela:

- **Logo:** copiar el PNG a `res/drawable/logo_escuela.png` y borrar `logo_escuela.xml`.
- **Ícono de la app:** clic derecho en `res` > **New > Image Asset**.
