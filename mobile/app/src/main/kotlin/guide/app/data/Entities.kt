package guide.app.data

import androidx.room.Dao
import androidx.room.Database
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.RoomDatabase

/** Room mirror of shared-core Models.kt (SPEC §4.3). */
@Entity(tableName = "visits")
data class VisitEntity(
    @PrimaryKey(autoGenerate = true) val uid: Int = 0,
    val poiId: String,
    val arrivedAt: Long,
    val leftAt: Long?,
    val packVersion: String,
)

@Entity(tableName = "notes")
data class NoteEntity(
    @PrimaryKey val poiId: String,
    val text: String,
    val updatedAt: Long,
)

@Dao
interface GuideDao {
    @Insert suspend fun insertVisit(v: VisitEntity)
    @Query("SELECT * FROM visits ORDER BY arrivedAt DESC") suspend fun visits(): List<VisitEntity>
    @Insert suspend fun upsertNote(n: NoteEntity)
    @Query("SELECT * FROM notes WHERE poiId = :poiId") suspend fun note(poiId: String): NoteEntity?
}

@Database(entities = [VisitEntity::class, NoteEntity::class], version = 1)
abstract class GuideDb : RoomDatabase() {
    abstract fun dao(): GuideDao
}
