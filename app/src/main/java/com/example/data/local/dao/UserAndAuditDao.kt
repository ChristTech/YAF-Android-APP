package com.example.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.local.entity.AuditLogEntity
import com.example.data.local.entity.SessionEntity
import com.example.data.local.entity.UserEntity
import com.example.data.model.AccountStatus
import com.example.data.model.DepartmentType
import com.example.data.model.UserRole
import kotlinx.coroutines.flow.Flow

@Dao
interface UserDao {

    @Query("SELECT * FROM users WHERE id = :userId LIMIT 1")
    fun getUserById(userId: String): Flow<UserEntity?>

    @Query("SELECT * FROM users WHERE id = :userId LIMIT 1")
    suspend fun getUserByIdSync(userId: String): UserEntity?

    @Query("SELECT * FROM users WHERE email = :email LIMIT 1")
    suspend fun getUserByEmail(email: String): UserEntity?

    @Query("SELECT * FROM users WHERE phone = :phone LIMIT 1")
    suspend fun getUserByPhone(phone: String): UserEntity?

    @Query("SELECT * FROM users ORDER BY fullName ASC")
    fun getAllUsers(): Flow<List<UserEntity>>

    @Query("SELECT * FROM users WHERE role = :role ORDER BY fullName ASC")
    fun getUsersByRole(role: UserRole): Flow<List<UserEntity>>

    @Query("SELECT * FROM users WHERE status = :status ORDER BY createdAt DESC")
    fun getUsersByStatus(status: AccountStatus): Flow<List<UserEntity>>

    @Query("SELECT * FROM users WHERE department = :department ORDER BY fullName ASC")
    fun getUsersByDepartment(department: DepartmentType): Flow<List<UserEntity>>

    @Query("UPDATE users SET department = :department, updatedAt = :updatedAt WHERE id = :userId")
    suspend fun assignUserDepartment(userId: String, department: DepartmentType, updatedAt: Long = System.currentTimeMillis())

    @Query("SELECT COUNT(*) FROM users")
    suspend fun getUserCount(): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUser(user: UserEntity)

    @Update
    suspend fun updateUser(user: UserEntity)

    @Query("UPDATE users SET status = :status, updatedAt = :updatedAt WHERE id = :userId")
    suspend fun updateUserStatus(userId: String, status: AccountStatus, updatedAt: Long = System.currentTimeMillis())

    @Query("UPDATE users SET role = :role, updatedAt = :updatedAt WHERE id = :userId")
    suspend fun updateUserRole(userId: String, role: UserRole, updatedAt: Long = System.currentTimeMillis())

    @Query("DELETE FROM users WHERE id = :userId")
    suspend fun deleteUser(userId: String)

    // Session Management
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveSession(session: SessionEntity)

    @Query("SELECT * FROM sessions WHERE token = :token LIMIT 1")
    suspend fun getSessionByToken(token: String): SessionEntity?

    @Query("SELECT * FROM sessions ORDER BY createdAt DESC LIMIT 1")
    fun getActiveSession(): Flow<SessionEntity?>

    @Query("SELECT * FROM sessions ORDER BY createdAt DESC LIMIT 1")
    suspend fun getActiveSessionSync(): SessionEntity?

    @Query("DELETE FROM sessions")
    suspend fun clearAllSessions()
}

@Dao
interface AuditLogDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLog(log: AuditLogEntity)

    @Query("SELECT * FROM audit_logs ORDER BY timestamp DESC LIMIT :limit")
    fun getRecentLogs(limit: Int = 100): Flow<List<AuditLogEntity>>

    @Query("SELECT * FROM audit_logs WHERE userId = :userId ORDER BY timestamp DESC")
    fun getLogsByUser(userId: String): Flow<List<AuditLogEntity>>

    @Query("SELECT * FROM audit_logs WHERE targetEntity = :entityType ORDER BY timestamp DESC")
    fun getLogsByEntity(entityType: String): Flow<List<AuditLogEntity>>
}
