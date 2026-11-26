package cl.duoc.tacticaldev.service;

import cl.duoc.tacticaldev.model.TipoSolicitud;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

class VerificacionServiceTest {

    private final VerificacionService v = new VerificacionService();

    @Test
    void distanciaEntreMismoPuntoEsCero() {
        assertEquals(0.0, v.distanciaMetros(-33.5, -70.7, -33.5, -70.7), 0.01);
    }

    @Test
    void distanciaConocidaAproximadaUnKilometro() {
        // 0.009 grados de latitud ~ 1 km
        double d = v.distanciaMetros(-33.5000, -70.7000, -33.5090, -70.7000);
        assertTrue(d > 990 && d < 1010, "distancia fue " + d);
    }

    @Test
    void dentroDelRadioQuedaVerificada() {
        var r = v.verificar(-33.5167, -70.7160, 15.0, false, -33.5168, -70.7161, 100);
        assertTrue(r.verificada());
    }

    @Test
    void fueraDelRadioQuedaSospechosa() {
        var r = v.verificar(-33.5000, -70.7000, 10.0, false, -33.5167, -70.7160, 100);
        assertFalse(r.verificada());
        assertTrue(r.motivo().contains("del punto de venta"));
    }

    @Test
    void gpsSimuladoNuncaSeVerifica() {
        var r = v.verificar(-33.5167, -70.7160, 5.0, true, -33.5167, -70.7160, 100);
        assertFalse(r.verificada());
    }

    @Test
    void precisionMalaNoSeVerifica() {
        var r = v.verificar(-33.5167, -70.7160, 500.0, false, -33.5167, -70.7160, 100);
        assertFalse(r.verificada());
    }

    @Test
    void fechaCapturaEnFuturoORantiguaSeRechaza() {
        LocalDateTime ahora = LocalDateTime.of(2026, 11, 18, 12, 0);
        assertNull(v.validarFechaCaptura(ahora.minusMinutes(1), ahora));
        assertNotNull(v.validarFechaCaptura(ahora.plusHours(1), ahora));
        assertNotNull(v.validarFechaCaptura(ahora.minusDays(4), ahora));
    }

    @Test
    void compatibilidadOperarioSegunTipoSolicitud() {
        assertTrue(v.operarioCompatible(TipoSolicitud.REPARACION, "TECNICO"));
        assertFalse(v.operarioCompatible(TipoSolicitud.REPARACION, "VENDEDOR"));
        assertTrue(v.operarioCompatible(TipoSolicitud.EQUIPO_NUEVO, "TRANSPORTISTA"));
        assertFalse(v.operarioCompatible(TipoSolicitud.EQUIPO_NUEVO, "TECNICO"));
        assertTrue(v.operarioCompatible(TipoSolicitud.REVISION, "VENDEDOR"));
        assertFalse(v.operarioCompatible(TipoSolicitud.RETIRO, null));
    }
}
