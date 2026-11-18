package com.example.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.DinasKaderRepository
import com.example.data.extractFirestoreErrorMessage
import com.example.model.AppNotificationItem
import com.example.model.AttendanceStatus
import com.example.model.ExecutorLevel
import com.example.model.ProgramType
import com.example.model.ScheduleAttendance
import com.example.model.ScheduleItem
import com.example.model.UiState
import com.example.model.UserParticipation
import com.example.model.UserProfile
import com.example.notifications.ScheduleNotificationHelper
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

enum class ScheduleViewMode { KALENDER, LIST }
enum class ScheduleStatusFilter(val label: String) { SEMUA("Semua"), MENDATANG("Mendatang"), SELESAI("Selesai") }
enum class RecapPeriodMode(val label: String) { BULANAN("Bulanan"), TAHUNAN("Tahunan") }

class DinasKaderViewModel(
    private val repository: DinasKaderRepository,
    private val currentUserId: String,
    defaultDisplayName: String,
    defaultEmail: String?,
    private val notificationHelper: ScheduleNotificationHelper? = null
) : ViewModel() {

    private val _notifications = MutableStateFlow<List<AppNotificationItem>>(emptyList())
    val notifications: StateFlow<List<AppNotificationItem>> = _notifications.asStateFlow()

    private val _selectedScheduleId = MutableStateFlow<String?>(null)
    val selectedScheduleId: StateFlow<String?> = _selectedScheduleId.asStateFlow()

    private val _feedbackMessage = MutableSharedFlow<String>(extraBufferCapacity = 8)
    val feedbackMessage = _feedbackMessage.asSharedFlow()

    private val _isMutating = MutableStateFlow(false)
    val isMutating: StateFlow<Boolean> = _isMutating.asStateFlow()

    val schedulesState: StateFlow<UiState<List<ScheduleItem>>> = repository.observeSchedules()
        .map<List<ScheduleItem>, UiState<List<ScheduleItem>>> { list ->
            notificationHelper?.let { helper ->
                _notifications.value = helper.processScheduleUpdates(list, _notifications.value)
            }
            UiState.Success(list)
        }
        .catch { error ->
            val msg = extractFirestoreErrorMessage(error, "Gagal memuat jadwal pengkaderan.")
            if (error is CancellationException && msg == error.message) {
                throw error
            }
            Log.w(TAG, "Error observing schedules", error)
            emit(UiState.Error(msg))
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000L),
            initialValue = UiState.Loading
        )

    val userProfileState: StateFlow<UiState<UserProfile?>> = repository.observeUserProfile(currentUserId)
        .map<UserProfile?, UiState<UserProfile?>> { UiState.Success(it) }
        .catch { error ->
            val msg = extractFirestoreErrorMessage(error, "Gagal memuat profil anggota.")
            if (error is CancellationException && msg == error.message) throw error
            Log.w(TAG, "Error observing profile", error)
            emit(UiState.Error(msg))
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000L),
            initialValue = UiState.Loading
        )

    val userParticipationsState: StateFlow<Map<String, AttendanceStatus>> =
        repository.observeUserParticipations(currentUserId)
            .map { list ->
                list.mapNotNull { part ->
                    AttendanceStatus.fromRawOrNull(part.status)?.let { status ->
                        part.scheduleId to status
                    }
                }.toMap()
            }
            .catch { error ->
                if (error is CancellationException) throw error
                Log.w(TAG, "Error observing participations", error)
                emit(emptyMap())
            }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5000L),
                initialValue = emptyMap()
            )

    val isAdminActive: StateFlow<Boolean> = repository.observeAdminSession(currentUserId)
        .catch { error ->
            if (error is CancellationException) throw error
            Log.w(TAG, "Error observing admin status", error)
            emit(false)
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000L),
            initialValue = false
        )

    @OptIn(ExperimentalCoroutinesApi::class)
    val selectedScheduleAttendancesState: StateFlow<UiState<List<ScheduleAttendance>>> =
        _selectedScheduleId
            .flatMapLatest { scheduleId ->
                if (scheduleId.isNullOrBlank()) {
                    flowOf(UiState.Success(emptyList()))
                } else {
                    repository.observeScheduleAttendances(scheduleId)
                        .map<List<ScheduleAttendance>, UiState<List<ScheduleAttendance>>> { UiState.Success(it) }
                        .catch { error ->
                            val msg = extractFirestoreErrorMessage(error, "Gagal memuat daftar anggota.")
                            if (error is CancellationException && msg == error.message) throw error
                            emit(UiState.Error(msg))
                        }
                }
            }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5000L),
                initialValue = UiState.Success(emptyList())
            )

    init {
        viewModelScope.launch {
            repository.ensureUserProfile(defaultDisplayName, defaultEmail)
        }
    }

    fun selectScheduleForDetail(scheduleId: String?) {
        _selectedScheduleId.value = scheduleId
    }

    fun markAllNotificationsRead() {
        _notifications.value = _notifications.value.map { it.copy(unread = false) }
    }

    fun updateProfile(fullName: String, unitOrigin: String, onSuccess: () -> Unit) {
        viewModelScope.launch {
            _isMutating.value = true
            val result = repository.updateUserProfile(fullName, unitOrigin)
            _isMutating.value = false
            result.fold(
                onSuccess = {
                    _feedbackMessage.tryEmit("Profil berhasil diperbarui.")
                    onSuccess()
                },
                onFailure = { e ->
                    _feedbackMessage.tryEmit(e.localizedMessage ?: "Gagal memperbarui profil.")
                }
            )
        }
    }

    fun loginAdmin(username: String, password: String, onSuccess: () -> Unit, onError: (String) -> Unit) {
        viewModelScope.launch {
            _isMutating.value = true
            val result = repository.loginAdmin(username, password)
            _isMutating.value = false
            result.fold(
                onSuccess = {
                    _feedbackMessage.tryEmit("Mode Admin aktif. Fitur kelola jadwal terbuka.")
                    onSuccess()
                },
                onFailure = { e ->
                    onError(e.localizedMessage ?: "Gagal masuk sebagai Admin.")
                }
            )
        }
    }

    fun logoutAdmin() {
        viewModelScope.launch {
            _isMutating.value = true
            val result = repository.logoutAdmin()
            _isMutating.value = false
            result.fold(
                onSuccess = {
                    _feedbackMessage.tryEmit("Mode Admin dinonaktifkan.")
                },
                onFailure = { e ->
                    _feedbackMessage.tryEmit(e.localizedMessage ?: "Gagal keluar dari mode Admin.")
                }
            )
        }
    }

    fun saveSchedule(
        editingId: String?,
        title: String,
        programType: ProgramType,
        executorLevel: ExecutorLevel,
        executorName: String,
        location: String,
        description: String,
        startDate: String,
        endDate: String,
        timeText: String,
        completed: Boolean,
        onSuccess: () -> Unit
    ) {
        viewModelScope.launch {
            _isMutating.value = true
            val result = if (editingId.isNullOrBlank()) {
                repository.createSchedule(
                    title = title,
                    programType = programType,
                    executorLevel = executorLevel,
                    executorName = executorName,
                    location = location,
                    description = description,
                    startDate = startDate,
                    endDate = endDate,
                    timeText = timeText,
                    completed = completed
                ).map { Unit }
            } else {
                repository.updateSchedule(
                    scheduleId = editingId,
                    title = title,
                    programType = programType,
                    executorLevel = executorLevel,
                    executorName = executorName,
                    location = location,
                    description = description,
                    startDate = startDate,
                    endDate = endDate,
                    timeText = timeText,
                    completed = completed
                )
            }
            _isMutating.value = false
            result.fold(
                onSuccess = {
                    _feedbackMessage.tryEmit(
                        if (editingId.isNullOrBlank()) "Jadwal baru berhasil ditambahkan."
                        else "Jadwal pengkaderan berhasil diperbarui."
                    )
                    onSuccess()
                },
                onFailure = { e ->
                    _feedbackMessage.tryEmit(e.localizedMessage ?: "Gagal menyimpan jadwal.")
                }
            )
        }
    }

    fun updateDescription(scheduleId: String, description: String, onSuccess: () -> Unit = {}) {
        viewModelScope.launch {
            _isMutating.value = true
            val result = repository.updateScheduleDescription(scheduleId, description)
            _isMutating.value = false
            result.fold(
                onSuccess = {
                    _feedbackMessage.tryEmit("Deskripsi jadwal diperbarui.")
                    onSuccess()
                },
                onFailure = { e ->
                    _feedbackMessage.tryEmit(e.localizedMessage ?: "Gagal memperbarui deskripsi.")
                }
            )
        }
    }

    fun toggleScheduleCompleted(scheduleId: String, completed: Boolean) {
        viewModelScope.launch {
            val result = repository.toggleScheduleCompleted(scheduleId, completed)
            result.fold(
                onSuccess = {
                    _feedbackMessage.tryEmit(
                        if (completed) "Jadwal ditandai selesai & masuk ke Rekapitulasi."
                        else "Status selesai jadwal dibatalkan."
                    )
                },
                onFailure = { e ->
                    _feedbackMessage.tryEmit(e.localizedMessage ?: "Gagal mengubah status jadwal.")
                }
            )
        }
    }

    fun deleteSchedule(scheduleId: String, onSuccess: () -> Unit = {}) {
        viewModelScope.launch {
            _isMutating.value = true
            val result = repository.deleteSchedule(scheduleId)
            _isMutating.value = false
            result.fold(
                onSuccess = {
                    _feedbackMessage.tryEmit("Jadwal berhasil dihapus.")
                    onSuccess()
                },
                onFailure = { e ->
                    _feedbackMessage.tryEmit(e.localizedMessage ?: "Gagal menghapus jadwal.")
                }
            )
        }
    }

    fun setAttendance(scheduleId: String, status: AttendanceStatus) {
        viewModelScope.launch {
            val profile = (userProfileState.value as? UiState.Success)?.data
            val memberName = profile?.fullName?.takeIf { it.isNotBlank() } ?: "Anggota Kader"
            val result = repository.setAttendanceStatus(scheduleId, memberName, status)
            result.fold(
                onSuccess = {
                    _feedbackMessage.tryEmit("Status kehadiran tercatat: ${status.label}")
                },
                onFailure = { e ->
                    _feedbackMessage.tryEmit(e.localizedMessage ?: "Gagal menyimpan kehadiran.")
                }
            )
        }
    }

    private companion object {
        const val TAG = "DinasKaderVM"
    }
}
