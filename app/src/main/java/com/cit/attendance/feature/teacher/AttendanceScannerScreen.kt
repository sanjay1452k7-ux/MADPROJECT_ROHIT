package com.cit.attendance.feature.teacher

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material.icons.outlined.NoPhotography
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.cit.attendance.core.designsystem.component.AppCard
import com.cit.attendance.core.designsystem.component.AppTopBar
import com.cit.attendance.core.designsystem.component.PrimaryButton
import com.cit.attendance.core.designsystem.component.SecondaryButton
import com.cit.attendance.core.designsystem.component.SeededAvatar
import com.cit.attendance.core.designsystem.theme.AppTheme
import com.cit.attendance.core.ui.formatPercentage
import com.cit.attendance.domain.model.VerifiedStudent

/**
 * The teacher's scanner (Sections 21 and 22).
 *
 * The camera is only ever a way to identify a student. Attendance is recorded
 * by the teacher pressing Mark present, after looking at the person in front of
 * them - never by the scan itself (Section 17).
 */
@Composable
fun AttendanceScannerScreen(
    viewModel: AttendanceSessionViewModel,
    onClose: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val context = LocalContext.current

    var hasCameraPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) ==
                PackageManager.PERMISSION_GRANTED,
        )
    }
    var permissionRequested by remember { mutableStateOf(false) }
    var cameraFailure by remember { mutableStateOf<String?>(null) }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
        onResult = { granted -> hasCameraPermission = granted },
    )

    LaunchedEffect(Unit) {
        if (!hasCameraPermission && !permissionRequested) {
            permissionRequested = true
            permissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    Column(modifier = modifier.fillMaxSize()) {
        AppTopBar(
            title = state.session?.subjectName ?: "Scan attendance",
            subtitle = state.session?.className,
        )

        if (!hasCameraPermission) {
            CameraPermissionDenied(
                onRetry = { permissionLauncher.launch(Manifest.permission.CAMERA) },
                onOpenSettings = { openAppSettings(context) },
                onClose = onClose,
            )
            return@Column
        }

        val failure = cameraFailure
        if (failure != null) {
            CameraUnavailable(
                message = failure,
                onRetry = { cameraFailure = null },
                onClose = onClose,
            )
            return@Column
        }

        Box(modifier = Modifier.fillMaxSize()) {
            when (val stage = state.stage) {
                ScanStage.Scanning, ScanStage.Validating -> ScannerSurface(
                    isValidating = stage == ScanStage.Validating,
                    presentCount = state.session?.presentCount ?: 0,
                    totalStudents = state.session?.totalStudents ?: 0,
                    onCodeScanned = viewModel::onCodeScanned,
                    onClose = onClose,
                    onCameraUnavailable = {
                        cameraFailure =
                            "The camera could not be started. It may be in use by another app."
                    },
                )

                is ScanStage.AwaitingConfirmation -> StudentVerificationCard(
                    student = stage.student,
                    isSubmitting = state.isSubmitting,
                    onConfirm = viewModel::confirmPresent,
                    onCancel = viewModel::resumeScanning,
                )

                is ScanStage.Recorded -> AttendanceRecordedPanel(
                    studentName = stage.result.studentName,
                    registerNumber = stage.result.registerNumber,
                    subjectName = stage.result.subjectName,
                    markedAtLabel = stage.result.markedAtLabel,
                    onContinue = viewModel::resumeScanning,
                )

                is ScanStage.Rejected -> ScanRejectedPanel(
                    title = stage.error.title,
                    message = stage.error.message,
                    onContinue = viewModel::resumeScanning,
                )
            }
        }
    }
}

@Composable
private fun ScannerSurface(
    isValidating: Boolean,
    presentCount: Int,
    totalStudents: Int,
    onCodeScanned: (String) -> Unit,
    onClose: () -> Unit,
    onCameraUnavailable: (Throwable) -> Unit,
) {
    val colors = AppTheme.colors

    Box(modifier = Modifier.fillMaxSize()) {
        QrScannerView(
            onCodeScanned = onCodeScanned,
            onCameraUnavailable = onCameraUnavailable,
            modifier = Modifier.fillMaxSize(),
        )

        // A dark wash keeps the frame legible over a bright classroom scene.
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(colors.background.copy(alpha = 0.55f)),
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(AppTheme.spacing.screenGutter),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(AppTheme.spacing.lg),
        ) {
            Text(
                text = "$presentCount of $totalStudents marked",
                style = AppTheme.typography.label,
                color = colors.textSecondary,
            )

            ScanningFrame(isValidating = isValidating)

            Text(
                text = if (isValidating) {
                    "Checking code..."
                } else {
                    "Align the QR code within the frame"
                },
                style = AppTheme.typography.bodyLarge,
                color = colors.textPrimary,
                textAlign = TextAlign.Center,
            )

            SecondaryButton(text = "End scanning", onClick = onClose)
        }
    }
}

/** The frame and its sweeping line: the only decoration on this screen. */
@Composable
private fun ScanningFrame(isValidating: Boolean) {
    val colors = AppTheme.colors
    val motion = AppTheme.motion

    val transition = rememberInfiniteTransition(label = "scanline")
    val sweep by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(2200, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "sweep",
    )

    Box(
        modifier = Modifier
            .fillMaxWidth(0.78f)
            .aspectRatio(1f)
            .clip(AppTheme.shapes.extraLarge)
            .border(
                width = 2.dp,
                color = if (isValidating) colors.accentBright else colors.outlineStrong,
                shape = AppTheme.shapes.extraLarge,
            ),
        contentAlignment = Alignment.Center,
    ) {
        if (isValidating) {
            CircularProgressIndicator(color = colors.accentBright)
        } else if (!motion.reduceMotion) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val y = size.height * sweep
                drawLine(
                    color = colors.accentBright,
                    start = androidx.compose.ui.geometry.Offset(0f, y),
                    end = androidx.compose.ui.geometry.Offset(size.width, y),
                    strokeWidth = 2.5f,
                )
            }
        }
    }
}

/**
 * The identity card the teacher checks against the person in front of them
 * (Section 22).
 */
@Composable
private fun StudentVerificationCard(
    student: VerifiedStudent,
    isSubmitting: Boolean,
    onConfirm: () -> Unit,
    onCancel: () -> Unit,
) {
    val colors = AppTheme.colors
    val spacing = AppTheme.spacing

    AnimatedVisibility(visible = true, enter = fadeIn() + scaleIn(initialScale = 0.96f)) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                // The panel replaces the camera without any navigation, so a
                // screen reader is told the student has been identified.
                .semantics { liveRegion = LiveRegionMode.Assertive }
                .verticalScroll(rememberScrollState())
                .padding(spacing.screenGutter),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(spacing.md),
        ) {
            Text(
                text = "STUDENT VERIFIED",
                style = AppTheme.typography.overline,
                color = colors.accentBright,
            )

            SeededAvatar(
                seed = student.avatarSeed,
                initials = student.initials,
                size = 96.dp,
                highlighted = true,
            )

            Text(
                text = student.name,
                style = AppTheme.typography.displayLarge,
                color = colors.textPrimary,
                textAlign = TextAlign.Center,
            )
            Text(
                text = student.registerNumber,
                style = AppTheme.typography.titleMedium,
                color = colors.textSecondary,
            )
            Text(
                text = "${student.department} - ${student.classLabel}",
                style = AppTheme.typography.bodyMedium,
                color = colors.textTertiary,
            )

            AppCard {
                DetailRow("Current attendance", formatPercentage(student.attendancePercentage))
                DetailRow("Session", student.subjectName)
                DetailRow("Status", "READY FOR VERIFICATION")
            }

            Text(
                text = "Check the student in front of you before recording attendance.",
                style = AppTheme.typography.bodyMedium,
                color = colors.textTertiary,
                textAlign = TextAlign.Center,
            )

            PrimaryButton(
                text = "Mark present",
                onClick = onConfirm,
                loading = isSubmitting,
            )
            SecondaryButton(text = "Cancel", onClick = onCancel, enabled = !isSubmitting)
        }
    }
}

@Composable
private fun DetailRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = AppTheme.spacing.xs),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(
            text = label,
            style = AppTheme.typography.bodyMedium,
            color = AppTheme.colors.textTertiary,
        )
        Text(
            text = value,
            style = AppTheme.typography.bodyLarge,
            color = AppTheme.colors.textPrimary,
        )
    }
}

/** The confirmation after recording (Section 23): clean, not celebratory. */
@Composable
private fun AttendanceRecordedPanel(
    studentName: String,
    registerNumber: String,
    subjectName: String,
    markedAtLabel: String,
    onContinue: () -> Unit,
) {
    val colors = AppTheme.colors

    AnimatedVisibility(visible = true, enter = fadeIn() + scaleIn(initialScale = 0.94f)) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .semantics { liveRegion = LiveRegionMode.Assertive }
                .padding(AppTheme.spacing.screenGutter),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(AppTheme.spacing.md),
        ) {
            Box(
                modifier = Modifier
                    .padding(top = AppTheme.spacing.xxl)
                    .size(76.dp)
                    .background(colors.successMuted, AppTheme.shapes.extraLarge),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = Icons.Outlined.CheckCircle,
                    contentDescription = null,
                    tint = colors.success,
                    modifier = Modifier.size(38.dp),
                )
            }

            Text(
                text = "ATTENDANCE MARKED",
                style = AppTheme.typography.overline,
                color = colors.success,
            )
            Text(
                text = studentName,
                style = AppTheme.typography.titleLarge,
                color = colors.textPrimary,
            )
            Text(
                text = registerNumber,
                style = AppTheme.typography.bodyLarge,
                color = colors.textSecondary,
            )
            Text(
                text = subjectName,
                style = AppTheme.typography.bodyMedium,
                color = colors.textTertiary,
            )
            Text(
                text = markedAtLabel,
                style = AppTheme.typography.bodyMedium,
                color = colors.textTertiary,
            )

            PrimaryButton(text = "Scan next student", onClick = onContinue)
        }
    }
}

/** A refused code: expired, invalid, reused, or already marked (Section 20). */
@Composable
private fun ScanRejectedPanel(
    title: String,
    message: String,
    onContinue: () -> Unit,
) {
    val colors = AppTheme.colors

    AnimatedVisibility(visible = true, enter = fadeIn(), exit = fadeOut()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                // A refused code must be announced, not just shown.
                .semantics { liveRegion = LiveRegionMode.Assertive }
                .padding(AppTheme.spacing.screenGutter),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(AppTheme.spacing.md),
        ) {
            Box(
                modifier = Modifier
                    .padding(top = AppTheme.spacing.xxl)
                    .size(76.dp)
                    .background(colors.errorMuted, AppTheme.shapes.extraLarge),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = Icons.Outlined.ErrorOutline,
                    contentDescription = null,
                    tint = colors.error,
                    modifier = Modifier.size(38.dp),
                )
            }
            Text(
                text = title,
                style = AppTheme.typography.titleLarge,
                color = colors.textPrimary,
                textAlign = TextAlign.Center,
            )
            Text(
                text = message,
                style = AppTheme.typography.bodyMedium,
                color = colors.textSecondary,
                textAlign = TextAlign.Center,
            )
            PrimaryButton(text = "Scan again", onClick = onContinue)
        }
    }
}

/** Camera permission is a normal state to handle, not an error (Section 37). */
@Composable
private fun CameraPermissionDenied(
    onRetry: () -> Unit,
    onOpenSettings: () -> Unit,
    onClose: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(AppTheme.spacing.screenGutter),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(AppTheme.spacing.md),
    ) {
        Icon(
            imageVector = Icons.Outlined.NoPhotography,
            contentDescription = null,
            tint = AppTheme.colors.textTertiary,
            modifier = Modifier
                .padding(top = AppTheme.spacing.xxl)
                .size(32.dp),
        )
        Text(
            text = "Camera access needed",
            style = AppTheme.typography.titleMedium,
            color = AppTheme.colors.textPrimary,
        )
        Text(
            text = "Scanning attendance codes needs the camera. You can still record attendance manually from the session screen.",
            style = AppTheme.typography.bodyMedium,
            color = AppTheme.colors.textSecondary,
            textAlign = TextAlign.Center,
        )
        PrimaryButton(text = "Allow camera", onClick = onRetry)
        SecondaryButton(text = "Open app settings", onClick = onOpenSettings)
        SecondaryButton(text = "Go back", onClick = onClose)
    }
}

/** Shown when the camera exists but will not start (Section 37). */
@Composable
private fun CameraUnavailable(
    message: String,
    onRetry: () -> Unit,
    onClose: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(AppTheme.spacing.screenGutter),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(AppTheme.spacing.md),
    ) {
        Icon(
            imageVector = Icons.Outlined.NoPhotography,
            contentDescription = null,
            tint = AppTheme.colors.error,
            modifier = Modifier
                .padding(top = AppTheme.spacing.xxl)
                .size(32.dp),
        )
        Text(
            text = "Camera unavailable",
            style = AppTheme.typography.titleMedium,
            color = AppTheme.colors.textPrimary,
        )
        Text(
            text = message,
            style = AppTheme.typography.bodyMedium,
            color = AppTheme.colors.textSecondary,
            textAlign = TextAlign.Center,
        )
        Text(
            text = "Attendance can still be recorded manually from the session screen.",
            style = AppTheme.typography.bodyMedium,
            color = AppTheme.colors.textTertiary,
            textAlign = TextAlign.Center,
        )
        PrimaryButton(text = "Try again", onClick = onRetry)
        SecondaryButton(text = "Go back", onClick = onClose)
    }
}

/** Opens this app's system settings page so a blocked permission can be granted. */
private fun openAppSettings(context: android.content.Context) {
    val intent = Intent(
        android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
        Uri.fromParts("package", context.packageName, null),
    ).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    runCatching { context.startActivity(intent) }
}
