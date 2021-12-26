package com.codexdroid

import io.ktor.application.*
import io.ktor.response.*
import io.ktor.request.*
import io.ktor.routing.*
import io.ktor.http.*
import io.ktor.gson.*
import io.ktor.features.*
import kotlinx.serialization.Serializable
import kotlin.random.Random

fun main(args: Array<String>): Unit = io.ktor.server.netty.EngineMain.main(args)


@Serializable
data class UserData(var status: Int, var message: String, var map:HashMap<String,String>)

@Suppress("unused") // Referenced in application.conf
@kotlin.jvm.JvmOverloads
fun Application.module(testing: Boolean = false) {
    install(ContentNegotiation) {
        gson {}
    }

    routing {
        get("/username/{name}"){
            val name = call.parameters["name"] ?: call.respond(HttpStatusCode.BadRequest,"Enter String only");
            call.respond(HttpStatusCode.OK,"Hello $name, How are you?")
        }

        get("/email/{email}"){
            val email = call.parameters["email"] ?: call.respond(HttpStatusCode.BadRequest,"Enter String only");
            call.respond(HttpStatusCode.OK,"Hello $email, We mail you Secret Key, Please Delete It with in 1 Second ")
        }

        get("/data"){
            val map = HashMap<String,String>()
            map["id"] = 100.toString()
            map["name"] = "Akshay Developer"
            map["email"] = "developer@Codexdroid.com"
            map["mobile"] = "+91 3344886655"

            call.respond(HttpStatusCode.OK,map)
        }

        get("/"){
            val map = HashMap<String,String>()
            Random.nextInt()
            var fname = ""
            var lname = ""
            repeat(6){
                fname += Random.nextInt(65,65+26).toChar().toString()
                lname += Random.nextInt(97,97+26).toChar().toString()
            }

            map["name"] = "$fname $lname"
            map["email"] = "${fname}@codexdroid.com"
            map["mobile"] = Random.nextLong(1111111111,9999999999).toString()

            call.respond(UserData(HttpStatusCode.OK.value,"Here's Your Data",map))
        }
    }
}


