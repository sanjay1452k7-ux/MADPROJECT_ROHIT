package com.cit.attendance.feature.shell

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AccountCircle
import androidx.compose.material.icons.outlined.Assessment
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.Class
import androidx.compose.material.icons.outlined.Dashboard
import androidx.compose.material.icons.outlined.Groups
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.QrCode2
import androidx.compose.material.icons.outlined.QrCodeScanner
import androidx.compose.material.icons.outlined.Insights
import androidx.compose.material.icons.outlined.Description
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.cit.attendance.core.designsystem.component.AppBottomBar
import com.cit.attendance.core.designsystem.component.BottomNavItem
import com.cit.attendance.core.designsystem.component.SegmentedTabs
import com.cit.attendance.core.designsystem.theme.AppTheme
import com.cit.attendance.core.navigation.AdminRoutes
import com.cit.attendance.domain.model.ReportType
import com.cit.attendance.core.navigation.StudentRoutes
import com.cit.attendance.core.navigation.TeacherRoutes
import com.cit.attendance.feature.admin.AdminOverviewScreen
import com.cit.attendance.feature.admin.AdminPeopleScreen
import com.cit.attendance.feature.admin.AdminProfileScreen
import com.cit.attendance.feature.admin.AuditTrailScreen
import com.cit.attendance.feature.admin.AdminViewModel
import com.cit.attendance.feature.analytics.AdminAnalyticsScreen
import com.cit.attendance.feature.analytics.ReportsScreen
import com.cit.attendance.feature.analytics.StudentAnalyticsScreen
import com.cit.attendance.feature.analytics.TeacherAnalyticsScreen
import com.cit.attendance.feature.student.StudentHistoryScreen
import com.cit.attendance.feature.student.StudentHomeScreen
import com.cit.attendance.feature.student.StudentNotificationsScreen
import com.cit.attendance.feature.student.StudentProfileScreen
import com.cit.attendance.feature.student.StudentQrScreen
import com.cit.attendance.feature.teacher.TeacherClassesScreen
import com.cit.attendance.feature.teacher.TeacherHomeScreen
import com.cit.attendance.feature.teacher.TeacherProfileScreen
import com.cit.attendance.feature.teacher.AttendanceScannerScreen
import com.cit.attendance.feature.teacher.AttendanceSessionViewModel
import com.cit.attendance.feature.teacher.TeacherLiveSessionScreen
import com.cit.attendance.feature.teacher.TeacherSessionLauncher
import com.cit.attendance.feature.teacher.TeacherViewModel

/**
 * The student shell (Section 14).
 *
 * Tab state is held here rather than in a nested NavHost: these are peer
 * destinations with no back-stack relationship, and the system back gesture
 * should leave the signed-in area rather than walk backwards through tabs.
 */
@Composable
fun StudentShell(
    modifier: Modifier = Modifier,
    shellViewModel: ShellViewModel = hiltViewModel(),
) {
    val unreadCount by shellViewModel.unreadCount.collectAsStateWithLifecycle()
    var selectedRoute by remember { mutableStateOf(StudentRoutes.HOME) }

    val items = listOf(
        BottomNavItem(StudentRoutes.HOME, "Home", Icons.Outlined.Dashboard),
        BottomNavItem(StudentRoutes.ATTENDANCE, "Attend", Icons.Outlined.QrCode2),
        BottomNavItem(StudentRoutes.ANALYTICS, "Insights", Icons.Outlined.Insights),
        BottomNavItem(
            route = StudentRoutes.NOTIFICATIONS,
            label = "Alerts",
            icon = Icons.Outlined.Notifications,
            badgeCount = unreadCount,
        ),
        BottomNavItem(StudentRoutes.PROFILE, "Profile", Icons.Outlined.AccountCircle),
    )

    ShellScaffold(
        modifier = modifier,
        items = items,
        selectedRoute = selectedRoute,
        onSelect = { route ->
            selectedRoute = route
            // Leaving the notifications tab may have cleared unread items.
            if (route != StudentRoutes.NOTIFICATIONS) shellViewModel.refreshUnreadCount()
        },
    ) {
        when (selectedRoute) {
            StudentRoutes.HOME -> StudentHomeScreen()
            StudentRoutes.ATTENDANCE -> StudentQrScreen()
            StudentRoutes.ANALYTICS -> StudentInsights()
            StudentRoutes.NOTIFICATIONS -> StudentNotificationsScreen()
            StudentRoutes.PROFILE -> StudentProfileScreen(onSignOut = shellViewModel::signOut)
        }
    }
}

/** The teacher shell (Section 15). */
@Composable
fun TeacherShell(
    modifier: Modifier = Modifier,
    shellViewModel: ShellViewModel = hiltViewModel(),
    teacherViewModel: TeacherViewModel = hiltViewModel(),
    sessionViewModel: AttendanceSessionViewModel = hiltViewModel(),
) {
    var selectedRoute by remember { mutableStateOf(TeacherRoutes.HOME) }

    val items = listOf(
        BottomNavItem(TeacherRoutes.HOME, "Today", Icons.Outlined.Dashboard),
        BottomNavItem(TeacherRoutes.ATTENDANCE, "Attend", Icons.Outlined.QrCodeScanner),
        BottomNavItem(TeacherRoutes.CLASSES, "Classes", Icons.Outlined.Class),
        BottomNavItem(TeacherRoutes.ANALYTICS, "Analytics", Icons.Outlined.Assessment),
        BottomNavItem(TeacherRoutes.PROFILE, "Profile", Icons.Outlined.AccountCircle),
    )

    ShellScaffold(
        modifier = modifier,
        items = items,
        selectedRoute = selectedRoute,
        onSelect = { selectedRoute = it },
    ) {
        when (selectedRoute) {
            TeacherRoutes.HOME -> TeacherHomeScreen(
                viewModel = teacherViewModel,
                onStartAttendance = { selectedRoute = TeacherRoutes.ATTENDANCE },
            )
            TeacherRoutes.ATTENDANCE -> TeacherAttendanceFlow(
                teacherViewModel = teacherViewModel,
                sessionViewModel = sessionViewModel,
            )
            TeacherRoutes.CLASSES -> TeacherClassesScreen(viewModel = teacherViewModel)
            TeacherRoutes.ANALYTICS -> TeacherInsights()
            TeacherRoutes.PROFILE -> TeacherProfileScreen(
                viewModel = teacherViewModel,
                onSignOut = shellViewModel::signOut,
            )
        }
    }
}

/** The administrator shell (Section 16). */
@Composable
fun AdminShell(
    modifier: Modifier = Modifier,
    shellViewModel: ShellViewModel = hiltViewModel(),
    adminViewModel: AdminViewModel = hiltViewModel(),
) {
    var selectedRoute by remember { mutableStateOf(AdminRoutes.OVERVIEW) }

    val items = listOf(
        BottomNavItem(AdminRoutes.OVERVIEW, "Overview", Icons.Outlined.CalendarMonth),
        BottomNavItem(AdminRoutes.PEOPLE, "People", Icons.Outlined.Groups),
        BottomNavItem(AdminRoutes.ANALYTICS, "Insights", Icons.Outlined.Insights),
        BottomNavItem(AdminRoutes.PROFILE, "Profile", Icons.Outlined.AccountCircle),
    )

    ShellScaffold(
        modifier = modifier,
        items = items,
        selectedRoute = selectedRoute,
        onSelect = { selectedRoute = it },
    ) {
        when (selectedRoute) {
            AdminRoutes.OVERVIEW -> AdminOverviewScreen(viewModel = adminViewModel)
            AdminRoutes.PEOPLE -> AdminPeopleScreen(viewModel = adminViewModel)
            AdminRoutes.ANALYTICS -> AdminInsights()
            AdminRoutes.PROFILE -> AdminProfileScreen(
                viewModel = adminViewModel,
                onSignOut = shellViewModel::signOut,
            )
        }
    }
}

/** Shared chrome: status-bar inset, content area, bottom bar. */
@Composable
private fun ShellScaffold(
    items: List<BottomNavItem>,
    selectedRoute: String,
    onSelect: (String) -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(AppTheme.colors.background)
            .windowInsetsPadding(WindowInsets.statusBars),
    ) {
        Column(modifier = Modifier.weight(1f)) {
            content()
        }
        AppBottomBar(
            items = items,
            selectedRoute = selectedRoute,
            onSelect = onSelect,
        )
    }
}

/** Where the teacher is inside the attendance tab. */
private enum class AttendanceStep { CHOOSE, LIVE, SCAN }

/**
 * The attendance tab's own small flow (Section 17).
 *
 * Kept as local state rather than nested navigation destinations: these are
 * steps of one task, and backing out of a scan should return to the session,
 * not unwind the whole tab.
 */
@Composable
private fun TeacherAttendanceFlow(
    teacherViewModel: TeacherViewModel,
    sessionViewModel: AttendanceSessionViewModel,
) {
    var step by remember { mutableStateOf(AttendanceStep.CHOOSE) }

    when (step) {
        AttendanceStep.CHOOSE -> TeacherSessionLauncher(
            teacherViewModel = teacherViewModel,
            sessionViewModel = sessionViewModel,
            onSessionStarted = { step = AttendanceStep.LIVE },
        )

        AttendanceStep.LIVE -> TeacherLiveSessionScreen(
            sessionViewModel = sessionViewModel,
            onScan = { step = AttendanceStep.SCAN },
            onClosed = { step = AttendanceStep.CHOOSE },
        )

        AttendanceStep.SCAN -> AttendanceScannerScreen(
            viewModel = sessionViewModel,
            onClose = {
                sessionViewModel.refreshSession()
                step = AttendanceStep.LIVE
            },
        )
    }
}

/** The report types staff may request. Mirrored by the server (Section 34). */
private val staffReportTypes = listOf(
    ReportType.CLASS_ATTENDANCE,
    ReportType.SUBJECT_ATTENDANCE,
    ReportType.LOW_ATTENDANCE,
    ReportType.ABSENTEE,
)

/**
 * The student's Insights tab.
 *
 * Analytics, the existing history list and reports are siblings here rather
 * than three separate bottom-bar destinations.
 */
@Composable
private fun StudentInsights() {
    var segment by remember { mutableStateOf(0) }

    Column(modifier = Modifier.fillMaxSize()) {
        SegmentedTabs(
            options = listOf("Analytics", "History", "Reports"),
            selectedIndex = segment,
            onSelect = { segment = it },
            modifier = Modifier.padding(
                horizontal = AppTheme.spacing.screenGutter,
                vertical = AppTheme.spacing.sm,
            ),
        )
        when (segment) {
            0 -> StudentAnalyticsScreen()
            1 -> StudentHistoryScreen()
            else -> ReportsScreen(availableTypes = listOf(ReportType.STUDENT_ATTENDANCE))
        }
    }
}

/** The teacher's Analytics tab: cohort analytics and report generation. */
@Composable
private fun TeacherInsights() {
    var segment by remember { mutableStateOf(0) }

    Column(modifier = Modifier.fillMaxSize()) {
        SegmentedTabs(
            options = listOf("Analytics", "Reports"),
            selectedIndex = segment,
            onSelect = { segment = it },
            modifier = Modifier.padding(
                horizontal = AppTheme.spacing.screenGutter,
                vertical = AppTheme.spacing.sm,
            ),
        )
        if (segment == 0) {
            TeacherAnalyticsScreen()
        } else {
            ReportsScreen(availableTypes = staffReportTypes)
        }
    }
}

/** The administrator's Insights tab. */
@Composable
private fun AdminInsights() {
    var segment by remember { mutableStateOf(0) }

    Column(modifier = Modifier.fillMaxSize()) {
        SegmentedTabs(
            options = listOf("Analytics", "Reports", "Activity"),
            selectedIndex = segment,
            onSelect = { segment = it },
            modifier = Modifier.padding(
                horizontal = AppTheme.spacing.screenGutter,
                vertical = AppTheme.spacing.sm,
            ),
        )
        when (segment) {
            0 -> AdminAnalyticsScreen()
            1 -> ReportsScreen(availableTypes = staffReportTypes)
            // The audit trail: who did what, readable only by an administrator.
            else -> AuditTrailScreen()
        }
    }
}
