package com.example.ui.theme

import androidx.compose.runtime.mutableStateOf

/**
 * Global Theme Manager ensuring Light Mode is default, with dynamic toggle to Dark Mode.
 */
object ThemeManager {
    val isDarkMode = mutableStateOf(false)

    fun toggle() {
        isDarkMode.value = !isDarkMode.value
    }

    fun setDark(dark: Boolean) {
        isDarkMode.value = dark
    }
}
