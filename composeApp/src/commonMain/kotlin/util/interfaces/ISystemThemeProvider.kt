package util.interfaces

import presentation.viewmodel.ThemeSelect
 
interface ISystemThemeProvider {
    suspend fun getSystemTheme(): ThemeSelect
} 