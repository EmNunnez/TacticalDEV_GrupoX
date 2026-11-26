package cl.duoc.tacticaldev.util

/**
 * Reglas de validación de formularios. Cada función devuelve null si el valor es válido
 * o el mensaje de error que se muestra en el supportingText del campo.
 * Son las mismas reglas que aplican los microservicios.
 */
object Validaciones {

    private val REGEX_EMAIL = Regex("^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$")
    private val REGEX_TELEFONO = Regex("^\\+?[0-9]{8,12}$")
    private val REGEX_CODIGO_EQUIPO = Regex("^[A-Za-z]{2}-\\d{4}$")

    fun nombre(valor: String): String? = when {
        valor.isBlank() -> "El nombre es obligatorio"
        valor.trim().length < 3 -> "Debe tener al menos 3 caracteres"
        valor.trim().length > 80 -> "Máximo 80 caracteres"
        else -> null
    }

    fun email(valor: String): String? = when {
        valor.isBlank() -> "El correo es obligatorio"
        !REGEX_EMAIL.matches(valor.trim()) -> "Formato de correo inválido"
        else -> null
    }

    fun password(valor: String): String? = when {
        valor.isBlank() -> "La contraseña es obligatoria"
        valor.length < 8 -> "Debe tener al menos 8 caracteres"
        !valor.any { it.isLetter() } || !valor.any { it.isDigit() } -> "Debe incluir letras y números"
        else -> null
    }

    /** En edición la contraseña es opcional: vacía = se mantiene la actual. */
    fun passwordOpcional(valor: String): String? = if (valor.isEmpty()) null else password(valor)

    fun confirmarPassword(password: String, confirmacion: String): String? = when {
        confirmacion.isBlank() -> "Confirme la contraseña"
        password != confirmacion -> "Las contraseñas no coinciden"
        else -> null
    }

    /** Opcional; si se ingresa debe tener 8 a 12 dígitos. */
    fun telefono(valor: String): String? =
        if (valor.isBlank() || REGEX_TELEFONO.matches(valor.trim())) null else "Teléfono inválido (8 a 12 dígitos)"

    fun obligatorio(valor: String, campo: String): String? =
        if (valor.isBlank()) "$campo es obligatorio" else null

    fun descripcion(valor: String): String? = when {
        valor.isBlank() -> "La descripción es obligatoria"
        valor.trim().length < 10 -> "Describa con al menos 10 caracteres"
        valor.trim().length > 500 -> "Máximo 500 caracteres"
        else -> null
    }

    fun latitud(valor: String): String? {
        val n = valor.replace(',', '.').toDoubleOrNull() ?: return "Latitud inválida"
        return if (n in -90.0..90.0) null else "Debe estar entre -90 y 90"
    }

    fun longitud(valor: String): String? {
        val n = valor.replace(',', '.').toDoubleOrNull() ?: return "Longitud inválida"
        return if (n in -180.0..180.0) null else "Debe estar entre -180 y 180"
    }

    fun radio(valor: String): String? {
        val n = valor.toIntOrNull() ?: return "Ingrese un número entero"
        return if (n in 20..1000) null else "Debe estar entre 20 y 1000 metros"
    }

    fun codigoEquipo(valor: String): String? = when {
        valor.isBlank() -> "El código es obligatorio"
        !REGEX_CODIGO_EQUIPO.matches(valor.trim()) -> "Formato XX-0000 (ej. VF-0001)"
        else -> null
    }

    /** Si el equipo quedó con falla, la observación es obligatoria (trazabilidad de la falla). */
    fun observacionVisita(valor: String, equipoConFalla: Boolean): String? = when {
        valor.trim().length > 500 -> "Máximo 500 caracteres"
        equipoConFalla && valor.trim().length < 10 -> "Describa la falla (mínimo 10 caracteres)"
        else -> null
    }
}
