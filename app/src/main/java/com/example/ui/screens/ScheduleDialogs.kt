package com.example.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AccessTime
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.Groups
import androidx.compose.material.icons.outlined.LocationOn
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.model.AttendanceStatus
import com.example.model.ExecutorLevel
import com.example.model.ProgramType
import com.example.model.ScheduleAttendance
import com.example.model.ScheduleItem
import com.example.model.UiState
import com.example.ui.components.AttendanceCheckBoxChip
import com.example.ui.components.formatIndonesianDate
import com.example.ui.theme.LakmudAccent
import com.example.ui.theme.MakestaAccent
import com.example.ui.theme.StatusDeclineRose
import com.example.ui.theme.StatusDoneGreen
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScheduleDetailBottomSheet(
    schedule: ScheduleItem,
    myAttendance: AttendanceStatus?,
    attendancesState: UiState<List<ScheduleAttendance>>,
    isAdminActive: Boolean,
    isMutating: Boolean,
    onDismiss: () -> Unit,
    onAttendanceChanged: (AttendanceStatus) -> Unit,
    onAdminCompletedToggle: (Boolean) -> Unit,
    onAdminUpdateDescription: (String) -> Unit,
    onAdminEditFull: () -> Unit,
    onAdminDelete: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var isEditingDescription by remember(schedule.id) { mutableStateOf(false) }
    var descriptionDraft by remember(schedule.id, schedule.description) {
        mutableStateOf(schedule.description)
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
                .padding(bottom = 28.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Header badges
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                val isMakesta = schedule.parsedProgramType == ProgramType.MAKESTA
                val typeColor = if (isMakesta) MakestaAccent else LakmudAccent
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = typeColor.copy(alpha = 0.14f)
                    ) {
                        Text(
                            text = schedule.parsedProgramType.label,
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = typeColor,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                        )
                    }
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = if (schedule.completed) StatusDoneGreen.copy(alpha = 0.14f)
                        else MaterialTheme.colorScheme.secondaryContainer
                    ) {
                        Text(
                            text = if (schedule.completed) "Selesai" else "Mendatang",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = if (schedule.completed) StatusDoneGreen
                            else MaterialTheme.colorScheme.onSecondaryContainer,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                        )
                    }
                }
                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier.minimumInteractiveComponentSize()
                ) {
                    Icon(imageVector = Icons.Outlined.Close, contentDescription = "Tutup")
                }
            }

            Text(
                text = schedule.title,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )

            Text(
                text = "${schedule.parsedExecutorLevel.label} • ${schedule.executorName}",
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.primary
            )

            // Date, time, location
            Card(
                shape = MaterialTheme.shapes.medium,
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainerLow
                )
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    val dateText = if (schedule.startDate == schedule.endDate) {
                        formatIndonesianDate(schedule.startDate)
                    } else {
                        "${formatIndonesianDate(schedule.startDate)} s.d. ${formatIndonesianDate(schedule.endDate)}"
                    }
                    DetailInfoRow(icon = Icons.Outlined.CalendarMonth, text = dateText)
                    DetailInfoRow(icon = Icons.Outlined.AccessTime, text = schedule.timeText)
                    DetailInfoRow(icon = Icons.Outlined.LocationOn, text = schedule.location)
                }
            }

            // Description section (Admin can edit anytime)
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Deskripsi Jadwal",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                    if (isAdminActive && !isEditingDescription) {
                        TextButton(
                            onClick = { isEditingDescription = true },
                            modifier = Modifier
                                .minimumInteractiveComponentSize()
                                .testTag("detail_edit_description_button")
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Edit,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Ubah Deskripsi")
                        }
                    }
                }

                if (isEditingDescription && isAdminActive) {
                    OutlinedTextField(
                        value = descriptionDraft,
                        onValueChange = { if (it.length <= 1000) descriptionDraft = it },
                        label = { Text("Deskripsi Jadwal") },
                        minLines = 3,
                        maxLines = 6,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("detail_description_input")
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        TextButton(onClick = {
                            descriptionDraft = schedule.description
                            isEditingDescription = false
                        }) {
                            Text("Batal")
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Button(
                            onClick = {
                                onAdminUpdateDescription(descriptionDraft)
                                isEditingDescription = false
                            },
                            enabled = !isMutating,
                            modifier = Modifier.testTag("detail_save_description_button")
                        ) {
                            Text("Simpan Deskripsi")
                        }
                    }
                } else {
                    Text(
                        text = schedule.description.ifBlank { "Belum ada deskripsi kegiatan." },
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // Admin Controls Box
            if (isAdminActive) {
                Card(
                    shape = MaterialTheme.shapes.medium,
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.45f)
                    )
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = "Kontrol Admin",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold
                        )
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Checkbox(
                                checked = schedule.completed,
                                onCheckedChange = onAdminCompletedToggle,
                                colors = CheckboxDefaults.colors(checkedColor = StatusDoneGreen),
                                modifier = Modifier.testTag("detail_admin_completed_checkbox")
                            )
                            Column {
                                Text(
                                    text = "Tandai Jadwal Selesai",
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Text(
                                    text = "Otomatis masuk ke Rekapitulasi Pengkaderan",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedButton(
                                onClick = onAdminEditFull,
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("Edit Lengkap")
                            }
                            OutlinedButton(
                                onClick = onAdminDelete,
                                colors = ButtonDefaults.outlinedButtonColors(
                                    contentColor = MaterialTheme.colorScheme.error
                                ),
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("Hapus")
                            }
                        }
                    }
                }
            }

            HorizontalDivider()

            // Member Attendance List Selection (CheckBox IKUT / TIDAK)
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = "Pengisian List Anggota",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    AttendanceCheckBoxChip(
                        label = "IKUT",
                        checked = myAttendance == AttendanceStatus.IKUT,
                        accentColor = StatusDoneGreen,
                        onCheckedChange = { onAttendanceChanged(AttendanceStatus.IKUT) },
                        testTag = "detail_checkbox_ikut"
                    )
                    AttendanceCheckBoxChip(
                        label = "TIDAK",
                        checked = myAttendance == AttendanceStatus.TIDAK,
                        accentColor = StatusDeclineRose,
                        onCheckedChange = { onAttendanceChanged(AttendanceStatus.TIDAK) },
                        testTag = "detail_checkbox_tidak"
                    )
                }
            }

            // Live list of members who responded
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Groups,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(18.dp)
                    )
                    Text(
                        text = "Daftar Pengawalan Anggota",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                }

                when (attendancesState) {
                    is UiState.Loading -> {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator(modifier = Modifier.size(24.dp))
                        }
                    }
                    is UiState.Error -> {
                        Text(
                            text = attendancesState.message,
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                    is UiState.Success -> {
                        val list = attendancesState.data
                        val ikutCount = list.count { it.parsedStatus == AttendanceStatus.IKUT }
                        val tidakCount = list.count { it.parsedStatus == AttendanceStatus.TIDAK }
                        Text(
                            text = "Ikut: $ikutCount anggota • Tidak: $tidakCount anggota",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        if (list.isEmpty()) {
                            Text(
                                text = "Belum ada anggota yang mengisi daftar kehadiran.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        } else {
                            list.forEach { entry ->
                                val isIkut = entry.parsedStatus == AttendanceStatus.IKUT
                                Surface(
                                    shape = RoundedCornerShape(10.dp),
                                    color = MaterialTheme.colorScheme.surfaceContainerLow,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(horizontal = 12.dp, vertical = 8.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = entry.memberName,
                                            style = MaterialTheme.typography.bodyMedium,
                                            fontWeight = FontWeight.Medium
                                        )
                                        Surface(
                                            shape = RoundedCornerShape(50),
                                            color = if (isIkut) StatusDoneGreen.copy(alpha = 0.14f)
                                            else StatusDeclineRose.copy(alpha = 0.14f)
                                        ) {
                                            Text(
                                                text = entry.parsedStatus.label,
                                                style = MaterialTheme.typography.labelSmall,
                                                fontWeight = FontWeight.Bold,
                                                color = if (isIkut) StatusDoneGreen else StatusDeclineRose,
                                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun DetailInfoRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    text: String
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(18.dp)
        )
        Text(text = text, style = MaterialTheme.typography.bodyMedium)
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun ScheduleEditorDialog(
    editingSchedule: ScheduleItem?,
    isMutating: Boolean,
    onDismiss: () -> Unit,
    onSave: (
        title: String,
        programType: ProgramType,
        executorLevel: ExecutorLevel,
        executorName: String,
        location: String,
        description: String,
        startDate: String,
        endDate: String,
        timeText: String,
        completed: Boolean
    ) -> Unit
) {
    var title by remember(editingSchedule) { mutableStateOf(editingSchedule?.title.orEmpty()) }
    var programType by remember(editingSchedule) {
        mutableStateOf(editingSchedule?.parsedProgramType ?: ProgramType.MAKESTA)
    }
    var executorLevel by remember(editingSchedule) {
        mutableStateOf(editingSchedule?.parsedExecutorLevel ?: ExecutorLevel.PAC)
    }
    var executorName by remember(editingSchedule) { mutableStateOf(editingSchedule?.executorName.orEmpty()) }
    var location by remember(editingSchedule) { mutableStateOf(editingSchedule?.location.orEmpty()) }
    var description by remember(editingSchedule) { mutableStateOf(editingSchedule?.description.orEmpty()) }
    var startDate by remember(editingSchedule) {
        mutableStateOf(editingSchedule?.startDate?.takeIf { it.isNotBlank() } ?: LocalDate.now().toString())
    }
    var endDate by remember(editingSchedule) {
        mutableStateOf(editingSchedule?.endDate?.takeIf { it.isNotBlank() } ?: LocalDate.now().toString())
    }
    var timeText by remember(editingSchedule) {
        mutableStateOf(editingSchedule?.timeText?.takeIf { it.isNotBlank() } ?: "08:00 WIB - Selesai")
    }
    var completed by remember(editingSchedule) { mutableStateOf(editingSchedule?.completed ?: false) }

    var levelExpanded by remember { mutableStateOf(false) }
    var pickingStart by remember { mutableStateOf(false) }
    var pickingEnd by remember { mutableStateOf(false) }
    var validationError by remember { mutableStateOf<String?>(null) }

    if (pickingStart || pickingEnd) {
        val initialIso = if (pickingStart) startDate else endDate
        val initialMillis = runCatching {
            LocalDate.parse(initialIso).atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()
        }.getOrElse { System.currentTimeMillis() }
        val pickerState = rememberDatePickerState(initialSelectedDateMillis = initialMillis)

        DatePickerDialog(
            onDismissRequest = {
                pickingStart = false
                pickingEnd = false
            },
            confirmButton = {
                TextButton(onClick = {
                    val millis = pickerState.selectedDateMillis
                    if (millis != null) {
                        val picked = Instant.ofEpochMilli(millis).atZone(ZoneOffset.UTC).toLocalDate()
                        if (pickingStart) {
                            startDate = picked.toString()
                            val currentEnd = runCatching { LocalDate.parse(endDate) }.getOrNull()
                            if (currentEnd == null || currentEnd.isBefore(picked)) {
                                endDate = picked.toString()
                            }
                        } else {
                            endDate = picked.toString()
                        }
                    }
                    pickingStart = false
                    pickingEnd = false
                }) {
                    Text("Pilih")
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    pickingStart = false
                    pickingEnd = false
                }) {
                    Text("Batal")
                }
            }
        ) {
            DatePicker(state = pickerState)
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = if (editingSchedule == null) "Tambah Jadwal Pengkaderan" else "Update Jadwal Pengkaderan",
                style = MaterialTheme.typography.titleLarge
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                if (validationError != null) {
                    Text(
                        text = validationError.orEmpty(),
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall
                    )
                }

                OutlinedTextField(
                    value = title,
                    onValueChange = { if (it.length <= 120) title = it },
                    label = { Text("Judul Kegiatan *") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("editor_title_input")
                )

                Text(
                    text = "Jenjang Pengkaderan",
                    style = MaterialTheme.typography.labelMedium
                )
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    ProgramType.entries.forEach { type ->
                        FilterChip(
                            selected = programType == type,
                            onClick = { programType = type },
                            label = { Text(type.label) },
                            modifier = Modifier.testTag("editor_type_${type.name.lowercase()}")
                        )
                    }
                }

                ExposedDropdownMenuBox(
                    expanded = levelExpanded,
                    onExpandedChange = { levelExpanded = !levelExpanded }
                ) {
                    OutlinedTextField(
                        value = executorLevel.label,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Tingkat Pelaksana") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = levelExpanded) },
                        modifier = Modifier
                            .menuAnchor(MenuAnchorType.PrimaryNotEditable)
                            .fillMaxWidth()
                    )
                    ExposedDropdownMenu(
                        expanded = levelExpanded,
                        onDismissRequest = { levelExpanded = false }
                    ) {
                        ExecutorLevel.entries.forEach { level ->
                            DropdownMenuItem(
                                text = { Text(level.label) },
                                onClick = {
                                    executorLevel = level
                                    levelExpanded = false
                                }
                            )
                        }
                    }
                }

                OutlinedTextField(
                    value = executorName,
                    onValueChange = { if (it.length <= 120) executorName = it },
                    label = { Text("Nama Pelaksana *") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("editor_executor_input")
                )

                OutlinedTextField(
                    value = location,
                    onValueChange = { if (it.length <= 150) location = it },
                    label = { Text("Lokasi Kegiatan *") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("editor_location_input")
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = { pickingStart = true },
                        modifier = Modifier
                            .weight(1f)
                            .minimumInteractiveComponentSize()
                            .testTag("editor_start_date_button")
                    ) {
                        Column(horizontalAlignment = Alignment.Start) {
                            Text("Mulai", style = MaterialTheme.typography.labelSmall)
                            Text(formatIndonesianDate(startDate), style = MaterialTheme.typography.bodySmall)
                        }
                    }
                    OutlinedButton(
                        onClick = { pickingEnd = true },
                        modifier = Modifier
                            .weight(1f)
                            .minimumInteractiveComponentSize()
                            .testTag("editor_end_date_button")
                    ) {
                        Column(horizontalAlignment = Alignment.Start) {
                            Text("Selesai", style = MaterialTheme.typography.labelSmall)
                            Text(formatIndonesianDate(endDate), style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }

                OutlinedTextField(
                    value = timeText,
                    onValueChange = { if (it.length <= 60) timeText = it },
                    label = { Text("Waktu Kegiatan *") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = description,
                    onValueChange = { if (it.length <= 1000) description = it },
                    label = { Text("Deskripsi Jadwal") },
                    minLines = 3,
                    maxLines = 5,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("editor_description_input")
                )

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Checkbox(
                        checked = completed,
                        onCheckedChange = { completed = it },
                        colors = CheckboxDefaults.colors(checkedColor = StatusDoneGreen),
                        modifier = Modifier.testTag("editor_completed_checkbox")
                    )
                    Text(
                        text = "Tandai sudah selesai (masuk Rekapitulasi)",
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val cleanTitle = title.trim()
                    val cleanExecutor = executorName.trim()
                    val cleanLocation = location.trim()
                    val cleanTime = timeText.trim()
                    if (cleanTitle.length < 3) {
                        validationError = "Judul kegiatan minimal 3 karakter."
                        return@Button
                    }
                    if (cleanExecutor.length < 2) {
                        validationError = "Nama pelaksana minimal 2 karakter."
                        return@Button
                    }
                    if (cleanLocation.length < 2) {
                        validationError = "Lokasi kegiatan minimal 2 karakter."
                        return@Button
                    }
                    if (cleanTime.isBlank()) {
                        validationError = "Waktu kegiatan wajib diisi."
                        return@Button
                    }
                    val startParsed = runCatching { LocalDate.parse(startDate) }.getOrNull()
                    val endParsed = runCatching { LocalDate.parse(endDate) }.getOrNull()
                    if (startParsed == null || endParsed == null || endParsed.isBefore(startParsed)) {
                        validationError = "Rentang tanggal Masehi tidak valid."
                        return@Button
                    }
                    validationError = null
                    onSave(
                        cleanTitle,
                        programType,
                        executorLevel,
                        cleanExecutor,
                        cleanLocation,
                        description.trim(),
                        startDate,
                        endDate,
                        cleanTime,
                        completed
                    )
                },
                enabled = !isMutating,
                modifier = Modifier.testTag("editor_save_button")
            ) {
                Text(if (isMutating) "Menyimpan..." else "Simpan Jadwal")
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
fun DeleteScheduleConfirmDialog(
    schedule: ScheduleItem,
    isMutating: Boolean,
    onDismiss: () -> Unit,
    onConfirmDelete: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Hapus Jadwal?") },
        text = {
            Text("Jadwal \"${schedule.title}\" akan dihapus dari daftar penjadwalan.")
        },
        confirmButton = {
            Button(
                onClick = onConfirmDelete,
                enabled = !isMutating,
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
            ) {
                Text("Hapus")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Batal")
            }
        }
    )
}
