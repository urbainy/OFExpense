package io.oworld.ofexpense.ui.screen

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import io.oworld.ofexpense.plus

@Composable
fun MainScreen(paddingValues: PaddingValues) {
    Column(Modifier.padding(paddingValues + PaddingValues(12.dp))) {
        Text(
            text = "MainScreen",
        )
    }
}