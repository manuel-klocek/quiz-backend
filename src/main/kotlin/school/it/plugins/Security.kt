package school.it.plugins

import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import io.ktor.client.*
import io.ktor.client.engine.apache.*
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.auth.jwt.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.server.sessions.*
import school.it.jwt.JwtUtil
import school.it.user.Login
import school.it.user.UserService
import java.time.Instant
import java.time.LocalDateTime
import java.util.*
import mu.KotlinLogging
import school.it.helper.Helper

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
                JWTPrincipal(credential.payload)
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
}
//    val jwtAudience = "jwt-audience"
//    val jwtDomain = "https://jwt-provider-domain/"
//    val jwtRealm = "ktor sample app"
//    val jwtSecret = "secret"
//    authentication {
//        jwt {
//            realm = jwtRealm
//            verifier(
//                JWT
//                    .require(Algorithm.HMAC256(jwtSecret))
//                    .withAudience(jwtAudience)
//                    .withIssuer(jwtDomain)
//                    .build()
//            )
//            validate { credential ->
//                if (credential.payload.audience.contains(jwtAudience)) JWTPrincipal(credential.payload) else null
//            }
//        }
//    }
//    data class MySession(val count: Int = 0)
//    install(Sessions) {
//        cookie<MySession>("MY_SESSION") {
//            cookie.extensions["SameSite"] = "lax"
//        }
//    }
//    authentication {
//        oauth("auth-oauth-google") {
//            urlProvider = { "http://localhost:8080/callback" }
//            providerLookup = {
//                OAuthServerSettings.OAuth2ServerSettings(
//                    name = "google",
//                    authorizeUrl = "https://accounts.google.com/o/oauth2/auth",
//                    accessTokenUrl = "https://accounts.google.com/o/oauth2/token",
//                    requestMethod = HttpMethod.Post,
//                    clientId = System.getenv("GOOGLE_CLIENT_ID"),
//                    clientSecret = System.getenv("GOOGLE_CLIENT_SECRET"),
//                    defaultScopes = listOf("https://www.googleapis.com/auth/userinfo.profile")
//                )
//            }
//            client = HttpClient(Apache)
//        }
//    }
//    routing {
//        get("/session/increment") {
//            val session = call.sessions.get<MySession>() ?: MySession()
//            call.sessions.set(session.copy(count = session.count + 1))
//            call.respondText("Counter is ${session.count}. Refresh to increment.")
//        }
//        authenticate("auth-oauth-google") {
//            get("login") {
//                call.respondRedirect("/callback")
//            }
//
//            get("/callback") {
//                val principal: OAuthAccessTokenResponse.OAuth2? = call.authentication.principal()
//                call.sessions.set(UserSession(principal?.accessToken.toString()))
//                call.respondRedirect("/hello")
//            }
//        }
//    }
//}

class UserSession(accessToken: String)

