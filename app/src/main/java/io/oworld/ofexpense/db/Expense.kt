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
data class Expense(
    @PrimaryKey
    val id: String = UUID.randomUUID().toString(),
    var categoryId: String,
    var cost: Int,
    var memo: String,
    var creator: String,
    var createTime: Long,
    var modifyTime: Long,
    var deleted: Boolean
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
    var deleted: Boolean
) {
    fun toExpense(): Expense {
        return Expense(id, categoryId, cost, memo, creator, createTime, modifyTime, deleted)
    }
}

data class CategorySummary(
    val categoryName: String,
    val summary: Int,
)

@Dao
interface ExpenseDao {
    @Query("SELECT e.*, c.name AS categoryName FROM Expense e INNER JOIN Category c ON e.categoryId=c.id ORDER BY createTime ASC")
    fun getAllWithCategoryName(): Flow<List<ExpenseWithCategoryName>>

    @Query("SELECT e.*, c.name AS categoryName FROM Expense e INNER JOIN Category c ON e.categoryId=c.id ORDER BY createTime ASC")
    fun getAllWithCategoryNameNoneFlow(): List<ExpenseWithCategoryName>

    @Query("SELECT e.*, c.name AS categoryName FROM Expense e INNER JOIN Category c ON e.categoryId=c.id WHERE e.deleted!=true ORDER BY createTime DESC")
    fun getAllValidWithCategoryName(): Flow<List<ExpenseWithCategoryName>>

    @Query("SELECT * FROM Expense WHERE Expense.modifyTime > :zeSyncMillis")
    fun getAllNew(zeSyncMillis: Long): List<Expense>

    @Query("SELECT * FROM Expense WHERE Expense.modifyTime > :zeSyncMillis AND Expense.creator=:me")
    fun getAllOfMyNew(me: String, zeSyncMillis: Long): List<Expense>

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

    @Query("SELECT (SELECT IFNULL(SUM(cost*myShare/100), 0) FROM Expense e INNER JOIN Category c ON e.categoryId=c.id WHERE e.createTime>(SELECT accountPeriodStart FROM Preference WHERE id=1) AND e.createTime<(SELECT accountPeriodEnd FROM Preference WHERE id=1) AND e.deleted!=true) - (SELECT IFNULL(SUM(cost), 0) FROM Expense, Preference WHERE creator=:me AND createTime>accountPeriodStart AND createTime<accountPeriodEnd AND deleted!=true)")
    fun meToZe(me: String): Flow<Int>

    @Query("SELECT IFNULL(SUM(cost), 0) FROM Expense, Preference WHERE creator=:who AND createTime>accountPeriodStart AND createTime<accountPeriodEnd AND deleted!=true")
    fun realExpense(who: String): Flow<Int>

    @Query("SELECT c.name AS categoryName, IFNULL(SUM(cost), 0) AS summary FROM Expense e INNER JOIN Category c ON e.categoryId=c.id WHERE e.createTime>(SELECT accountPeriodStart FROM Preference WHERE id=1) AND e.createTime<(SELECT accountPeriodEnd FROM Preference WHERE id=1) AND e.deleted!=true AND e.creator=:who GROUP BY c.name")
    fun categorySummary(who: String): Flow<List<CategorySummary>>
}
