package com.example.data

import android.content.Context
import com.example.R
import com.example.model.AttendanceStatus
import com.example.model.ExecutorLevel
import com.example.model.ProgramType
import com.example.model.ScheduleAttendance
import com.example.model.ScheduleItem
import com.example.model.UserParticipation
import com.example.model.UserProfile
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.snapshots
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await
import java.security.MessageDigest
import java.time.LocalDate

class DinasKaderRepository(
    private val db: FirebaseFirestore,
    private val auth: FirebaseAuth = Firebase.auth
) {
    constructor(context: Context) : this(
        FirebaseFirestore.getInstance(
            context.applicationContext.getString(R.string.firestore_database_id)
        )
    )

    private fun requireUserId(): String {
        val uid = auth.currentUser?.uid
        check(!uid.isNullOrBlank()) { "Pengguna harus masuk dengan Google terlebih dahulu." }
        return uid
    }

    fun observeSchedules(): Flow<List<ScheduleItem>> = flow {
        requireUserId()
        db.collection(COLLECTION_SCHEDULES)
            .whereEqualTo("organizationId", ORG_ID)
            .snapshots()
            .map { snapshot ->
                snapshot.toObjects(
                    ScheduleItem::class.java,
                    DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                ).sortedWith(compareBy<ScheduleItem> { it.startDate }.thenBy { it.title })
            }
            .collect { emit(it) }
    }

    fun observeUserProfile(userId: String): Flow<UserProfile?> = flow {
        val uid = requireUserId()
        check(uid == userId) { "Akses profil hanya diizinkan untuk pemilik akun." }
        db.collection(COLLECTION_USERS)
            .document(userId)
            .snapshots()
            .map { snapshot ->
                if (!snapshot.exists()) null
                else snapshot.toObject(
                    UserProfile::class.java,
                    DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                )
            }
            .collect { emit(it) }
    }

    fun observeUserParticipations(userId: String): Flow<List<UserParticipation>> = flow {
        val uid = requireUserId()
        check(uid == userId) { "Akses partisipasi hanya diizinkan untuk pemilik akun." }
        db.collection(COLLECTION_USERS)
            .document(userId)
            .collection(SUBCOLLECTION_PARTICIPATIONS)
            .snapshots()
            .map { snapshot ->
                snapshot.toObjects(
                    UserParticipation::class.java,
                    DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                )
            }
            .collect { emit(it) }
    }

    fun observeScheduleAttendances(scheduleId: String): Flow<List<ScheduleAttendance>> = flow {
        requireUserId()
        require(scheduleId.isNotBlank()) { "ID jadwal tidak boleh kosong." }
        db.collection(COLLECTION_SCHEDULES)
            .document(scheduleId)
            .collection(SUBCOLLECTION_ATTENDANCES)
            .whereEqualTo("scheduleId", scheduleId)
            .snapshots()
            .map { snapshot ->
                snapshot.toObjects(
                    ScheduleAttendance::class.java,
                    DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                ).sortedWith(compareBy<ScheduleAttendance> { it.status != AttendanceStatus.IKUT.name }.thenBy { it.memberName })
            }
            .collect { emit(it) }
    }

    fun observeAdminSession(userId: String): Flow<Boolean> = flow {
        val uid = requireUserId()
        check(uid == userId) { "Akses sesi admin tidak valid." }
        db.collection(COLLECTION_ADMINS)
            .document(userId)
            .snapshots()
            .map { it.exists() }
            .collect { emit(it) }
    }

    suspend fun ensureUserProfile(
        defaultName: String,
        email: String?
    ): Result<Unit> {
        var path = "users/unauthenticated"
        return try {
            val uid = requireUserId()
            val docRef = db.collection(COLLECTION_USERS).document(uid)
            path = docRef.path
            val snapshot = docRef.get().await()
            if (!snapshot.exists()) {
                val safeName = defaultName.trim().ifBlank { "Anggota Kader" }.take(100)
                val payload = buildMap<String, Any> {
                    put("userId", uid)
                    put("fullName", safeName)
                    if (!email.isNullOrBlank()) put("email", email.trim().take(150))
                    put("createdAt", FieldValue.serverTimestamp())
                    put("updatedAt", FieldValue.serverTimestamp())
                }
                docRef.set(payload).await()
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, path)
            Result.failure(e)
        }
    }

    suspend fun updateUserProfile(
        fullName: String,
        unitOrigin: String
    ): Result<Unit> {
        var path = "users/unauthenticated"
        return try {
            val uid = requireUserId()
            val cleanName = fullName.trim()
            require(cleanName.length in 1..100) {
                "Nama lengkap wajib diisi (maksimal 100 karakter)."
            }
            val cleanOrigin = unitOrigin.trim()
            require(cleanOrigin.length <= 120) {
                "Asal pimpinan maksimal 120 karakter."
            }
            val docRef = db.collection(COLLECTION_USERS).document(uid)
            path = docRef.path
            val snapshot = docRef.get().await()
            if (snapshot.exists()) {
                docRef.update(
                    mapOf(
                        "fullName" to cleanName,
                        "unitOrigin" to cleanOrigin,
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            } else {
                val email = auth.currentUser?.email
                val payload = buildMap<String, Any> {
                    put("userId", uid)
                    put("fullName", cleanName)
                    put("unitOrigin", cleanOrigin)
                    if (!email.isNullOrBlank()) put("email", email.trim().take(150))
                    put("createdAt", FieldValue.serverTimestamp())
                    put("updatedAt", FieldValue.serverTimestamp())
                }
                docRef.set(payload).await()
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, path)
            Result.failure(e)
        }
    }

    suspend fun loginAdmin(username: String, password: String): Result<Unit> {
        var path = "admins/unauthenticated"
        return try {
            val uid = requireUserId()
            val digest = sha256Hex("${username.trim()}:$password")
            if (digest != EXPECTED_ADMIN_DIGEST) {
                return Result.failure(
                    IllegalArgumentException("Kredensial Admin tidak sesuai. Periksa kembali User dan Password.")
                )
            }
            val docRef = db.collection(COLLECTION_ADMINS).document(uid)
            path = docRef.path
            val existing = docRef.get().await()
            if (!existing.exists()) {
                docRef.set(
                    mapOf(
                        "userId" to uid,
                        "role" to "admin",
                        "verificationDigest" to digest,
                        "createdAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, path)
            Result.failure(e)
        }
    }

    suspend fun logoutAdmin(): Result<Unit> {
        var path = "admins/unauthenticated"
        return try {
            val uid = requireUserId()
            val docRef = db.collection(COLLECTION_ADMINS).document(uid)
            path = docRef.path
            docRef.delete().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, path)
            Result.failure(e)
        }
    }

    suspend fun createSchedule(
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
    ): Result<String> {
        var path = COLLECTION_SCHEDULES
        return try {
            val uid = requireUserId()
            val validated = validateScheduleFields(
                title = title,
                executorName = executorName,
                location = location,
                description = description,
                startDate = startDate,
                endDate = endDate,
                timeText = timeText
            )
            val parsedStart = LocalDate.parse(validated.startDate)
            val docRef = db.collection(COLLECTION_SCHEDULES).document()
            path = docRef.path
            val payload = mapOf(
                "organizationId" to ORG_ID,
                "title" to validated.title,
                "programType" to programType.name,
                "executorLevel" to executorLevel.label,
                "executorName" to validated.executorName,
                "location" to validated.location,
                "description" to validated.description,
                "startDate" to validated.startDate,
                "endDate" to validated.endDate,
                "timeText" to validated.timeText,
                "year" to parsedStart.year.toLong(),
                "month" to parsedStart.monthValue.toLong(),
                "completed" to completed,
                "createdBy" to uid,
                "createdAt" to FieldValue.serverTimestamp(),
                "updatedAt" to FieldValue.serverTimestamp()
            )
            docRef.set(payload).await()
            Result.success(docRef.id)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, path)
            Result.failure(e)
        }
    }

    suspend fun updateSchedule(
        scheduleId: String,
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
    ): Result<Unit> {
        var path = "$COLLECTION_SCHEDULES/$scheduleId"
        return try {
            requireUserId()
            require(scheduleId.isNotBlank()) { "ID jadwal wajib tersedia." }
            val validated = validateScheduleFields(
                title = title,
                executorName = executorName,
                location = location,
                description = description,
                startDate = startDate,
                endDate = endDate,
                timeText = timeText
            )
            val parsedStart = LocalDate.parse(validated.startDate)
            val docRef = db.collection(COLLECTION_SCHEDULES).document(scheduleId)
            path = docRef.path
            docRef.update(
                mapOf(
                    "title" to validated.title,
                    "programType" to programType.name,
                    "executorLevel" to executorLevel.label,
                    "executorName" to validated.executorName,
                    "location" to validated.location,
                    "description" to validated.description,
                    "startDate" to validated.startDate,
                    "endDate" to validated.endDate,
                    "timeText" to validated.timeText,
                    "year" to parsedStart.year.toLong(),
                    "month" to parsedStart.monthValue.toLong(),
                    "completed" to completed,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, path)
            Result.failure(e)
        }
    }

    suspend fun updateScheduleDescription(scheduleId: String, description: String): Result<Unit> {
        var path = "$COLLECTION_SCHEDULES/$scheduleId"
        return try {
            requireUserId()
            require(scheduleId.isNotBlank()) { "ID jadwal wajib tersedia." }
            val cleanDesc = description.trim()
            require(cleanDesc.length <= 1000) { "Deskripsi maksimal 1000 karakter." }
            val docRef = db.collection(COLLECTION_SCHEDULES).document(scheduleId)
            path = docRef.path
            docRef.update(
                mapOf(
                    "description" to cleanDesc,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, path)
            Result.failure(e)
        }
    }

    suspend fun toggleScheduleCompleted(scheduleId: String, completed: Boolean): Result<Unit> {
        var path = "$COLLECTION_SCHEDULES/$scheduleId"
        return try {
            requireUserId()
            require(scheduleId.isNotBlank()) { "ID jadwal wajib tersedia." }
            val docRef = db.collection(COLLECTION_SCHEDULES).document(scheduleId)
            path = docRef.path
            docRef.update(
                mapOf(
                    "completed" to completed,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, path)
            Result.failure(e)
        }
    }

    suspend fun deleteSchedule(scheduleId: String): Result<Unit> {
        var path = "$COLLECTION_SCHEDULES/$scheduleId"
        return try {
            requireUserId()
            require(scheduleId.isNotBlank()) { "ID jadwal wajib tersedia." }
            val docRef = db.collection(COLLECTION_SCHEDULES).document(scheduleId)
            path = docRef.path
            docRef.delete().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, path)
            Result.failure(e)
        }
    }

    suspend fun setAttendanceStatus(
        scheduleId: String,
        memberName: String,
        status: AttendanceStatus
    ): Result<Unit> {
        var path = "$COLLECTION_SCHEDULES/$scheduleId/$SUBCOLLECTION_ATTENDANCES"
        return try {
            val uid = requireUserId()
            require(scheduleId.isNotBlank()) { "ID jadwal wajib tersedia." }
            val cleanName = memberName.trim().ifBlank { "Anggota Kader" }.take(100)

            val attendanceRef = db.collection(COLLECTION_SCHEDULES)
                .document(scheduleId)
                .collection(SUBCOLLECTION_ATTENDANCES)
                .document(uid)
            val participationRef = db.collection(COLLECTION_USERS)
                .document(uid)
                .collection(SUBCOLLECTION_PARTICIPATIONS)
                .document(scheduleId)
            path = attendanceRef.path

            val attSnap = attendanceRef.get().await()
            val partSnap = participationRef.get().await()

            val batch = db.batch()
            if (attSnap.exists()) {
                batch.update(
                    attendanceRef,
                    mapOf(
                        "memberName" to cleanName,
                        "status" to status.name,
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                )
            } else {
                batch.set(
                    attendanceRef,
                    mapOf(
                        "userId" to uid,
                        "scheduleId" to scheduleId,
                        "memberName" to cleanName,
                        "status" to status.name,
                        "createdAt" to FieldValue.serverTimestamp(),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                )
            }

            if (partSnap.exists()) {
                batch.update(
                    participationRef,
                    mapOf(
                        "status" to status.name,
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                )
            } else {
                batch.set(
                    participationRef,
                    mapOf(
                        "userId" to uid,
                        "scheduleId" to scheduleId,
                        "status" to status.name,
                        "createdAt" to FieldValue.serverTimestamp(),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                )
            }

            batch.commit().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, path)
            Result.failure(e)
        }
    }

    private data class ValidatedSchedule(
        val title: String,
        val executorName: String,
        val location: String,
        val description: String,
        val startDate: String,
        val endDate: String,
        val timeText: String
    )

    private fun validateScheduleFields(
        title: String,
        executorName: String,
        location: String,
        description: String,
        startDate: String,
        endDate: String,
        timeText: String
    ): ValidatedSchedule {
        val cleanTitle = title.trim()
        val cleanExecutor = executorName.trim()
        val cleanLocation = location.trim()
        val cleanDescription = description.trim()
        val cleanStart = startDate.trim()
        val cleanEnd = endDate.trim().ifBlank { cleanStart }
        val cleanTime = timeText.trim()

        require(cleanTitle.length in 3..120) {
            "Judul kegiatan wajib diisi (3 - 120 karakter)."
        }
        require(cleanExecutor.length in 2..120) {
            "Nama pelaksana wajib diisi (2 - 120 karakter)."
        }
        require(cleanLocation.length in 2..150) {
            "Lokasi kegiatan wajib diisi (2 - 150 karakter)."
        }
        require(cleanDescription.length <= 1000) {
            "Deskripsi maksimal 1000 karakter."
        }
        require(cleanTime.length in 1..60) {
            "Waktu kegiatan wajib diisi (1 - 60 karakter)."
        }
        val parsedStart = runCatching { LocalDate.parse(cleanStart) }.getOrNull()
            ?: throw IllegalArgumentException("Format tanggal mulai tidak valid (YYYY-MM-DD).")
        val parsedEnd = runCatching { LocalDate.parse(cleanEnd) }.getOrNull()
            ?: throw IllegalArgumentException("Format tanggal selesai tidak valid (YYYY-MM-DD).")
        require(!parsedEnd.isBefore(parsedStart)) {
            "Tanggal selesai tidak boleh lebih awal dari tanggal mulai."
        }
        require(parsedStart.year in 2000..2100) {
            "Tahun jadwal harus antara 2000 hingga 2100."
        }

        return ValidatedSchedule(
            title = cleanTitle,
            executorName = cleanExecutor,
            location = cleanLocation,
            description = cleanDescription,
            startDate = parsedStart.toString(),
            endDate = parsedEnd.toString(),
            timeText = cleanTime
        )
    }

    private fun sha256Hex(input: String): String {
        val bytes = MessageDigest.getInstance("SHA-256").digest(input.toByteArray(Charsets.UTF_8))
        return bytes.joinToString("") { "%02x".format(it) }
    }

    companion object {
        private const val ORG_ID = "pc_mojokerto"
        private const val COLLECTION_SCHEDULES = "schedules"
        private const val COLLECTION_USERS = "users"
        private const val COLLECTION_ADMINS = "admins"
        private const val SUBCOLLECTION_ATTENDANCES = "attendances"
        private const val SUBCOLLECTION_PARTICIPATIONS = "participations"
        private const val EXPECTED_ADMIN_DIGEST =
            "1fbc93077e21e0f32bb1e6a41de3c018ab7457498f0965c9479a2984c3d71606"
    }
}
