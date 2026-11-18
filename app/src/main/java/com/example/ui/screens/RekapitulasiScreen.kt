package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
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
import androidx.compose.material.icons.outlined.AutoGraph
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.ExpandLess
import androidx.compose.material.icons.outlined.ExpandMore
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.ExecutorLevel
import com.example.model.ProgramType
import com.example.model.ScheduleItem
import com.example.model.UiState
import com.example.ui.components.formatIndonesianDate
import com.example.ui.theme.JetBrainsMonoFamily
import com.example.ui.theme.LakmudAccent
import com.example.ui.theme.MakestaAccent
import com.example.ui.theme.StatusDoneGreen
import com.example.viewmodel.RecapPeriodMode
import java.time.LocalDate
import java.time.Month
import java.time.format.TextStyle
import java.util.Locale

private val ID_LOCALE = Locale("id", "ID")

@Composable
fun RekapitulasiScreen(
    schedulesState: UiState<List<ScheduleItem>>,
    onOpenScheduleDetail: (ScheduleItem) -> Unit,
    modifier: Modifier = Modifier
) {
    val now = remember { LocalDate.now() }
    var periodMode by remember { mutableStateOf(RecapPeriodMode.BULANAN) }
    var selectedYear by remember { mutableIntStateOf(now.year) }
    var selectedMonth by remember { mutableIntStateOf(now.monthValue) }
    var expandedLevel by remember { mutableStateOf<ExecutorLevel?>(null) }

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
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }
            is UiState.Success -> {
                val allSchedules = schedulesState.data
                val availableYears = remember(allSchedules, now) {
                    (allSchedules.map { it.year.toInt() } + listOf(now.year - 1, now.year, now.year + 1))
                        .distinct()
                        .sortedDescending()
                }

                // Schedules that match selected period (Bulanan or Tahunan)
                val periodSchedules = remember(allSchedules, periodMode, selectedYear, selectedMonth) {
                    allSchedules.filter { item ->
                        val matchesYear = item.year.toInt() == selectedYear
                        val matchesMonth = periodMode == RecapPeriodMode.TAHUNAN || item.month.toInt() == selectedMonth
                        matchesYear && matchesMonth
                    }
                }

                // Completed schedules automatically enter Rekapitulasi Pengkaderan
                val completedInPeriod = remember(periodSchedules) {
                    periodSchedules.filter { it.completed }
                }
                val upcomingInPeriodCount = periodSchedules.count { !it.completed }
                val makestaCompleted = remember(completedInPeriod) {
                    completedInPeriod.filter { it.parsedProgramType == ProgramType.MAKESTA }
                }
                val lakmudCompleted = remember(completedInPeriod) {
                    completedInPeriod.filter { it.parsedProgramType == ProgramType.LAKMUD }
                }

                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .widthIn(max = 720.dp),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    // Auto-sync banner
                    item {
                        Surface(
                            shape = MaterialTheme.shapes.large,
                            color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(14.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.CheckCircle,
                                    contentDescription = null,
                                    tint = StatusDoneGreen
                                )
                                Text(
                                    text = "Jadwal yang ditandai selesai oleh Admin otomatis masuk ke Rekapitulasi Pengkaderan.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                            }
                        }
                    }

                    // 1. Rekap Penjadwalan Pengkaderan (Bulanan dan Tahunan)
                    item {
                        SingleChoiceSegmentedButtonRow(
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("rekap_period_toggle")
                        ) {
                            RecapPeriodMode.entries.forEachIndexed { idx, mode ->
                                SegmentedButton(
                                    selected = periodMode == mode,
                                    onClick = { periodMode = mode },
                                    shape = SegmentedButtonDefaults.itemShape(index = idx, count = 2),
                                    modifier = Modifier.testTag("rekap_${mode.name.lowercase()}")
                                ) {
                                    Text("Rekap ${mode.label}")
                                }
                            }
                        }
                    }

                    // Year chips
                    item {
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            items(availableYears) { yr ->
                                FilterChip(
                                    selected = selectedYear == yr,
                                    onClick = { selectedYear = yr },
                                    label = { Text(yr.toString(), fontFamily = JetBrainsMonoFamily) }
                                )
                            }
                        }
                    }

                    // Month chips (when Bulanan)
                    if (periodMode == RecapPeriodMode.BULANAN) {
                        item {
                            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                items((1..12).toList()) { m ->
                                    val mName = Month.of(m).getDisplayName(TextStyle.SHORT, ID_LOCALE)
                                        .replaceFirstChar { it.titlecase(ID_LOCALE) }
                                    FilterChip(
                                        selected = selectedMonth == m,
                                        onClick = { selectedMonth = m },
                                        label = { Text(mName) }
                                    )
                                }
                            }
                        }
                    }

                    // 2, 3, 4. Overall, MAKESTA, and LAKMUD Recap Cards
                    item {
                        val periodTitle = if (periodMode == RecapPeriodMode.BULANAN) {
                            val mName = Month.of(selectedMonth).getDisplayName(TextStyle.FULL, ID_LOCALE)
                                .replaceFirstChar { it.titlecase(ID_LOCALE) }
                            "$mName $selectedYear"
                        } else {
                            "Tahun $selectedYear"
                        }

                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = MaterialTheme.shapes.large,
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surfaceContainerLowest
                            ),
                            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                        ) {
                            Column(
                                modifier = Modifier.padding(16.dp),
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column {
                                        Text(
                                            text = "REKAPITULASI PENGKADERAN",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.primary,
                                            fontWeight = FontWeight.Bold
                                        )
                                        Text(
                                            text = periodTitle,
                                            style = MaterialTheme.typography.titleLarge
                                        )
                                    }
                                    Icon(
                                        imageVector = Icons.Outlined.AutoGraph,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                }

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    RecapMetricBox(
                                        title = "Total Selesai",
                                        value = completedInPeriod.size,
                                        subtext = "$upcomingInPeriodCount terjadwal",
                                        accent = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.weight(1f)
                                    )
                                    RecapMetricBox(
                                        title = "MAKESTA",
                                        value = makestaCompleted.size,
                                        subtext = "Terlaksana",
                                        accent = MakestaAccent,
                                        modifier = Modifier
                                            .weight(1f)
                                            .testTag("rekap_makesta_card")
                                    )
                                    RecapMetricBox(
                                        title = "LAKMUD",
                                        value = lakmudCompleted.size,
                                        subtext = "Terlaksana",
                                        accent = LakmudAccent,
                                        modifier = Modifier
                                            .weight(1f)
                                            .testTag("rekap_lakmud_card")
                                    )
                                }

                                // Visual ratio bar for MAKESTA vs LAKMUD
                                val totalDone = completedInPeriod.size
                                if (totalDone > 0) {
                                    val makestaRatio = makestaCompleted.size.toFloat() / totalDone.toFloat()
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(10.dp)
                                            .clip(RoundedCornerShape(50))
                                            .background(LakmudAccent)
                                    ) {
                                        if (makestaRatio > 0f) {
                                            Box(
                                                modifier = Modifier
                                                    .fillMaxHeight()
                                                    .fillMaxWidth(makestaRatio)
                                                    .background(MakestaAccent)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // 5. Rekapitulasi Pelaksana (PAC / Ranting / Komisariat / Kom. Ponpes / Kom. PT)
                    item {
                        Text(
                            text = "Rekapitulasi Tiap Pelaksana",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    items(ExecutorLevel.entries, key = { it.name }) { level ->
                        val itemsForLevel = completedInPeriod.filter { it.parsedExecutorLevel == level }
                        val makestaCount = itemsForLevel.count { it.parsedProgramType == ProgramType.MAKESTA }
                        val lakmudCount = itemsForLevel.count { it.parsedProgramType == ProgramType.LAKMUD }
                        val isExpanded = expandedLevel == level

                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    expandedLevel = if (isExpanded) null else level
                                }
                                .testTag("rekap_level_${level.name.lowercase()}"),
                            shape = MaterialTheme.shapes.large,
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surfaceContainerLowest
                            ),
                            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = level.label,
                                            style = MaterialTheme.typography.titleSmall,
                                            fontWeight = FontWeight.Bold
                                        )
                                        Text(
                                            text = "$makestaCount MAKESTA • $lakmudCount LAKMUD",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Surface(
                                            shape = RoundedCornerShape(50),
                                            color = MaterialTheme.colorScheme.primaryContainer
                                        ) {
                                            Text(
                                                text = "${itemsForLevel.size} Selesai",
                                                style = MaterialTheme.typography.labelMedium,
                                                fontWeight = FontWeight.Bold,
                                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                            )
                                        }
                                        Icon(
                                            imageVector = if (isExpanded) Icons.Outlined.ExpandLess else Icons.Outlined.ExpandMore,
                                            contentDescription = if (isExpanded) "Tutup rincian" else "Lihat rincian"
                                        )
                                    }
                                }

                                AnimatedVisibility(visible = isExpanded) {
                                    Column(
                                        modifier = Modifier.padding(top = 12.dp),
                                        verticalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        HorizontalDivider()
                                        if (itemsForLevel.isEmpty()) {
                                            Text(
                                                text = "Belum ada kegiatan selesai untuk tingkat ${level.label} pada periode ini.",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        } else {
                                            itemsForLevel.groupBy { it.executorName }.forEach { (unitName, unitItems) ->
                                                Surface(
                                                    shape = RoundedCornerShape(12.dp),
                                                    color = MaterialTheme.colorScheme.surfaceContainerLow,
                                                    modifier = Modifier.fillMaxWidth()
                                                ) {
                                                    Column(
                                                        modifier = Modifier.padding(12.dp),
                                                        verticalArrangement = Arrangement.spacedBy(6.dp)
                                                    ) {
                                                        Row(
                                                            modifier = Modifier.fillMaxWidth(),
                                                            horizontalArrangement = Arrangement.SpaceBetween
                                                        ) {
                                                            Text(
                                                                text = unitName,
                                                                style = MaterialTheme.typography.labelLarge,
                                                                color = MaterialTheme.colorScheme.primary
                                                            )
                                                            Text(
                                                                text = "${unitItems.size} Kegiatan",
                                                                style = MaterialTheme.typography.labelSmall,
                                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                                            )
                                                        }
                                                        unitItems.forEach { ev ->
                                                            Row(
                                                                modifier = Modifier
                                                                    .fillMaxWidth()
                                                                    .clickable { onOpenScheduleDetail(ev) }
                                                                    .padding(vertical = 2.dp),
                                                                horizontalArrangement = Arrangement.SpaceBetween
                                                            ) {
                                                                Text(
                                                                    text = "• ${ev.title} (${ev.parsedProgramType.label})",
                                                                    style = MaterialTheme.typography.bodySmall,
                                                                    modifier = Modifier.weight(1f)
                                                                )
                                                                Text(
                                                                    text = formatIndonesianDate(ev.startDate),
                                                                    style = MaterialTheme.typography.labelSmall,
                                                                    color = MaterialTheme.colorScheme.onSurfaceVariant
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

                    // Detailed list of MAKESTA & LAKMUD completed in period
                    if (completedInPeriod.isNotEmpty()) {
                        item {
                            Text(
                                text = "Daftar Kegiatan Masuk Rekapitulasi",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(top = 6.dp)
                            )
                        }
                        items(completedInPeriod, key = { "recap_${it.id}" }) { item ->
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
                                            text = "${item.executorName} • ${formatIndonesianDate(item.startDate)}",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                    Surface(
                                        shape = RoundedCornerShape(8.dp),
                                        color = (if (item.parsedProgramType == ProgramType.MAKESTA) MakestaAccent else LakmudAccent)
                                            .copy(alpha = 0.14f)
                                    ) {
                                        Text(
                                            text = item.parsedProgramType.label,
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = FontWeight.Bold,
                                            color = if (item.parsedProgramType == ProgramType.MAKESTA) MakestaAccent else LakmudAccent,
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }

                    item { Spacer(modifier = Modifier.height(16.dp)) }
                }
            }
        }
    }
}

@Composable
private fun RecapMetricBox(
    title: String,
    value: Int,
    subtext: String,
    accent: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(14.dp),
        color = accent.copy(alpha = 0.10f)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(
                text = value.toString(),
                fontFamily = JetBrainsMonoFamily,
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = accent
            )
            Text(
                text = title,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = subtext,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
