package school.it.user

import mu.KotlinLogging
import school.it.helper.Helper
import school.it.jwt.JwtUtil

class UserService (private val userRepository: UserRepository) {
    private val log = KotlinLogging.logger {}

    fun saveUser(user: User): Boolean {
        user.encodePass()
        user.creation()

        log.info("Saving User: $user")

        return userRepository.insert(user)
    }

    fun getUserById(id: String): UserDto? {
        val userDto = userRepository.findById(id)?.toResponseDto() ?: return null

        userDto.totallyAnsweredQuestions = userRepository.getAnsweredIds(id)?.size ?: 0

        return userDto
    }

    fun updateUser(user: User): Boolean {
        return userRepository.updateUser(user).modifiedCount == 1L
    }

    fun getUsersForScoreboard(pageNumber: Int? = null, pageSize: Int? = null): List<User> {
        if(pageNumber != null && pageSize != null)
            return userRepository.getUsersForHighscore(pageNumber, pageSize)

        return userRepository.getAllUsersForHighscore()
    }

    fun checkUserCredentials(login: Login): Boolean {
        val found = userRepository.findByUsername(login.username.trim()) ?: return false
        return Helper.checkPass(login.password, found.password!!)
    }

    fun getExistingSessionToken(name: String): String? {
        return userRepository.getSessionTokenByUsername(name)
    }

    fun generateAndStoreSessionTokenForUser(name: String): String {
        val user = userRepository.findByUsername(name)!!

        val token = JwtUtil.generateToken(
            userId = user.id.toString(),
            username = user.username!!,
            issuer = "http://localhost:8080",
            audience = "/api/**",
            isAdmin = user.type == UserType.ADMIN
        )
        userRepository.updateSessionTokenByUsername(name, token)
        return token
    }

    fun getAnsweredQuestionIds(userId: String): List<String>? {
        return userRepository.getAnsweredIds(userId)
    }

    fun addAnsweredQuestionsToUser(userId: String, answerIds: List<String>): Boolean {
        return userRepository.addAnsweredIds(userId, answerIds).modifiedCount == 1L
    }

    fun deleteSessionToken(userId: String): Boolean {
        return userRepository.deleteSessionToken(userId).modifiedCount == 1L
    }
}