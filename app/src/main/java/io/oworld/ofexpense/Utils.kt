package io.oworld.ofexpense

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.ui.unit.LayoutDirection

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