package school.it.user

import org.bson.types.ObjectId
import school.it.helper.Helper
import school.it.jwt.JwtUtil

class UserService (private val userRepository: UserRepository) {

    fun saveUser(user: User): Boolean {
        user.encodePass()
        user.creation()

        println("Saving User: $user")

        return userRepository.insert(user)
    }

    fun getUserById(id: String): User {
        return userRepository.findById(id)
    }

    fun updateHighscore(userId: String, score: Int): Boolean {
        return userRepository.updateHighscore(userId, score).modifiedCount == 1L
    }

    fun updateUserInfo(user:User): Boolean {
        return userRepository.updateUserInfo(user).modifiedCount == 1L
    }


    fun checkUserCredentials(login: Login): Boolean {
        val found = userRepository.findByUsername(login.username) ?: return false
        return Helper.checkPass(login.password, found.password)
    }

    fun getExistingApiTokenByUserId(id: String): String? {
        return userRepository.getApiTokenByUserId(ObjectId(id))
    }

    fun saveApiToken(userId: String, token: String) {
        userRepository.updateApiTokenByUserId(userId, token)
    }

    fun getExistingSessionToken(name: String): String? {
        return userRepository.getSessionTokenByUsername(name)
    }

    fun generateAndStoreSessionTokenForUser(name: String): String {
        val user = userRepository.findByUsername(name)!!

        val token = JwtUtil.generateToken(
            userId = user.id.toString(),
            username = user.username,
            issuer = "http://localhost:8080",
            audience = "/api/**",
            isAdmin = user.type == UserType.ADMIN
        )
        userRepository.updateSessionTokenByUsername(user.username, token)
        return token
    }
}