package com.duchamp.design

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp

/** Unified state chip vocabulary, mapped from [com.duchamp.data.tweak.TweakStatus] by callers. */
enum class StatusKind {
    ON,
    OFF,
    DIVERGED,
    UNAVAILABLE,
    ERROR,
}

private fun StatusKind.color(): Color = when (this) {
    StatusKind.ON -> DuchampColor.Ok
    StatusKind.OFF -> DuchampColor.TextSecondary
    StatusKind.DIVERGED -> DuchampColor.Warn
    StatusKind.UNAVAILABLE -> DuchampColor.TextTertiary
    StatusKind.ERROR -> DuchampColor.Hot
}

private fun StatusKind.label(): String = when (this) {
    StatusKind.ON -> "АКТИВНО"
    StatusKind.OFF -> "ВЫКЛ"
    StatusKind.DIVERGED -> "РАСХОЖДЕНИЕ"
    StatusKind.UNAVAILABLE -> "НЕДОСТУПНО"
    StatusKind.ERROR -> "ОШИБКА"
}

@Composable
fun StatusChip(
    kind: StatusKind,
    modifier: Modifier = Modifier,
) {
    Surface(
        shape = ChipShape,
        color = Color.Transparent,
        contentColor = color(),
        border = androidx.compose.foundation.border.BorderStroke(1.dp, color().copy(alpha = 0.4)),
        modifier = modifier,
    ) {
        Text(
            text = label(),
            style = MaterialTheme.typography.labelSmall,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
        )
    }
}

/** Small monospace section header, e.g. "ТЕМПЕРАТУРА · SOC". */
@Composable
fun SectionLabel(
    text: String,
    modifier: Modifier = Modifier,
) {
    Text(
        text = text,
        style = MonoLabel,
        modifier = modifier.padding(start = 4.dp, top = 8.dp, bottom = 4.dp),
    )
}

/** Tabular-figures monospace text used for all telemetry. */
@Composable
fun MonoText(
    text: String,
    modifier: Modifier = Modifier,
    style: androidx.compose.ui.text.TextStyle = monoStyle(),
) {
    Text(
        text = text,
        style = style,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
        modifier = modifier,
    )
}

/** A raised surface with a hairline border instead of a shadow. */
@Composable
fun DataCard(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    Surface(
        shape = CardShape,
        color = DuchampColor.Surface,
        contentColor = DuchampColor.TextPrimary,
        border = androidx.compose.foundation.border.BorderStroke(1.dp, DuchampColor.Hairline),
        modifier = modifier,
    ) {
        content()
    }
}

/** Compact status row: label on the left, chip on the right. */
@Composable
fun StatusRow(
    label: String,
    kind: StatusKind,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(label, style = MaterialTheme.typography.labelMedium, color = DuchampColor.TextSecondary)
        StatusChip(kind)
    }
}

@Composable
fun Pill(
    text: String,
    modifier: Modifier = Modifier,
) {
    Surface(
        shape = ChipShape,
        color = DuchampColor.SurfaceRaised,
        contentColor = DuchampColor.TextSecondary,
        border = androidx.compose.foundation.border.BorderStroke(1.dp, DuchampColor.Hairline),
        modifier = modifier,
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
        )
    }
}

@Composable
fun EmptyState(
    text: String,
    modifier: Modifier = Modifier,
) {
    Box(modifier = modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
        Text(text, style = MaterialTheme.typography.bodyMedium, color = DuchampColor.TextTertiary)
    }
}

@Composable
fun DotIndicator(
    color: Color,
    modifier: Modifier = Modifier,
) {
    Box(modifier = modifier.size(6.dp).border(1.dp, color, ChipShape), contentAlignment = Alignment.Center)
}
