package cl.duoc.tacticaldev.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "solicitudes")
@Getter
@Setter
@NoArgsConstructor
public class Solicitud {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private TipoSolicitud tipo;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private EstadoSolicitud estado = EstadoSolicitud.PENDIENTE;

    @Column(nullable = false, length = 500)
    private String descripcion;

    /** Solo para EQUIPO_NUEVO: qué tipo de equipo pide el cliente. */
    @Enumerated(EnumType.STRING)
    @Column(name = "tipo_equipo_solicitado", length = 20)
    private TipoEquipo tipoEquipoSolicitado;

    /** Foto opcional que adjunta el cliente (ej. la falla). JPEG en Base64. */
    @Lob
    @Column(name = "foto_cliente_base64", columnDefinition = "LONGTEXT")
    private String fotoClienteBase64;

    @Column(name = "cliente_id", nullable = false)
    private Long clienteId;

    @Column(name = "cliente_nombre", length = 80)
    private String clienteNombre;

    @ManyToOne(optional = false)
    @JoinColumn(name = "punto_venta_id")
    private PuntoVenta puntoVenta;

    /** Null en EQUIPO_NUEVO hasta que el supervisor asigna uno del stock. */
    @ManyToOne
    @JoinColumn(name = "equipo_id")
    private Equipo equipo;

    @Column(name = "operario_id")
    private Long operarioId;

    @Column(name = "operario_nombre", length = 80)
    private String operarioNombre;

    @Column(name = "fecha_creacion", nullable = false)
    private LocalDateTime fechaCreacion = LocalDateTime.now();

    @Column(name = "fecha_asignacion")
    private LocalDateTime fechaAsignacion;

    @Column(name = "fecha_cierre")
    private LocalDateTime fechaCierre;
}
