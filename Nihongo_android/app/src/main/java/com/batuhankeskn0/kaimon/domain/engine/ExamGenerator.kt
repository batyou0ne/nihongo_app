package com.batuhankeskn0.kaimon.domain.engine

import com.batuhankeskn0.kaimon.data.local.JLPTLevel
import com.batuhankeskn0.kaimon.data.model.ExamMode
import com.batuhankeskn0.kaimon.data.model.ExamQuestion
import com.batuhankeskn0.kaimon.data.model.ExamQuestionType
import com.batuhankeskn0.kaimon.data.model.GrammarPoint
import com.batuhankeskn0.kaimon.data.model.GrammarQuestion
import com.batuhankeskn0.kaimon.data.repository.ContentStore
import java.util.UUID

/**
 * `ContentStore` verilerinden karma JLPT Seviye Kapı ve Test-Out sınavı soruları
 * (8 Kanji + 12 Kelime + 10 Gramer = 30 soru) üreten yardımcı servis.
 */
object ExamGenerator {

    fun generateExam(
        level: JLPTLevel,
        mode: ExamMode = ExamMode.GATEWAY,
        contentStore: ContentStore = ContentStore.shared
    ): List<ExamQuestion> {
        val questions = mutableListOf<ExamQuestion>()

        // 1. Kanji Soruları (8 adet)
        val allKanji = contentStore.loadKanji(level = level.rawValue)
        if (allKanji.size >= 4) {
            val selectedKanji = allKanji.shuffled().take(8)
            for (kanji in selectedKanji) {
                val distractors = allKanji
                    .filter { it.id != kanji.id && it.meaning != kanji.meaning }
                    .shuffled()
                    .take(3)
                    .map { it.meaning }
                val options = (listOf(kanji.meaning) + distractors).shuffled()

                val reading = if (kanji.kunyomi.isEmpty()) {
                    kanji.onyomi
                } else {
                    "${kanji.kunyomi} · ${kanji.onyomi}"
                }

                questions.add(
                    ExamQuestion(
                        id = "exam_kanji_${kanji.character}_${UUID.randomUUID().toString().take(6)}",
                        type = ExamQuestionType.KANJI,
                        prompt = kanji.character,
                        subPrompt = "Okunuş: $reading",
                        options = options,
                        correctAnswer = kanji.meaning,
                        explanation = "${kanji.character} = ${kanji.meaning}"
                    )
                )
            }
        }

        // 2. Kelime Soruları (12 adet)
        val allVocab = contentStore.loadVocabulary(level = level.rawValue)
        if (allVocab.size >= 4) {
            val selectedVocab = allVocab.shuffled().take(12)
            for (word in selectedVocab) {
                val distractors = allVocab
                    .filter { it.id != word.id && it.turkishMeaning != word.turkishMeaning }
                    .shuffled()
                    .take(3)
                    .map { it.turkishMeaning }
                val options = (listOf(word.turkishMeaning) + distractors).shuffled()

                val reading = if (word.hiragana.isEmpty()) {
                    word.romaji
                } else {
                    "${word.hiragana} (${word.romaji})"
                }

                questions.add(
                    ExamQuestion(
                        id = "exam_vocab_${word.displayText}_${UUID.randomUUID().toString().take(6)}",
                        type = ExamQuestionType.VOCABULARY,
                        prompt = word.displayText,
                        subPrompt = reading,
                        options = options,
                        correctAnswer = word.turkishMeaning,
                        explanation = "${word.displayText} = ${word.turkishMeaning}"
                    )
                )
            }
        }

        // 3. Gramer Soruları (10 adet)
        val allGrammar = contentStore.loadGrammar(level = level.rawValue)
        val grammarQuestionsPool = mutableListOf<Pair<GrammarPoint, GrammarQuestion>>()
        for (point in allGrammar) {
            for (q in point.questions) {
                if (q.choices.size >= 4 && q.choices.contains(q.answer)) {
                    grammarQuestionsPool.add(point to q)
                }
            }
        }

        if (grammarQuestionsPool.isNotEmpty()) {
            val selectedGrammar = grammarQuestionsPool.shuffled().take(10)
            for ((point, q) in selectedGrammar) {
                questions.add(
                    ExamQuestion(
                        id = "exam_grammar_${point.key}_${UUID.randomUUID().toString().take(6)}",
                        type = ExamQuestionType.GRAMMAR,
                        prompt = q.prompt,
                        subPrompt = "${point.title} (${point.pattern})",
                        options = q.choices.shuffled(),
                        correctAnswer = q.answer,
                        explanation = q.hint ?: "${point.pattern} kuralı"
                    )
                )
            }
        }

        return questions.shuffled()
    }
}
