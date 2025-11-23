package io.oworld.ofexpense.ui.screen

import android.app.AlertDialog
import android.os.Environment
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.input.InputTransformation
import androidx.compose.foundation.text.input.clearText
import androidx.compose.foundation.text.input.maxLength
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.foundation.text.input.setTextAndPlaceCursorAtEnd
import androidx.compose.foundation.text.input.then
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AddCircle
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.text.isDigitsOnly
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.LocalViewModelStoreOwner
import com.github.doyaaaaaken.kotlincsv.dsl.csvReader
import com.github.doyaaaaaken.kotlincsv.dsl.csvWriter
import dagger.hilt.android.lifecycle.HiltViewModel
import io.oworld.ofexpense.R
import io.oworld.ofexpense.db.AppDatabase
import io.oworld.ofexpense.db.Category
import io.oworld.ofexpense.db.Expense
import io.oworld.ofexpense.db.Preference
import io.oworld.ofexpense.utils.getStr
import io.oworld.ofexpense.utils.plus
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.io.File
import java.time.Clock
import javax.inject.Inject

@Composable
fun SettingsScreen(
    paddingValues: PaddingValues, viewModel: SettingsViewModel = hiltViewModel(
        checkNotNull(LocalViewModelStoreOwner.current) {
            "No ViewModelStoreOwner was provided via LocalViewModelStoreOwner"
        },
        null
    )
) {
    val columnNameWeight = 0.5f
    val columnMyShareWeight = 0.25f
    val columnZeShareWeight = 0.25f
    val columnActionWidth = 80.dp
    var showDumpDialog by remember { mutableStateOf(false) }
    var showLoadDialog by remember { mutableStateOf(false) }
    val resources = LocalResources.current

    val addCategoryDialogBuilder: AlertDialog.Builder = AlertDialog.Builder(LocalContext.current)
    addCategoryDialogBuilder.setTitle(getStr(R.string.attention))
        .setMessage(getStr(R.string.hint_add_category))
        .setPositiveButton(getStr(R.string.confirm)) { _, _ -> }
        .setNegativeButton(getStr(R.string.cancel)) { _, _ -> }
    val addCategoryDialog = addCategoryDialogBuilder.create()
    Column(Modifier.padding(paddingValues + PaddingValues(12.dp))) {
        Row(modifier = Modifier.fillMaxWidth()) {
            Text(
                getStr(R.string.label_new_category_name),
                Modifier.weight(columnNameWeight),
                fontSize = 12.sp
            )
            Text(
                getStr(R.string.label_my_share),
                Modifier.weight(columnMyShareWeight),
                fontSize = 12.sp
            )
            Text(
                getStr(R.string.label_ze_share),
                Modifier.weight(columnZeShareWeight),
                fontSize = 12.sp
            )
            Spacer(Modifier.width(columnActionWidth))
        }
        val categoryList by viewModel.categoryListStateFlow.collectAsState()
        LazyColumn(Modifier.weight(1f)) {
            itemsIndexed(items = categoryList) { index, category ->
                var isEditing by remember { mutableStateOf(false) }
                if (isEditing) {
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        val categoryNameState = rememberTextFieldState(category.name)
                        val myShareState = rememberTextFieldState(category.myShare.toString())
                        val zeShareState = rememberTextFieldState(category.zeShare.toString())
                        TextField(
                            state = categoryNameState,
                            modifier = Modifier
                                .padding(end = 6.dp)
                                .weight(columnNameWeight),
                            inputTransformation = InputTransformation.maxLength(10),
                            textStyle = TextStyle(fontSize = 12.sp)
                        )
                        TextField(
                            state = myShareState,
                            modifier = Modifier
                                .padding(end = 6.dp)
                                .weight(columnMyShareWeight),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            inputTransformation = InputTransformation.maxLength(3).then {
                                if (!asCharSequence().isDigitsOnly()) {
                                    revertAllChanges()
                                }
                            },
                            textStyle = TextStyle(fontSize = 12.sp)
                        )
                        TextField(
                            state = zeShareState,
                            modifier = Modifier
                                .padding(end = 6.dp)
                                .weight(columnZeShareWeight),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            inputTransformation = InputTransformation.maxLength(3).then {
                                if (!asCharSequence().isDigitsOnly()) {
                                    revertAllChanges()
                                }
                            },
                            textStyle = TextStyle(fontSize = 12.sp)
                        )
                        Row(
                            horizontalArrangement = Arrangement.End,
                            modifier = Modifier.width(columnActionWidth)
                        ) {
                            var showDeleteDialog by remember { mutableStateOf(false) }
                            if (showDeleteDialog) {
                                AlertDialog(
                                    icon = {
                                        Icon(
                                            Icons.Rounded.Warning,
                                            contentDescription = getStr(R.string.attention)
                                        )
                                    },
                                    title = { Text(text = getStr(R.string.attention)) },
                                    text = { Text(text = getStr(R.string.hint_delete_category)) },
                                    onDismissRequest = {},
                                    confirmButton = {
                                        TextButton(onClick = {
                                            viewModel.deleteCategory(category)
                                            showDeleteDialog = false
                                            isEditing = false
                                        }) {
                                            Text(getStr(R.string.confirm))
                                        }
                                    },
                                    dismissButton = {
                                        TextButton(onClick = { showDeleteDialog = false }) {
                                            Text(getStr(R.string.cancel))
                                        }
                                    }
                                )
                            }
                            IconButton(onClick = { showDeleteDialog = true }) {
                                Icon(
                                    Icons.Rounded.Delete,
                                    contentDescription = getStr(R.string.delete)
                                )
                            }
                            IconButton(onClick = {
                                val myShareInt = myShareState.text.toString().toIntOrNull() ?: 0
                                val zeShareInt = zeShareState.text.toString().toIntOrNull() ?: 0
                                val categoryName = categoryNameState.text.toString()
                                if ((myShareInt + zeShareInt == 100) && (categoryName != "") && !categoryList.any { it.name == categoryName && it.id != category.id }
                                ) {
                                    category.name = categoryName
                                    category.myShare = myShareInt
                                    category.zeShare = zeShareInt
                                    category.modifyTime = Clock.systemUTC().millis()
                                    viewModel.updateCategory(category)
                                    isEditing = false
                                } else {
                                    addCategoryDialog.show()
                                }
                            }) {
                                Icon(
                                    Icons.Rounded.Check,
                                    contentDescription = getStr(R.string.confirm)
                                )
                            }
                        }
                    }
                } else {
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            category.name,
                            Modifier.weight(columnNameWeight),
                            fontSize = 12.sp
                        )
                        Text(
                            category.myShare.toString(),
                            Modifier.weight(columnMyShareWeight),
                            fontSize = 12.sp
                        )
                        Text(
                            category.zeShare.toString(),
                            Modifier.weight(columnZeShareWeight),
                            fontSize = 12.sp
                        )
                        Row(
                            horizontalArrangement = Arrangement.End,
                            modifier = Modifier.width(columnActionWidth)
                        ) {
                            if (category.creator == getStr(R.string.me)) {
                                Icon(
                                    Icons.Rounded.Edit,
                                    contentDescription = getStr(R.string.edit),
                                    Modifier.clickable(onClick = { isEditing = true })
                                )
                            }
                        }
                    }
                }
            }
        }
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            val newCategoryNameState = rememberTextFieldState("")
            val newCategoryMyShareState = rememberTextFieldState("50")
            val newCategoryZeShareState = rememberTextFieldState("50")
            TextField(
                state = newCategoryNameState,
                modifier = Modifier
                    .padding(end = 6.dp)
                    .weight(columnNameWeight),
                inputTransformation = InputTransformation.maxLength(10),
                textStyle = TextStyle(fontSize = 12.sp)
            )
            TextField(
                state = newCategoryMyShareState,
                modifier = Modifier
                    .padding(end = 6.dp)
                    .weight(columnMyShareWeight),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                inputTransformation = InputTransformation.maxLength(3).then {
                    if (!asCharSequence().isDigitsOnly()) {
                        revertAllChanges()
                    }
                },
                textStyle = TextStyle(fontSize = 12.sp)
            )
            TextField(
                state = newCategoryZeShareState,
                modifier = Modifier
                    .padding(end = 6.dp)
                    .weight(columnZeShareWeight),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                inputTransformation = InputTransformation.maxLength(3).then {
                    if (!asCharSequence().isDigitsOnly()) {
                        revertAllChanges()
                    }
                },
                textStyle = TextStyle(fontSize = 12.sp)
            )
            Row(
                horizontalArrangement = Arrangement.End,
                modifier = Modifier.width(columnActionWidth)
            ) {
                Icon(
                    Icons.Rounded.AddCircle,
                    contentDescription = getStr(R.string.add),
                    Modifier.clickable(onClick = {
                        val myShareInt = newCategoryMyShareState.text.toString().toIntOrNull() ?: 0
                        val zeShareInt = newCategoryZeShareState.text.toString().toIntOrNull() ?: 0

                        if ((myShareInt + zeShareInt == 100) && (newCategoryNameState.text.toString() != "") && !categoryList.any { it.name == newCategoryNameState.text.toString() }
                        ) {
                            val now = Clock.systemUTC().millis()
                            val newCategory = Category(
                                name = newCategoryNameState.text.toString(),
                                creator = resources.getString(R.string.me),
                                createTime = now,
                                modifyTime = now,
                                myShare = myShareInt,
                                zeShare = zeShareInt,
                                deleted = false
                            )
                            viewModel.addCategory(newCategory)
                            newCategoryNameState.clearText()
                            newCategoryMyShareState.setTextAndPlaceCursorAtEnd("50")
                            newCategoryZeShareState.setTextAndPlaceCursorAtEnd("50")
                        } else {
                            addCategoryDialog.show()
                        }
                    })
                )
            }
        }
        Spacer(Modifier.height(20.dp))
        if (showDumpDialog) {
            AlertDialog(
                title = { Text(text = resources.getString(R.string.dump)) },
                text = { Text(text = resources.getString(R.string.msg_dump_db)) },
                onDismissRequest = { showDumpDialog = false },
                confirmButton = {
                    TextButton(
                        onClick = {
                            viewModel.dump()
                            showDumpDialog = false
                        }
                    ) {
                        Text(resources.getString(R.string.confirm))
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showDumpDialog = false }) {
                        Text(resources.getString(R.string.cancel))
                    }
                }
            )
        }
        Button(
            onClick = { showDumpDialog = true },
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.errorContainer)
        ) {
            Text(resources.getString(R.string.dump))
        }
        if (showLoadDialog) {
            AlertDialog(
                title = { Text(text = resources.getString(R.string.load)) },
                text = { Text(text = resources.getString(R.string.msg_load_csv)) },
                onDismissRequest = { showLoadDialog = false },
                confirmButton = {
                    TextButton(
                        onClick = {
                            viewModel.load()
                            showLoadDialog = false
                        }
                    ) {
                        Text(resources.getString(R.string.confirm))
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showLoadDialog = false }) {
                        Text(resources.getString(R.string.cancel))
                    }
                }
            )
        }
        Button(
            onClick = { showLoadDialog = true },
            Modifier
                .fillMaxWidth()
                .padding(top = 12.dp),
            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.errorContainer)
        ) {
            Text(resources.getString(R.string.load))
        }
    }
}

@HiltViewModel
class SettingsViewModel @Inject constructor(private val appDatabase: AppDatabase) : ViewModel() {
    val categoryListStateFlow: StateFlow<List<Category>> =
        appDatabase.categoryDao().getAllValid().stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5000),
            emptyList()
        )

    fun addCategory(category: Category) = viewModelScope.launch {
        appDatabase.categoryDao().insert(category)
    }

    fun updateCategory(category: Category) = viewModelScope.launch {
        appDatabase.categoryDao().update(category)
    }

    fun deleteCategory(category: Category) = viewModelScope.launch {
        val now = Clock.systemUTC().millis()
        val deletedCategory = category.copy(deleted = true, modifyTime = now)
        appDatabase.categoryDao().update(deletedCategory)
    }

    private val expenseFile = File(
        Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS),
        "expense.csv"
    )
    private val categoryFile = File(
        Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS),
        "category.csv"
    )
    private val preferenceFile = File(
        Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS),
        "preference.csv"
    )

    fun dump() = viewModelScope.launch(Dispatchers.IO) {
        val categoryList = appDatabase.categoryDao().getAllNonFlow()
        val categorySerializedList = categoryList.map { category ->
            listOf(
                category.id,
                category.name,
                category.creator,
                category.createTime.toString(),
                category.modifyTime.toString(),
                category.myShare.toString(),
                category.zeShare.toString(),
                category.deleted.toString(),
            )
        }
        val expenseList = appDatabase.expenseDao().getAllWithCategoryNameNoneFlow()
        val expenseSerializedList = expenseList.map { expense ->
            listOf(
                expense.id,
                expense.categoryId,
                expense.categoryName,
                expense.cost.toString(),
                expense.memo,
                expense.creator,
                expense.createTime.toString(),
                expense.modifyTime.toString(),
                expense.deleted.toString()
            )
        }
        val preferenceList = listOf(appDatabase.preferenceDao().get())
        val preferenceSerializedList = preferenceList.map { preference ->
            val preference = preference ?: Preference(
                id = 1,
                syncDateTime = 0,
                accountPeriodStart = 0,
                accountPeriodEnd = 0,
            )
            listOf(
                preference.id.toString(),
                preference.syncDateTime.toString(),
                preference.accountPeriodStart.toString(),
                preference.accountPeriodEnd.toString()
            )
        }
        csvWriter().writeAll(categorySerializedList, categoryFile)
        csvWriter().writeAll(expenseSerializedList, expenseFile)
        csvWriter().writeAll(preferenceSerializedList, preferenceFile)
    }

    fun load() = viewModelScope.launch(Dispatchers.IO) {
        val categorySerializedList = csvReader().readAll(categoryFile)
        val expenseSerializedList = csvReader().readAll(expenseFile)
        val preferenceSerializedList = csvReader().readAll(preferenceFile)
        val categoryList = categorySerializedList.map {
            Category(
                id = it[0],
                name = it[1],
                creator = it[2],
                createTime = it[3].toLong(),
                modifyTime = it[4].toLong(),
                myShare = it[5].toInt(),
                zeShare = it[6].toInt(),
                deleted = it[7].toBoolean(),
            )
        }
        val expenseList = expenseSerializedList.map {
            Expense(
                id = it[0],
                categoryId = it[1],
                cost = it[3].toInt(),
                memo = it[4],
                creator = it[5],
                createTime = it[6].toLong(),
                modifyTime = it[7].toLong(),
                deleted = it[8].toBoolean(),
            )
        }
        val preference = Preference(
            id = preferenceSerializedList[0][0].toInt(),
            syncDateTime = preferenceSerializedList[0][1].toLong(),
            accountPeriodStart = preferenceSerializedList[0][2].toLong(),
            accountPeriodEnd = preferenceSerializedList[0][3].toLong()
        )
        appDatabase.categoryDao().upsert(categoryList)
        appDatabase.expenseDao().upsert(expenseList)
        appDatabase.preferenceDao().upsert(preference)
    }
}