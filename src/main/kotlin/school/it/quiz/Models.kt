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
data class QuestionDto(
    val id: String? = null,
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

fun Question.toDto() =
    QuestionDto(
        this.id.toString(),
        this.category,
        this.type,
        this.difficulty,
        this.question,
        this.correctAnswer,
        this.incorrectAnswers
    )