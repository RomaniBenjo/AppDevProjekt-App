package com.example.commingsoon.language

import androidx.annotation.StringRes
import androidx.compose.runtime.Composable

@Composable
fun appString(
    @StringRes id: Int,
    vararg args: Any
): String {
    return LocalLocalizedContext.current.resources.getString(id, *args)
}