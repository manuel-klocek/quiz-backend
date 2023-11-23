package school.it.user

import com.mongodb.BasicDBObject
import com.mongodb.MongoException
import com.mongodb.client.model.IndexOptions
import com.mongodb.client.model.Indexes
import com.mongodb.client.result.DeleteResult
import com.mongodb.client.result.UpdateResult
import mu.KotlinLogging
import org.bson.Document
import org.bson.types.ObjectId
import org.litote.kmongo.*
import school.it.helper.GetEnvVars
import kotlin.reflect.full.memberProperties

class UserRepository {
    private val database = GetEnvVars.getMongoDbAsDb
    private val userCollection = database.getCollection<User>()
    private val log = KotlinLogging.logger {}

    init {
        userCollection.createIndex(Indexes.ascending(User::username.name), IndexOptions().unique(true))
        userCollection.createIndex(Indexes.descending(User::highscore.name), IndexOptions().unique(false))

        val admin = User(type = UserType.ADMIN, username = "admin", password = "admin", mail = "admin.local@quiz.me", icon = "Avatar1")

        insert(admin)
    }

    fun insert(user: User): Boolean {
        return try {
            userCollection.insertOne(user)
            log.info("Inserted User: $user")
            true
        } catch (ignored: MongoException) {
            log.error("Insert failed, User already exists!")
            false
        }
    }

    fun findById(id: String): User? {
        return userCollection.findOne(User::id eq ObjectId(id))
    }

    fun getUsersForHighscore(pageNumber: Int, pageSize: Int): List<User> {
        val skip = (pageNumber - 1) * pageSize
        return userCollection.find(User::type eq UserType.PLAYER).hint(Indexes.descending(User::highscore.name)).skip(skip).limit(pageSize).toList()
    }

    fun getAllUsersForHighscore(): List<User> {
        return userCollection.find(User::type eq UserType.PLAYER).hint(Indexes.descending(User::highscore.name)).toList()
    }

    fun updateUser(user: User): UpdateResult {
        user.modify()
        val updateFields = BasicDBObject()
        val propsToBeSkipped = listOf(User::id.name, User::createdAt.name)

        User::class.memberProperties.forEach {
            val prop = it.name
            val value = it.get(user)
            if(value != null && !propsToBeSkipped.contains(prop)) {
                log.info("Updating User: ${user.id}, with: $prop = $value")
                updateFields.append(prop, value)
            }
        }

        return userCollection.updateOne(User::id eq user.id, BasicDBObject("\$set", updateFields))
    }

    fun findByUsername(name: String): User? {
        return userCollection.findOne(User::username eq name)
    }

    fun getSessionTokenByUsername(name: String): String? {
        return userCollection.findOne(User::username eq name)?.sessionToken
    }

    fun updateSessionTokenByUsername(name: String, token: String): UpdateResult {
        val updateDoc = BasicDBObject("\$set", Document(User::sessionToken.name, token))
        log.info("Storing Session-Token, for User: $name")

        return userCollection.updateOne(User::username eq name, updateDoc)
    }

    fun getAnsweredIds(userId: String): List<String>? {
        return userCollection.findOne(User::id eq ObjectId(userId))!!.answeredQuestionIds
    }

    fun addAnsweredIds(userId: String, answerIds: List<String>): UpdateResult {
        val user = userCollection.findOne(User::id eq ObjectId(userId))!!
        user.modify()
        if(user.answeredQuestionIds.isNullOrEmpty())
            user.answeredQuestionIds = answerIds
        else {
            val list = answerIds.toMutableList()
            list.addAll(user.answeredQuestionIds!!)
            user.answeredQuestionIds = list
        }
        return userCollection.updateOne(User::id eq user.id, user)
    }

    fun deleteSessionToken(userId: String): UpdateResult {
        val updateDoc = Document("\$set", Document(User::sessionToken.name, ""))
        return userCollection.updateOne(User::id eq ObjectId(userId), updateDoc)
    }

    fun deleteUser(userId: String): DeleteResult {
        return userCollection.deleteOne(User::id eq ObjectId(userId))
    }
}