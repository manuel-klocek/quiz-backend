package school.it.helper

import io.ktor.http.decodeURLQueryComponent
import io.ktor.util.decodeBase64String
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.mindrot.jbcrypt.BCrypt
import school.it.quiz.Question

object Helper {
    fun encodePass(password: String): String {
        return BCrypt.hashpw(password, BCrypt.gensalt())
    }

    fun checkPass(password: String, hashedPassword: String): Boolean {
        return BCrypt.checkpw(password, hashedPassword)
    }

    fun mapQuestionsFromJson(jsonArr: JsonArray): List<Question> {
        val questionDtos = mutableListOf<Question>()
        jsonArr.forEach { element ->
            val question = element.jsonObject
            questionDtos.add(
                Question(
                    category = question["category"]!!.jsonPrimitive.content.decodeBase64String(),
                    type = question["type"]!!.jsonPrimitive.content.decodeBase64String(),
                    difficulty = question["difficulty"]!!.jsonPrimitive.content.decodeBase64String(),
                    question = question["question"]!!.jsonPrimitive.content.decodeBase64String(),
                    correctAnswer = question["correct_answer"]!!.jsonPrimitive.content.decodeBase64String(),
                    incorrectAnswers = question["incorrect_answers"]!!.jsonArray.map { it.jsonPrimitive.content.decodeBase64String() }
                )
            )
        }
        return questionDtos
    }
}