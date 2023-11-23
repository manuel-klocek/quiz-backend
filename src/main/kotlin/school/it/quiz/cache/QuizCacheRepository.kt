package school.it.quiz.cache

import mu.KotlinLogging
import org.litote.kmongo.eq
import org.litote.kmongo.findOne
import org.litote.kmongo.findOneById
import org.litote.kmongo.getCollection
import org.litote.kmongo.updateOne
import school.it.helper.GetEnvVars
import school.it.quiz.QuestionAnswer

class QuizCacheRepository {
    private val database = GetEnvVars.getMongoDbAsDb
    private val cacheCollection = database.getCollection<CacheObject>()
    private val log = KotlinLogging.logger {}

    fun saveFor(userId: String, answer: QuestionAnswer) {
        log.info("Saving Answer of Question: ${answer.questionId} for User: $userId")
        val entry = cacheCollection.findOneById(userId)
        if(entry == null)
            cacheCollection.insertOne(CacheObject(userId, listOf(answer)))
        else {
            val list = entry.currentAnsweredQuestions.toMutableList()
            list.add(answer)
            entry.currentAnsweredQuestions = list
            cacheCollection.updateOne(CacheObject::userId eq userId, entry)
        }
    }

    fun getFor(userId: String): CacheObject? {
        return cacheCollection.findOne(CacheObject::userId eq userId)
    }

    fun clearCacheFor(userId: String) {
        log.info("Clearing cache for User: $userId")
        cacheCollection.deleteOne(CacheObject::userId eq userId)
    }
}



