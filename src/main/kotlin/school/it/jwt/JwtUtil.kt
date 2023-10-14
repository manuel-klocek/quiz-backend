package school.it.jwt

import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import com.auth0.jwt.interfaces.DecodedJWT
import java.lang.Exception
import java.security.SecureRandom
import java.time.Instant
import java.util.*

object JwtUtil {
    var secret: String = generateSecret()

    private fun generateSecret(): String {
        val random = SecureRandom()
        val secretBytes = ByteArray(64)
        random.nextBytes(secretBytes)
         return Base64.getUrlEncoder().withoutPadding().encodeToString(secretBytes)
    }

    fun generateToken(userId: String, username: String, issuer: String, audience: String, isAdmin: Boolean = false): String {
        ("Generating token for $username and expires at ${Date.from(Instant.now().plusSeconds(84000))} as Admin: $isAdmin")

        val rawJwt = JWT.create()
            .withAudience(audience)
            .withIssuer(issuer)
            .withClaim("username", username)
            .withSubject(userId)
            .withExpiresAt(Date.from(Instant.now().plusSeconds(84000)))

        if(isAdmin)
            rawJwt.withClaim("permission", "Access to complete Api")

        return rawJwt
            .sign(Algorithm.HMAC512(secret))
    }

    fun validateToken(token: String): Boolean {
        val jwt: DecodedJWT
        try {
            jwt = JWT.require(Algorithm.HMAC512(secret)).build().verify(token)
        } catch (_: Exception) {
            println("Token was created with another secret -> New generated Token is required")
            return false
        }

        return jwt.expiresAtAsInstant > Instant.now()
    }
}