package com.codexdroid

import com.google.gson.Gson
import io.ktor.application.*
import io.ktor.response.*
import io.ktor.request.*
import io.ktor.routing.*
import io.ktor.http.*
import io.ktor.gson.*
import io.ktor.features.*
import kotlinx.serialization.Serializable
import org.ktorm.database.Database
import org.ktorm.dsl.insert
import org.ktorm.schema.Table
import org.ktorm.schema.int
import org.ktorm.schema.long
import org.ktorm.schema.varchar
import kotlin.random.Random

private var database: Database? = null

fun main(args: Array<String>): Unit = io.ktor.server.netty.EngineMain.main(args)


//Create table for inserting data into database
object PersonTable : Table<Nothing>(tableName = "person"){          // This class define to send data to database // 'person' is tablename define/created in  phpmyadmin
    val id = int("id").primaryKey()
    val name = varchar("name")
    val email = varchar("email")
    val mobile = varchar("mobile")
}

fun getDatabase() : Database? {
    database = if(database == null){
        Database.connect(
            url = "jdbc:mysql://test-db-do-user-10480409-0.b.db.ondigitalocean.com",
            driver = "com.mysql.cj.jdbc.Driver",
            user = "doadmin",
            password = "9AvhLIGrecrpdHfl")
    }else database
    return database
}


@Serializable
data class Person(var id:Int,var name: String,var email:String,var mobile:String)



@Serializable
data class UserData(var status: Int, var message: String, var person : Person)

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

        post("/data"){
            val person = call.receive<Person>()

            println("====> ${call.request.authorization()}")

            val affectedRows = getDatabase()?.insert(PersonTable){
                set(PersonTable.id,person.id)
                set(PersonTable.name,person.name)
                set(PersonTable.email,person.email)
                set(PersonTable.mobile,person.mobile)
            }

            if(affectedRows == 1){
                call.respond("Data inserted")
            }else{
                call.respond("Data Could not insert")
            }


            call.respond(HttpStatusCode.OK,person)
        }

        get("/"){
            Random.nextInt()
            var fname = ""
            var lname = ""
            repeat(6){
                fname += Random.nextInt(65,65+26).toChar().toString()
                lname += Random.nextInt(97,97+26).toChar().toString()
            }

            val person = Person(Random.nextInt(100,999),"$fname $lname","${fname}@codexdroid.com",Random.nextLong(1111111111,9999999999).toString())
            call.respond(UserData(HttpStatusCode.OK.value,"Here's Your Data",person))
        }

        get("/info"){
            call.respond("No Information available for now, pls keep visiting")
        }
    }
}


