package com.example.buybye.auth

import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import java.math.BigInteger
import java.security.MessageDigest
import java.util.*


@Component
class AuthTokenGenerator(
    @Value("\${secret.access-token}") val accessToken: String,
    @Value("\${secret.secret-token}") val secretToken: String,
) {

    fun generateJwt(): String {
        val algorithm = Algorithm.HMAC256(secretToken)
        return JWT.create()
            .withClaim("access_key", accessToken)
            .withClaim("nonce", UUID.randomUUID().toString())
            .sign(algorithm)
    }

    fun generateJwtWithQueryString(query: java.lang.String): String {
        val md = MessageDigest.getInstance("SHA-512")
        md.update(query.getBytes("utf8"))

        val queryHash = String.format("%0128x", BigInteger(1, md.digest()))
        val algorithm: Algorithm = Algorithm.HMAC256(secretToken)
        return JWT.create()
            .withClaim("access_key", accessToken)
            .withClaim("nonce", UUID.randomUUID().toString())
            .withClaim("query_hash", queryHash)
            .withClaim("query_hash_alg", "SHA512")
            .sign(algorithm)
    }
}