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
data class Expense(
    @PrimaryKey
    val id: String = UUID.randomUUID().toString(),
    var categoryId: String,
    var cost: Int,
    var memo: String,
    val creator: String,
    val createTime: Long,
    var modifyTime: Long,
) : Serializable

data class ExpenseWithCategoryName(
    val id: String,
    var categoryId: String,
    var categoryName: String,
    var cost: Int,
    var memo: String,
    val creator: String,
    val createTime: Long,
    var modifyTime: Long,
) {
    fun toExpense(): Expense {
        return Expense(id, categoryId, cost, memo, creator, createTime, modifyTime)
    }
}

@Dao
interface ExpenseDao {
    @Query("SELECT e.*, c.name AS categoryName FROM Expense e INNER JOIN Category c ON e.categoryId=c.id")
    fun getAllWithCategoryName(): Flow<List<ExpenseWithCategoryName>>

    @Query("SELECT e.*, c.name AS categoryName FROM Expense e INNER JOIN Category c ON e.categoryId=c.id WHERE e.id=:id")
    fun get(id: String): Flow<ExpenseWithCategoryName?>

    @Insert
    suspend fun insert(expense: Expense)

    @Insert
    suspend fun insert(expense: List<Expense>)

    @Update
    suspend fun update(expense: Expense)

    @Update
    suspend fun update(expenses: List<Expense>)

    @Delete
    suspend fun delete(expense: Expense)
}