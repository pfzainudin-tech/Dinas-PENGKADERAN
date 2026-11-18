package com.example.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.GoldSecondary
import com.example.ui.theme.GoldSecondaryContainer
import com.example.ui.theme.JetBrainsMonoFamily
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun KaderisasiCrestLogo(
    modifier: Modifier = Modifier,
    size: Dp = 44.dp
) {
    val primary = MaterialTheme.colorScheme.primary
    val primaryContainer = MaterialTheme.colorScheme.primaryContainer
    val gold = GoldSecondary

    Box(
        modifier = modifier
            .size(size)
            .clip(RoundedCornerShape(14.dp))
            .background(
                Brush.linearGradient(
                    colors = listOf(primary, Color(0xFF063D29))
                )
            ),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.size(size * 0.78f)) {
            val w = this.size.width
            val h = this.size.height

            // Left vertical pillar of stylized K
            val pillarPath = Path().apply {
                moveTo(w * 0.16f, h * 0.16f)
                lineTo(w * 0.30f, h * 0.16f)
                lineTo(w * 0.30f, h * 0.84f)
                lineTo(w * 0.16f, h * 0.84f)
                close()
            }
            drawPath(pillarPath, color = Color.White)

            // Lower diagonal leg of K
            val lowerLeg = Path().apply {
                moveTo(w * 0.34f, h * 0.52f)
                lineTo(w * 0.48f, h * 0.48f)
                lineTo(w * 0.82f, h * 0.84f)
                lineTo(w * 0.65f, h * 0.84f)
                close()
            }
            drawPath(lowerLeg, color = primaryContainer)

            // Upward gold ribbon arrow forming the upper wing of K
            drawLine(
                color = gold,
                start = Offset(w * 0.24f, h * 0.66f),
                end = Offset(w * 0.80f, h * 0.18f),
                strokeWidth = w * 0.13f,
                cap = StrokeCap.Round
            )
            val arrowHead = Path().apply {
                moveTo(w * 0.85f, h * 0.14f)
                lineTo(w * 0.64f, h * 0.17f)
                lineTo(w * 0.78f, h * 0.34f)
                close()
            }
            drawPath(arrowHead, color = gold)

            // Small 8-point star motif representing IPNU-IPPNU kaderisasi
            val cx = w * 0.74f
            val cy = h * 0.52f
            val outerR = w * 0.10f
            val innerR = w * 0.045f
            val starPath = Path()
            for (i in 0 until 16) {
                val r = if (i % 2 == 0) outerR else innerR
                val angle = (i * PI / 8.0) - PI / 2.0
                val x = cx + (r * cos(angle)).toFloat()
                val y = cy + (r * sin(angle)).toFloat()
                if (i == 0) starPath.moveTo(x, y) else starPath.lineTo(x, y)
            }
            starPath.close()
            drawPath(starPath, color = GoldSecondaryContainer)
        }
    }
}

@Composable
fun DinasKaderTopBar(
    isAdminActive: Boolean,
    unreadNotificationCount: Int,
    onNotificationsClick: () -> Unit,
    onAdminBadgeClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 1.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                KaderisasiCrestLogo(size = 42.dp)
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "DINAS KADER",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = "PC IPNU IPPNU KAB. MOJOKERTO",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                if (isAdminActive) {
                    Surface(
                        onClick = onAdminBadgeClick,
                        shape = RoundedCornerShape(50),
                        color = MaterialTheme.colorScheme.secondaryContainer,
                        modifier = Modifier
                            .minimumInteractiveComponentSize()
                            .testTag("top_bar_admin_badge")
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Filled.AdminPanelSettings,
                                contentDescription = "Mode Admin Aktif",
                                tint = MaterialTheme.colorScheme.onSecondaryContainer,
                                modifier = Modifier.size(16.dp)
                            )
                            Text(
                                text = "ADMIN",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSecondaryContainer
                            )
                        }
                    }
                }

                Box(contentAlignment = Alignment.TopEnd) {
                    IconButton(
                        onClick = onNotificationsClick,
                        modifier = Modifier
                            .minimumInteractiveComponentSize()
                            .testTag("notifications_button")
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Notifications,
                            contentDescription = "Notifikasi Jadwal",
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    }
                    if (unreadNotificationCount > 0) {
                        Box(
                            modifier = Modifier
                                .padding(top = 6.dp, end = 6.dp)
                                .size(18.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.error),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = if (unreadNotificationCount > 9) "9+" else unreadNotificationCount.toString(),
                                color = MaterialTheme.colorScheme.onError,
                                fontFamily = JetBrainsMonoFamily,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }
    }
}
