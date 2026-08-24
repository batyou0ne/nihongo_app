import Foundation
import SwiftData
import Observation

/// Quiz'e sokulabilecek her öğenin (karakter ya da kelime) uyması gereken arayüz.
/// QuizViewModel'in hem Hiragana/Katakana kartlarını hem de kelime setini aynı
/// çoktan seçmeli mantıkla çalıştırabilmesini sağlar.
protocol QuizItem: Identifiable {
    var id: String { get }
    var prompt: String { get }        // Ekranda büyük gösterilen Japonca metin.
    var correctAnswer: String { get } // Doğru cevap (romaji ya da Türkçe anlam).
}

extension JapaneseCharacter: QuizItem {
    var prompt: String { character }
    var correctAnswer: String { romaji }
}

extension Kanji: QuizItem {
    var prompt: String { character }
    var correctAnswer: String { meaning }
}

/// `FlashcardSessionView`'ın (Hiragana/Katakana/Kanji ortak akışı) ihtiyaç duyduğu ek bilgiler:
/// kart çevrilince üstte gösterilen özet satırı ve örnek kelimeler.
protocol FlashcardItem: QuizItem {
    var flipRecap: String { get }
    var exampleWords: [ExampleWord] { get }
}

extension JapaneseCharacter: FlashcardItem {
    var flipRecap: String { "\(character) = \(romaji) · \(turkishPronunciation)" }
}

extension Kanji: FlashcardItem {
    var flipRecap: String {
        let reading = kunyomi.isEmpty ? onyomi : (onyomi.isEmpty ? kunyomi : "\(kunyomi) / \(onyomi)")
        return "\(character) — \(reading)"
    }
}

/// Tek bir quiz oturumunu yönetir: soru sırası, çoktan seçmeli şıklar, skor ve
/// her cevaptan sonra SpacedRepetitionService üzerinden ilerlemeyi kaydetme.
@Observable
final class QuizViewModel<Item: QuizItem> {
    private(set) var questions: [Item]
    private(set) var currentIndex = 0
    private(set) var options: [String] = []
    private(set) var selectedAnswer: String?
    private(set) var isAnswerCorrect: Bool?
    private(set) var score = 0

    private let itemKind: LearnableItemKind
    private let progressLookup: (String) -> LearningItemProgress?
    private let modelContext: ModelContext

    /// Yanlış şıklar bu havuzdan seçilir. Varsayılan olarak `questions` ile aynıdır, ama
    /// küçük bir soru seti (ör. sadece 2-3 karakterlik bir "yanlışları tekrar et" turu)
    /// kullanılırken 4 şık tamamlanamaz — bu yüzden çağıran taraf isterse daha geniş bir
    /// havuz (ör. modülün tüm 46 karakteri) verebilir.
    private let distractorPool: [Item]

    var currentQuestion: Item? {
        questions.indices.contains(currentIndex) ? questions[currentIndex] : nil
    }

    var isFinished: Bool { currentIndex >= questions.count }
    var progressFraction: Double {
        questions.isEmpty ? 0 : Double(currentIndex) / Double(questions.count)
    }

    init(
        questions: [Item],
        itemKind: LearnableItemKind,
        modelContext: ModelContext,
        progressLookup: @escaping (String) -> LearningItemProgress?,
        shuffled: Bool = true,
        distractorPool: [Item]? = nil
    ) {
        self.questions = shuffled ? questions.shuffled() : questions
        self.itemKind = itemKind
        self.modelContext = modelContext
        self.progressLookup = progressLookup
        self.distractorPool = distractorPool ?? questions
        generateOptions()
    }

    /// Doğru cevap + rastgele 3 yanlış şık üretir. Şıklar her zaman `distractorPool`'dan
    /// gelir, `questions` (o turun kendi soru seti) değil — böylece 3 karakterlik bir tekrar
    /// turunda bile eksiksiz 4 şık çıkar.
    private func generateOptions() {
        guard let question = currentQuestion else {
            options = []
            return
        }
        var distractors = distractorPool
            .filter { $0.id != question.id }
            .map(\.correctAnswer)
        distractors.shuffle()

        var choices = Set(distractors.prefix(3))
        choices.insert(question.correctAnswer)
        options = Array(choices).shuffled()
    }

    func submitAnswer(_ answer: String) {
        guard let question = currentQuestion, selectedAnswer == nil else { return }
        selectedAnswer = answer
        let correct = answer == question.correctAnswer
        isAnswerCorrect = correct
        if correct { score += 1 }

        if let progress = progressLookup(question.id) {
            SpacedRepetitionService.schedule(progress, quality: correct ? .good : .again)
            try? modelContext.save()
        }
    }

    func moveToNext() {
        selectedAnswer = nil
        isAnswerCorrect = nil
        currentIndex += 1
        generateOptions()
    }
}
