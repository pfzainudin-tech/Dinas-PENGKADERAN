package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.ProgramType
import com.example.model.ScheduleItem
import com.example.ui.theme.JetBrainsMonoFamily
import com.example.ui.theme.LakmudAccent
import com.example.ui.theme.MakestaAccent
import com.example.ui.theme.StatusDoneGreen
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.TextStyle
import java.util.Locale

private val INDONESIAN_LOCALE = Locale("id", "ID")
private val WEEKDAY_LABELS = listOf("Sen", "Sel", "Rab", "Kam", "Jum", "Sab", "Min")

fun formatIndonesianMonthYear(yearMonth: YearMonth): String {
    val monthName = yearMonth.month.getDisplayName(TextStyle.FULL, INDONESIAN_LOCALE)
        .replaceFirstChar { if (it.isLowerCase()) it.titlecase(INDONESIAN_LOCALE) else it.toString() }
    return "$monthName ${yearMonth.year}"
}

fun formatIndonesianDate(isoDate: String): String {
    val date = runCatching { LocalDate.parse(isoDate) }.getOrNull() ?: return isoDate
    val monthName = date.month.getDisplayName(TextStyle.SHORT, INDONESIAN_LOCALE)
        .replaceFirstChar { if (it.isLowerCase()) it.titlecase(INDONESIAN_LOCALE) else it.toString() }
    return "${date.dayOfMonth} $monthName ${date.year}"
}

fun schedulesOnDate(schedules: List<ScheduleItem>, date: LocalDate): List<ScheduleItem> {
    return schedules.filter { item ->
        val start = runCatching { LocalDate.parse(item.startDate) }.getOrNull() ?: return@filter false
        val end = runCatching { LocalDate.parse(item.endDate) }.getOrNull() ?: start
        !date.isBefore(start) && !date.isAfter(end)
    }
}

@Composable
fun MasehiCalendarCard(
    currentMonth: YearMonth,
    selectedDate: LocalDate,
    schedules: List<ScheduleItem>,
    onPreviousMonth: () -> Unit,
    onNextMonth: () -> Unit,
    onTodayClick: () -> Unit,
    onDateSelected: (LocalDate) -> Unit,
    modifier: Modifier = Modifier
) {
    val today = remember { LocalDate.now() }

    val dayEventsMap = remember(currentMonth, schedules) {
        val map = mutableMapOf<Int, List<ScheduleItem>>()
        val daysInMonth = currentMonth.lengthOfMonth()
        for (day in 1..daysInMonth) {
            val d = currentMonth.atDay(day)
            val events = schedulesOnDate(schedules, d)
            if (events.isNotEmpty()) {
                map[day] = events
            }
        }
        map
    }

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLowest
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        text = "KALENDER MASEHI",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = formatIndonesianMonthYear(currentMonth),
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    TextButton(
                        onClick = onTodayClick,
                        modifier = Modifier
                            .minimumInteractiveComponentSize()
                            .testTag("calendar_today_button")
                    ) {
                        Text("Hari Ini", style = MaterialTheme.typography.labelMedium)
                    }
                    IconButton(
                        onClick = onPreviousMonth,
                        modifier = Modifier
                            .minimumInteractiveComponentSize()
                            .testTag("calendar_prev_month_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                            contentDescription = "Bulan Sebelumnya"
                        )
                    }
                    IconButton(
                        onClick = onNextMonth,
                        modifier = Modifier
                            .minimumInteractiveComponentSize()
                            .testTag("calendar_next_month_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                            contentDescription = "Bulan Berikutnya"
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Weekday Headers (Sen..Min)
            Row(modifier = Modifier.fillMaxWidth()) {
                WEEKDAY_LABELS.forEachIndexed { index, label ->
                    val isSunday = index == 6
                    Text(
                        text = label,
                        modifier = Modifier.weight(1f),
                        textAlign = TextAlign.Center,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = if (isSunday) MaterialTheme.colorScheme.error
                        else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            val firstDayOfMonth = currentMonth.atDay(1)
            val leadingEmptyCells = firstDayOfMonth.dayOfWeek.value - 1 // Monday = 1 -> 0 offset
            val daysInMonth = currentMonth.lengthOfMonth()
            val totalCells = leadingEmptyCells + daysInMonth
            val rows = (totalCells + 6) / 7

            for (row in 0 until rows) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    for (col in 0 until 7) {
                        val cellIndex = row * 7 + col
                        val dayNumber = cellIndex - leadingEmptyCells + 1
                        if (dayNumber in 1..daysInMonth) {
                            val cellDate = currentMonth.atDay(dayNumber)
                            val isSelected = cellDate == selectedDate
                            val isToday = cellDate == today
                            val events = dayEventsMap[dayNumber].orEmpty()
                            val hasMakesta = events.any { it.parsedProgramType == ProgramType.MAKESTA }
                            val hasLakmud = events.any { it.parsedProgramType == ProgramType.LAKMUD }
                            val hasCompleted = events.any { it.completed }

                            CalendarDayCell(
                                dayNumber = dayNumber,
                                isSelected = isSelected,
                                isToday = isToday,
                                isSunday = col == 6,
                                hasMakesta = hasMakesta,
                                hasLakmud = hasLakmud,
                                hasCompleted = hasCompleted,
                                onClick = { onDateSelected(cellDate) },
                                modifier = Modifier.weight(1f)
                            )
                        } else {
                            Spacer(modifier = Modifier.weight(1f).aspectRatio(1f))
                        }
                    }
                }
                if (row < rows - 1) {
                    Spacer(modifier = Modifier.height(4.dp))
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Legend row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                CalendarLegendDot(color = MakestaAccent, label = "MAKESTA")
                CalendarLegendDot(color = LakmudAccent, label = "LAKMUD")
                CalendarLegendDot(color = StatusDoneGreen, label = "Selesai")
            }
        }
    }
}

@Composable
private fun CalendarDayCell(
    dayNumber: Int,
    isSelected: Boolean,
    isToday: Boolean,
    isSunday: Boolean,
    hasMakesta: Boolean,
    hasLakmud: Boolean,
    hasCompleted: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val bgColor = when {
        isSelected -> MaterialTheme.colorScheme.primary
        isToday -> MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.65f)
        else -> Color.Transparent
    }
    val textColor = when {
        isSelected -> MaterialTheme.colorScheme.onPrimary
        isSunday -> MaterialTheme.colorScheme.error
        isToday -> MaterialTheme.colorScheme.onPrimaryContainer
        else -> MaterialTheme.colorScheme.onSurface
    }
    val borderMod = if (isToday && !isSelected) {
        Modifier.border(1.5.dp, MaterialTheme.colorScheme.primary, RoundedCornerShape(12.dp))
    } else Modifier

    Box(
        modifier = modifier
            .aspectRatio(1f)
            .clip(RoundedCornerShape(12.dp))
            .background(bgColor)
            .then(borderMod)
            .clickable(onClick = onClick)
            .testTag("calendar_day_$dayNumber"),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = dayNumber.toString(),
                fontFamily = JetBrainsMonoFamily,
                fontSize = 13.sp,
                fontWeight = if (isSelected || isToday) FontWeight.Bold else FontWeight.Medium,
                color = textColor
            )
            if (hasMakesta || hasLakmud || hasCompleted) {
                Spacer(modifier = Modifier.height(2.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                    if (hasMakesta) {
                        Box(
                            modifier = Modifier
                                .size(5.dp)
                                .clip(CircleShape)
                                .background(if (isSelected) Color.White else MakestaAccent)
                        )
                    }
                    if (hasLakmud) {
                        Box(
                            modifier = Modifier
                                .size(5.dp)
                                .clip(CircleShape)
                                .background(if (isSelected) Color(0xFFFFE082) else LakmudAccent)
                        )
                    }
                    if (hasCompleted && !hasMakesta && !hasLakmud) {
                        Box(
                            modifier = Modifier
                                .size(5.dp)
                                .clip(CircleShape)
                                .background(if (isSelected) Color.White else StatusDoneGreen)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun CalendarLegendDot(color: Color, label: String) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(5.dp)
    ) {
        Box(
            modifier = Modifier
                .size(8.dp)
                .clip(CircleShape)
                .background(color)
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
