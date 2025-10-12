package school.it.quiz.cache

import kotlinx.serialization.Serializable
import org.bson.codecs.pojo.annotations.BsonId
import school.it.quiz.QuestionAnswer
import school.it.quiz.QuestionAnswerSecure

@Serializable
data class CacheObject(
    @BsonId
    val userId: String,
    var currentAnsweredQuestions: List<QuestionAnswer>
)