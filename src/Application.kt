package com.codexdroid

import com.google.gson.Gson
import com.typesafe.config.ConfigFactory
import io.ktor.application.*
import io.ktor.auth.*
import io.ktor.auth.jwt.*
import io.ktor.config.*
import io.ktor.routing.*
import io.ktor.gson.*
import io.ktor.features.*
import io.ktor.http.*
import io.ktor.request.*
import io.ktor.response.*
import kotlinx.serialization.Serializable
import org.ktorm.database.Database
import org.ktorm.dsl.*
import org.ktorm.schema.Table
import org.ktorm.schema.int
import org.ktorm.schema.varchar

/**** ********************** PROTOTYPES ******************
 *
 *      Add Database in Digital Ocean
 *
 * *******/

private var database: Database? = null

object PersonTable : Table<Nothing>(tableName = "person") {
    val id = int("id").primaryKey()
    val name = varchar("name")
    val email = varchar("email")
    val mobile = varchar("mobile")
    val token = varchar("token")
}

//Received Class
@Serializable
data class PersonRequest(var id: Int,var name: String, var email: String, var mobile: String)

//Respond Class
@Serializable
data class PersonRespond(var id: Int, var name: String, var email: String, var mobile: String, var token: String)

@Serializable
data class CommonResponse(var status: Int, var message: String, var data: List<PersonRespond>?)

@Serializable
data class OneRowResponse(var status: Int, var message: String, var data: PersonRespond?)

@Serializable
data class Tokens(var token :String?)

val config = HoconApplicationConfig(ConfigFactory.load())
private var tokenManager = TokenManager(config)

val personList = mutableListOf<PersonRespond>()

fun main(args: Array<String>): Unit = io.ktor.server.netty.EngineMain.main(args)

@Suppress("unused") // Referenced in application.conf
fun Application.module() {
    install(ContentNegotiation) {
        gson {
            setPrettyPrinting()
            setLenient()
        }
    }

    /****
    install(Authentication) {
        jwt {
            verifier(tokenManager.verifyToken())
            realm = config.property("realm").getString()
            validate { jwtCredential ->

                val email = jwtCredential.payload.getClaim("email").asString()
                if(email.isNotEmpty() || email.endsWith("@gmail.com")){
                    JWTPrincipal(jwtCredential.payload)
                }else null

            }
        }
    }
    ***/

    routing {

        get("/welcome") {
            call.respond("Welcome to codexdroid.com Again")
        }
        registerUser()
        loginUser()
        displayUsers()
        displaySingleUser()
        updateUser()
        deleteUser()
    }
}

private fun getDatabase(): Database? {
    database = if (database == null) {
        Database.connect(
            url = "jdbc:db-test-cluster-do-user-10480409-0.b.db.ondigitalocean.com:25060/defaultdb",
            driver = "com.mysql.cj.jdbc.Driver",
            user = "doadmin",  //same like root
            password = "Up11D8ZXHVjKBn77"
        )
    } else database
    return database
}

private fun checkUserExistsOrNot(request: PersonRequest) : PersonRespond? {
    return getDatabase()?.from(PersonTable)
        ?.select()
        ?.where{ PersonTable.mobile eq request.mobile }
        ?.map { PersonRespond(
            it[PersonTable.id]!!,
            it[PersonTable.name]!!,
            it[PersonTable.email]!!,
            it[PersonTable.mobile]!!,
            it[PersonTable.token]!!,
        )
        }?.firstOrNull()
}

private fun getServerToken(clientToken : String) : Tokens?{

    return getDatabase()
        ?.from(PersonTable)
        ?.select(PersonTable.token)
        ?.where { PersonTable.token eq clientToken }
        ?.map { queryRowSet -> Tokens(queryRowSet[PersonTable.token]) }
        ?.firstOrNull()
}

private fun Routing.registerUser() {

    post("/register") {

        val request: PersonRequest = call.receive()

        if(checkUserExistsOrNot(request) != null){
            //User Already Exists
            call.respond(CommonResponse(HttpStatusCode.Found.value,"Mobile No ${request.mobile} already Exists, Please Login",null))
        }else{
            //User Not registered with Us
            val token = "Bearer ${tokenManager.generateToken(request.mobile)}"
            val affectedRows = getDatabase()?.insert(PersonTable) {
                set(PersonTable.id, 0)
                set(PersonTable.name, request.name)
                set(PersonTable.email, request.email)
                set(PersonTable.mobile, request.mobile)
                set(PersonTable.token, token)
            }
            personList.clear()
            personList.add(PersonRespond(request.id,request.name,request.email,request.mobile,token))

            if (affectedRows!! > 0) {
                call.respond(CommonResponse(HttpStatusCode.OK.value, "Data Inserted Successfully", personList))
            } else {
                call.respond(CommonResponse(HttpStatusCode.NotFound.value, "Error to Insert Data", null))
            }
        }
    }
}

private fun Routing.loginUser(){

    var token = ""

    post("/login"){
        val request = call.receive<PersonRequest>()
        val person = checkUserExistsOrNot(request)

        if( person != null){
            val affectedRows = getDatabase()?.update(PersonTable) {
                token = tokenManager.generateToken(request.mobile)
                set(PersonTable.token,"Bearer $token")
                where {
                    (PersonTable.id eq request.id) and (PersonTable.mobile eq request.mobile)
                }
            }
            if(affectedRows!! > 0){
                call.respond(CommonResponse(HttpStatusCode.Found.value,"User Login Successfully \n New Token : Bearer $token",null))
            }else{
                call.respond(CommonResponse(HttpStatusCode.NotFound.value,"Login Fail, Check Mobile and ID",null))
            }
        }else {
            call.respond(CommonResponse(HttpStatusCode.NotFound.value,"Mobile ${request.mobile} Not registered with us",null))
        }
    }
}

private fun Routing.displayUsers() {

    //Logic : Existing User Can See List of other user based on Token

    get("/view") {
        val clientToken = call.request.authorization().toString()
        println("Client Token $clientToken === ${getServerToken(clientToken)} ")
        if (clientToken == getServerToken(clientToken)?.token) {
            personList.clear()
            val query = getDatabase()?.from(PersonTable)?.select()
            for (row in query!!) {
                personList.add(
                    PersonRespond(
                        row[PersonTable.id]!!,
                        row[PersonTable.name]!!,
                        row[PersonTable.email]!!,
                        row[PersonTable.mobile]!!,
                        row[PersonTable.token]!!))
            }
            if (personList.isNotEmpty()) {
                call.respond(CommonResponse(HttpStatusCode.OK.value, "All Data Retrieved Successfully", personList))
            } else {
                call.respond(CommonResponse(HttpStatusCode.OK.value, "Some Error detected to Retrieved Data", null))
            }
        }else {
            call.respond(CommonResponse(HttpStatusCode.NotFound.value,"Please Provide Token to get access",null))
        }
    }
}

private fun Routing.displaySingleUser(){

    get("/view/{id}"){

        val clientToken = call.request.authorization().toString()

        if (clientToken == getServerToken(clientToken)?.token) {

            val id = call.parameters["id"]?.toInt()
            val person = getDatabase()?.from(PersonTable)?.select()?.where{ (PersonTable.id eq id!!) and ( PersonTable.id eq id)
            }?.map {
                PersonRespond(
                    it[PersonTable.id]!!,
                    it[PersonTable.name]!!,
                    it[PersonTable.email]!!,
                    it[PersonTable.mobile]!!,
                    it[PersonTable.token]!!,
                )
            }?.firstOrNull()

            if(person != null){
                call.respond(OneRowResponse(HttpStatusCode.Found.value,"We Found Your Data", person))
            }else{
                call.respond(OneRowResponse(HttpStatusCode.NotFound.value,"We Cannot Found Your Data, Match Id and token", null))
            }
        }else{
            call.respond(CommonResponse(HttpStatusCode.NotFound.value,"Please Provide Token to get access",null))
        }
    }
}

private fun Routing.updateUser(){

    put("/update"){

        val clientToken = call.request.authorization().toString()
        if (clientToken == getServerToken(clientToken)?.token) {

            val person = call.receive<PersonRequest>()
            val affectedRow = getDatabase()?.update(PersonTable){
                set(PersonTable.name,person.name)
                set(PersonTable.email,person.email)
                set(PersonTable.mobile,person.mobile)
                where { (PersonTable.id eq person.id) and (PersonTable.mobile eq person.mobile)}
            }
            if(affectedRow!! > 0){
                call.respond(CommonResponse(status = HttpStatusCode.OK.value, message = "Data of ${person.name} Updated",null))
            }else {
                call.respond(CommonResponse(status = HttpStatusCode.NotFound.value, message = "Unable to Update data,id, mobile and token should be same", null))
            }
        }else{
            call.respond(CommonResponse(HttpStatusCode.NotFound.value,"Please Provide Token to get access",null))
        }
    }
}

private fun Routing.deleteUser(){

    //Make sure Id and token is in same row to delete data

    delete("/deleteUser/{id}"){

        val clientToken = call.request.authorization().toString()

        if (clientToken == getServerToken(clientToken)?.token) {

            val id = call.parameters["id"]?.toInt()

            val person = getDatabase()?.from(PersonTable)?.select()?.where{ PersonTable.id eq id!! }?.map {
                PersonRespond(
                    it[PersonTable.id]!!,
                    it[PersonTable.name]!!,
                    it[PersonTable.email]!!,
                    it[PersonTable.mobile]!!,
                    it[PersonTable.token]!!)
            }?.firstOrNull()

            println("Person : ${Gson().toJson(person)} ====> $id == ${person?.id}")

            if(person?.id == id){
                val affectedRow = getDatabase()?.delete(PersonTable){ PersonTable.id eq id!! }

                if(affectedRow!! > 0){
                    call.respond(CommonResponse(status = HttpStatusCode.OK.value, message = "Data Deleted",null))
                }else {
                    call.respond(CommonResponse(status = HttpStatusCode.NotFound.value, message = "Unable to Delete Data, please send correct Id", null))
                }
            }else{
                call.respond(CommonResponse(status = HttpStatusCode.NotFound.value, message = "You are not authorised to delete data, please send correct id and token", null))
            }
        }else{
            call.respond(CommonResponse(HttpStatusCode.NotFound.value,"Please Provide Token to get access",null))
        }
    }
}





