package cl.duoc.tacticaldev

import kotlin.reflect.KClass
import kotlin.reflect.KType
import kotlin.reflect.full.instanceParameter
import kotlin.reflect.full.memberFunctions
import kotlin.reflect.full.memberProperties
import kotlin.reflect.full.primaryConstructor
import kotlin.reflect.jvm.isAccessible

/**
 * Verifica el "contrato" de los modelos de la app usando reflexión:
 *
 *  - data class: igualdad por valor (equals/hashCode), copy(), componentN(), getters y toString().
 *    Para cada propiedad crea una copia con ese único valor cambiado y comprueba que ya NO es igual.
 *  - data object: equals/hashCode/toString.
 *  - enum: todos sus valores, valueOf() y la propiedad "etiqueta" que muestra la interfaz.
 *
 * Así se recorren también los métodos que Kotlin genera automáticamente, que cuentan
 * en la cobertura de clases, métodos y ramas.
 */
object ContratoModelos {

    // ---------------------------------------------------------------- data class

    fun verificarDataClass(clase: KClass<*>) {
        require(clase.isData) { "${clase.simpleName} no es data class" }
        val a = crear(clase, 0)
        val igual = crear(clase, 0)

        check(a == a) { "${clase.simpleName}: no es igual a sí misma" }
        check(a == igual) { "${clase.simpleName}: dos instancias con los mismos valores no son iguales" }
        check(a.hashCode() == igual.hashCode()) { "${clase.simpleName}: hashCode distinto con los mismos valores" }
        check(!a.equals(null)) { "${clase.simpleName}: igual a null" }
        check(a != Any()) { "${clase.simpleName}: igual a otro tipo" }
        check(a.toString().startsWith(clase.simpleName!!)) { "${clase.simpleName}: toString inesperado" }

        // Getters (incluye propiedades calculadas como "puedeEnviar" o "hayErrores")
        clase.memberProperties.forEach { p ->
            p.isAccessible = true
            p.getter.call(a)
        }
        // component1(), component2(), ...
        clase.memberFunctions
            .filter { it.name.matches(Regex("component\\d+")) }
            .forEach { it.call(a) }

        // copy() sin argumentos y con cada propiedad cambiada
        val copy = clase.memberFunctions.first { it.name == "copy" }
        val instancia = copy.instanceParameter!!
        check(copy.callBy(mapOf(instancia to a)) == a) { "${clase.simpleName}: copy() sin cambios no es igual" }
        copy.parameters.filter { it != instancia }.forEach { param ->
            val otroValor = valor(param.type, 1, 0)
            val cambiada = copy.callBy(mapOf(instancia to a, param to otroValor))!!
            check(cambiada != a) { "${clase.simpleName}.${param.name}: cambiar el valor no altera equals" }
            cambiada.hashCode()
            if (param.type.isMarkedNullable) {
                val conNull = copy.callBy(mapOf(instancia to a, param to null))!!
                check(conNull != a) { "${clase.simpleName}.${param.name}: null no altera equals" }
                conNull.hashCode()
                conNull.toString()
            }
        }
    }

    // ---------------------------------------------------------------- data object

    fun verificarObjeto(objeto: Any) {
        check(objeto == objeto)
        check(!objeto.equals(null))
        check(objeto != Any())
        objeto.hashCode()
        check(objeto.toString().isNotBlank())
    }

    // ---------------------------------------------------------------- enum

    fun verificarEnum(clase: KClass<out Enum<*>>) {
        val constantes = clase.java.enumConstants
        check(constantes.size >= 2) { "${clase.simpleName}: se esperaban al menos 2 valores" }
        val valueOf = clase.java.getMethod("valueOf", String::class.java)
        clase.java.getMethod("values").invoke(null)
        runCatching { clase.java.getMethod("getEntries").invoke(null) }
        val etiqueta = clase.memberProperties.firstOrNull { it.name == "etiqueta" }
        constantes.forEach { c ->
            check(valueOf.invoke(null, c.name) == c)
            if (etiqueta != null) {
                val texto = etiqueta.getter.call(c) as String
                check(texto.isNotBlank()) { "${clase.simpleName}.${c.name}: etiqueta vacía" }
            }
        }
    }

    // ---------------------------------------------------------------- generación de valores

    /** Crea una instancia con valores de prueba. variante 0 y 1 producen objetos distintos. */
    fun crear(clase: KClass<*>, variante: Int, profundidad: Int = 0): Any {
        check(profundidad < 8) { "Modelo demasiado anidado: ${clase.simpleName}" }
        clase.objectInstance?.let { return it }
        if (clase.java.isEnum) {
            val c = clase.java.enumConstants
            return c[variante % c.size]
        }
        if (clase.isSealed || clase.isAbstract) {
            // Para una clase sellada se usa la primera subclase que tenga constructor
            val sub = clase.sealedSubclasses.firstOrNull { it.objectInstance == null && it.primaryConstructor != null }
                ?: clase.sealedSubclasses.first()
            return crear(sub, variante, profundidad + 1)
        }
        val constructor = clase.primaryConstructor ?: error("${clase.simpleName} no tiene constructor primario")
        constructor.isAccessible = true
        val args = constructor.parameters.associateWith { valor(it.type, variante, profundidad + 1) }
        return constructor.callBy(args)
    }

    fun valor(tipo: KType, variante: Int, profundidad: Int): Any? {
        val clase = tipo.classifier as? KClass<*> ?: error("Tipo no soportado: $tipo")
        return when (clase) {
            String::class -> "valor$variante"
            Long::class -> variante + 1L
            Int::class -> variante + 1
            Double::class -> variante + 1.5
            Float::class -> variante + 1.5f
            Boolean::class -> variante % 2 == 0
            List::class -> List(variante) { "elemento$it" }
            Set::class -> (0 until variante).map { it.toLong() }.toSet()
            Map::class -> (0 until variante).associate { "clave$it" to "valor$it" }
            else -> crear(clase, variante, profundidad)
        }
    }
}
