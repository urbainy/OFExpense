package io.oworld.ofexpense.ui.screen

import android.app.AlertDialog
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
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
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import dagger.hilt.android.lifecycle.HiltViewModel
import io.oworld.ofexpense.R
import io.oworld.ofexpense.db.AppDatabase
import io.oworld.ofexpense.db.Category
import io.oworld.ofexpense.utils.getStr
import io.oworld.ofexpense.utils.plus
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
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
    val resources = LocalResources.current
    val addCategoryDialogBuilder: AlertDialog.Builder = AlertDialog.Builder(LocalContext.current)
    addCategoryDialogBuilder.setTitle(getStr(R.string.attention))
        .setMessage(getStr(R.string.hint_add_category))
        .setPositiveButton(getStr(R.string.confirm)) { _, _ -> }
        .setNegativeButton(getStr(R.string.cancel)) { _, _ -> }
    val modifyCategoryDialog = addCategoryDialogBuilder.create()
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
                                    modifyCategoryDialog.show()
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
                                IconButton(
                                    onClick = { isEditing = true },
                                ) {
                                    Icon(
                                        Icons.Rounded.Edit,
                                        contentDescription = getStr(R.string.edit),
                                    )
                                }
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
                IconButton(onClick = {
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
                        )
                        viewModel.addCategory(newCategory)
                        newCategoryNameState.clearText()
                        newCategoryMyShareState.setTextAndPlaceCursorAtEnd("50")
                        newCategoryZeShareState.setTextAndPlaceCursorAtEnd("50")
                    } else {
                        modifyCategoryDialog.show()
                    }
                }) {
                    Icon(Icons.Rounded.AddCircle, getStr(R.string.add))
                }
            }
        }
    }
}

@HiltViewModel
class SettingsViewModel @Inject constructor(private val appDatabase: AppDatabase) : ViewModel() {
    fun addCategory(category: Category) = viewModelScope.launch {
        appDatabase.categoryDao().insert(category)
    }

    fun updateCategory(category: Category) = viewModelScope.launch {
        appDatabase.categoryDao().update(category)
    }

    fun deleteCategory(category: Category) = viewModelScope.launch {
        appDatabase.categoryDao().delete(category)
    }

    val categoryListStateFlow: StateFlow<List<Category>> =
        appDatabase.categoryDao().getAll().stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5000),
            emptyList()
        )
}