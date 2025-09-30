package io.oworld.ofexpense.ui.screen

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import io.oworld.ofexpense.db.AppDatabase
import io.oworld.ofexpense.db.Expense
import io.oworld.ofexpense.utils.plus
import kotlinx.coroutines.launch
import javax.inject.Inject

@Composable
fun ExpenseEditScreen(paddingValues: PaddingValues) {
    Column(Modifier.padding(paddingValues + PaddingValues(12.dp))) {
        Text(
            text = "AddExpenseScreen",
        )
    }
}

@HiltViewModel
class ExpenseEditViewModel @Inject constructor(
    private val appDatabase: AppDatabase,
) :
    ViewModel() {


    fun save(expense: Expense) = viewModelScope.launch {
        appDatabase.expenseDao().insert(expense)
    }
}

