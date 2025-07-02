package com.example.myapp.service

import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service

/**
 * UserService handles user-related business logic This is a clean code file without any sensitive
 * information
 */
@Service
class UserService(
        private val userRepository: UserRepository,
        private val emailService: EmailService,
        private val cacheManager: CacheManager
) {

    private val logger = LoggerFactory.getLogger(UserService::class.java)

    @Value("\${app.user.session.timeout:3600}") private val sessionTimeout: Long = 3600

    @Value("\${app.user.max.login.attempts:5}") private val maxLoginAttempts: Int = 5

    companion object {
        private const val CACHE_PREFIX = "user_cache"
        private const val DEFAULT_PAGE_SIZE = 20
        private const val MAX_PAGE_SIZE = 100
    }

    /** Retrieves user by ID with caching */
    suspend fun getUserById(userId: Long): User? {
        logger.info("Fetching user with ID: {}", userId)

        val cacheKey = "$CACHE_PREFIX:$userId"
        val cachedUser = cacheManager.get(cacheKey, User::class.java)

        if (cachedUser != null) {
            logger.debug("User found in cache")
            return cachedUser
        }

        val user = userRepository.findById(userId)
        if (user != null) {
            cacheManager.put(cacheKey, user, sessionTimeout)
        }

        return user
    }

    /** Creates a new user account */
    suspend fun createUser(userRequest: CreateUserRequest): User {
        logger.info("Creating new user with email: {}", userRequest.email)

        // Validate user input
        validateUserRequest(userRequest)

        // Check if user already exists
        val existingUser = userRepository.findByEmail(userRequest.email)
        if (existingUser != null) {
            throw UserAlreadyExistsException("User with email ${userRequest.email} already exists")
        }

        // Create user entity
        val user =
                User(
                        email = userRequest.email.lowercase(),
                        firstName = userRequest.firstName.trim(),
                        lastName = userRequest.lastName.trim(),
                        createdAt = LocalDateTime.now(),
                        isActive = true,
                        loginAttempts = 0
                )

        // Save user
        val savedUser = userRepository.save(user)

        // Send welcome email
        emailService.sendWelcomeEmail(savedUser.email, savedUser.firstName)

        // Clear cache
        invalidateUserCache(savedUser.id)

        logger.info("User created successfully with ID: {}", savedUser.id)
        return savedUser
    }

    /** Updates user information */
    suspend fun updateUser(userId: Long, updateRequest: UpdateUserRequest): User {
        logger.info("Updating user with ID: {}", userId)

        val existingUser =
                getUserById(userId)
                        ?: throw UserNotFoundException("User not found with ID: $userId")

        val updatedUser =
                existingUser.copy(
                        firstName = updateRequest.firstName?.trim() ?: existingUser.firstName,
                        lastName = updateRequest.lastName?.trim() ?: existingUser.lastName,
                        phoneNumber = updateRequest.phoneNumber?.trim() ?: existingUser.phoneNumber,
                        updatedAt = LocalDateTime.now()
                )

        val savedUser = userRepository.save(updatedUser)
        invalidateUserCache(userId)

        logger.info("User updated successfully")
        return savedUser
    }

    /** Retrieves paginated list of users */
    suspend fun getUsers(page: Int = 0, size: Int = DEFAULT_PAGE_SIZE): PagedResult<User> {
        val actualSize = minOf(size, MAX_PAGE_SIZE)
        logger.debug("Fetching users - page: {}, size: {}", page, actualSize)

        return userRepository.findAll(page, actualSize)
    }

    /** Searches users by criteria */
    suspend fun searchUsers(criteria: UserSearchCriteria): Flow<User> = flow {
        logger.info("Searching users with criteria: {}", criteria)

        val users =
                userRepository.search(
                        email = criteria.email,
                        firstName = criteria.firstName,
                        lastName = criteria.lastName,
                        isActive = criteria.isActive,
                        createdAfter = criteria.createdAfter,
                        createdBefore = criteria.createdBefore
                )

        users.forEach { user -> emit(user) }
    }

    /** Deactivates a user account */
    suspend fun deactivateUser(userId: Long, reason: String) {
        logger.info("Deactivating user with ID: {} - Reason: {}", userId, reason)

        val user =
                getUserById(userId)
                        ?: throw UserNotFoundException("User not found with ID: $userId")

        val deactivatedUser =
                user.copy(
                        isActive = false,
                        deactivatedAt = LocalDateTime.now(),
                        deactivationReason = reason
                )

        userRepository.save(deactivatedUser)
        invalidateUserCache(userId)

        // Send deactivation notification
        emailService.sendAccountDeactivationEmail(user.email, user.firstName, reason)

        logger.info("User deactivated successfully")
    }

    /** Generates user activity report */
    suspend fun generateUserReport(startDate: LocalDateTime, endDate: LocalDateTime): UserReport {
        logger.info("Generating user report from {} to {}", startDate, endDate)

        val totalUsers = userRepository.countAll()
        val activeUsers = userRepository.countByIsActive(true)
        val newUsers = userRepository.countByCreatedAtBetween(startDate, endDate)
        val deactivatedUsers = userRepository.countByDeactivatedAtBetween(startDate, endDate)

        return UserReport(
                totalUsers = totalUsers,
                activeUsers = activeUsers,
                newUsers = newUsers,
                deactivatedUsers = deactivatedUsers,
                reportGeneratedAt = LocalDateTime.now(),
                periodStart = startDate,
                periodEnd = endDate
        )
    }

    /** Validates user request data */
    private fun validateUserRequest(request: CreateUserRequest) {
        if (request.email.isBlank()) {
            throw ValidationException("Email is required")
        }

        if (!isValidEmail(request.email)) {
            throw ValidationException("Invalid email format")
        }

        if (request.firstName.isBlank()) {
            throw ValidationException("First name is required")
        }

        if (request.lastName.isBlank()) {
            throw ValidationException("Last name is required")
        }

        if (request.firstName.length > 50) {
            throw ValidationException("First name cannot exceed 50 characters")
        }

        if (request.lastName.length > 50) {
            throw ValidationException("Last name cannot exceed 50 characters")
        }
    }

    /** Validates email format using simple regex */
    private fun isValidEmail(email: String): Boolean {
        val emailRegex = "^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$"
        return email.matches(emailRegex.toRegex())
    }

    /** Invalidates user cache entries */
    private fun invalidateUserCache(userId: Long) {
        val cacheKey = "$CACHE_PREFIX:$userId"
        cacheManager.evict(cacheKey)
        logger.debug("Cache invalidated for user ID: {}", userId)
    }

    /** Formats user display name */
    fun formatUserDisplayName(user: User): String {
        return "${user.firstName} ${user.lastName}".trim()
    }

    /** Generates user activity summary */
    fun generateActivitySummary(user: User): String {
        val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")
        val createdDate = user.createdAt.format(formatter)
        val status = if (user.isActive) "Active" else "Inactive"

        return "User: ${formatUserDisplayName(user)} | Status: $status | Created: $createdDate"
    }
}

/** Data classes for user operations */
data class CreateUserRequest(
        val email: String,
        val firstName: String,
        val lastName: String,
        val phoneNumber: String? = null
)

data class UpdateUserRequest(
        val firstName: String? = null,
        val lastName: String? = null,
        val phoneNumber: String? = null
)

data class UserSearchCriteria(
        val email: String? = null,
        val firstName: String? = null,
        val lastName: String? = null,
        val isActive: Boolean? = null,
        val createdAfter: LocalDateTime? = null,
        val createdBefore: LocalDateTime? = null
)

data class UserReport(
        val totalUsers: Long,
        val activeUsers: Long,
        val newUsers: Long,
        val deactivatedUsers: Long,
        val reportGeneratedAt: LocalDateTime,
        val periodStart: LocalDateTime,
        val periodEnd: LocalDateTime
)

data class PagedResult<T>(
        val content: List<T>,
        val page: Int,
        val size: Int,
        val totalElements: Long,
        val totalPages: Int
)

/** Custom exceptions */
class UserNotFoundException(message: String) : RuntimeException(message)

class UserAlreadyExistsException(message: String) : RuntimeException(message)

class ValidationException(message: String) : RuntimeException(message)
