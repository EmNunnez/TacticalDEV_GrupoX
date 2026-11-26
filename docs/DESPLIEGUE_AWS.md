# Despliegue del backend en AWS (EC2)

Así quedó instalado el backend `tacticaldev-backend` en la nube.

| Elemento | Valor |
|---|---|
| Servicio | AWS EC2 (AWS Academy, región us-east-1) |
| Sistema operativo | Amazon Linux 2023 |
| IP pública fija (Elastic IP) | **52.71.145.213** |
| Java | Amazon Corretto 17 |
| Base de datos | MariaDB 10.5 (compatible con MySQL), en la misma instancia |
| Puerto del backend | 8080 |
| Arranque automático | Servicio `systemd` llamado `tacticaldev` |

## 1. Instalar Java 17 y MariaDB
```bash
sudo dnf install -y java-17-amazon-corretto-headless mariadb105-server
sudo systemctl enable --now mariadb
```

## 2. Crear el usuario de la base de datos
```bash
sudo mysql <<'SQL'
CREATE USER IF NOT EXISTS 'tactical'@'localhost' IDENTIFIED BY '<CLAVE>';
GRANT ALL PRIVILEGES ON *.* TO 'tactical'@'localhost';
FLUSH PRIVILEGES;
SQL
```
La base `tacticaldev` y sus tablas se crean solas al arrancar el backend (`createDatabaseIfNotExist` + `ddl-auto: update`). Los datos ficticios de prueba también se cargan solos.

## 3. Compilar y subir el .jar
En el PC, con Java 17:
```bash
cd tacticaldev-backend
mvn clean package
scp -i <llave>.pem target/tacticaldev-backend.jar ec2-user@52.71.145.213:~
```

## 4. Servicio systemd (arranca solo al encender la instancia)
```bash
sudo tee /etc/systemd/system/tacticaldev.service > /dev/null <<'UNIT'
[Unit]
Description=Backend TacticalDEV (Spring Boot)
After=network.target mariadb.service
Requires=mariadb.service

[Service]
User=ec2-user
Environment="DB_USER=tactical"
Environment="DB_PASS=<CLAVE>"
ExecStart=/usr/bin/java -Xmx400m -jar /home/ec2-user/tacticaldev-backend.jar
Restart=always
RestartSec=5

[Install]
WantedBy=multi-user.target
UNIT

sudo systemctl daemon-reload
sudo systemctl enable --now tacticaldev
sudo systemctl status tacticaldev       # Active: active (running)
sudo journalctl -u tacticaldev -f       # ver logs
```

## 5. Red
- **Security Group:** puertos **22** (SSH) y **8080** (backend) abiertos.
- **Elastic IP:** `52.71.145.213`, asociada a la instancia para que la IP no cambie al reiniciar el laboratorio.

## 6. Mostrar los datos de la base (defensa)
Se instaló el comando `bd` en `/usr/local/bin/bd`:

| Comando | Muestra |
|---|---|
| `bd resumen` | Cantidad de registros por tabla |
| `bd tablas` | Tablas de la base de datos |
| `bd usuarios` | Usuarios y roles (sin contraseñas) |
| `bd puntos` | Puntos de venta con GPS y radio |
| `bd equipos` | Equipos y su ubicación |
| `bd solicitudes` | Solicitudes con su estado |
| `bd visitas` | Visitas: GPS, distancia, verificación, temperatura y validación |
| `bd visitas 3` | Detalle de un registro por id |
| `bd estructura equipos` | Columnas de una tabla |

## 7. Entrar a la instancia desde cualquier PC
Consola de AWS → **EC2 → Instancias** → selecciona la instancia → **Conectar** → **EC2 Instance Connect** → **Conectar**.
No necesita la llave `.pem`.
