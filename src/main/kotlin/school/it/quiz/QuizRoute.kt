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
import school.it.quiz.cache.QuizCacheService
import school.it.user.User
import school.it.user.UserService

private val log = KotlinLogging.logger {}

fun Routing.routeQuiz(
    quizService: QuizService,
    userService: UserService,
    quizCacheService: QuizCacheService
) {
    authenticate("jwt-player") {
        get("/api/questions") {
            val userId = call.principal<JWTPrincipal>()!!.payload.subject
            val categoryId = call.parameters["category"]

            if(categoryId == null) {
                log.error("CategoryId must be provided")
                call.respond(HttpStatusCode.BadRequest, "Category must be provided")
                return@get
            }

            val answeredIds = userService.getAnsweredQuestionIds(userId)
            val questions = quizService.getQuestionsSecureForCategoryExcept(answeredIds, categoryId)

            call.respond(HttpStatusCode.OK, questions)
        }

        //get available categories
        get("/api/categories") {
            val categories = quizService.getCategories()

            call.respond(categories)
        }

        post("/api/answer") {
            val userId = call.principal<JWTPrincipal>()!!.payload.subject
            val answer = call.receive<QuestionAnswerSecure>()

            quizCacheService.cacheAnswerFor(userId, answer)
            val correctAnswer = quizService.getAnswerFor(answer.questionId)

            call.respond(HttpStatusCode.Accepted, hashMapOf("correctAnswer" to correctAnswer))
        }

        post("/api/finish-quiz") {
            val userId = call.principal<JWTPrincipal>()!!.payload.subject
            val player = userService.getUserById(userId)

            val answers = quizCacheService.getQuestionsAnswered(userId)
            val score = quizService.calculatePoints(answers)

            if(player!!.highscore!! < score) {
                userService.updateUser(
                    User(
                        id = ObjectId(userId),
                        highscore = score,
                        icon = player.icon
                    )
                )
            }

            val answerIds = mutableListOf<String>()
            answers.forEach { answerIds.add(it.questionId) }
            userService.addAnsweredQuestionsToUser(userId, answerIds)

            quizCacheService.clearCacheFor(userId)

            call.respond(HttpStatusCode.OK, hashMapOf("score" to score, "highscore" to player.highscore))
        }
    }
}