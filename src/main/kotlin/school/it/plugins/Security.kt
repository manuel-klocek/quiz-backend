package school.it.plugins

import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.auth.jwt.*
import io.ktor.server.plugins.cors.routing.*
import io.ktor.server.response.*
import java.time.Instant
import java.util.*
import mu.KotlinLogging
import school.it.jwt.JwtUtil

fun Application.configureSecurity() {
    val log = KotlinLogging.logger {}

    install(Authentication) {
        jwt("jwt-player") {
            realm = "Player Access to Api"
            try {
                verifier(
                    JWT
                        .require(Algorithm.HMAC512(JwtUtil.secret))
                        .withAudience("/api/**")
                        .withIssuer("http://localhost:8080")
                        .build()
                )
            } catch (_: Exception) {
                log.error("Authentication: Token decryption failed with secret")
            }

            validate { credential ->
                if(credential.expiresAt!!.after(Date.from(Instant.now().plusSeconds(300)))) {
                    JWTPrincipal(credential.payload)
                } else{
                    null
                }
            }

            challenge { _, _ ->
                call.respond(HttpStatusCode.Unauthorized, "Token is not valid or expired!")
            }
        }

        jwt("jwt-admin") {
            realm = "Admin Access to Api"
            try {
                verifier(
                    JWT
                        .require(Algorithm.HMAC512(JwtUtil.secret))
                        .withAudience("/**")
                        .withIssuer("http://localhost:8080")
                        .withClaim("permission", "Access to complete Api")
                        .build()
                )
            } catch (_: Exception) {
                log.error("Authentication: Token decryption failed with secret")
            }
            validate { credential ->
                JWTPrincipal(credential.payload)
            }
            challenge { _, _ ->
                call.respond(
                    HttpStatusCode.Unauthorized,
                    "Token is invalid, expired or does not have enough rights to Access resource"
                )
            }
        }
    }

    install(CORS) {
        anyHost()
        allowMethod(HttpMethod.Get)
        allowMethod(HttpMethod.Post)
        allowMethod(HttpMethod.Put)
        allowMethod(HttpMethod.Delete)
        allowMethod(HttpMethod.Head)
        allowMethod(HttpMethod.Options)
        allowHeader("Authorization")
        allowNonSimpleContentTypes = true
    }
}