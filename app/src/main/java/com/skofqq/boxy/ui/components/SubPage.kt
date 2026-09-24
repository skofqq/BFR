package com.skofqq.boxy.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.skofqq.boxy.R
import com.skofqq.boxy.ui.theme.Boxy
import com.skofqq.boxy.ui.theme.BoxyIcons

/** Compact "← Back" pill used on pages opened from another page. */
@Composable
fun BackPill(onClick: () -> Unit) {
    Row(
        Modifier
            .padding(start = 12.dp, top = 8.dp)
            .clip(RoundedCornerShape(50))
            .background(Boxy.colors.surface2)
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(BoxyIcons.ArrowBack, null, Modifier.size(18.dp), tint = Boxy.colors.text)
        Spacer(Modifier.width(6.dp))
        Text(stringResource(R.string.action_back), style = MaterialTheme.typography.labelLarge, color = Boxy.colors.text)
    }
}

/** Back pill, then the page title with actions on the same row. */
@Composable
fun SubPageHeader(title: String, subtitle: String?, onBack: (() -> Unit)?, actions: @Composable RowScope.() -> Unit = {}) {
    Column {
        if (onBack != null) BackPill(onBack)
        Row(Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.headlineLarge, color = Boxy.colors.text, maxLines = 2, overflow = TextOverflow.Ellipsis)
                if (!subtitle.isNullOrEmpty()) {
                    Spacer(Modifier.height(6.dp))
                    Text(subtitle, style = MaterialTheme.typography.bodyLarge, color = Boxy.colors.text2, maxLines = 2, overflow = TextOverflow.Ellipsis)
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically, content = actions)
        }
    }
}

/** Single-line text input in the BFR rounded style. */
@Composable
fun BoxyTextField(
    value: String,
    onChange: (String) -> Unit,
    hint: String,
    modifier: Modifier = Modifier,
    singleLine: Boolean = true,
    keyboardType: KeyboardType = KeyboardType.Text,
    leading: (@Composable () -> Unit)? = null,
    trailing: (@Composable () -> Unit)? = null,
) {
    Row(
        modifier.clip(RoundedCornerShape(16.dp)).background(Boxy.colors.surface2).padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (leading != null) {
            leading()
            Spacer(Modifier.width(10.dp))
        }
        Box(Modifier.weight(1f)) {
            if (value.isEmpty()) Text(hint, color = Boxy.colors.text2, maxLines = 1, overflow = TextOverflow.Ellipsis)
            BasicTextField(
                value,
                onChange,
                singleLine = singleLine,
                textStyle = MaterialTheme.typography.bodyLarge.copy(color = Boxy.colors.text),
                cursorBrush = SolidColor(Boxy.colors.accent),
                keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
                modifier = Modifier.fillMaxWidth(),
            )
        }
        if (trailing != null) {
            Spacer(Modifier.width(8.dp))
            trailing()
        }
    }
}

/**
 * Editable list of strings (SSIDs, URLs, file names). Each row has a delete button;
 * an optional pick button on the row start (scan Wi‑Fi / hotspot clients).
 */
@Composable
fun StringListEditor(
    items: List<String>,
    onChange: (List<String>) -> Unit,
    hint: String,
    onPick: ((Int) -> Unit)? = null,
    pickIcon: androidx.compose.ui.graphics.vector.ImageVector? = null,
) {
    Column(Modifier.fillMaxWidth().padding(horizontal = 18.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        items.forEachIndexed { i, value ->
            BoxyTextField(
                value = value,
                onChange = { v -> onChange(items.toMutableList().also { it[i] = v }) },
                hint = hint,
                modifier = Modifier.fillMaxWidth(),
                leading = if (onPick != null && pickIcon != null) {
                    { Icon(pickIcon, null, Modifier.size(22.dp).clickable { onPick(i) }, tint = Boxy.colors.accent) }
                } else {
                    null
                },
                trailing = {
                    Icon(
                        BoxyIcons.Close,
                        stringResource(R.string.action_delete),
                        Modifier.size(20.dp).clickable { onChange(items.toMutableList().also { it.removeAt(i) }) },
                        tint = Boxy.colors.text2,
                    )
                },
            )
        }
        Row(
            Modifier.clip(RoundedCornerShape(50)).clickable { onChange(items + "") }.padding(horizontal = 10.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(BoxyIcons.Add, null, Modifier.size(20.dp), tint = Boxy.colors.accent)
            Spacer(Modifier.width(6.dp))
            Text(stringResource(R.string.list_append_item), color = Boxy.colors.accent, style = MaterialTheme.typography.labelLarge)
        }
    }
}
