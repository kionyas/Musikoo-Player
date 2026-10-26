package com.reikoonya.musikoo.ui.components

import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember

/** Scroll positions of lists and grids, kept while the app runs so going back restores them. */
object ScrollMemory {
    private val positions = HashMap<String, Pair<Int, Int>>()

    fun get(key: String): Pair<Int, Int> = positions[key] ?: (0 to 0)

    fun put(key: String, index: Int, offset: Int) {
        positions[key] = index to offset
    }
}

@Composable
fun rememberPersistentLazyListState(key: String): LazyListState {
    val (index, offset) = remember(key) { ScrollMemory.get(key) }
    val state = rememberLazyListState(
        initialFirstVisibleItemIndex = index,
        initialFirstVisibleItemScrollOffset = offset,
    )
    DisposableEffect(state) {
        onDispose { ScrollMemory.put(key, state.firstVisibleItemIndex, state.firstVisibleItemScrollOffset) }
    }
    return state
}

@Composable
fun rememberPersistentLazyGridState(key: String): LazyGridState {
    val (index, offset) = remember(key) { ScrollMemory.get(key) }
    val state = rememberLazyGridState(
        initialFirstVisibleItemIndex = index,
        initialFirstVisibleItemScrollOffset = offset,
    )
    DisposableEffect(state) {
        onDispose { ScrollMemory.put(key, state.firstVisibleItemIndex, state.firstVisibleItemScrollOffset) }
    }
    return state
}
