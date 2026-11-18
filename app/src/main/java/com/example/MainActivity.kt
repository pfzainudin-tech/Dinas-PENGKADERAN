package com.example

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Assessment
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Assessment
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.credentials.CredentialManager
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.data.DinasKaderRepository
import com.example.data.authStateFlow
import com.example.data.signOutGoogle
import com.example.model.ScheduleItem
import com.example.model.UiState
import com.example.notifications.ScheduleNotificationHelper
import com.example.ui.components.DinasKaderTopBar
import com.example.ui.screens.AuthScreen
import com.example.ui.screens.BerandaScreen
import com.example.ui.screens.DeleteScheduleConfirmDialog
import com.example.ui.screens.JadwalScreen
import com.example.ui.screens.NotificationsBottomSheet
import com.example.ui.screens.ProfilScreen
import com.example.ui.screens.RekapitulasiScreen
import com.example.ui.screens.ScheduleDetailBottomSheet
import com.example.ui.screens.ScheduleEditorDialog
import com.example.ui.theme.DinasKaderTheme
import com.example.viewmodel.DinasKaderViewModel
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FirebaseFirestore

private enum class AppTab(
    val route: String,
    val label: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector
) {
    BERANDA("beranda", "Beranda", Icons.Filled.Home, Icons.Outlined.Home),
    JADWAL("jadwal", "Jadwal", Icons.Filled.CalendarMonth, Icons.Outlined.CalendarMonth),
    REKAP("rekap", "Rekap", Icons.Filled.Assessment, Icons.Outlined.Assessment),
    PROFIL("profil", "Profil", Icons.Filled.Person, Icons.Outlined.Person)
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            DinasKaderTheme {
                AppNavigation()
            }
        }
    }
}

@Composable
fun AppNavigation() {
    val auth = remember { Firebase.auth }
    val currentUser by auth.authStateFlow().collectAsStateWithLifecycle(initialValue = auth.currentUser)
    val user = currentUser

    if (user == null) {
        AuthScreen(onAuthSuccess = { })
    } else {
        key(user.uid) {
            AuthenticatedDinasKaderApp(
                currentUserId = user.uid,
                defaultDisplayName = user.displayName ?: "Anggota Kader",
                defaultEmail = user.email
            )
        }
    }
}

@Composable
private fun AuthenticatedDinasKaderApp(
    currentUserId: String,
    defaultDisplayName: String,
    defaultEmail: String?
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val credentialManager = remember(context) { CredentialManager.create(context) }

    val viewModel: DinasKaderViewModel = viewModel(
        key = currentUserId,
        factory = viewModelFactory {
            initializer {
                val app = checkNotNull(this[APPLICATION_KEY])
                val databaseId = app.getString(R.string.firestore_database_id)
                val db = FirebaseFirestore.getInstance(databaseId)
                DinasKaderViewModel(
                    repository = DinasKaderRepository(db),
                    currentUserId = currentUserId,
                    defaultDisplayName = defaultDisplayName,
                    defaultEmail = defaultEmail,
                    notificationHelper = ScheduleNotificationHelper(app.applicationContext)
                )
            }
        }
    )

    // Request POST_NOTIFICATIONS runtime permission on Android 13+ for automatic notifications
    val notifPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
        onResult = { }
    )
    LaunchedEffect(Unit) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val granted = ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
            if (!granted) {
                notifPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    }

    val schedulesState by viewModel.schedulesState.collectAsStateWithLifecycle()
    val profileState by viewModel.userProfileState.collectAsStateWithLifecycle()
    val participations by viewModel.userParticipationsState.collectAsStateWithLifecycle()
    val isAdminActive by viewModel.isAdminActive.collectAsStateWithLifecycle()
    val selectedScheduleId by viewModel.selectedScheduleId.collectAsStateWithLifecycle()
    val selectedAttendancesState by viewModel.selectedScheduleAttendancesState.collectAsStateWithLifecycle()
    val notifications by viewModel.notifications.collectAsStateWithLifecycle()
    val isMutating by viewModel.isMutating.collectAsStateWithLifecycle()

    val snackbarHostState = remember { SnackbarHostState() }
    LaunchedEffect(viewModel) {
        viewModel.feedbackMessage.collect { msg ->
            snackbarHostState.showSnackbar(msg)
        }
    }

    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route ?: AppTab.BERANDA.route

    var showNotificationsSheet by remember { mutableStateOf(false) }
    var showEditorDialog by remember { mutableStateOf(false) }
    var editingSchedule by remember { mutableStateOf<ScheduleItem?>(null) }
    var deletingSchedule by remember { mutableStateOf<ScheduleItem?>(null) }

    val allSchedules = (schedulesState as? UiState.Success)?.data.orEmpty()
    val selectedSchedule = remember(allSchedules, selectedScheduleId) {
        allSchedules.firstOrNull { it.id == selectedScheduleId }
    }
    val memberName = (profileState as? UiState.Success)?.data?.fullName
        ?.takeIf { it.isNotBlank() } ?: defaultDisplayName

    if (selectedSchedule != null) {
        ScheduleDetailBottomSheet(
            schedule = selectedSchedule,
            myAttendance = participations[selectedSchedule.id],
            attendancesState = selectedAttendancesState,
            isAdminActive = isAdminActive,
            isMutating = isMutating,
            onDismiss = { viewModel.selectScheduleForDetail(null) },
            onAttendanceChanged = { status -> viewModel.setAttendance(selectedSchedule.id, status) },
            onAdminCompletedToggle = { completed ->
                viewModel.toggleScheduleCompleted(selectedSchedule.id, completed)
            },
            onAdminUpdateDescription = { desc ->
                viewModel.updateDescription(selectedSchedule.id, desc)
            },
            onAdminEditFull = {
                editingSchedule = selectedSchedule
                viewModel.selectScheduleForDetail(null)
                showEditorDialog = true
            },
            onAdminDelete = {
                deletingSchedule = selectedSchedule
                viewModel.selectScheduleForDetail(null)
            }
        )
    }

    if (showEditorDialog) {
        ScheduleEditorDialog(
            editingSchedule = editingSchedule,
            isMutating = isMutating,
            onDismiss = {
                showEditorDialog = false
                editingSchedule = null
            },
            onSave = { title, programType, executorLevel, executorName, location, description, startDate, endDate, timeText, completed ->
                viewModel.saveSchedule(
                    editingId = editingSchedule?.id,
                    title = title,
                    programType = programType,
                    executorLevel = executorLevel,
                    executorName = executorName,
                    location = location,
                    description = description,
                    startDate = startDate,
                    endDate = endDate,
                    timeText = timeText,
                    completed = completed,
                    onSuccess = {
                        showEditorDialog = false
                        editingSchedule = null
                    }
                )
            }
        )
    }

    deletingSchedule?.let { target ->
        DeleteScheduleConfirmDialog(
            schedule = target,
            isMutating = isMutating,
            onDismiss = { deletingSchedule = null },
            onConfirmDelete = {
                viewModel.deleteSchedule(target.id) {
                    deletingSchedule = null
                }
            }
        )
    }

    if (showNotificationsSheet) {
        NotificationsBottomSheet(
            notifications = notifications,
            onDismiss = { showNotificationsSheet = false },
            onMarkAllRead = { viewModel.markAllNotificationsRead() },
            onSelectScheduleId = { id ->
                viewModel.selectScheduleForDetail(id)
            }
        )
    }

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val useNavRail = maxWidth >= 600.dp

        Scaffold(
            contentWindowInsets = WindowInsets(0, 0, 0, 0),
            snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
            topBar = {
                DinasKaderTopBar(
                    isAdminActive = isAdminActive,
                    unreadNotificationCount = notifications.count { it.unread },
                    onNotificationsClick = { showNotificationsSheet = true },
                    onAdminBadgeClick = {
                        navController.navigate(AppTab.PROFIL.route) {
                            launchSingleTop = true
                            restoreState = true
                        }
                    }
                )
            },
            floatingActionButton = {
                if (isAdminActive && (currentRoute == AppTab.JADWAL.route || currentRoute == AppTab.BERANDA.route)) {
                    ExtendedFloatingActionButton(
                        onClick = {
                            editingSchedule = null
                            showEditorDialog = true
                        },
                        icon = { Icon(Icons.Outlined.Add, contentDescription = null) },
                        text = { Text("Tambah Jadwal") },
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary,
                        modifier = Modifier.testTag("admin_add_schedule_fab")
                    )
                }
            },
            bottomBar = {
                if (!useNavRail) {
                    NavigationBar {
                        AppTab.entries.forEach { tab ->
                            val selected = currentRoute == tab.route
                            NavigationBarItem(
                                selected = selected,
                                onClick = {
                                    navController.navigate(tab.route) {
                                        popUpTo(AppTab.BERANDA.route) { saveState = true }
                                        launchSingleTop = true
                                        restoreState = true
                                    }
                                },
                                icon = {
                                    Icon(
                                        imageVector = if (selected) tab.selectedIcon else tab.unselectedIcon,
                                        contentDescription = tab.label
                                    )
                                },
                                label = { Text(tab.label) },
                                modifier = Modifier.testTag("nav_${tab.route}")
                            )
                        }
                    }
                }
            }
        ) { innerPadding ->
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                if (useNavRail) {
                    NavigationRail(
                        modifier = Modifier.navigationBarsPadding()
                    ) {
                        AppTab.entries.forEach { tab ->
                            val selected = currentRoute == tab.route
                            NavigationRailItem(
                                selected = selected,
                                onClick = {
                                    navController.navigate(tab.route) {
                                        popUpTo(AppTab.BERANDA.route) { saveState = true }
                                        launchSingleTop = true
                                        restoreState = true
                                    }
                                },
                                icon = {
                                    Icon(
                                        imageVector = if (selected) tab.selectedIcon else tab.unselectedIcon,
                                        contentDescription = tab.label
                                    )
                                },
                                label = { Text(tab.label) },
                                modifier = Modifier.testTag("rail_${tab.route}")
                            )
                        }
                    }
                }

                NavHost(
                    navController = navController,
                    startDestination = AppTab.BERANDA.route,
                    modifier = Modifier.weight(1f)
                ) {
                    composable(AppTab.BERANDA.route) {
                        BerandaScreen(
                            schedulesState = schedulesState,
                            participations = participations,
                            isAdminActive = isAdminActive,
                            memberName = memberName,
                            onOpenScheduleDetail = { viewModel.selectScheduleForDetail(it.id) },
                            onOpenAllSchedules = {
                                navController.navigate(AppTab.JADWAL.route) {
                                    popUpTo(AppTab.BERANDA.route) { saveState = true }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            },
                            onAttendanceChanged = { id, status -> viewModel.setAttendance(id, status) },
                            onAdminCompletedToggle = { id, completed ->
                                viewModel.toggleScheduleCompleted(id, completed)
                            },
                            onAdminEditClick = {
                                editingSchedule = it
                                showEditorDialog = true
                            },
                            onAdminDeleteClick = { deletingSchedule = it }
                        )
                    }

                    composable(AppTab.JADWAL.route) {
                        JadwalScreen(
                            schedulesState = schedulesState,
                            participations = participations,
                            isAdminActive = isAdminActive,
                            onOpenScheduleDetail = { viewModel.selectScheduleForDetail(it.id) },
                            onAttendanceChanged = { id, status -> viewModel.setAttendance(id, status) },
                            onAdminCompletedToggle = { id, completed ->
                                viewModel.toggleScheduleCompleted(id, completed)
                            },
                            onAdminEditClick = {
                                editingSchedule = it
                                showEditorDialog = true
                            },
                            onAdminDeleteClick = { deletingSchedule = it }
                        )
                    }

                    composable(AppTab.REKAP.route) {
                        RekapitulasiScreen(
                            schedulesState = schedulesState,
                            onOpenScheduleDetail = { viewModel.selectScheduleForDetail(it.id) }
                        )
                    }

                    composable(AppTab.PROFIL.route) {
                        ProfilScreen(
                            userProfileState = profileState,
                            schedulesState = schedulesState,
                            participations = participations,
                            isAdminActive = isAdminActive,
                            isMutating = isMutating,
                            fallbackName = defaultDisplayName,
                            fallbackEmail = defaultEmail,
                            onUpdateProfile = { name, unit, onDone ->
                                viewModel.updateProfile(name, unit, onDone)
                            },
                            onLoginAdmin = { user, pass, onSuccess, onError ->
                                viewModel.loginAdmin(user, pass, onSuccess, onError)
                            },
                            onLogoutAdmin = { viewModel.logoutAdmin() },
                            onSignOutGoogle = {
                                signOutGoogle(credentialManager, scope)
                            },
                            onOpenScheduleDetail = { viewModel.selectScheduleForDetail(it.id) }
                        )
                    }
                }
            }
        }
    }
}
