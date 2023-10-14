package school.it.quiz

import com.auth0.jwt.exceptions.TokenExpiredException
import io.ktor.client.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.utils.io.errors.IOException
import kotlinx.serialization.json.*
import school.it.helper.Helper
import java.time.Instant

class QuizService(
    private val quizRepository: QuizRepository
) {
    private val points = hashMapOf(
            "easy" to 25,
            "medium" to 75,
            "hard" to 150
    )
    private val client = HttpClient()

    suspend fun fetchQuizApiToken(): String {
        val response = client.post("https://opentdb.com/api_token.php?command=request")
        return with(response) {
            if (!status.isSuccess()) throw IOException("Unexpected code $response")

            Json.parseToJsonElement(bodyAsText()).jsonObject["token"]!!.jsonPrimitive.content
        }
    }

    suspend fun fetchQuestions(category: String, apiToken: String): List<Question> {
        println("Fetching questions of category $category")
        val response = client.get {
            url {
                host = "opentdb.com/api.php"
                parameters.append("amount", "10")
                parameters.append("category", category)
                parameters.append("token", apiToken)
                parameters.append("encode", "base64")
            }
        }

        return with(response) {
            val body = Json.parseToJsonElement(bodyAsText()).jsonObject

            when (body["response_code"].toString()) {
                "0" -> Helper.mapQuestionsFromJson(body["results"]!!.jsonArray)
                "1" -> throw IOException("Not enough questions in this category yet!")
                "2" -> throw IOException("Invalid parameter in request url ${response.request.url}")
                "3" -> throw TokenExpiredException("Token expired", Instant.now())
                "4" -> throw TokenExpiredException("All questions for Token used", Instant.now())
                else -> throw IOException("Status Code is not handled!")
            }
        }
    }

    fun saveQuestions(questions: List<Question>): List<Question> {
        return quizRepository.insertAndGet(questions)
    }

    fun calculatePoints(answers: List<QuestionAnswer>): Int {
        val questions = quizRepository.getQuestions(answers)
        val score = 0

        questions.forEach { score.plus(points[it.difficulty] ?: 0) }

        return score
    }
}