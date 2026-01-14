package io.oworld.ofexpense.ui.screen

import android.annotation.SuppressLint
import android.content.Context
import android.os.Build
import androidx.annotation.RequiresExtension
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.getSelectedDate
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.LocalViewModelStoreOwner
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import io.oworld.ofexpense.R
import io.oworld.ofexpense.db.AppDatabase
import io.oworld.ofexpense.db.CategorySummary
import io.oworld.ofexpense.db.Preference
import io.oworld.ofexpense.utils.datePickerToUtcMillis
import io.oworld.ofexpense.utils.getStr
import io.oworld.ofexpense.utils.plus
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject
import kotlin.time.ExperimentalTime

@RequiresExtension(extension = Build.VERSION_CODES.TIRAMISU, version = 7)
@SuppressLint("DefaultLocale")
@OptIn(ExperimentalTime::class, ExperimentalMaterial3Api::class)
@Composable
fun StatisticsScreen(
    paddingValues: PaddingValues, viewModel: StatisticsViewModel = hiltViewModel(
        checkNotNull(LocalViewModelStoreOwner.current) {
            "No ViewModelStoreOwner was provided via LocalViewModelStoreOwner"
        },
        null
    )
) {
    var showStartDatePicker by remember { mutableStateOf(false) }
    var showEndDatePicker by remember { mutableStateOf(false) }
    val preference by viewModel.preferenceStateFlow.collectAsState()
    if (preference != null) {
        val startDatePickerState =
            rememberDatePickerState(initialSelectedDateMillis = preference!!.accountPeriodStart)
        val endDatePickerState =
            rememberDatePickerState(initialSelectedDateMillis = preference!!.accountPeriodEnd)
        Column(Modifier.padding(paddingValues + PaddingValues(12.dp))) {
            Row(Modifier.fillMaxWidth()) {
                Button(
                    onClick = { showStartDatePicker = !showStartDatePicker },
                    modifier = Modifier
                        .weight(1f)
                ) {
                    Text(
                        startDatePickerState.getSelectedDate().toString()
                    )
                }
                if (showStartDatePicker) {
                    DatePickerDialog(
                        onDismissRequest = {
                            showStartDatePicker = false
                        },
                        confirmButton = {
                            TextButton(onClick = {
                                viewModel.updateAccountPeriodPreference(
                                    startDatePickerState.selectedDateMillis!!,
                                    endDatePickerState.selectedDateMillis!!
                                )
                                showStartDatePicker = false
                            }) {
                                Text(getStr(R.string.confirm))
                            }
                        },
                        dismissButton = {
                            TextButton(onClick = {
                                showStartDatePicker = false
                            }) {
                                Text(getStr(R.string.cancel))
                            }
                        }
                    ) {
                        DatePicker(state = startDatePickerState)
                    }
                }
                Spacer(Modifier.width(6.dp))
                Button(
                    onClick = { showEndDatePicker = !showEndDatePicker },
                    modifier = Modifier
                        .weight(1f)
                ) {
                    Text(
                        endDatePickerState.getSelectedDate().toString()
                    )
                }
                if (showEndDatePicker) {
                    DatePickerDialog(
                        onDismissRequest = {
                            showEndDatePicker = false
                        },
                        confirmButton = {
                            TextButton(onClick = {
                                viewModel.updateAccountPeriodPreference(
                                    startDatePickerState.selectedDateMillis!!,
                                    endDatePickerState.selectedDateMillis!!
                                )
                                showEndDatePicker = false
                            }) {
                                Text(getStr(R.string.confirm))
                            }
                        },
                        dismissButton = {
                            TextButton(onClick = {
                                showEndDatePicker = false
                            }) {
                                Text(getStr(R.string.cancel))
                            }
                        }
                    ) {
                        DatePicker(state = endDatePickerState)
                    }
                }
            }
            Spacer(Modifier.height(32.dp))
            val myRealExpense by viewModel.myRealExpenseFlow.collectAsState()
            Row(
                Modifier
                    .fillMaxWidth()
                    .height(42.dp)
                    .background(color = MaterialTheme.colorScheme.primaryContainer),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = getStr(R.string.my_real_expense),
                    fontSize = 24.sp
                )
                Spacer(Modifier.weight(1f))
                Text(
                    text = String.format(
                        "%.2f",
                        myRealExpense / 100F
                    ),
                    fontSize = 24.sp
                )
            }
            val myCategorySummary by viewModel.myCategorySummaryStateFlow.collectAsState()
            LazyColumn {
                items(items = myCategorySummary) { categorySummary ->
                    if (categorySummary.summary != 0) {
                        Row(
                            Modifier
                                .fillMaxWidth()
                        ) {
                            Text(
                                text = categorySummary.categoryName,
                                fontSize = 16.sp
                            )
                            Spacer(Modifier.weight(1f))
                            Text(
                                text = String.format(
                                    "%.2f",
                                    categorySummary.summary / 100F
                                ),
                                fontSize = 16.sp
                            )
                        }
                    }
                }
            }
            Spacer(Modifier.height(32.dp))
            val zeRealExpense by viewModel.zeRealExpenseFlow.collectAsState()
            Row(
                Modifier
                    .fillMaxWidth()
                    .height(42.dp)
                    .background(color = MaterialTheme.colorScheme.primaryContainer),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = getStr(R.string.ze_real_expense),
                    fontSize = 24.sp
                )
                Spacer(Modifier.weight(1f))
                Text(
                    text = String.format(
                        "%.2f",
                        zeRealExpense / 100F
                    ),
                    fontSize = 24.sp
                )
            }
            val zeCategorySummary by viewModel.zeCategorySummaryStateFlow.collectAsState()
            LazyColumn {
                items(items = zeCategorySummary) { categorySummary ->
                    if (categorySummary.summary != 0) {
                        Row(
                            Modifier
                                .fillMaxWidth()
                        ) {
                            Text(
                                text = categorySummary.categoryName,
                                fontSize = 16.sp
                            )
                            Spacer(Modifier.weight(1f))
                            Text(
                                text = String.format(
                                    "%.2f",
                                    categorySummary.summary / 100F
                                ),
                                fontSize = 16.sp
                            )
                        }
                    }
                }
            }
            Spacer(Modifier.height(32.dp))
            val meToZe by viewModel.meToZeStateFlow.collectAsState()
            Row(
                Modifier
                    .fillMaxWidth()
                    .height(42.dp)
                    .background(color = MaterialTheme.colorScheme.primaryContainer),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = getStr(R.string.settlement),
                    fontSize = 24.sp
                )
                Spacer(Modifier.weight(1f))
                Text(
                    text = String.format("%.2f", meToZe / 100F),
                    fontSize = 24.sp
                )
            }
        }
    }
}

@HiltViewModel
class StatisticsViewModel @Inject constructor(
    @ApplicationContext appContext: Context,
    private val appDatabase: AppDatabase
) : ViewModel() {
    private val resources = appContext.resources

    val preferenceStateFlow: StateFlow<Preference?> = appDatabase.preferenceDao().getFlow().stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000), null
    )

    @OptIn(ExperimentalTime::class)
    fun updateAccountPeriodPreference(startDatePickerMillis: Long, endDatePickerMillis: Long) =
        viewModelScope.launch(Dispatchers.IO) {
            val accountPeriodStart = datePickerToUtcMillis(startDatePickerMillis)
            val accountPeriodEnd =
                datePickerToUtcMillis(endDatePickerMillis) + 86400000 - 1
            val preference = appDatabase.preferenceDao().get()
            appDatabase.preferenceDao().upsert(
                preference!!.copy(
                    accountPeriodStart = accountPeriodStart,
                    accountPeriodEnd = accountPeriodEnd
                )
            )
        }

    val meToZeStateFlow: StateFlow<Int> =
        appDatabase.expenseDao().meToZe(me = resources.getString(R.string.me)).stateIn(
            viewModelScope, SharingStarted.WhileSubscribed(5000), 0
        )
    val myRealExpenseFlow: StateFlow<Int> =
        appDatabase.expenseDao().realExpense(who = resources.getString(R.string.me)).stateIn(
            viewModelScope, SharingStarted.WhileSubscribed(5000), 0
        )
    val zeRealExpenseFlow: StateFlow<Int> =
        appDatabase.expenseDao().realExpense(who = resources.getString(R.string.ze)).stateIn(
            viewModelScope, SharingStarted.WhileSubscribed(5000), 0
        )
    val myCategorySummaryStateFlow: StateFlow<List<CategorySummary>> =
        appDatabase.expenseDao().categorySummary(who = resources.getString(R.string.me)).stateIn(
            viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList()
        )
    val zeCategorySummaryStateFlow: StateFlow<List<CategorySummary>> =
        appDatabase.expenseDao().categorySummary(who = resources.getString(R.string.ze)).stateIn(
            viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList()
        )
}