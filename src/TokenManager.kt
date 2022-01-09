package com.codexdroid

import com.auth0.jwt.JWT
import com.auth0.jwt.JWTVerifier
import com.auth0.jwt.algorithms.Algorithm
import io.ktor.config.*
import java.util.*

class TokenManager(val config : HoconApplicationConfig) {

    private val oneMin = 60_000
    private val oneHr = oneMin * 60

    private val audience = config.property("audience").getString()
    private val secret = config.property("secret").getString()
    private val issuer = config.property("issuer").getString()
    private val tokenExpiration = Date(System.currentTimeMillis().plus(5000L))
    //Token Expiration Exaplanation : if you generate 10 tokens after generating single one with in 5 sec all tokens will be same
    // if you want to generate different token in each request then reduce token expiration time in milisec. ex 100
    //Token expired after every 5 sec


    //We should be compulsory pass here to generate new token base on unit key
    //here I am passing different mobile no to generate different token

    fun generateToken(mobile:String): String {
        return JWT.create()
            .withAudience(audience)
            .withIssuer(issuer)
            .withClaim("mobile",mobile)
            .withExpiresAt(tokenExpiration)
            .sign(Algorithm.HMAC256(secret))
    }

    //Not Call yet
    fun verifyToken() : JWTVerifier{
        return JWT.require(Algorithm.HMAC256(secret))
            .withAudience(audience)
            .withIssuer(issuer)
            .build()
    }
}