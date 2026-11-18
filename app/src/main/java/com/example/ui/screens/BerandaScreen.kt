package com.example.ui.screens

import androidx.compose.foundation.background
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.EventAvailable
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.AttendanceStatus
import com.example.model.ProgramType
import com.example.model.ScheduleItem
import com.example.model.UiState
import com.example.ui.components.MasehiCalendarCard
import com.example.ui.components.ScheduleCard
import com.example.ui.components.formatIndonesianDate
import com.example.ui.components.schedulesOnDate
import com.example.ui.theme.JetBrainsMonoFamily
import com.example.ui.theme.LakmudAccent
import com.example.ui.theme.MakestaAccent
import com.example.ui.theme.StatusDoneGreen
import com.example.ui.theme.StatusUpcomingAmber
import java.time.LocalDate
import java.time.YearMonth

@Composable
fun BerandaScreen(
    schedulesState: UiState<List<ScheduleItem>>,
    participations: Map<String, AttendanceStatus>,
    isAdminActive: Boolean,
    memberName: String,
    onOpenScheduleDetail: (ScheduleItem) -> Unit,
    onOpenAllSchedules: () -> Unit,
    onAttendanceChanged: (String, AttendanceStatus) -> Unit,
    onAdminCompletedToggle: (String, Boolean) -> Unit,
    onAdminEditClick: (ScheduleItem) -> Unit,
    onAdminDeleteClick: (ScheduleItem) -> Unit,
    modifier: Modifier = Modifier
) {
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
                val schedules = schedulesState.data
                val completedList = remember(schedules) { schedules.filter { it.completed } }
                val upcomingList = remember(schedules) {
                    schedules.filter { !it.completed }.sortedBy { it.startDate }
                }
                val selectedDaySchedules = remember(schedules, selectedDate) {
                    schedulesOnDate(schedules, selectedDate)
                }
                val completedMakesta = completedList.count { it.parsedProgramType == ProgramType.MAKESTA }
                val completedLakmud = completedList.count { it.parsedProgramType == ProgramType.LAKMUD }

                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .widthIn(max = 720.dp),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Hero Welcome Card
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(
                                    brush = Brush.linearGradient(
                                        colors = listOf(
                                            MaterialTheme.colorScheme.primary,
                                            Color(0xFF063D29)
                                        )
                                    ),
                                    shape = MaterialTheme.shapes.extraLarge
                                )
                                .padding(20.dp)
                        ) {
                            Column {
                                Text(
                                    text = "Assalamu'alaikum, $memberName",
                                    style = MaterialTheme.typography.titleLarge,
                                    color = Color.White
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "Ringkasan Pengkaderan & Kalender Masehi PC IPNU IPPNU Kabupaten Mojokerto",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Color.White.copy(alpha = 0.85f)
                                )
                                Spacer(modifier = Modifier.height(14.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    HeroMetricPill(
                                        label = "MAKESTA Selesai",
                                        value = completedMakesta.toString(),
                                        modifier = Modifier.weight(1f)
                                    )
                                    HeroMetricPill(
                                        label = "LAKMUD Selesai",
                                        value = completedLakmud.toString(),
                                        modifier = Modifier.weight(1f)
                                    )
                                }
                            }
                        }
                    }

                    // 1. Ringkasan Pengkaderan yang selesai & 2. Ringkasan Jadwal yang akan datang
                    item {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            SummaryStatCard(
                                title = "Pengkaderan Selesai",
                                value = completedList.size.toString(),
                                subtitle = "$completedMakesta MAKESTA • $completedLakmud LAKMUD",
                                icon = Icons.Outlined.CheckCircle,
                                accentColor = StatusDoneGreen,
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("beranda_completed_summary")
                            )
                            SummaryStatCard(
                                title = "Jadwal Mendatang",
                                value = upcomingList.size.toString(),
                                subtitle = upcomingList.firstOrNull()?.let {
                                    "Terdekat: ${formatIndonesianDate(it.startDate)}"
                                } ?: "Belum ada jadwal",
                                icon = Icons.Outlined.Schedule,
                                accentColor = StatusUpcomingAmber,
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("beranda_upcoming_summary")
                            )
                        }
                    }

                    // 3. Kalender Masehi Asli
                    item {
                        MasehiCalendarCard(
                            currentMonth = currentMonth,
                            selectedDate = selectedDate,
                            schedules = schedules,
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

                    // Selected date agenda
                    item {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "Agenda ${formatIndonesianDate(selectedDate.toString())}",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            TextButton(
                                onClick = onOpenAllSchedules,
                                modifier = Modifier
                                    .minimumInteractiveComponentSize()
                                    .testTag("beranda_see_all_schedules")
                            ) {
                                Text("Lihat Semua")
                            }
                        }
                    }

                    if (selectedDaySchedules.isEmpty()) {
                        item {
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                shape = MaterialTheme.shapes.large,
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.surfaceContainerLow
                                )
                            ) {
                                Row(
                                    modifier = Modifier.padding(16.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Outlined.EventAvailable,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                    Text(
                                        text = "Tidak ada agenda pengkaderan pada tanggal ini. Pilih tanggal bertanda titik di Kalender Masehi.",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    } else {
                        items(selectedDaySchedules, key = { "day_${it.id}" }) { schedule ->
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

                    // Ringkasan Jadwal Yang Akan Datang (Nearest Upcoming)
                    item {
                        Text(
                            text = "Ringkasan Jadwal yang Akan Datang",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(top = 4.dp)
                        )
                    }

                    if (upcomingList.isEmpty()) {
                        item {
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                shape = MaterialTheme.shapes.large,
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.surfaceContainerLow
                                )
                            ) {
                                Text(
                                    text = "Belum ada jadwal pengkaderan yang akan datang.",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.padding(16.dp)
                                )
                            }
                        }
                    } else {
                        items(upcomingList.take(4), key = { "up_${it.id}" }) { schedule ->
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

                    // Ringkasan Pengkaderan yang Selesai (Latest Completed)
                    if (completedList.isNotEmpty()) {
                        item {
                            Text(
                                text = "Pengkaderan Terakhir Selesai",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(top = 4.dp)
                            )
                        }
                        items(completedList.takeLast(3).reversed(), key = { "done_${it.id}" }) { schedule ->
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

                    item { Spacer(modifier = Modifier.height(16.dp)) }
                }
            }
        }
    }
}

@Composable
private fun HeroMetricPill(
    label: String,
    value: String,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(14.dp),
        color = Color.White.copy(alpha = 0.14f)
    ) {
        Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp)) {
            Text(
                text = value,
                fontFamily = JetBrainsMonoFamily,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = Color.White.copy(alpha = 0.85f)
            )
        }
    }
}

@Composable
private fun SummaryStatCard(
    title: String,
    value: String,
    subtitle: String,
    icon: ImageVector,
    accentColor: Color,
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
        Column(modifier = Modifier.padding(14.dp)) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = accentColor,
                modifier = Modifier.size(22.dp)
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = value,
                fontFamily = JetBrainsMonoFamily,
                fontSize = 26.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = title,
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
