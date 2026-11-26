package cl.duoc.tacticaldev.config;

import cl.duoc.tacticaldev.model.*;
import cl.duoc.tacticaldev.repository.EquipoRepository;
import cl.duoc.tacticaldev.repository.PuntoVentaRepository;
import cl.duoc.tacticaldev.repository.SolicitudRepository;
import cl.duoc.tacticaldev.repository.UsuarioRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Component;

/**
 * Datos FICTICIOS de demostración (el caso prohíbe datos reales).
 * Se insertan solo si la base está vacía.
 *
 * Punto 1: coordenadas aproximadas de la sede Duoc UC Plaza Oeste (defensa con celular físico).
 * Punto 3: ubicación por defecto del emulador de Android (Googleplex), para que la
 *          verificación GPS funcione en el emulador sin configurar nada.
 */
@Component
public class DataLoader implements CommandLineRunner {

    private final UsuarioRepository usuarioRepo;
    private final PuntoVentaRepository puntoRepo;
    private final EquipoRepository equipoRepo;
    private final SolicitudRepository solicitudRepo;
    private final BCryptPasswordEncoder encoder;

    public DataLoader(UsuarioRepository usuarioRepo, PuntoVentaRepository puntoRepo, EquipoRepository equipoRepo,
                      SolicitudRepository solicitudRepo, BCryptPasswordEncoder encoder) {
        this.usuarioRepo = usuarioRepo;
        this.puntoRepo = puntoRepo;
        this.equipoRepo = equipoRepo;
        this.solicitudRepo = solicitudRepo;
        this.encoder = encoder;
    }

    @Override
    public void run(String... args) {
        if (usuarioRepo.count() > 0) return;

        // ---------- Usuarios (3 roles) ----------
        usuario("Sofía Supervisora", "supervisor@tacticaldev.cl", "Super1234", Rol.SUPERVISOR, null);
        usuario("Tomás Técnico", "tecnico@tacticaldev.cl", "Opera1234", Rol.OPERARIO, TipoOperario.TECNICO);
        usuario("Valentina Vendedora", "vendedor@tacticaldev.cl", "Opera1234", Rol.OPERARIO, TipoOperario.VENDEDOR);
        usuario("Tito Transportista", "transportista@tacticaldev.cl", "Opera1234", Rol.OPERARIO, TipoOperario.TRANSPORTISTA);
        Usuario donPepe = usuario("Minimarket Don Pepe", "cliente@minimarket.cl", "Cliente1234", Rol.CLIENTE, null);
        Usuario laEsquina = usuario("Almacén La Esquina", "cliente2@minimarket.cl", "Cliente1234", Rol.CLIENTE, null);

        // ---------- Puntos de venta ----------
        PuntoVenta p1 = punto("Minimarket Don Pepe - Sede", "Av. Ficticia 1501", "Cerrillos", -33.5167, -70.7160, 400, donPepe.getId());
        PuntoVenta p2 = punto("Minimarket Don Pepe - Villa", "Pasaje Inventado 742", "Maipú", -33.5110, -70.7580, 100, donPepe.getId());
        PuntoVenta p3 = punto("Almacén La Esquina (Emulador)", "Calle Demo 1600", "Mountain View", 37.4220, -122.0841, 300, laEsquina.getId());

        // ---------- Equipos ----------
        equipo("VF-0001", TipoEquipo.VITRINA_FREEZER, "FrioMax", "VF-300", EstadoEquipo.OPERATIVO, p1);
        Equipo e2 = equipo("CN-0002", TipoEquipo.CONSERVADORA, "PolarCool", "CN-120", EstadoEquipo.CON_FALLA, p1);
        equipo("VF-0003", TipoEquipo.VITRINA_FREEZER, "FrioMax", "VF-450", EstadoEquipo.OPERATIVO, p2);
        equipo("VF-0004", TipoEquipo.VITRINA_FREEZER, "FrioMax", "VF-300", EstadoEquipo.OPERATIVO, null); // bodega
        equipo("CN-0005", TipoEquipo.CONSERVADORA, "PolarCool", "CN-200", EstadoEquipo.OPERATIVO, null);   // bodega
        Equipo e6 = equipo("VF-0006", TipoEquipo.VITRINA_FREEZER, "IceLine", "IL-500", EstadoEquipo.OPERATIVO, p3);

        // ---------- Solicitudes de ejemplo ----------
        solicitud(TipoSolicitud.REPARACION, "La conservadora no enfría y hace un ruido fuerte al encender", donPepe, p1, e2);
        solicitud(TipoSolicitud.REVISION, "Revisión periódica programada de la vitrina", laEsquina, p3, e6);
    }

    private Usuario usuario(String nombre, String email, String pass, Rol rol, TipoOperario tipo) {
        Usuario u = new Usuario();
        u.setNombre(nombre);
        u.setEmail(email);
        u.setPasswordHash(encoder.encode(pass));
        u.setTelefono("+56900000000");
        u.setRol(rol);
        u.setTipoOperario(tipo);
        return usuarioRepo.save(u);
    }

    private PuntoVenta punto(String nombre, String dir, String comuna, double lat, double lon, int radio, Long clienteId) {
        PuntoVenta p = new PuntoVenta();
        p.setNombre(nombre);
        p.setDireccion(dir);
        p.setComuna(comuna);
        p.setLatitud(lat);
        p.setLongitud(lon);
        p.setRadioMetros(radio);
        p.setClienteId(clienteId);
        return puntoRepo.save(p);
    }

    private Equipo equipo(String codigo, TipoEquipo tipo, String marca, String modelo, EstadoEquipo estado, PuntoVenta pv) {
        Equipo e = new Equipo();
        e.setCodigo(codigo);
        e.setTipo(tipo);
        e.setMarca(marca);
        e.setModelo(modelo);
        e.setEstado(estado);
        e.setPuntoVenta(pv);
        return equipoRepo.save(e);
    }

    private void solicitud(TipoSolicitud tipo, String descripcion, Usuario cliente, PuntoVenta pv, Equipo equipo) {
        Solicitud s = new Solicitud();
        s.setTipo(tipo);
        s.setDescripcion(descripcion);
        s.setClienteId(cliente.getId());
        s.setClienteNombre(cliente.getNombre());
        s.setPuntoVenta(pv);
        s.setEquipo(equipo);
        solicitudRepo.save(s);
    }
}
