package school.it.user

import com.mongodb.BasicDBObject
import com.mongodb.MongoException
import com.mongodb.client.model.IndexOptions
import com.mongodb.client.model.Indexes
import com.mongodb.client.result.UpdateResult
import org.bson.Document
import org.bson.types.ObjectId
import org.litote.kmongo.*

class UserRepository {
    private val client = KMongo.createClient()
    private val database = client.getDatabase("Quiz")
    private val userCollection = database.getCollection<User>()

    init {
        userCollection.createIndex(Indexes.ascending(User::username.name), IndexOptions().unique(true))
    }

    fun insert(user: User): Boolean {
        return try {
            userCollection.insertOne(user)
            println("Inserted User: $user")
            true
        } catch (ignored: MongoException) {
            println("Insert failed, User already exists!")
            false
        }
    }

    fun findById(id: String): User {
        return userCollection.findOne(User::id eq ObjectId(id))!!
    }

    fun updateHighscore(id: String, score: Int): UpdateResult {
        val updateFields = BasicDBObject(User::highscore.name, score)
        return userCollection.updateOne(User::id eq ObjectId(id), BasicDBObject("\$set", updateFields))
    }

    fun updateUserInfo(user: User): UpdateResult {
        println("Started Update with Values: $user")
        user.modify()
        user.encodePass()

        val updateFields = BasicDBObject()
        updateFields.append(User::username.name, user.username)
        updateFields.append(User::password.name, user.password)
        updateFields.append(User::mail.name, user.mail)
        updateFields.append(User::lastModifiedAt.name, user.lastModifiedAt)
        val updateDoc = BasicDBObject("\$set", updateFields)

        return userCollection.updateOne(User::id eq user.id, updateDoc)
    }

    fun findByUsername(name: String): User? {
        return userCollection.findOne(User::username eq name)
    }

    fun getSessionTokenByUsername(name: String): String? {
        return userCollection.findOne(User::username eq name)?.sessionToken
    }

    fun updateSessionTokenByUsername(name: String, token: String): UpdateResult {
        val updateDoc = BasicDBObject("\$set", Document(User::sessionToken.name, token))
        println("Storing Session-Token, for User: $name")

        return userCollection.updateOne(User::username eq name, updateDoc)
    }

    fun getApiTokenByUserId(id: ObjectId): String? {
        return userCollection.findOne(User::id eq id)!!.quizApiToken
    }

    fun updateApiTokenByUserId(id: String, token: String) {
        val updateFields = BasicDBObject()
        updateFields.append(User::quizApiToken.name, token)
        val updateDoc = BasicDBObject("\$set", updateFields)
        println("Storing Api-Token, for User with Id: $id")

        userCollection.updateOne(User::id eq ObjectId(id), updateDoc)
    }
}