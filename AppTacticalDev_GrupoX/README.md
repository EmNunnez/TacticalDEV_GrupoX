# TacticalDEV Visitas — App Android

**Asignatura:** DSY1105 Desarrollo de Aplicaciones Móviles · Evaluación Parcial 4
**Caso:** TacticalDEV, trazabilidad y verificación de visitas a vitrinas freezer y conservadoras

## 1. Nombre del proyecto
**AppTacticalDev_GrupoX**: app Android en Kotlin + Jetpack Compose + Material 3 con arquitectura **MVVM**, conectada a un microservicio **Spring Boot + MariaDB desplegado en AWS EC2**.

## 2. Integrante
**Emilio Nuñez** — desarrollo completo (app Android, backend, despliegue en AWS, pruebas y firma).

## 3. Funcionalidades
| Rol | Qué hace |
|---|---|
| **Cliente** (minimarket) | Pide equipo nuevo, reparación o retiro, con foto opcional desde cámara o galería. Edita o cancela sus solicitudes pendientes y ve el historial de sus equipos |
| **Operario** (técnico, vendedor o transportista) | Registra la visita con **foto + GPS tomados en el mismo instante**. Si está fuera del radio del local no puede enviarla. Funciona sin conexión (Room) y sincroniza después |
| **Supervisor** | Asigna solicitudes al operario compatible, valida o rechaza visitas (las sospechosas se marcan en rojo) y hace el CRUD de puntos de venta, equipos y usuarios |

- **Pantallas y formularios:** formularios validados con `isError` y `supportingText`; navegación con `NavHost` + `sealed class Rutas`, TopAppBar, NavigationDrawer y NavigationBar.
- **Estado y adaptabilidad:** `StateFlow` + animaciones (`AnimatedVisibility`, `animateColorAsState`, transiciones entre pantallas). Las listas usan grillas adaptativas.
- **Persistencia local:** **DataStore** guarda la sesión y **Room** guarda las visitas sin conexión.
- **Recursos nativos:** cámara (`TakePicture` + `FileProvider`), galería y GPS (`FusedLocationProvider`), con detección de GPS falso.
- **API externa:** **Open-Meteo**, con su propio Retrofit. Muestra la temperatura del local al registrar la visita.

## 4. Endpoints utilizados
Backend en la nube: **`http://52.71.145.213:8080/`** (EC2 con IP elástica fija).

| Recurso | Endpoints |
|---|---|
| Auth | `POST /api/auth/login` · `POST /api/auth/registro` |
| Usuarios | `GET /api/usuarios?rol=` · `GET /api/usuarios/{id}` · `POST /api/usuarios` · `PUT /api/usuarios/{id}` · `DELETE /api/usuarios/{id}` |
| Puntos de venta | `GET /api/puntos-venta?clienteId=` · `POST` · `PUT /{id}` · `DELETE /{id}` |
| Equipos | `GET /api/equipos?puntoVentaId=&enBodega=` · `GET /api/equipos/{id}/historial` · `POST` · `PUT /{id}` · `DELETE /{id}` |
| Solicitudes | `GET /api/solicitudes?clienteId=&operarioId=&estado=` · `GET /{id}` · `POST` · `PUT /{id}` · `PUT /{id}/asignar` · `DELETE /{id}` |
| Visitas | `GET /api/visitas?operarioId=&estado=` · `GET /{id}` · `POST` · `PUT /{id}/validar` |
| **API externa** | `GET https://api.open-meteo.com/v1/forecast?latitude=&longitude=&current=temperature_2m,relative_humidity_2m,weather_code` |

## 5. Pasos para ejecutar
1. Inicia el laboratorio de AWS. El backend arranca solo con la instancia (servicio `systemd`).
2. Para comprobar que el backend responde, abre en el navegador `http://52.71.145.213:8080/api/usuarios?rol=OPERARIO`.
3. Abre esta carpeta en **Android Studio**, espera el *Gradle Sync* y presiona **Run ▶**.
4. La URL del backend se configura en `gradle.properties` (`API_USUARIOS_URL` y `API_OPERACIONES_URL`).

**Cuentas de prueba (datos ficticios):**

| Rol | Correo | Contraseña |
|---|---|---|
| Supervisor | supervisor@tacticaldev.cl | Super1234 |
| Técnico | tecnico@tacticaldev.cl | Opera1234 |
| Vendedor | vendedor@tacticaldev.cl | Opera1234 |
| Cliente | cliente@minimarket.cl | Cliente1234 |

### Pruebas unitarias y cobertura (≥ 80 %)
```bash
./gradlew testDebugUnitTest        # Kotest + MockK + coroutines-test + MockWebServer
./gradlew koverHtmlReportDebug     # reporte: app/build/reports/kover/htmlDebug/index.html
./gradlew koverVerifyDebug         # falla si líneas, ramas o instrucciones bajan de 80 %
./gradlew connectedDebugAndroidTest  # prueba de UI del Login (emulador o celular)
```
- **Qué cubren:** ViewModels, Repositories, utilidades, navegación, todos los modelos (`data class`, `enum`, `data object`) y las interfaces Retrofit.
- **`ContratoBackendTest`:** usa un servidor HTTP falso que responde con el **mismo JSON que el backend en AWS**, para comprobar cada endpoint, método HTTP, parámetro y campo.
- **Qué se excluye de la medición:** solo lo que no puede ejecutarse en una prueba unitaria de JVM. Es la interfaz Compose, que se prueba con Compose UI Test; las clases que necesitan un dispositivo (Activity, Room, DataStore, GPS, cámara); y el código generado (`BuildConfig`, `R`).

## 6. APK firmado y llave (.jks)
- Llave: `keystore/tacticaldev-release.jks` (alias `tacticaldev`, válida hasta 2051)
- Configuración: `signingConfigs { create("release") }` en `app/build.gradle.kts`, que lee `keystore.properties`
- Generar el APK firmado:
  ```bash
  ./gradlew assembleRelease
  # resultado: app/build/outputs/apk/release/app-release.apk
  ```
  También se puede hacer desde **Build → Generate Signed App Bundle / APK → APK → release**.
- Instalarlo en el celular: copia `app-release.apk` al teléfono y ábrelo (permite "Instalar apps desconocidas"), o usa `adb install -r app-release.apk`.

## 7. Planificación
- Tablero Trello: *(enlace)*
- Repositorio GitHub: *(enlace)*
