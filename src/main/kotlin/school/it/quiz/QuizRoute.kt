package school.it.quiz

import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.auth.jwt.*
import io.ktor.server.request.receive
import io.ktor.server.response.*
import io.ktor.server.routing.*
import mu.KotlinLogging
import org.bson.types.ObjectId
import school.it.helper.Helper
import school.it.user.User
import school.it.user.UserService

private val log = KotlinLogging.logger {}

fun Routing.routeQuiz(
    quizService: QuizService,
    userService: UserService
) {
    authenticate("jwt-player") {
        //get Questions
        get("/api/questions") {
            val userId = call.principal<JWTPrincipal>()!!.payload.subject
            val categoryId = call.parameters["category"]

            if(categoryId == null) {
                log.error("CategoryId must be provided")
                call.respond(HttpStatusCode.BadRequest, "Category must be provided")
                return@get
            }

            val answeredIds = userService.getAnsweredQuestionIds(userId)
            val questions = quizService.getQuestionsForCategoryExcept(answeredIds, categoryId)

            val questionDtos = mutableListOf<QuestionDto>()
            questions.forEach { questionDtos.add(it.toDto()) }

            call.respond(HttpStatusCode.OK, questionDtos)
        }

        //get available categories
        get("/api/categories") {
            val categories = quizService.getCategories()

            call.respond(categories)
        }

        //send answers to Backend
        post("/api/answers") {
            val userId = call.principal<JWTPrincipal>()!!.payload.subject
            val answers = call.receive<List<QuestionAnswer>>()
            val player = userService.getUserById(userId)

            val score = quizService.calculatePoints(answers)

            if(player.highscore!! < score) {
                userService.updateUser(
                    User (
                        id = ObjectId(userId),
                        highscore = score
                    )
                )
            }

            val answerIds = mutableListOf<String>()
            answers.forEach { answerIds.add(it.questionId) }
            userService.addAnsweredQuestionsToUser(userId, answerIds)

            call.respond(HttpStatusCode.OK, hashMapOf("score" to score, "highscore" to player.highscore))
        }
    }
}