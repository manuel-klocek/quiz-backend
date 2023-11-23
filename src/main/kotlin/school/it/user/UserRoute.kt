package school.it.user

import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.auth.jwt.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import school.it.jwt.JwtUtil

fun Routing.routeUser(
    userService: UserService
) {
    post("/api/login") {
        val login = call.receive<Login>()

        if(!userService.checkUserCredentials(login)) {
            call.respond(HttpStatusCode.Unauthorized, "Credentials dont match")
            return@post
        }

        var token = userService.getExistingSessionToken(login.username)

        if(token == null || !JwtUtil.validateToken(token))
        {
            token = userService.generateAndStoreSessionTokenForUser(login.username)
        }

        call.respond(HttpStatusCode.Created, hashMapOf("token" to token))
    }

    post("/api/user") {
        val userDto = call.receive<UserDto>()

        userDto.highscore = 0
        userDto.totallyAnsweredQuestions = 0

        if(userService.saveUser(userDto.toUser()))
            call.respond(HttpStatusCode.Created)
        else
            call.respond(HttpStatusCode.Conflict, "User could not be created")
    }

    authenticate("jwt-player") {
        get("/api/user-info") {
            val requestId = call.principal<JWTPrincipal>()!!.payload.subject

            val userDto = userService.getUserById(requestId)

            if (userDto == null) {
                call.respond(HttpStatusCode.BadRequest, "User not found")
                return@get
            }

            call.respond(hashMapOf("user-info" to userDto))
        }

        get("/api/scoreboard") {

            val pageNumber = (call.parameters["pageNumber"] ?: "0").toInt()
            val pageSize = call.parameters["pageSize"]?.toInt()

            val users: List<User>
            if(pageSize != null)
                users = userService.getUsersForScoreboard(pageNumber, pageSize)
            else {
                users = userService.getUsersForScoreboard()
            }

            val userDtos = mutableListOf<UserDto>()

            users.forEach {
                val dto = it.toResponseDto()
                dto.totallyAnsweredQuestions = it.answeredQuestionIds?.size ?: 0
                userDtos.add(dto)
            }

            call.respond(userDtos)
        }

        put("/api/user") {
            val userDto = call.receive<UserDto>()
            val requesterId = call.principal<JWTPrincipal>()!!.payload.subject

            if(userDto.id.isNullOrEmpty()) {
                call.respond(HttpStatusCode.BadRequest, "An id is required to update the resource")
                return@put
            }

            if(userDto.id != requesterId) {
                call.respond(HttpStatusCode.Forbidden, "Access to resource is restricted")
                return@put
            }

            val updated = userService.updateUser(userDto.toUser())

            if(!updated) {
                call.respond(HttpStatusCode.BadRequest, "User could not be updated or wasn't found")
                return@put
            }

            call.respond(HttpStatusCode.Accepted)
        }

        delete("/api/user") {
            val requesterId = call.principal<JWTPrincipal>()!!.payload.subject

            val deleted = userService.deleteUser(requesterId)

            if(deleted)
                call.respond(HttpStatusCode.OK)
            else
                call.respond(HttpStatusCode.BadRequest)
        }

        delete("/api/logout") {
            val userId = call.principal<JWTPrincipal>()!!.payload.subject

            userService.deleteSessionToken(userId)

            call.respond(HttpStatusCode.OK)
        }
    }

    authenticate("jwt-admin") {
        post("/api/user") {
            val adminDto = call.receive<UserDto>()

            val saved = userService.saveUser(adminDto.toAdmin())

            if(!saved)
                call.respond(HttpStatusCode.BadRequest, "Resource could not be created")
            else
                call.respond(HttpStatusCode.Created)
        }
    }
}