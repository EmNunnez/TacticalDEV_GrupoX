# tacticaldev-backend (Spring Boot + MySQL/MariaDB)

Un solo microservicio, puerto **8080**, base de datos **tacticaldev** (se crea sola).

## Estructura
```
src/main/java/cl/duoc/tacticaldev/
├── TacticalDevApplication.java   # clase principal
├── controller/   # endpoints REST (/api/...)
├── service/      # reglas de negocio
├── repository/   # acceso a la BD (Spring Data JPA)
├── model/        # entidades JPA y enums
├── dto/          # objetos de entrada/salida
├── exception/    # manejo de errores {"mensaje": "..."}
└── config/       # BCrypt y datos de prueba
src/main/resources/application.yml
```

## Empaquetar (IntelliJ)
Panel **Maven → Lifecycle → clean** y luego **package** → `target/tacticaldev-backend.jar`

## Ejecutar
```bash
DB_USER=tactical DB_PASS='***' java -jar tacticaldev-backend.jar
```

## Endpoints
| Recurso | Endpoints |
|---|---|
| Auth | `POST /api/auth/login` · `POST /api/auth/registro` |
| Usuarios | `GET/POST /api/usuarios` · `GET/PUT/DELETE /api/usuarios/{id}` |
| Puntos de venta | `GET/POST /api/puntos-venta` · `GET/PUT/DELETE /api/puntos-venta/{id}` |
| Equipos | `GET/POST /api/equipos` · `GET/PUT/DELETE /api/equipos/{id}` · `GET /api/equipos/{id}/historial` |
| Solicitudes | `GET/POST /api/solicitudes` · `GET/PUT/DELETE /api/solicitudes/{id}` · `PUT /api/solicitudes/{id}/asignar` |
| Visitas | `GET/POST /api/visitas` · `GET /api/visitas/{id}` · `PUT /api/visitas/{id}/validar` |

## Usuarios de prueba (ficticios)
supervisor@tacticaldev.cl / Super1234 · tecnico@tacticaldev.cl / Opera1234 · cliente@minimarket.cl / Cliente1234
