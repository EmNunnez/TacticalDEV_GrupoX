package cl.duoc.tacticaldev.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface VisitaPendienteDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertar(visita: VisitaPendienteEntity)

    @Query("SELECT * FROM visitas_pendientes WHERE operarioId = :operarioId ORDER BY creadoEn ASC")
    fun observar(operarioId: Long): Flow<List<VisitaPendienteEntity>>

    @Query("SELECT * FROM visitas_pendientes WHERE operarioId = :operarioId ORDER BY creadoEn ASC")
    suspend fun obtenerTodas(operarioId: Long): List<VisitaPendienteEntity>

    @Query("SELECT solicitudId FROM visitas_pendientes WHERE operarioId = :operarioId")
    suspend fun solicitudesConPendiente(operarioId: Long): List<Long>

    @Query("UPDATE visitas_pendientes SET ultimoError = :error WHERE idLocal = :idLocal")
    suspend fun registrarError(idLocal: String, error: String?)

    @Query("DELETE FROM visitas_pendientes WHERE idLocal = :idLocal")
    suspend fun eliminar(idLocal: String)
}
