package io.oworld.ofexpense.ui.screen

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.input.InputTransformation
import androidx.compose.foundation.text.input.TextFieldLineLimits
import androidx.compose.foundation.text.input.maxLength
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.foundation.text.input.setTextAndPlaceCursorAtEnd
import androidx.compose.foundation.text.input.then
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuAnchorType
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TimePicker
import androidx.compose.material3.TimePickerDialog
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.SavedStateHandle
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
import kotlinx.datetime.toInstant
import kotlinx.datetime.toLocalDateTime
import java.util.Locale
import javax.inject.Inject
import kotlin.time.Clock
import kotlin.time.ExperimentalTime
import kotlin.time.Instant

@OptIn(ExperimentalMaterial3Api::class, ExperimentalTime::class)
@Composable
fun ExpenseEditScreen(
    paddingValues: PaddingValues,
    navController: NavController,
    viewModel: ExpenseEditViewModel = hiltViewModel(
        checkNotNull(LocalViewModelStoreOwner.current) {
            "No ViewModelStoreOwner was provided via LocalViewModelStoreOwner"
        },
        null
    )
) {
    val expense by viewModel.expenseStateFlow.collectAsState()
    val categoryList by viewModel.categoryListStateFlow.collectAsState()
    if (expense != null && categoryList.isNotEmpty()) {
        Column(Modifier.padding(paddingValues + PaddingValues(12.dp))) {
            var categoryMenuExpanded by remember { mutableStateOf(false) }
            val categoryNameState = rememberTextFieldState(expense!!.categoryName)
            var categoryId by remember { mutableStateOf(expense!!.categoryId) }
            val costState = rememberTextFieldState((expense!!.cost / 100F).toString())
            val memoState = rememberTextFieldState(expense!!.memo)
            var showDateDialog by remember { mutableStateOf(false) }
            var showTimeDialog by remember { mutableStateOf(false) }
            val myTimeZone = TimeZone.currentSystemDefault()
            val createTimeInstant = Instant.fromEpochMilliseconds(expense!!.createTime)
            val millisForDatePicker =
                createTimeInstant.toLocalDateTime(myTimeZone).toInstant(TimeZone.UTC)
                    .toEpochMilliseconds()
            val datePickerState =
                rememberDatePickerState(initialSelectedDateMillis = millisForDatePicker)
            val localDateTime = createTimeInstant.toLocalDateTime(myTimeZone)
            val timePickerState = rememberTimePickerState(
                initialHour = localDateTime.hour,
                initialMinute = localDateTime.minute,
                is24Hour = true,
            )
            val dateFormat = LocalDateTime.Format {
                year()
                char('/')
                monthNumber()
                char('/')
                day()
            }
            val dateTimeFormat = LocalDateTime.Format {
                year()
                char('/')
                monthNumber()
                char('/')
                day()
                char(' ')
                hour()
                char(':')
                minute()
            }
            if (expense!!.createTime != expense!!.modifyTime) {
                val modifyTimeInstant = Instant.fromEpochMilliseconds(expense!!.modifyTime)
                val modifyDateTime =
                    modifyTimeInstant.toLocalDateTime(myTimeZone).format(dateTimeFormat)
                Text(
                    String.format(getStr(R.string.hint_modify_time), modifyDateTime),
                    Modifier
                        .fillMaxWidth()
                        .padding(bottom = 6.dp)
                )
            }
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(bottom = 6.dp)
            ) {

                Button(
                    onClick = { showDateDialog = !showDateDialog },
                    modifier = Modifier
                        .weight(1f)
                        .padding(end = 6.dp)
                ) {
                    Text(
                        Instant.fromEpochMilliseconds(datePickerState.selectedDateMillis!!)
                            .toLocalDateTime(myTimeZone).format(dateFormat)
                    )
                }
                if (showDateDialog) {
                    DatePickerDialog(
                        onDismissRequest = {
                            showDateDialog = false
                        },
                        confirmButton = {
                            TextButton(onClick = {
                                showDateDialog = false
                            }) {
                                Text(getStr(R.string.confirm))
                            }
                        },
                        dismissButton = {
                            TextButton(onClick = {
                                showDateDialog = false
                            }) {
                                Text(getStr(R.string.cancel))
                            }
                        }
                    ) {
                        DatePicker(state = datePickerState)
                    }
                }
                Button(onClick = { showTimeDialog = !showTimeDialog }, Modifier.weight(1f)) {
                    Text(
                        String.format(
                            Locale.getDefault(),
                            "%02d:%02d",
                            timePickerState.hour,
                            timePickerState.minute
                        )
                    )
                }
                if (showTimeDialog) {
                    TimePickerDialog(
                        title = {},
                        onDismissRequest = { showTimeDialog = false },
                        confirmButton = {
                            TextButton(onClick = { showTimeDialog = false }) {
                                Text(getStr(R.string.confirm))
                            }
                        },
                        dismissButton = {
                            TextButton(onClick = {
                                showTimeDialog = false
                            }) {
                                Text(getStr(R.string.cancel))
                            }
                        }
                    ) { TimePicker(state = timePickerState) }
                }
            }
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(bottom = 6.dp)
            ) {
                ExposedDropdownMenuBox(
                    modifier = Modifier
                        .weight(1f)
                        .padding(end = 6.dp),
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
                        modifier = Modifier.menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable)
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
                    modifier = Modifier.weight(1f),
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
            }
            TextField(
                state = memoState,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 12.dp),
                lineLimits = TextFieldLineLimits.SingleLine,
                label = { Text(getStr(R.string.memo)) },
                inputTransformation = InputTransformation.maxLength(30)
            )
            Button(onClick = {
                val datePickerToUtcMillis =
                    Instant.fromEpochMilliseconds(datePickerState.selectedDateMillis!!)
                        .toLocalDateTime(TimeZone.UTC).toInstant(myTimeZone).toEpochMilliseconds()
                val editedCreateTime =
                    datePickerToUtcMillis + timePickerState.hour * 3600L * 1000L + timePickerState.minute * 60L * 1000L
                val modifyTime = Clock.System.now().toEpochMilliseconds()
                val editedExpense = expense!!.toExpense().copy(
                    categoryId = categoryId,
                    cost = ((costState.text.toString().toFloatOrNull() ?: 0F) * 100).toInt(),
                    memo = memoState.text.toString(),
                    createTime = editedCreateTime,
                    modifyTime = modifyTime,
                )
                viewModel.save(editedExpense)
                navController.popBackStack()
            }, modifier = Modifier.fillMaxWidth()) {
                Text(getStr(R.string.save))
            }
            var showDeleteAlertDialog by remember { mutableStateOf(false) }
            if (showDeleteAlertDialog) {
                AlertDialog(
                    icon = {
                        Icon(
                            Icons.Rounded.Warning,
                            contentDescription = getStr(R.string.attention)
                        )
                    },
                    title = { Text(text = getStr(R.string.attention)) },
                    text = { Text(text = getStr(R.string.hint_delete_expense)) },
                    onDismissRequest = { showDeleteAlertDialog = false },
                    confirmButton = {
                        TextButton(onClick = {
                            showDeleteAlertDialog = false
                            navController.popBackStack()
                            viewModel.delete(expense!!.toExpense())
                        }) {
                            Text(getStr(R.string.confirm))
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = {
                            showDeleteAlertDialog = false
                        }) {
                            Text(getStr(R.string.cancel))
                        }
                    }
                )
            }
            Button(
                onClick = {
                    showDeleteAlertDialog = true
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 36.dp),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
            ) {
                Text(getStr(R.string.delete))
            }
        }
    }
}

@HiltViewModel
class ExpenseEditViewModel @Inject constructor(
    private val appDatabase: AppDatabase, savedStateHandle: SavedStateHandle
) :
    ViewModel() {
    val expenseId = checkNotNull(savedStateHandle.get<String>("expenseId"))
    val expenseStateFlow: StateFlow<ExpenseWithCategoryName?> =
        appDatabase.expenseDao().get(expenseId).stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5000),
            null
        )

    val categoryListStateFlow: StateFlow<List<Category>> =
        appDatabase.categoryDao().getAll().stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5000),
            emptyList()
        )

    fun save(expense: Expense) = viewModelScope.launch {
        appDatabase.expenseDao().update(expense)
    }

    fun delete(expense: Expense) = viewModelScope.launch {
        appDatabase.expenseDao().delete(expense)
    }
}

