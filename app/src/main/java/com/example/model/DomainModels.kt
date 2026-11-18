package com.example.model

import androidx.annotation.Keep
import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.ServerTimestamp

enum class ProgramType(val label: String) {
    MAKESTA("MAKESTA"),
    LAKMUD("LAKMUD");

    companion object {
        fun fromRaw(raw: String?): ProgramType {
            return entries.firstOrNull { it.name.equals(raw, ignoreCase = true) } ?: MAKESTA
        }
    }
}

enum class ExecutorLevel(val label: String, val shortLabel: String) {
    PAC("Pimpinan Anak Cabang", "PAC"),
    RANTING("Ranting", "Ranting"),
    KOMISARIAT("Komisariat", "Komisariat"),
    KOMISARIAT_PONPES("Komisariat Pondok Pesantren", "Kom. Ponpes"),
    KOMISARIAT_PT("Komisariat Perguruan Tinggi", "Kom. PT");

    companion object {
        fun fromRaw(raw: String?): ExecutorLevel {
            return entries.firstOrNull {
                it.label.equals(raw, ignoreCase = true) ||
                    it.name.equals(raw, ignoreCase = true) ||
                    it.shortLabel.equals(raw, ignoreCase = true)
            } ?: PAC
        }
    }
}

enum class AttendanceStatus(val label: String) {
    IKUT("IKUT"),
    TIDAK("TIDAK");

    companion object {
        fun fromRawOrNull(raw: String?): AttendanceStatus? {
            return entries.firstOrNull { it.name.equals(raw, ignoreCase = true) }
        }
    }
}

@Keep
@IgnoreExtraProperties
data class ScheduleItem(
    @DocumentId val id: String = "",
    val organizationId: String = "pc_mojokerto",
    val title: String = "",
    val programType: String = ProgramType.MAKESTA.name,
    val executorLevel: String = ExecutorLevel.PAC.label,
    val executorName: String = "",
    val location: String = "",
    val description: String = "",
    val startDate: String = "",
    val endDate: String = "",
    val timeText: String = "",
    val year: Long = 2026L,
    val month: Long = 1L,
    val completed: Boolean = false,
    val createdBy: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
) {
    val parsedProgramType: ProgramType
        get() = ProgramType.fromRaw(programType)

    val parsedExecutorLevel: ExecutorLevel
        get() = ExecutorLevel.fromRaw(executorLevel)
}

@Keep
@IgnoreExtraProperties
data class ScheduleAttendance(
    @DocumentId val id: String = "",
    val userId: String = "",
    val scheduleId: String = "",
    val memberName: String = "",
    val status: String = AttendanceStatus.IKUT.name,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
) {
    val parsedStatus: AttendanceStatus
        get() = AttendanceStatus.fromRawOrNull(status) ?: AttendanceStatus.IKUT
}

@Keep
@IgnoreExtraProperties
data class UserProfile(
    @DocumentId val id: String = "",
    val userId: String = "",
    val fullName: String = "",
    val unitOrigin: String = "",
    val email: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class UserParticipation(
    @DocumentId val id: String = "",
    val userId: String = "",
    val scheduleId: String = "",
    val status: String = AttendanceStatus.IKUT.name,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class AdminSession(
    @DocumentId val id: String = "",
    val userId: String = "",
    val role: String = "",
    val verificationDigest: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null
)

data class AppNotificationItem(
    val id: String,
    val scheduleId: String,
    val title: String,
    val message: String,
    val timestampMillis: Long,
    val unread: Boolean,
    val category: String
)

sealed interface UiState<out T> {
    data object Loading : UiState<Nothing>
    data class Success<T>(val data: T) : UiState<T>
    data class Error(val message: String) : UiState<Nothing>
}
