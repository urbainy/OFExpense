package io.oworld.ofexpense.db

import androidx.room.Dao
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Entity
data class Preference(
    @PrimaryKey(autoGenerate = true)
    val id: Int,
    val syncDateTime: Long
)

@Dao
interface PreferenceDao {
    @Query("SELECT * FROM Preference")
    fun getAll(): Flow<List<Preference>>

    @Insert
    suspend fun insert(pref: Preference)

    @Update
    suspend fun update(pref: Preference)
}