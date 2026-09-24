package com.skofqq.boxy.ui.screens

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.skofqq.boxy.R
import com.skofqq.boxy.ui.components.PageHeader
import com.skofqq.boxy.ui.components.SectionCard
import com.skofqq.boxy.ui.theme.Boxy

@Composable
fun LogsScreen(contentPadding: PaddingValues, header: @Composable () -> Unit = {}) =
    Placeholder(contentPadding, header, R.string.logs_title, R.string.logs_subtitle)

@Composable
private fun Placeholder(contentPadding: PaddingValues, header: @Composable () -> Unit, title: Int, subtitle: Int) {
    LazyColumn(Modifier.fillMaxSize(), contentPadding = contentPadding) {
        item { header() }
        item { PageHeader(stringResource(title), stringResource(subtitle)) }
        item {
            SectionCard(null) {
                Box(Modifier.fillMaxWidth().padding(24.dp), contentAlignment = Alignment.Center) {
                    Text(stringResource(R.string.coming_soon), style = MaterialTheme.typography.bodyLarge, color = Boxy.colors.text2)
                }
            }
        }
    }
}
