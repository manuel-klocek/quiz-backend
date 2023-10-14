package school.it.quiz

import com.mongodb.client.model.IndexOptions
import com.mongodb.client.model.Indexes
import org.bson.types.ObjectId
import org.litote.kmongo.KMongo
import org.litote.kmongo.eq
import org.litote.kmongo.findOne
import org.litote.kmongo.getCollection

class QuizRepository {

    private val client = KMongo.createClient()
    private val database = client.getDatabase("Quiz")
    private val quizCollection = database.getCollection<Question>()

    init {
        quizCollection.createIndex(Indexes.ascending(Question::question.name), IndexOptions().unique(true))
    }

    fun insertAndGet(questions: List<Question>): List<Question> {
        val found = mutableListOf<Question>()

        questions.forEach {
            try {
                quizCollection.insertOne(it)
            } catch (_: Exception) { }

            found.add(quizCollection.findOne(Question::question eq it.question)!!)
        }

        return found
    }

    fun getQuestions(answers: List<QuestionAnswer>): List<Question> {
        val questions = mutableListOf<Question>()
        answers.forEach { quizCollection.findOne(Question::id eq ObjectId(it.questionId)) }
        return questions
    }
}