package school.it

import io.ktor.server.application.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import io.ktor.server.routing.*
import io.netty.handler.codec.http.HttpObjectDecoder
import io.netty.handler.codec.http.HttpServerCodec
import school.it.plugins.*
import school.it.quiz.QuizRepository
import school.it.quiz.QuizService
import school.it.quiz.cache.QuizCacheRepository
import school.it.quiz.cache.QuizCacheService
import school.it.quiz.routeQuiz
import school.it.user.UserRepository
import school.it.user.UserService
import school.it.user.routeUser

fun main() {
    embeddedServer(
        Netty,
        applicationEngineEnvironment {
            connector {
                port = 8080
            }
            module { configure() }
        },
    ) {
        httpServerCodec = {
            HttpServerCodec(
                HttpObjectDecoder.DEFAULT_MAX_INITIAL_LINE_LENGTH,
                32 * 1024, //max header size
                HttpObjectDecoder.DEFAULT_MAX_CHUNK_SIZE
            )
        }
    }
        .start(wait = true)
}

fun Application.configure() {
    val userRepository = UserRepository()
    val userService = UserService(userRepository)
    val quizRepository = QuizRepository()
    val quizService = QuizService(quizRepository)
    val quizCacheRepository = QuizCacheRepository()
    val quizCacheService = QuizCacheService(quizCacheRepository, quizRepository)

    configureSecurity()
    configureSerialization()

    routing {
        routeUser(userService)
        routeQuiz(quizService, userService, quizCacheService)
    }
}
