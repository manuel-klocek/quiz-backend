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
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import mu.KotlinLogging
import org.bson.Document

class QuizService(
    private val quizRepository: QuizRepository
) {
    val log = KotlinLogging.logger {}
    private val client = HttpClient()
    private val points = hashMapOf(
            "easy" to 25,
            "medium" to 75,
            "hard" to 150
    )

    init {
        runBlocking {
            updateDb()
        }
    }

    private suspend fun updateDb() {
        val categories = fetchCategories()
        if(categories.isEmpty()) return
        val apiToken = fetchQuizApiToken()
        val questions = mutableListOf<Question>()

        categories.forEach {
            questions.addAll(
                fetchQuestions(
                    category = it.keys.toList()[0],
                    questionNum = it.values.toList()[0].toString().toInt(),
                    apiToken = apiToken
                )
            )
        }

        quizRepository.deleteAllAndInsertNewQuestions(questions)
    }

    private suspend fun fetchQuizApiToken(): String {
        log.info("Fetching Api Token...")
        val response = client.post("https://opentdb.com/api_token.php?command=request")
        return with(response) {
            if (!status.isSuccess()) throw IOException("Unexpected code $response")

            Json.parseToJsonElement(bodyAsText()).jsonObject["token"]!!.jsonPrimitive.content
        }
    }


    private suspend fun fetchCategories(): List<Document> {
        log.info("Fetching Categories and the number of contained questions...")
        val response = client.get("https://opentdb.com/api_count_global.php")
        val categoryList = mutableListOf<Document>()

        val dbQuestionCount = quizRepository.getQuestionCount()
        log.info("Found $dbQuestionCount items in Database")

        return with(response) {
            val body = Json.parseToJsonElement(bodyAsText()).jsonObject

            if(body["overall"]!!.jsonObject["total_num_of_verified_questions"]!!.jsonPrimitive.content.toLong() == dbQuestionCount) {
                log.info("Database is up-to-date!")
                return listOf()
            }

            val categories = body["categories"]!!.jsonObject

            categories.forEach { category ->
                categoryList.add(Document((category.key), category.value.jsonObject["total_num_of_verified_questions"]))
            }

            categoryList
        }
    }

    private suspend fun fetchQuestions(category: String, questionNum: Int, apiToken: String): List<Question> {
        val maxQuestionPerRequest = 50
        var questionsLeftToRequest = questionNum
        var questionNumOnRequest: Int
        val foundQuestions = mutableListOf<Question>()

        while (questionsLeftToRequest > 0) {
            if(questionsLeftToRequest <= maxQuestionPerRequest) {
                questionNumOnRequest = questionsLeftToRequest
                questionsLeftToRequest = 0
            }
            else {
                questionNumOnRequest = 50
                questionsLeftToRequest -= maxQuestionPerRequest
            }


            log.info("Fetching $questionNumOnRequest Questions of category $category")
            val response = client.get {
                url {
                    host = "opentdb.com/api.php"
                    parameters.append("amount", questionNumOnRequest.toString())
                    parameters.append("category", category)
                    parameters.append("token", apiToken)
                    parameters.append("encode", "base64")
                }
            }

            foundQuestions.addAll(with(response) {
                val body = Json.parseToJsonElement(bodyAsText()).jsonObject

                when (body["response_code"].toString()) {
                    "0" -> Helper.mapQuestionsFromJson(body["results"]!!.jsonArray, category)
                    "1" -> throw IOException("Not enough questions in this category yet!")
                    "2" -> throw IOException("Invalid parameter in request url ${response.request.url}")
                    "3" -> throw TokenExpiredException("Token expired", Instant.now())
                    "4" -> throw TokenExpiredException("All questions for Token used", Instant.now())
                    else -> throw IOException("Status Code is not handled!")
                }
            })
        }
        return foundQuestions
    }

    fun calculatePoints(answers: List<QuestionAnswer>): Int {
        val questions = quizRepository.getQuestions(answers)
        var score = 0

        for(i in questions.indices) {
            if(answers[i].correctAnswered)
                score += points[questions[i].difficulty] ?: 0
        }

        return score
    }

    fun getQuestionsForCategoryExcept(answeredIds: List<String>?, categoryId: String, amount: Int = 10): List<Question> {
        return quizRepository.getQuestionsForCategoryExcept(categoryId, answeredIds, amount)
    }

    fun getQuestionsSecureForCategoryExcept(answeredIds: List<String>?, categoryId: String, amount: Int = 10): List<QuestionSecure> {
        return quizRepository.getQuestionsSecureForCategoryExcept(categoryId, answeredIds, amount)
    }

    fun getCategories(): List<Category> {
        return quizRepository.getCategories()
    }

    fun getAnswerFor(questionId: String): String {
        return quizRepository.getQuestion(questionId)?.correctAnswer ?: ""
    }
}