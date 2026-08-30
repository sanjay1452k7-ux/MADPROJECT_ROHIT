package com.cit.attendance.feature.analytics

import android.content.Intent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.cit.attendance.core.designsystem.component.AppCard
import com.cit.attendance.core.designsystem.component.AppTopBar
import com.cit.attendance.core.designsystem.component.PrimaryButton
import com.cit.attendance.core.designsystem.component.RoleCard
import com.cit.attendance.core.designsystem.component.SecondaryButton
import com.cit.attendance.core.designsystem.component.SectionHeader
import com.cit.attendance.core.designsystem.theme.AppTheme
import com.cit.attendance.domain.model.ReportFormat
import com.cit.attendance.domain.model.ReportType
import java.io.File

/**
 * Report generation (Section 33).
 *
 * The file is rendered by the server and written to app storage, then handed to
 * whatever viewer the device has through a content URI - the app does not try
 * to render a PDF itself.
 */
@Composable
fun ReportsScreen(
    availableTypes: List<ReportType>,
    modifier: Modifier = Modifier,
    viewModel: ReportsViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val spacing = AppTheme.spacing

    LaunchedEffect(availableTypes) {
        viewModel.configureFor(availableTypes)
    }

    Column(modifier = modifier.fillMaxSize()) {
        AppTopBar(title = "Reports", subtitle = "Generate and share")

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = spacing.screenGutter),
            verticalArrangement = Arrangement.spacedBy(spacing.md),
        ) {
            SectionHeader(title = "Report type")

            state.availableTypes.forEach { type ->
                RoleCard(
                    title = type.label,
                    description = type.description,
                    icon = Icons.Outlined.Description,
                    selected = type == state.selectedType,
                    onClick = { viewModel.onTypeSelected(type) },
                )
            }

            SectionHeader(title = "Format", modifier = Modifier.padding(top = spacing.sm))

            Row(horizontalArrangement = Arrangement.spacedBy(spacing.md)) {
                ReportFormat.entries.forEach { format ->
                    FormatOption(
                        format = format,
                        selected = format == state.selectedFormat,
                        onClick = { viewModel.onFormatSelected(format) },
                        modifier = Modifier.weight(1f),
                    )
                }
            }

            PrimaryButton(
                text = "Generate report",
                onClick = viewModel::generate,
                loading = state.isGenerating,
                modifier = Modifier.padding(top = spacing.sm),
            )

            val error = state.error
            if (error != null) {
                AppCard {
                    Row(verticalAlignment = Alignment.Top) {
                        Icon(
                            imageVector = Icons.Outlined.ErrorOutline,
                            contentDescription = null,
                            tint = AppTheme.colors.error,
                            modifier = Modifier.size(18.dp),
                        )
                        Column(modifier = Modifier.padding(start = spacing.md)) {
                            Text(
                                text = error.title,
                                style = AppTheme.typography.titleMedium,
                                color = AppTheme.colors.textPrimary,
                            )
                            Text(
                                text = error.message,
                                style = AppTheme.typography.bodyMedium,
                                color = AppTheme.colors.textSecondary,
                            )
                        }
                    }
                }
            }

            val generated = state.lastGenerated
            if (generated != null) {
                AppCard {
                    Text(
                        text = "READY",
                        style = AppTheme.typography.overline,
                        color = AppTheme.colors.success,
                    )
                    Text(
                        text = generated.fileName,
                        style = AppTheme.typography.titleMedium,
                        color = AppTheme.colors.textPrimary,
                        modifier = Modifier.padding(top = spacing.xs),
                    )
                    Text(
                        text = "${generated.sizeBytes / 1024} KB - saved to the app's files",
                        style = AppTheme.typography.bodyMedium,
                        color = AppTheme.colors.textSecondary,
                    )
                    SecondaryButton(
                        text = "Open",
                        onClick = { openReport(context, generated.absolutePath, generated.format) },
                        modifier = Modifier.padding(top = spacing.md),
                    )
                }
            }

            Text(
                text = "Reports are produced by the attendance service from recorded " +
                    "attendance, so the PDF and the spreadsheet always agree.",
                style = AppTheme.typography.bodyMedium,
                color = AppTheme.colors.textTertiary,
                modifier = Modifier.padding(bottom = spacing.xxl),
            )
        }
    }
}

@Composable
private fun FormatOption(
    format: ReportFormat,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = AppTheme.colors

    AppCard(modifier = modifier, onClick = onClick, selected = selected) {
        Text(
            text = format.label,
            style = AppTheme.typography.titleMedium,
            color = if (selected) colors.accentBright else colors.textPrimary,
        )
        Text(
            text = ".${format.extension}",
            style = AppTheme.typography.bodyMedium,
            color = colors.textTertiary,
        )
    }
}

/**
 * Hands the file to another app.
 *
 * A FileProvider URI is used rather than a file:// path, which Android has
 * refused since API 24.
 */
private fun openReport(
    context: android.content.Context,
    path: String,
    format: ReportFormat,
) {
    val file = File(path)
    if (!file.exists()) return

    val uri = FileProvider.getUriForFile(
        context,
        "${context.packageName}.reports",
        file,
    )
    val mimeType = when (format) {
        ReportFormat.PDF -> "application/pdf"
        ReportFormat.XLSX ->
            "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"
    }

    val intent = Intent(Intent.ACTION_VIEW).apply {
        setDataAndType(uri, mimeType)
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    }

    // A device with no PDF or spreadsheet viewer is a normal situation, not a
    // crash: offer the chooser and let the user decide.
    runCatching { context.startActivity(Intent.createChooser(intent, "Open report")) }
}
