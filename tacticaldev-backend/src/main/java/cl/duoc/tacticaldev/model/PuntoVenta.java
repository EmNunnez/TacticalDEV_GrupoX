package cl.duoc.tacticaldev.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "puntos_venta")
@Getter
@Setter
@NoArgsConstructor
public class PuntoVenta {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 100)
    private String nombre;

    @Column(nullable = false, length = 150)
    private String direccion;

    @Column(nullable = false, length = 60)
    private String comuna;

    @Column(nullable = false)
    private Double latitud;

    @Column(nullable = false)
    private Double longitud;

    /** Distancia máxima (metros) a la que debe estar el operario al tomar la foto. */
    @Column(name = "radio_metros", nullable = false)
    private Integer radioMetros = 100;

    /** Id del usuario CLIENTE dueño del local. */
    @Column(name = "cliente_id", nullable = false)
    private Long clienteId;
}
