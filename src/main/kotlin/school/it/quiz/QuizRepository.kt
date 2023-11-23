package school.it.quiz

import mu.KotlinLogging
import org.bson.Document
import org.bson.types.ObjectId
import org.litote.kmongo.eq
import org.litote.kmongo.findOne
import org.litote.kmongo.getCollection
import school.it.helper.GetEnvVars

class QuizRepository {
    private val database = GetEnvVars.getMongoDbAsDb
    private val quizCollection = database.getCollection<Question>()
    private val log = KotlinLogging.logger {}

    fun getQuestion(questionId: String): Question? {
        return quizCollection.findOne(Question::id eq ObjectId(questionId))
    }

    fun getQuestions(answers: List<QuestionAnswer>): List<Question> {
        val questions = mutableListOf<Question>()
        answers.forEach { questions.add(quizCollection.findOne(Question::id eq ObjectId(it.questionId))!!) }
        return questions
    }

    fun deleteAllAndInsertNewQuestions(questions: List<Question>) {
        if(questions.isEmpty()) return

        try {
            log.info("Dropping Database to reload Questions from Api")
            quizCollection.drop()
        } catch (_: Exception) {
            log.info("Dropping of collection failed -> this collection might not exist anymore")
            log.warn("Code execution will continue!")
        }
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

    fun getQuestionsSecureForCategoryExcept(categoryId: String, answeredIds: List<String>? = listOf(), amount: Int): List<QuestionSecure> {
        var filter = Document()
        if(!answeredIds.isNullOrEmpty())
            filter = Document("_id", Document("\$nin", answeredIds))

        filter.append("categoryId", categoryId)

        val secureQuestions = mutableListOf<QuestionSecure>()
        quizCollection.find(filter).toList().shuffled().take(amount).forEach { secureQuestions.add(it.toSecure()) }

        return secureQuestions
    }

    fun getQuestionCount(): Long {
        return quizCollection.countDocuments()
    }

    fun getCategories(): List<Category> {
        val categoryIds = quizCollection.distinct("categoryId", String::class.java)
        val categories = mutableListOf<Category>()
        categoryIds.forEach{
            val question = quizCollection.find(Question::categoryId eq it).first()
            categories.add(Category(it, question!!.category))
        }
        return categories
    }
}