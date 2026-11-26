package cl.duoc.tacticaldev.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * Registro de la visita en terreno. La foto y el GPS se capturan en el mismo instante
 * en la app y el servidor vuelve a verificar la distancia al punto de venta al recibirla.
 */
@Entity
@Table(name = "visitas")
@Getter
@Setter
@NoArgsConstructor
public class Visita {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** UUID generado en el teléfono: evita duplicados al sincronizar sin conexión. */
    @Column(name = "id_local", nullable = false, unique = true, length = 40)
    private String idLocal;

    @ManyToOne(optional = false)
    @JoinColumn(name = "solicitud_id")
    private Solicitud solicitud;

    @Column(name = "operario_id", nullable = false)
    private Long operarioId;

    @Column(name = "operario_nombre", length = 80)
    private String operarioNombre;

    @Enumerated(EnumType.STRING)
    @Column(name = "estado_equipo", nullable = false, length = 20)
    private EstadoEquipo estadoEquipo;

    @Column(length = 500)
    private String observaciones;

    @Lob
    @Column(name = "foto_base64", nullable = false, columnDefinition = "LONGTEXT")
    private String fotoBase64;

    // ---- Evidencia GPS ligada a la foto ----
    @Column(nullable = false)
    private Double latitud;

    @Column(nullable = false)
    private Double longitud;

    @Column(name = "precision_metros")
    private Double precisionMetros;

    @Column(name = "ubicacion_simulada", nullable = false)
    private boolean ubicacionSimulada;

    /** Momento exacto en que se tomó la foto + GPS (reloj del teléfono). */
    @Column(name = "fecha_captura", nullable = false)
    private LocalDateTime fechaCaptura;

    /** Momento en que llegó al servidor (no manipulable por el operario). */
    @Column(name = "fecha_registro", nullable = false)
    private LocalDateTime fechaRegistro = LocalDateTime.now();

    @Column(name = "distancia_metros", nullable = false)
    private Double distanciaMetros;

    @Column(name = "ubicacion_verificada", nullable = false)
    private boolean ubicacionVerificada;

    @Column(name = "motivo_verificacion", length = 200)
    private String motivoVerificacion;

    /** Dato de la API externa Open-Meteo al momento de la visita. */
    @Column(name = "temperatura_ambiente")
    private Double temperaturaAmbiente;

    @Enumerated(EnumType.STRING)
    @Column(name = "estado_validacion", nullable = false, length = 20)
    private EstadoValidacion estadoValidacion = EstadoValidacion.PENDIENTE;

    @Column(name = "comentario_supervisor", length = 300)
    private String comentarioSupervisor;

    @Column(name = "fecha_validacion")
    private LocalDateTime fechaValidacion;
}
