package io.oworld.ofexpense.db

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow
import java.io.Serializable
import java.util.UUID

@Entity
data class Category(
    @PrimaryKey
    val id: String = UUID.randomUUID().toString(),
    var name: String,
    val creator: String,
    val createTime: Long,
    var modifyTime: Long,
    var myShare: Int,
    var zeShare: Int,
) : Serializable

@Dao
interface CategoryDao {
    @Query("SELECT * FROM Category ORDER BY createTime ASC")
    fun getAll(): Flow<List<Category>>

    @Insert
    suspend fun insert(categories: List<Category>)

    @Insert
    suspend fun insert(category: Category)

    @Update
    suspend fun update(category: Category)

    @Update
    suspend fun update(categories: List<Category>)

    @Delete
    suspend fun delete(category: Category)

    @Delete
    suspend fun delete(categories: List<Category>)
}