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
    //create User
    post("/api/user") {
        val userDto = call.receive<UserDto>()

        if(userService.saveUser(userDto.toUser()))
            call.respond(HttpStatusCode.Created)
        else
            call.respond(HttpStatusCode.Conflict, "User could not be created")
    }

    //update User
    authenticate("jwt-player") {
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
    }

    //create Admin Users
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

    //login endpoint
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
}