package school.it.quiz

import com.auth0.jwt.exceptions.TokenExpiredException
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.auth.jwt.*
import io.ktor.server.request.receive
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.utils.io.errors.*
import kotlinx.serialization.json.Json
import org.litote.kmongo.json
import school.it.user.UserService

fun Routing.routeQuiz(
    quizService: QuizService,
    userService: UserService
) {
    authenticate("jwt-player") {
        //get Questions
        get("/api/questions") {
            val userId = call.principal<JWTPrincipal>()!!.payload.subject
            val category = call.parameters["category"]

            if(category == null) {
                call.respond(HttpStatusCode.BadRequest, "Category must be provided")
                return@get
            }

            var apiToken = userService.getExistingApiTokenByUserId(userId)

            if(apiToken == null) {
                apiToken = quizService.fetchQuizApiToken()
                userService.saveApiToken(userId, apiToken)
            }

            val questions = try {
                quizService.fetchQuestions(category, apiToken)
            } catch (ex: TokenExpiredException) {
                println("Expired token for User: $userId. Regenerating and retrying...")
                val newToken = quizService.fetchQuizApiToken()
                userService.saveApiToken(userId, newToken)
                println("Saved regenerated Token")
                quizService.fetchQuestions(category, newToken)
            } catch (ex: IOException) {
                println("Critical Failure on Quiz-Api request")
                call.respond(HttpStatusCode.Conflict, ex.message!!)
                return@get
            }

            println("Successfully fetched ${questions.size} Questions")

            val saved = quizService.saveQuestions(questions)
            println("Successfully saved ${saved.size} Questions")

            val questionDtos = mutableListOf<QuestionDto>()
            saved.forEach { questionDtos.add(it.toDto()) }

            call.respond(HttpStatusCode.OK, questionDtos)
        }
    }

    authenticate("jwt-player") {
        //post answers and get points and highscore
        post("/api/answers") {
            val userId = call.principal<JWTPrincipal>()!!.payload.subject
            val answers = call.receive<List<QuestionAnswer>>()
            val player = userService.getUserById(userId)

            val score = quizService.calculatePoints(answers)

            userService.saveAnsweredQuestions(userId, answers)

            if(player.highscore!! < score) {
                userService.updateHighscore(userId, score)
            }

            call.respond(HttpStatusCode.OK, hashMapOf("score" to score, "highscore" to player.highscore))
        }
    }
}