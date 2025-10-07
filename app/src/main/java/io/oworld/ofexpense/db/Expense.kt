package io.oworld.ofexpense.db

import androidx.room.Dao
import androidx.room.Delete
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
data class Expense(
    @PrimaryKey
    val id: String = UUID.randomUUID().toString(),
    var categoryId: String,
    var cost: Int,
    var memo: String,
    var creator: String,
    var createTime: Long,
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
    @Query("SELECT e.*, c.name AS categoryName FROM Expense e INNER JOIN Category c ON e.categoryId=c.id ORDER BY createTime ASC")
    fun getAllWithCategoryName(): Flow<List<ExpenseWithCategoryName>>

    @Query("SELECT * FROM Expense ORDER BY createTime ASC")
    fun getAll(): Flow<List<Expense>>

    @Query("SELECT * FROM Expense e, Preference p WHERE e.modifyTime > p.syncDateTime AND e.creator=:me")
    fun getAllOfMyNew(me: String): List<Expense>

    @Query("SELECT e.*, c.name AS categoryName FROM Expense e INNER JOIN Category c ON e.categoryId=c.id WHERE e.id=:id")
    fun get(id: String): Flow<ExpenseWithCategoryName?>

    @Insert
    suspend fun insert(expense: Expense)

    @Insert
    suspend fun insert(expenseList: List<Expense>)

    @Update
    suspend fun update(expense: Expense)

    @Update
    suspend fun update(expenses: List<Expense>)

    @Upsert
    suspend fun upsert(expenses: List<Expense>)

    @Delete
    suspend fun delete(expense: Expense)

    @Query("SELECT (SELECT SUM(cost*myShare/100) FROM Expense e INNER JOIN Category c ON e.categoryId=c.id WHERE e.createTime>(SELECT accountPeriodStart FROM Preference WHERE id=1) AND e.createTime<(SELECT accountPeriodEnd FROM Preference WHERE id=1)) - (SELECT SUM(cost) FROM Expense, Preference WHERE creator=:me AND createTime>accountPeriodStart AND createTime<accountPeriodEnd)")
    fun meToZe(me: String): Flow<Int?>
}