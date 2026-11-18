package com.example.ui.screens

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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.List
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.EventNote
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
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
import com.example.model.ProgramType
import com.example.model.ScheduleItem
import com.example.model.UiState
import com.example.ui.components.MasehiCalendarCard
import com.example.ui.components.ScheduleCard
import com.example.ui.components.formatIndonesianDate
import com.example.ui.components.schedulesOnDate
import com.example.viewmodel.ScheduleStatusFilter
import com.example.viewmodel.ScheduleViewMode
import java.time.LocalDate
import java.time.YearMonth

@Composable
fun JadwalScreen(
    schedulesState: UiState<List<ScheduleItem>>,
    participations: Map<String, AttendanceStatus>,
    isAdminActive: Boolean,
    onOpenScheduleDetail: (ScheduleItem) -> Unit,
    onAttendanceChanged: (String, AttendanceStatus) -> Unit,
    onAdminCompletedToggle: (String, Boolean) -> Unit,
    onAdminEditClick: (ScheduleItem) -> Unit,
    onAdminDeleteClick: (ScheduleItem) -> Unit,
    modifier: Modifier = Modifier
) {
    var viewMode by remember { mutableStateOf(ScheduleViewMode.LIST) }
    var statusFilter by remember { mutableStateOf(ScheduleStatusFilter.SEMUA) }
    var programFilter by remember { mutableStateOf<ProgramType?>(null) }
    var searchQuery by remember { mutableStateOf("") }
    var currentMonth by remember { mutableStateOf(YearMonth.now()) }
    var selectedDate by remember { mutableStateOf(LocalDate.now()) }

    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.TopCenter
    ) {
        when (schedulesState) {
            is UiState.Loading -> {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            }
            is UiState.Error -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = schedulesState.message,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodyLarge
                    )
                }
            }
            is UiState.Success -> {
                val allSchedules = schedulesState.data
                val filteredSchedules = remember(allSchedules, statusFilter, programFilter, searchQuery) {
                    allSchedules.filter { item ->
                        val matchesStatus = when (statusFilter) {
                            ScheduleStatusFilter.SEMUA -> true
                            ScheduleStatusFilter.MENDATANG -> !item.completed
                            ScheduleStatusFilter.SELESAI -> item.completed
                        }
                        val matchesProgram = programFilter == null || item.parsedProgramType == programFilter
                        val q = searchQuery.trim()
                        val matchesQuery = q.isEmpty() ||
                            item.title.contains(q, ignoreCase = true) ||
                            item.executorName.contains(q, ignoreCase = true) ||
                            item.location.contains(q, ignoreCase = true)
                        matchesStatus && matchesProgram && matchesQuery
                    }
                }

                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .widthIn(max = 720.dp),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // 2 Opsi: Tampilan Kalender & Tampilan List
                    item {
                        SingleChoiceSegmentedButtonRow(
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("jadwal_view_mode_toggle")
                        ) {
                            SegmentedButton(
                                selected = viewMode == ScheduleViewMode.KALENDER,
                                onClick = { viewMode = ScheduleViewMode.KALENDER },
                                shape = SegmentedButtonDefaults.itemShape(index = 0, count = 2),
                                icon = {
                                    Icon(
                                        imageVector = Icons.Outlined.CalendarMonth,
                                        contentDescription = null,
                                        modifier = Modifier.size(18.dp)
                                    )
                                },
                                modifier = Modifier.testTag("mode_kalender_button")
                            ) {
                                Text("Tampilan Kalender")
                            }
                            SegmentedButton(
                                selected = viewMode == ScheduleViewMode.LIST,
                                onClick = { viewMode = ScheduleViewMode.LIST },
                                shape = SegmentedButtonDefaults.itemShape(index = 1, count = 2),
                                icon = {
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Outlined.List,
                                        contentDescription = null,
                                        modifier = Modifier.size(18.dp)
                                    )
                                },
                                modifier = Modifier.testTag("mode_list_button")
                            ) {
                                Text("Tampilan List")
                            }
                        }
                    }

                    if (viewMode == ScheduleViewMode.KALENDER) {
                        item {
                            MasehiCalendarCard(
                                currentMonth = currentMonth,
                                selectedDate = selectedDate,
                                schedules = allSchedules,
                                onPreviousMonth = { currentMonth = currentMonth.minusMonths(1) },
                                onNextMonth = { currentMonth = currentMonth.plusMonths(1) },
                                onTodayClick = {
                                    val now = LocalDate.now()
                                    currentMonth = YearMonth.from(now)
                                    selectedDate = now
                                },
                                onDateSelected = { selectedDate = it }
                            )
                        }

                        val daySchedules = schedulesOnDate(allSchedules, selectedDate)
                        item {
                            Text(
                                text = "Jadwal Tanggal ${formatIndonesianDate(selectedDate.toString())}",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        if (daySchedules.isEmpty()) {
                            item {
                                EmptyScheduleCard(
                                    message = "Tidak ada jadwal pengkaderan pada ${formatIndonesianDate(selectedDate.toString())}."
                                )
                            }
                        } else {
                            items(daySchedules, key = { "cal_${it.id}" }) { schedule ->
                                ScheduleCard(
                                    schedule = schedule,
                                    myAttendance = participations[schedule.id],
                                    isAdminActive = isAdminActive,
                                    onCardClick = { onOpenScheduleDetail(schedule) },
                                    onAttendanceChanged = { status -> onAttendanceChanged(schedule.id, status) },
                                    onAdminCompletedToggle = { completed -> onAdminCompletedToggle(schedule.id, completed) },
                                    onAdminEditClick = { onAdminEditClick(schedule) },
                                    onAdminDeleteClick = { onAdminDeleteClick(schedule) }
                                )
                            }
                        }
                    } else {
                        // Search & Filters for Tampilan List
                        item {
                            OutlinedTextField(
                                value = searchQuery,
                                onValueChange = { searchQuery = it },
                                placeholder = { Text("Cari kegiatan, pelaksana, atau lokasi...") },
                                leadingIcon = {
                                    Icon(imageVector = Icons.Outlined.Search, contentDescription = null)
                                },
                                singleLine = true,
                                shape = RoundedCornerShape(16.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("jadwal_search_input")
                            )
                        }

                        item {
                            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                items(ScheduleStatusFilter.entries) { filter ->
                                    FilterChip(
                                        selected = statusFilter == filter,
                                        onClick = { statusFilter = filter },
                                        label = { Text(filter.label) },
                                        modifier = Modifier.testTag("filter_${filter.name.lowercase()}")
                                    )
                                }
                                items(ProgramType.entries) { program ->
                                    FilterChip(
                                        selected = programFilter == program,
                                        onClick = {
                                            programFilter = if (programFilter == program) null else program
                                        },
                                        label = { Text(program.label) },
                                        modifier = Modifier.testTag("filter_${program.name.lowercase()}")
                                    )
                                }
                            }
                        }

                        if (filteredSchedules.isEmpty()) {
                            item {
                                EmptyScheduleCard(
                                    message = if (allSchedules.isEmpty()) {
                                        if (isAdminActive) {
                                            "Belum ada jadwal pengkaderan. Ketuk tombol '+ Tambah Jadwal' untuk membuat jadwal pertama."
                                        } else {
                                            "Belum ada jadwal pengkaderan tersedia. Admin dapat menambahkan jadwal setelah login di Admin Security."
                                        }
                                    } else {
                                        "Tidak ada jadwal yang sesuai dengan filter atau pencarian."
                                    }
                                )
                            }
                        } else {
                            items(filteredSchedules, key = { "list_${it.id}" }) { schedule ->
                                ScheduleCard(
                                    schedule = schedule,
                                    myAttendance = participations[schedule.id],
                                    isAdminActive = isAdminActive,
                                    onCardClick = { onOpenScheduleDetail(schedule) },
                                    onAttendanceChanged = { status -> onAttendanceChanged(schedule.id, status) },
                                    onAdminCompletedToggle = { completed -> onAdminCompletedToggle(schedule.id, completed) },
                                    onAdminEditClick = { onAdminEditClick(schedule) },
                                    onAdminDeleteClick = { onAdminDeleteClick(schedule) }
                                )
                            }
                        }
                    }

                    item { Spacer(modifier = Modifier.height(72.dp)) }
                }
            }
        }
    }
}

@Composable
private fun EmptyScheduleCard(message: String) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(
                imageVector = Icons.Outlined.EventNote,
                contentDescription = null,
                modifier = Modifier.size(36.dp),
                tint = MaterialTheme.colorScheme.primary
            )
            Text(
                text = message,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
