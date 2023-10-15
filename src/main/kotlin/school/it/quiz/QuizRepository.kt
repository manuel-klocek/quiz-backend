package school.it.quiz

import mu.KotlinLogging
import org.bson.Document
import org.bson.types.ObjectId
import org.litote.kmongo.KMongo
import org.litote.kmongo.eq
import org.litote.kmongo.findOne
import org.litote.kmongo.getCollection

class QuizRepository {

    private val client = KMongo.createClient()
    private val database = client.getDatabase("Quiz")
    private val quizCollection = database.getCollection<Question>()
    private val log = KotlinLogging.logger {}

    fun getQuestions(answers: List<QuestionAnswer>): List<Question> {
        val questions = mutableListOf<Question>()
        answers.forEach { questions.add(quizCollection.findOne(Question::id eq ObjectId(it.questionId))!!) }
        return questions
    }

    fun deleteAllAndInsertNewQuestions(questions: List<Question>) {
        if(questions.isEmpty()) return

        log.info("Dropping Database to reload Questions from Api")
        quizCollection.drop()
        log.info("Inserting ${questions.size} Questions")
        quizCollection.insertMany(questions)
        log.info("Inserting successful")
    }

    fun getQuestionsForCategoryExcept(categoryId: String, answeredIds: List<String>? = listOf(), amount: Int): List<Question> {
        var filter = Document()
        if(!answeredIds.isNullOrEmpty())
            filter = Document("_id", Document("\$nin", answeredIds))

        filter.append("categoryId", categoryId)

        return quizCollection.find(filter).toList().shuffled().take(amount)
    }

    fun getQuestionCount(): Long {
        return quizCollection.countDocuments()
    }
}