package io.oworld.ofexpense.ui.screen

import android.annotation.SuppressLint
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.input.InputTransformation
import androidx.compose.foundation.text.input.TextFieldLineLimits
import androidx.compose.foundation.text.input.clearText
import androidx.compose.foundation.text.input.maxLength
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.foundation.text.input.setTextAndPlaceCursorAtEnd
import androidx.compose.foundation.text.input.setTextAndSelectAll
import androidx.compose.foundation.text.input.then
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AddCircle
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuAnchorType
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.LocalViewModelStoreOwner
import androidx.navigation.NavController
import dagger.hilt.android.lifecycle.HiltViewModel
import io.oworld.ofexpense.R
import io.oworld.ofexpense.db.AppDatabase
import io.oworld.ofexpense.db.Category
import io.oworld.ofexpense.db.Expense
import io.oworld.ofexpense.db.ExpenseWithCategoryName
import io.oworld.ofexpense.utils.getStr
import io.oworld.ofexpense.utils.plus
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.format
import kotlinx.datetime.format.char
import kotlinx.datetime.toLocalDateTime
import java.time.Clock
import javax.inject.Inject
import kotlin.time.ExperimentalTime
import kotlin.time.Instant

@SuppressLint("DefaultLocale")
@OptIn(ExperimentalMaterial3Api::class, ExperimentalTime::class)
@Composable
fun ExpensesScreen(
    paddingValues: PaddingValues,
    navController: NavController,
    viewModel: ExpensesViewModel = hiltViewModel(
        checkNotNull(LocalViewModelStoreOwner.current) {
            "No ViewModelStoreOwner was provided via LocalViewModelStoreOwner"
        },
        null
    )
) {
    Column(Modifier.padding(paddingValues + PaddingValues(12.dp))) {
        val focusRequester = remember { FocusRequester() }
        val resources = LocalResources.current
        val categoryList by viewModel.categoryListStateFlow.collectAsState()
        val expenseList by viewModel.expenseListStateFlow.collectAsState()
        if (categoryList.isNotEmpty()) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                var categoryMenuExpanded by remember { mutableStateOf(false) }
                val categoryNameState = rememberTextFieldState(categoryList[0].name)
                var categoryId by remember { mutableStateOf(categoryList[0].id) }
                val costState = rememberTextFieldState("0")
                val memoState = rememberTextFieldState("")
                Column(Modifier.weight(1f)) {
                    Row(Modifier.fillMaxWidth()) {
                        ExposedDropdownMenuBox(
                            modifier = Modifier.weight(1f),
                            expanded = categoryMenuExpanded,
                            onExpandedChange = { categoryMenuExpanded = !categoryMenuExpanded }) {
                            TextField(
                                state = categoryNameState,
                                readOnly = true,
                                lineLimits = TextFieldLineLimits.SingleLine,
                                trailingIcon = {
                                    ExposedDropdownMenuDefaults.TrailingIcon(
                                        expanded = categoryMenuExpanded,
                                    )
                                },
                                modifier = Modifier
                                    .padding(end = 6.dp, bottom = 6.dp)
                                    .menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable)
                            )
                            ExposedDropdownMenu(
                                expanded = categoryMenuExpanded,
                                onDismissRequest = {
                                    categoryMenuExpanded = false
                                },
                            ) {
                                categoryList.forEach { category ->
                                    DropdownMenuItem(
                                        text = { Text(category.name) },
                                        onClick = {
                                            categoryNameState.setTextAndPlaceCursorAtEnd(category.name)
                                            categoryId = category.id
                                            categoryMenuExpanded = false
                                        }
                                    )
                                }
                            }
                        }
                        TextField(
                            state = costState,
                            modifier = Modifier
                                .padding(end = 6.dp, bottom = 6.dp)
                                .weight(1f)
                                .focusRequester(focusRequester),
                            lineLimits = TextFieldLineLimits.SingleLine,
                            textStyle = LocalTextStyle.current.copy(textAlign = TextAlign.End),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            inputTransformation = InputTransformation.maxLength(12).then {
                                val regex = Regex("^\\d*(\\.\\d{0,2})?$")
                                if (!regex.matches(asCharSequence())) {
                                    revertAllChanges()
                                }
                            }
                        )
                        DisposableEffect(Unit) {
                            focusRequester.requestFocus()
                            costState.edit { selection = TextRange(0, costState.text.length) }
                            onDispose {
                                focusRequester.freeFocus()
                            }
                        }
                    }
                    TextField(
                        state = memoState,
                        modifier = Modifier
                            .padding(end = 6.dp, bottom = 6.dp)
                            .fillMaxWidth(),
                        lineLimits = TextFieldLineLimits.SingleLine,
                        label = { Text(getStr(R.string.memo)) },
                        inputTransformation = InputTransformation.maxLength(30)
                    )
                }

                Icon(
                    Icons.Rounded.AddCircle,
                    contentDescription = getStr(R.string.add),
                    modifier = Modifier.clickable(onClick = {
                        val costInt =
                            ((costState.text.toString().toFloatOrNull() ?: 0F) * 100).toInt()
                        val now = Clock.systemUTC().millis()
                        val newExpense = Expense(
                            categoryId = categoryId,
                            cost = costInt,
                            memo = memoState.text.toString(),
                            creator = resources.getString(R.string.me),
                            createTime = now,
                            modifyTime = now,
                            deleted = false,
                        )
                        viewModel.addExpense(newExpense)
                        costState.setTextAndSelectAll("0")
                        memoState.clearText()
                        focusRequester.requestFocus()
                    })
                )
            }
        }
        LazyColumn {
            itemsIndexed(items = expenseList) { index, expense ->
                val myTimeZone = TimeZone.currentSystemDefault()
                val createTimeInstant = Instant.fromEpochMilliseconds(expense.createTime)
                val dateTimeFormat = LocalDateTime.Format {
                    monthNumber()
                    char('/')
                    day()
                    char(' ')
                    hour()
                    char(':')
                    minute()
                }
                val readableCreateTime =
                    createTimeInstant.toLocalDateTime(myTimeZone).format(dateTimeFormat)
                val rowColor = if (index % 2 == 1) {
                    MaterialTheme.colorScheme.primaryContainer
                } else {
                    MaterialTheme.colorScheme.background
                }
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.background(rowColor)
                ) {
                    Text(
                        readableCreateTime,
                        fontSize = 12.sp,
                        fontFamily = FontFamily.Monospace,
                        modifier = Modifier.padding(end = 6.dp)
                    )
                    Text(expense.categoryName, fontSize = 12.sp, modifier = Modifier.width(65.dp))
                    Text(
                        text = String.format("%.2f", expense.cost / 100F),
                        fontSize = 12.sp,
                        modifier = Modifier
                            .width(100.dp)
                            .padding(end = 12.dp),
                        textAlign = TextAlign.End
                    )
                    Text(expense.memo, fontSize = 12.sp, modifier = Modifier.weight(1f))
                    Row(
                        modifier = Modifier.size(12.dp)
                    ) {
                        if (expense.creator == getStr(R.string.me)) {
                            Icon(
                                Icons.Rounded.Edit,
                                contentDescription = getStr(R.string.edit),
                                Modifier.clickable(onClick = {
                                    navController.navigate(
                                        resources.getString(
                                            R.string.edit_expense
                                        ) + "/" + expense.id
                                    )
                                })
                            )
                        }
                    }
                }
            }
        }
    }
}

@HiltViewModel
class ExpensesViewModel @Inject constructor(private val appDatabase: AppDatabase) : ViewModel() {
    fun addExpense(expense: Expense) = viewModelScope.launch {
        appDatabase.expenseDao().insert(expense)
    }

    val categoryListStateFlow: StateFlow<List<Category>> =
        appDatabase.categoryDao().getAllValid().stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5000),
            emptyList()
        )
    val expenseListStateFlow: StateFlow<List<ExpenseWithCategoryName>> =
        appDatabase.expenseDao().getAllValidWithCategoryName().stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5000),
            emptyList()
        )
}