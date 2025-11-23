package io.oworld.ofexpense.db

import androidx.room.Dao
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Update
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow
import java.io.Serializable
import java.util.UUID

@Entity
data class Category(
    @PrimaryKey
    val id: String = UUID.randomUUID().toString(),
    var name: String,
    var creator: String,
    val createTime: Long,
    var modifyTime: Long,
    var myShare: Int,
    var zeShare: Int,
    var deleted: Boolean
) : Serializable

@Dao
interface CategoryDao {
    @Query("SELECT * FROM Category WHERE Category.deleted!=true ORDER BY createTime ASC")
    fun getAllValid(): Flow<List<Category>>

    @Query("SELECT * FROM Category ORDER BY createTime ASC")
    fun getAllNonFlow(): List<Category>

    @Query("SELECT * FROM Category WHERE Category.modifyTime > :zeSyncMillis")
    fun getAllNew(zeSyncMillis: Long): List<Category>

    @Query("SELECT * FROM Category WHERE Category.modifyTime > :zeSyncMillis AND Category.creator=:me")
    fun getAllOfMyNew(me: String, zeSyncMillis: Long): List<Category>

    @Insert
    suspend fun insert(categories: List<Category>)

    @Insert
    suspend fun insert(category: Category)

    @Update
    suspend fun update(category: Category)

    @Update
    suspend fun update(categories: List<Category>)

    @Upsert
    suspend fun upsert(categories: List<Category>)
}