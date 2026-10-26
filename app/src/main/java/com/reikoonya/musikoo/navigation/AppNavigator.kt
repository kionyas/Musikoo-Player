package com.reikoonya.musikoo.navigation

import androidx.compose.runtime.mutableStateListOf

/**
 * Стек экранов. Последний элемент — текущий экран.
 * Логика вынесена из MainActivity, чтобы её можно было читать и проверять отдельно.
 * Это подготовка к Navigation Compose: маршруты уже описаны одним sealed-классом.
 */
class AppNavigator {
    val backStack = mutableStateListOf<Screen>(Screen.MainMenu)

    val currentScreen: Screen
        get() = backStack.lastOrNull() ?: Screen.MainMenu

    val canGoBack: Boolean
        get() = backStack.size > 1

    /** Открывает экран поверх текущего. Повтор текущего экрана игнорируется. */
    fun navigateTo(screen: Screen) {
        if (currentScreen != screen) {
            backStack.add(screen)
        }
    }

    /** Возвращает на предыдущий экран. На главном экране ничего не делает. */
    fun popBack() {
        if (canGoBack) {
            backStack.removeAt(backStack.lastIndex)
        }
    }

    /** Закрывает всё, кроме главного экрана. */
    fun popToMainMenu() {
        backStack.removeAll { it !is Screen.MainMenu }
    }
}
