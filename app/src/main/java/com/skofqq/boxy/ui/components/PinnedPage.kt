package com.skofqq.boxy.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.skofqq.boxy.ui.theme.Boxy

/**
 * Page with a header that stays in place while the list below it scrolls
 * (title, back button and action buttons are always reachable).
 */
@Composable
fun PinnedLazyPage(
    contentPadding: PaddingValues,
    header: @Composable ColumnScope.() -> Unit,
    state: LazyListState = rememberLazyListState(),
    verticalArrangement: Arrangement.Vertical = Arrangement.Top,
    content: LazyListScope.() -> Unit,
) {
    Column(Modifier.fillMaxSize().padding(top = contentPadding.calculateTopPadding())) {
        Column(Modifier.fillMaxWidth().background(Boxy.colors.page).padding(bottom = 4.dp), content = header)
        LazyColumn(
            Modifier.fillMaxSize(),
            state = state,
            contentPadding = PaddingValues(top = 4.dp, bottom = contentPadding.calculateBottomPadding()),
            verticalArrangement = verticalArrangement,
            content = content,
        )
    }
}
