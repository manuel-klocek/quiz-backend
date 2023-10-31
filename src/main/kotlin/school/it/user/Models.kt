package school.it.user

import kotlinx.serialization.Serializable
import org.bson.codecs.pojo.annotations.BsonId
import org.bson.types.ObjectId
import school.it.helper.Helper
import java.time.LocalDateTime

enum class UserType {
    ADMIN,
    PLAYER
}

data class User (
    @BsonId
    var id: ObjectId? = null,
    val type: UserType? = UserType.PLAYER,
    val username: String? = null,
    var password: String? = null,
    var createdAt: LocalDateTime? = LocalDateTime.now(),
    var lastModifiedAt: LocalDateTime? = LocalDateTime.now(),
    val mail: String? = null,
    var highscore: Int? = null,
    var sessionToken: String? = null,
    var answeredQuestionIds: List<String>? = null
)
@Serializable
data class UserDto(
    val id: String? = null,
    val username: String,
    val password: String? = null,
    var highscore: Int? = null,
    var totallyAnsweredQuestions: Int? = null,
    val mail: String
)

@Serializable
data class Login(
    val username: String,
    val password: String
)

fun UserDto.toUser(): User =
    User(
        id = returnIdOrNull(this.id),
        username = this.username.trim(),
        password = this.password,
        mail = this.mail
    )

fun UserDto.toAdmin(): User =
    User(
        id = returnIdOrNull(this.id),
        username = this.username,
        password = this.password,
        type = UserType.ADMIN,
        mail = this.mail
    )

fun User.encodePass() {
    this.password = Helper.encodePass(this.password!!)
}

fun User.creation() {
    this.createdAt = LocalDateTime.now()
    this.lastModifiedAt = LocalDateTime.now()
    this.highscore = 0
}

fun User.modify() {
    this.lastModifiedAt = LocalDateTime.now()
}

fun User.toResponseDto() =
    UserDto(
        id = this.id.toString(),
        username = this.username!!,
        highscore = this.highscore,
        mail = this.mail ?: ""
    )

private fun returnIdOrNull(id: String?): ObjectId? {
    return try {
        ObjectId(id)
    } catch(_: Exception) {
        null
    }
}