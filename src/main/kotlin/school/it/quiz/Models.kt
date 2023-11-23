package school.it.quiz

import kotlinx.serialization.Serializable
import org.bson.Document
import org.bson.codecs.pojo.annotations.BsonId
import org.bson.types.ObjectId

data class Question(
    @BsonId
    val id: ObjectId? = null,
    var categoryId: String,
    var category: String,
    var type: String,
    var difficulty: String,
    var question: String,
    var correctAnswer: String,
    var incorrectAnswers: List<String>
)

@Serializable
data class QuestionSecure(
    val id: String,
    val categoryId: String,
    val category: String,
    val type: String,
    val difficulty: String,
    val question: String,
    val answers: List<String>
)

fun Question.toSecure(): QuestionSecure {
    val allAnswers = incorrectAnswers.toMutableList()
    allAnswers.add(correctAnswer)

    return QuestionSecure(
        id = id.toString(),
        categoryId = categoryId,
        category = category,
        type = type,
        difficulty = difficulty,
        question = question,
        answers = allAnswers
    )
}


@Serializable
data class QuestionDto(
    val id: String? = null,
    val categoryId: String,
    val category: String,
    val type: String,
    val difficulty: String,
    val question: String,
    val correctAnswer: String,
    val incorrectAnswers: List<String>
)

@Serializable
data class QuestionAnswer(
    val questionId: String,
    val correctAnswered: Boolean
)

@Serializable
data class QuestionAnswerSecure(
    val questionId: String,
    val takenAnswer: String
)

fun Question.toDto() =
    QuestionDto(
        this.id.toString(),
        this.categoryId,
        this.category,
        this.type,
        this.difficulty,
        this.question,
        this.correctAnswer,
        this.incorrectAnswers
    )

@Serializable
data class Category(
    val categoryId: String,
    val categoryName: String
)