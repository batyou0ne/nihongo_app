import Foundation

/// ContentStore verilerinden karma JLPT Seviye Kapı ve Test-Out sınavı soruları üreten yardımcı servis.
enum ExamGenerator {
    static func generateExam(for level: JLPTLevel, mode: ExamMode) -> [ExamQuestion] {
        var questions: [ExamQuestion] = []

        // 1. Kanji Soruları (8 adet)
        let allKanji = ContentStore.loadKanji(level: level.rawValue)
        if allKanji.count >= 4 {
            let selectedKanji = allKanji.shuffled().prefix(8)
            for kanji in selectedKanji {
                var options = [kanji.meaning]
                let distractors = allKanji
                    .filter { $0.id != kanji.id && $0.meaning != kanji.meaning }
                    .shuffled()
                    .prefix(3)
                    .map(\.meaning)
                options.append(contentsOf: distractors)
                options.shuffle()

                let reading = kanji.kunyomi.isEmpty ? kanji.onyomi : "\(kanji.kunyomi) · \(kanji.onyomi)"
                questions.append(ExamQuestion(
                    id: "exam_kanji_\(kanji.character)_\(UUID().uuidString.prefix(6))",
                    type: .kanji,
                    prompt: kanji.character,
                    subPrompt: "Okunuş: \(reading)",
                    options: options,
                    correctAnswer: kanji.meaning,
                    explanation: "\(kanji.character) = \(kanji.meaning)"
                ))
            }
        }

        // 2. Kelime Soruları (12 adet)
        let allVocab = ContentStore.loadVocabulary(level: level.rawValue)
        if allVocab.count >= 4 {
            let selectedVocab = allVocab.shuffled().prefix(12)
            for word in selectedVocab {
                var options = [word.turkishMeaning]
                let distractors = allVocab
                    .filter { $0.id != word.id && $0.turkishMeaning != word.turkishMeaning }
                    .shuffled()
                    .prefix(3)
                    .map(\.turkishMeaning)
                options.append(contentsOf: distractors)
                options.shuffle()

                let reading = word.hiragana.isEmpty ? word.romaji : "\(word.hiragana) (\(word.romaji))"
                questions.append(ExamQuestion(
                    id: "exam_vocab_\(word.displayText)_\(UUID().uuidString.prefix(6))",
                    type: .vocabulary,
                    prompt: word.displayText,
                    subPrompt: reading,
                    options: options,
                    correctAnswer: word.turkishMeaning,
                    explanation: "\(word.displayText) = \(word.turkishMeaning)"
                ))
            }
        }

        // 3. Gramer Soruları (10 adet)
        let allGrammar = ContentStore.loadGrammar(level: level.rawValue)
        var grammarQuestionsPool: [(point: GrammarPoint, question: GrammarQuestion)] = []
        for point in allGrammar {
            for q in point.questions {
                if q.choices.count >= 4 {
                    grammarQuestionsPool.append((point, q))
                }
            }
        }

        if !grammarQuestionsPool.isEmpty {
            let selectedGrammar = grammarQuestionsPool.shuffled().prefix(10)
            for (point, q) in selectedGrammar {
                questions.append(ExamQuestion(
                    id: "exam_grammar_\(point.key)_\(UUID().uuidString.prefix(6))",
                    type: .grammar,
                    prompt: q.prompt,
                    subPrompt: "\(point.title) (\(point.pattern))",
                    options: q.choices.shuffled(),
                    correctAnswer: q.answer,
                    explanation: q.hint ?? "\(point.pattern) kuralı"
                ))
            }
        }

        return questions.shuffled()
    }
}
