package com.example.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Logout
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material.icons.outlined.Badge
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material.icons.outlined.VisibilityOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.AttendanceStatus
import com.example.model.ProgramType
import com.example.model.ScheduleItem
import com.example.model.UiState
import com.example.model.UserProfile
import com.example.ui.components.formatIndonesianDate
import com.example.ui.theme.JetBrainsMonoFamily
import com.example.ui.theme.LakmudAccent
import com.example.ui.theme.MakestaAccent
import com.example.ui.theme.StatusDoneGreen

@Composable
fun ProfilScreen(
    userProfileState: UiState<UserProfile?>,
    schedulesState: UiState<List<ScheduleItem>>,
    participations: Map<String, AttendanceStatus>,
    isAdminActive: Boolean,
    isMutating: Boolean,
    fallbackName: String,
    fallbackEmail: String?,
    onUpdateProfile: (fullName: String, unitOrigin: String, onDone: () -> Unit) -> Unit,
    onLoginAdmin: (username: String, password: String, onSuccess: () -> Unit, onError: (String) -> Unit) -> Unit,
    onLogoutAdmin: () -> Unit,
    onSignOutGoogle: () -> Unit,
    onOpenScheduleDetail: (ScheduleItem) -> Unit,
    modifier: Modifier = Modifier
) {
    var showEditProfileDialog by remember { mutableStateOf(false) }
    var showAdminLoginDialog by remember { mutableStateOf(false) }

    val profile = (userProfileState as? UiState.Success)?.data
    val fullName = profile?.fullName?.takeIf { it.isNotBlank() } ?: fallbackName.ifBlank { "Anggota Kader" }
    val unitOrigin = profile?.unitOrigin.orEmpty()
    val email = profile?.email?.takeIf { it.isNotBlank() } ?: fallbackEmail.orEmpty()

    val schedules = (schedulesState as? UiState.Success)?.data.orEmpty()
    val participatedSchedules = remember(schedules, participations) {
        schedules.filter { participations[it.id] == AttendanceStatus.IKUT }
    }
    val makestaParticipations = remember(participatedSchedules) {
        participatedSchedules.filter { it.parsedProgramType == ProgramType.MAKESTA }
    }
    val lakmudParticipations = remember(participatedSchedules) {
        participatedSchedules.filter { it.parsedProgramType == ProgramType.LAKMUD }
    }

    if (showEditProfileDialog) {
        EditProfileDialog(
            initialName = fullName,
            initialUnit = unitOrigin,
            isMutating = isMutating,
            onDismiss = { showEditProfileDialog = false },
            onSave = { name, unit ->
                onUpdateProfile(name, unit) {
                    showEditProfileDialog = false
                }
            }
        )
    }

    if (showAdminLoginDialog) {
        AdminLoginDialog(
            isMutating = isMutating,
            onDismiss = { showAdminLoginDialog = false },
            onSubmit = { user, pass, onError ->
                onLoginAdmin(user, pass, { showAdminLoginDialog = false }, onError)
            }
        )
    }

    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.TopCenter
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .widthIn(max = 720.dp),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // 1. Nama Lengkap Card
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = MaterialTheme.shapes.large,
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceContainerLowest
                    ),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(18.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f)
                        ) {
                            Surface(
                                modifier = Modifier
                                    .size(56.dp)
                                    .clip(CircleShape),
                                color = MaterialTheme.colorScheme.primaryContainer
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = Icons.Outlined.Person,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                        modifier = Modifier.size(28.dp)
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.width(14.dp))
                            Column {
                                Text(
                                    text = "Nama Lengkap",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.primary,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = fullName,
                                    style = MaterialTheme.typography.titleLarge,
                                    modifier = Modifier.testTag("profil_full_name")
                                )
                                if (unitOrigin.isNotBlank()) {
                                    Text(
                                        text = unitOrigin,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                if (email.isNotBlank()) {
                                    Text(
                                        text = email,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }

                        IconButton(
                            onClick = { showEditProfileDialog = true },
                            modifier = Modifier
                                .minimumInteractiveComponentSize()
                                .testTag("profil_edit_button")
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Edit,
                                contentDescription = "Ubah Nama Lengkap",
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }
            }

            // 2 & 3. Jumlah Partisipasi Pengawalan Pengkaderan MAKESTA & LAKMUD
            item {
                Text(
                    text = "Partisipasi Pengawalan Pengkaderan",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }

            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    ParticipationCountCard(
                        title = "Pengawalan MAKESTA",
                        total = makestaParticipations.size,
                        completedCount = makestaParticipations.count { it.completed },
                        accentColor = MakestaAccent,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("profil_makesta_count")
                    )
                    ParticipationCountCard(
                        title = "Pengawalan LAKMUD",
                        total = lakmudParticipations.size,
                        completedCount = lakmudParticipations.count { it.completed },
                        accentColor = LakmudAccent,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("profil_lakmud_count")
                    )
                }
            }

            // Admin Security Card
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = MaterialTheme.shapes.large,
                    colors = CardDefaults.cardColors(
                        containerColor = if (isAdminActive)
                            MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.55f)
                        else MaterialTheme.colorScheme.surfaceContainerLow
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Icon(
                                imageVector = if (isAdminActive) Icons.Filled.AdminPanelSettings else Icons.Outlined.Lock,
                                contentDescription = null,
                                tint = if (isAdminActive) MaterialTheme.colorScheme.secondary
                                else MaterialTheme.colorScheme.primary
                            )
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Admin Security",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = if (isAdminActive) {
                                        "Mode Admin aktif. Fitur tambah, edit, hapus jadwal, ubah deskripsi, dan ceklis selesai telah diaktifkan."
                                    } else {
                                        "Masuk sebagai Admin untuk mengaktifkan fitur kelola jadwal, ubah deskripsi, dan tandai jadwal selesai."
                                    },
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        if (isAdminActive) {
                            OutlinedButton(
                                onClick = onLogoutAdmin,
                                enabled = !isMutating,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .minimumInteractiveComponentSize()
                                    .testTag("admin_logout_button")
                            ) {
                                Text("Keluar Mode Admin")
                            }
                        } else {
                            Button(
                                onClick = { showAdminLoginDialog = true },
                                enabled = !isMutating,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .minimumInteractiveComponentSize()
                                    .testTag("open_admin_login_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.Badge,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Log-in sebagai Admin")
                            }
                        }
                    }
                }
            }

            // Riwayat Pengawalan Saya
            item {
                Text(
                    text = "Riwayat Pengawalan Saya (IKUT)",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }

            if (participatedSchedules.isEmpty()) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = MaterialTheme.shapes.large,
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceContainerLow
                        )
                    ) {
                        Text(
                            text = "Anda belum mencentang IKUT pada jadwal pengkaderan mana pun.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(16.dp)
                        )
                    }
                }
            } else {
                items(participatedSchedules, key = { "part_${it.id}" }) { item ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onOpenScheduleDetail(item) },
                        shape = MaterialTheme.shapes.medium,
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceContainerLowest
                        )
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(14.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = item.title,
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "${item.parsedProgramType.label} • ${formatIndonesianDate(item.startDate)}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Surface(
                                shape = RoundedCornerShape(50),
                                color = if (item.completed) StatusDoneGreen.copy(alpha = 0.14f)
                                else MaterialTheme.colorScheme.primaryContainer
                            ) {
                                Text(
                                    text = if (item.completed) "Selesai" else "Terdaftar IKUT",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = if (item.completed) StatusDoneGreen
                                    else MaterialTheme.colorScheme.onPrimaryContainer,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                )
                            }
                        }
                    }
                }
            }

            // Sign Out Google Account
            item {
                OutlinedButton(
                    onClick = onSignOutGoogle,
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = MaterialTheme.colorScheme.error
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .minimumInteractiveComponentSize()
                        .testTag("sign_out_google_button")
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Outlined.Logout,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Keluar Akun Google")
                }
            }

            item { Spacer(modifier = Modifier.height(16.dp)) }
        }
    }
}

@Composable
private fun ParticipationCountCard(
    title: String,
    total: Int,
    completedCount: Int,
    accentColor: androidx.compose.ui.graphics.Color,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLowest
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = total.toString(),
                fontFamily = JetBrainsMonoFamily,
                fontSize = 30.sp,
                fontWeight = FontWeight.Bold,
                color = accentColor
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "$completedCount selesai dikawal",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun EditProfileDialog(
    initialName: String,
    initialUnit: String,
    isMutating: Boolean,
    onDismiss: () -> Unit,
    onSave: (String, String) -> Unit
) {
    var name by remember { mutableStateOf(initialName) }
    var unit by remember { mutableStateOf(initialUnit) }
    var errorText by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Perbarui Profil Anggota") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                if (errorText != null) {
                    Text(
                        text = errorText.orEmpty(),
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
                OutlinedTextField(
                    value = name,
                    onValueChange = { if (it.length <= 100) name = it },
                    label = { Text("Nama Lengkap *") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("edit_profile_name_input")
                )
                OutlinedTextField(
                    value = unit,
                    onValueChange = { if (it.length <= 120) unit = it },
                    label = { Text("Asal PAC / Komisariat / Ranting") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val trimmed = name.trim()
                    if (trimmed.isEmpty()) {
                        errorText = "Nama lengkap tidak boleh kosong."
                        return@Button
                    }
                    onSave(trimmed, unit.trim())
                },
                enabled = !isMutating,
                modifier = Modifier.testTag("edit_profile_save_button")
            ) {
                Text("Simpan")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Batal")
            }
        }
    )
}

@Composable
private fun AdminLoginDialog(
    isMutating: Boolean,
    onDismiss: () -> Unit,
    onSubmit: (username: String, password: String, onError: (String) -> Unit) -> Unit
) {
    var username by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Admin Security") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = "Masukkan kredensial pengelola Dinas Kader untuk mengaktifkan fitur admin.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                if (errorMessage != null) {
                    Text(
                        text = errorMessage.orEmpty(),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error
                    )
                }
                OutlinedTextField(
                    value = username,
                    onValueChange = { username = it },
                    label = { Text("User") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("admin_username_input")
                )
                OutlinedTextField(
                    value = password,
                    onValueChange = { password = it },
                    label = { Text("Password") },
                    singleLine = true,
                    visualTransformation = if (passwordVisible) VisualTransformation.None
                    else PasswordVisualTransformation(),
                    trailingIcon = {
                        IconButton(onClick = { passwordVisible = !passwordVisible }) {
                            Icon(
                                imageVector = if (passwordVisible) Icons.Outlined.VisibilityOff
                                else Icons.Outlined.Visibility,
                                contentDescription = if (passwordVisible) "Sembunyikan sandi" else "Tampilkan sandi"
                            )
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("admin_password_input")
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (username.isBlank() || password.isBlank()) {
                        errorMessage = "User dan Password wajib diisi."
                        return@Button
                    }
                    errorMessage = null
                    onSubmit(username, password) { err ->
                        errorMessage = err
                    }
                },
                enabled = !isMutating,
                modifier = Modifier.testTag("admin_login_submit_button")
            ) {
                Text(if (isMutating) "Memverifikasi..." else "Masuk Admin")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Batal")
            }
        }
    )
}
