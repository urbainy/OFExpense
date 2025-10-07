package io.oworld.ofexpense.utils

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.unit.LayoutDirection
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toInstant
import kotlinx.datetime.toLocalDateTime
import kotlin.time.ExperimentalTime
import kotlin.time.Instant

operator fun PaddingValues.plus(other: PaddingValues): PaddingValues {
    return PaddingValues(
        start = this.calculateLeftPadding(LayoutDirection.Ltr) + other.calculateLeftPadding(
            LayoutDirection.Ltr
        ),
        top = this.calculateTopPadding() + other.calculateTopPadding(),
        end = this.calculateRightPadding(LayoutDirection.Ltr) + other.calculateRightPadding(
            LayoutDirection.Ltr
        ),
        bottom = this.calculateBottomPadding() + other.calculateBottomPadding()
    )
}

@Composable
fun getStr(resourceId: Int): String {
    return LocalResources.current.getString(resourceId)
}

@OptIn(ExperimentalTime::class)
fun datePickerToUtcMillis(datePickerMillis: Long): Long {
    val myTimeZone = TimeZone.currentSystemDefault()
    val instant = Instant.fromEpochMilliseconds(datePickerMillis)
    return instant.toLocalDateTime(TimeZone.UTC).toInstant(myTimeZone).toEpochMilliseconds()
}