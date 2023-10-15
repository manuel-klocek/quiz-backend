package school.it.user

import com.mongodb.BasicDBObject
import com.mongodb.MongoException
import com.mongodb.client.model.IndexOptions
import com.mongodb.client.model.Indexes
import com.mongodb.client.result.UpdateResult
import mu.KotlinLogging
import org.bson.Document
import org.bson.types.ObjectId
import org.litote.kmongo.*
import school.it.helper.Helper
import school.it.quiz.QuestionAnswer
import kotlin.reflect.full.memberProperties
import kotlin.reflect.jvm.internal.impl.load.kotlin.JvmType

class UserRepository {
    private val client = KMongo.createClient()
    private val database = client.getDatabase("Quiz")
    private val userCollection = database.getCollection<User>()
    private val log = KotlinLogging.logger {}

    init {
        userCollection.createIndex(Indexes.ascending(User::username.name), IndexOptions().unique(true))
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

    fun findById(id: String): User {
        return userCollection.findOne(User::id eq ObjectId(id))!!
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
        else
            user.answeredQuestionIds?.toMutableList()?.addAll(answerIds)
        return userCollection.updateOne(User::id eq ObjectId(userId), user)
    }
}