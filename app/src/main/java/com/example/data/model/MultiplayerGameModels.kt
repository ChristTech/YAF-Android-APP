package com.example.data.model

enum class GameType(val displayName: String, val description: String) {
    TRIVIA_CHALLENGE("Trivia Challenge", "Answer timed Bible and kingdom knowledge questions"),
    WHO_AM_I("Who Am I?", "Identify mystery Bible characters from progressive clues"),
    SCRIPTURE_SCRAMBLE("Scripture Scramble", "Reconstruct scrambled Bible verses in the correct order")
}

enum class SessionStatus(val displayName: String) {
    DRAFT("Draft"),
    SCHEDULED("Scheduled"),
    ACTIVE("Active"),
    COMPLETED("Completed"),
    CANCELLED("Cancelled")
}

enum class SessionSubstate(val displayName: String) {
    WAITING_TO_START("Waiting to Start"),
    QUESTION_OPEN("Question Open"),
    LEADERBOARD("Leaderboard Standings")
}

data class MultiplayerQuestion(
    val id: Int,
    val gameType: GameType,
    val prompt: String,
    val options: List<String> = emptyList(),
    val correctAnswer: String,
    val clues: List<String> = emptyList(), // For WHO_AM_I (2-4 clues, hardest to easiest)
    val scrambledWords: List<String> = emptyList(), // For SCRIPTURE_SCRAMBLE
    val scriptureReference: String = "",
    val translation: String = "KJV",
    val timerSeconds: Int = 20,
    val points: Int = 100,
    val explanation: String = ""
)

fun scrambleVerse(verse: String): List<String> {
    val clean = verse.trim()
        .replace(Regex("[,.;:!?\"'()—\\[\\]]"), "")
        .split(Regex("\\s+"))
        .filter { it.isNotBlank() }
    val shuffled = clean.shuffled()
    return if (shuffled == clean && clean.size > 1) {
        clean.reversed()
    } else {
        shuffled
    }
}

data class MultiplayerLeaderboardEntry(
    val rank: Int,
    val userId: String,
    val userName: String,
    val userRole: String = "Member",
    val score: Int,
    val pointsFromLastQuestion: Int = 0,
    val rankMovement: Int = 0,
    val isCurrentUser: Boolean = false,
    val answeredCount: Int = 0
)

sealed class SubmissionResult {
    data class Success(
        val pointsAwarded: Int,
        val isCorrect: Boolean,
        val speedBonus: Int,
        val message: String
    ) : SubmissionResult()

    data class DuplicateRejected(val message: String) : SubmissionResult()
    data class DeadlineExpired(val message: String) : SubmissionResult()
    data class MissedQuestion(val message: String) : SubmissionResult()
    data class Error(val message: String) : SubmissionResult()
}
