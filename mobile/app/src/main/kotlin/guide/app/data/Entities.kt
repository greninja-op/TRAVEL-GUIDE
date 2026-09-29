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
    val photoUri: String? = null,
    val updatedAt: Long,
)

@Dao
interface GuideDao {
    @Insert suspend fun insertVisit(v: VisitEntity)
    @Query("SELECT * FROM visits ORDER BY arrivedAt DESC") suspend fun visits(): List<VisitEntity>
    @androidx.room.Insert(onConflict = androidx.room.OnConflictStrategy.REPLACE) suspend fun upsertNote(n: NoteEntity)
    @Query("SELECT * FROM notes WHERE poiId = :poiId") suspend fun note(poiId: String): NoteEntity?
    @Query("DELETE FROM notes WHERE poiId = :poiId") suspend fun deleteNote(poiId: String)
    @Query("SELECT * FROM notes ORDER BY updatedAt DESC") suspend fun allNotes(): List<NoteEntity>
}

@Database(entities = [VisitEntity::class, NoteEntity::class], version = 2)
abstract class GuideDb : RoomDatabase() {
    abstract fun dao(): GuideDao

    companion object {
        @Volatile
        private var INSTANCE: GuideDb? = null

        fun getInstance(context: android.content.Context): GuideDb =
            INSTANCE ?: synchronized(this) {
                INSTANCE ?: androidx.room.Room.databaseBuilder(
                    context.applicationContext,
                    GuideDb::class.java,
                    "guide.db",
                ).fallbackToDestructiveMigration().build().also { INSTANCE = it }
            }
    }
}
