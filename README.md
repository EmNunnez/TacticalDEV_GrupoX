# TacticalDEV Visitas — Trazabilidad y verificación de visitas en terreno

**Asignatura:** DSY1105 Desarrollo de Aplicaciones Móviles · Evaluación Parcial 3
**Caso:** TacticalDEV, CITT Plaza Oeste

## 1. Nombre del proyecto
**TacticalDEV Visitas**. Lo componen:
- `AppTacticalDev_GrupoX/`: app Android (Kotlin + Jetpack Compose + Material 3, arquitectura MVVM).
- `tacticaldev-backend/`: microservicio Spring Boot + MariaDB, **desplegado en AWS EC2**.

Hoy las visitas a vitrinas freezer y conservadoras se informan por WhatsApp, Excel o papel, y muchas nunca ocurrieron ("visitas fantasma"). La app las reemplaza por un registro digital. **La foto de evidencia y el GPS se capturan en el mismo instante.** La app verifica la distancia al punto de venta y el backend la vuelve a verificar al recibir la visita.

## 2. Integrante
**Emilio Nuñez**: desarrollo completo del proyecto (app Android, backend, despliegue en AWS, pruebas unitarias y firma del APK).

## 3. Funcionalidades

| Rol | Qué hace |
|---|---|
| **Cliente** (dueño del minimarket) | Pide equipo nuevo, reparación o retiro, con foto opcional desde cámara o galería. Edita o cancela sus solicitudes pendientes y ve el historial de sus equipos |
| **Operario** (técnico, vendedor o transportista) | Registra la visita con **foto + GPS ligados**. Si está fuera del radio del local, si el GPS es falso o si la señal es débil, no puede enviarla. Funciona sin conexión y sincroniza después |
| **Supervisor** | Asigna solicitudes al operario compatible, valida o rechaza visitas (las sospechosas se marcan en rojo), programa revisiones y hace el CRUD de puntos de venta, equipos y usuarios |

**Flujo completo (CRUD app ↔ backend):**
```
Cliente crea solicitud ......................... POST   /api/solicitudes
Supervisor la asigna ........................... PUT    /api/solicitudes/{id}/asignar
Operario: foto -> GPS en ese instante -> envía . POST   /api/visitas   (el servidor recalcula la distancia)
Supervisor aprueba o rechaza ................... PUT    /api/visitas/{id}/validar
Cliente cancela una solicitud pendiente ........ DELETE /api/solicitudes/{id}
```

**Requisitos técnicos cubiertos:**
- **Formularios:** validados con `isError` y `supportingText`.
- **Navegación:** `NavHost` + `sealed class Rutas`, con TopAppBar, NavigationDrawer y NavigationBar.
- **Estado y animaciones:** `StateFlow`, `AnimatedVisibility`, `animateColorAsState` y transiciones; las listas usan grillas adaptativas.
- **Persistencia local:** **DataStore** guarda la sesión y **Room** guarda las visitas sin conexión.
- **Recursos nativos:** cámara (`TakePicture` + `FileProvider`), galería y GPS (`FusedLocationProvider`), con detección de GPS falso.
- **API externa:** **Open-Meteo** vía Retrofit, separada del backend propio. Muestra la temperatura del local y alerta si es ≥ 30 °C.

## 4. Endpoints utilizados

**Backend propio en AWS:** `http://52.71.145.213:8080` (EC2 con IP elástica fija)

| Recurso | Endpoints |
|---|---|
| Auth | `POST /api/auth/login` · `POST /api/auth/registro` |
| Usuarios | `GET /api/usuarios?rol=` · `GET/PUT/DELETE /api/usuarios/{id}` · `POST /api/usuarios` |
| Puntos de venta | `GET /api/puntos-venta?clienteId=` · `POST` · `GET/PUT/DELETE /api/puntos-venta/{id}` |
| Equipos | `GET /api/equipos?puntoVentaId=&enBodega=` · `POST` · `GET/PUT/DELETE /api/equipos/{id}` · `GET /api/equipos/{id}/historial` |
| Solicitudes | `GET /api/solicitudes?clienteId=&operarioId=&estado=` · `POST` · `GET/PUT/DELETE /api/solicitudes/{id}` · `PUT /api/solicitudes/{id}/asignar` |
| Visitas | `GET /api/visitas?operarioId=&estado=` · `POST` · `GET /api/visitas/{id}` · `PUT /api/visitas/{id}/validar` |

**API externa:**
`GET https://api.open-meteo.com/v1/forecast?latitude=..&longitude=..&current=temperature_2m,relative_humidity_2m,weather_code`

## 5. Pasos para ejecutar

### Backend (AWS)
El backend ya está desplegado y **arranca solo** al iniciar el laboratorio de AWS. Los detalles de la instalación están en [docs/DESPLIEGUE_AWS.md](docs/DESPLIEGUE_AWS.md).

Para comprobar que responde: `http://52.71.145.213:8080/api/usuarios?rol=OPERARIO`

Para compilarlo localmente (requiere Java 17):
```bash
cd tacticaldev-backend
mvn clean package          # -> target/tacticaldev-backend.jar
```

### App Android
1. Abre `AppTacticalDev_GrupoX/` en Android Studio y espera el *Gradle Sync*.
2. La URL del backend está en `gradle.properties`: `API_USUARIOS_URL` y `API_OPERACIONES_URL` = `http://52.71.145.213:8080/`.
3. Presiona **Run ▶**, o instala el APK firmado de la carpeta `apk/`.

Desde la terminal hay que usar Java 17: `export JAVA_HOME=$(/usr/libexec/java_home -v 17)`.

### Pruebas unitarias (cobertura ≥ 80 %)
```bash
cd AppTacticalDev_GrupoX
./gradlew testDebugUnitTest koverHtmlReportDebug koverVerifyDebug
# reporte: app/build/reports/kover/htmlDebug/index.html
```
- **Herramientas:** Kotest, MockK, kotlinx-coroutines-test y MockWebServer.
- **Qué cubren:** ViewModels, Repositories, utilidades, navegación, modelos e interfaces Retrofit.
- **Contrato con el backend:** `ContratoBackendTest` verifica los 26 endpoints contra el mismo JSON que devuelve el backend en AWS.
- **Regla de verificación:** `koverVerifyDebug` exige **≥ 80 % en líneas, ramas e instrucciones** del proyecto completo. Solo se excluyen la interfaz Compose (probada con Compose UI Test), las clases que necesitan un dispositivo y el código generado.

**Resultados obtenidos** (`./gradlew testDebugUnitTest koverVerifyDebug` → `BUILD SUCCESSFUL`):

| Pruebas unitarias | Fallas | Éxito |
|---|---|---|
| **142** | **0** | **100 %** |

| Cobertura | Clases | Métodos | Ramas | Líneas | Instrucciones |
|---|---|---|---|---|---|
| **Proyecto completo** | **100 %** (155/155) | **99 %** (311/314) | **98 %** (581/593) | **100 %** (1133/1133) | **99,9 %** (11694/11704) |

Reportes completos (abrir `index.html` en el navegador):
- [docs/reportes/pruebas-unitarias/index.html](docs/reportes/pruebas-unitarias/index.html)
- [docs/reportes/cobertura-kover/index.html](docs/reportes/cobertura-kover/index.html)

### Cuentas de prueba (datos ficticios)
| Rol | Correo | Contraseña |
|---|---|---|
| Supervisor | supervisor@tacticaldev.cl | Super1234 |
| Técnico | tecnico@tacticaldev.cl | Opera1234 |
| Vendedor | vendedor@tacticaldev.cl | Opera1234 |
| Transportista | transportista@tacticaldev.cl | Opera1234 |
| Cliente | cliente@minimarket.cl | Cliente1234 |

## 6. APK firmado y llave
- **APK:** [`apk/app-release.apk`](apk/) (versión 1.1.0)
- **Llave:** `AppTacticalDev_GrupoX/keystore/tacticaldev-release.jks` (alias `tacticaldev`, a nombre de *CN=Emilio Nuñez, OU=DSY1105 Grupo X, O=Duoc UC*, válida hasta 2051)
- **Configuración:** `signingConfigs { create("release") }` en `AppTacticalDev_GrupoX/app/build.gradle.kts`, que lee `keystore.properties`
- **Cómo se genera:** `./gradlew assembleRelease`
- **Cómo se verifica:** `apksigner verify --print-certs apk/app-release.apk` → `Verified using v2 scheme: true`

Resultado de la verificación:
```
Verified using v2 scheme (APK Signature Scheme v2): true
Signer #1 certificate DN: CN=Emilio Nuñez, OU=DSY1105 Grupo X, O=Duoc UC, L=Santiago, ST=RM, C=CL
```

## Planificación
- Tablero GitHub proyects(Trello) : https://github.com/users/EmNunnez/projects/1/views/1
- Repositorio GitHub: https://github.com/EmNunnez/TacticalDEV_GrupoX

## Estructura del repositorio
```
TacticalDEV_GrupoX/
├── AppTacticalDev_GrupoX/          # App Android (MVVM)
│   └── app/src/main/java/cl/duoc/tacticaldev/
│       ├── data/ (model, remote, local)   # DTOs, Retrofit, Room, DataStore
│       ├── repository/                    # acceso a datos (offline first)
│       ├── viewmodel/                     # lógica de presentación (StateFlow)
│       ├── ui/ (screens, components, navigation, theme)
│       └── util/                          # validaciones, GPS, Haversine
├── tacticaldev-backend/            # Spring Boot: controller → service → repository → MariaDB
├── apk/                            # APK release firmado
└── docs/                           # despliegue AWS, reportes de pruebas y capturas
```
