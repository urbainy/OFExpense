package io.oworld.ofexpense.db

import androidx.room.Dao
import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Upsert

@Entity
data class Preference(
    @PrimaryKey(autoGenerate = true)
    val id: Int,
    val syncDateTime: Long,
    val accountPeriodStart: Long,
    val accountPeriodEnd: Long
)

@Dao
interface PreferenceDao {
    @Query("SELECT * FROM Preference WHERE id=1")
    fun get(): Preference?

    @Upsert
    suspend fun upsert(preference: Preference)
}