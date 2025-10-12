package school.it.quiz.cache

import mu.KotlinLogging
import school.it.quiz.QuestionAnswer
import school.it.quiz.QuestionAnswerSecure
import school.it.quiz.QuizRepository

class QuizCacheService(
    private val cacheRepository: QuizCacheRepository,
    private val quizRepository: QuizRepository
) {
    val log = KotlinLogging.logger {}

    private fun evaluateAnswer(questionId: String, takenAnswer: String): Boolean {
        val question = quizRepository.getQuestion(questionId) ?: return false
        return question.correctAnswer == takenAnswer
    }

    fun cacheAnswerFor(userId: String, answer: QuestionAnswerSecure) {
        cacheRepository.saveFor(
            userId,
            QuestionAnswer(
                questionId = answer.questionId,
                correctAnswered = evaluateAnswer(answer.questionId, answer.takenAnswer)
            )
        )
    }

    fun clearCacheFor(userId: String) {
        cacheRepository.clearCacheFor(userId)
    }

    fun getQuestionsAnswered(userId: String): List<QuestionAnswer> {
        return cacheRepository.getFor(userId)?.currentAnsweredQuestions ?: listOf()
    }
}