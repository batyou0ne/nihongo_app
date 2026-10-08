import Foundation
import SwiftData
import SwiftUI

/// JLPT seviye ve bölüm kilitleme mantığını yöneten merkezi servis.
/// Tüm modüller (Kanji, Kelime, Gramer, Okuma) kilit durumlarını bu servis üzerinden sorgular.
@MainActor
final class LevelProgressionService {
    static let shared = LevelProgressionService()

    private init() {}

    // MARK: - Başlangıç Durumu Oluşturma

    /// Uygulama ilk açıldığında N5-N1 seviye kayıtlarını hazırlar.
    /// N5 varsayılan açık; N4, N3, N2, N1 kilitli başlar.
    @discardableResult
    func ensureInitialProgress(context: ModelContext) -> [UserLevelProgress] {
        let descriptor = FetchDescriptor<UserLevelProgress>()
        let existingRecords = (try? context.fetch(descriptor)) ?? []
        var recordDict: [String: UserLevelProgress] = [:]
        for record in existingRecords {
            recordDict[record.levelRaw] = record
        }

        var results: [UserLevelProgress] = []
        for level in JLPTLevel.allCases {
            if let existing = recordDict[level.rawValue] {
                results.append(existing)
            } else {
                let isN5 = (level == .n5)
                let newProgress = UserLevelProgress(
                    level: level.rawValue,
                    isUnlocked: isN5,
                    isCompleted: false
                )
                context.insert(newProgress)
                results.append(newProgress)
            }
        }
        try? context.save()
        return results
    }

    /// Belirtilen seviyeye ait ilerleme kaydını döner.
    func getProgress(for level: JLPTLevel, context: ModelContext) -> UserLevelProgress {
        let raw = level.rawValue
        let descriptor = FetchDescriptor<UserLevelProgress>(
            predicate: #Predicate { $0.levelRaw == raw }
        )
        if let existing = (try? context.fetch(descriptor))?.first {
            return existing
        }
        let list = ensureInitialProgress(context: context)
        return list.first(where: { $0.levelRaw == level.rawValue }) ?? UserLevelProgress(level: level.rawValue, isUnlocked: level == .n5)
    }

    // MARK: - Seviye Kilidi Sorgulama

    /// Seviyenin kilidinin açık olup olmadığını döner.
    func isLevelUnlocked(_ level: JLPTLevel, context: ModelContext) -> Bool {
        if level == .n5 { return true }
        let progress = getProgress(for: level, context: context)
        return progress.isUnlocked
    }

    // MARK: - Bölüm İçi (Intra-Level) Parça Kilidi

    /// Bir seviye içindeki belirli bir parçanın (partIndex: 0-indexed) açık olup olmadığını döner.
    /// Kural:
    /// 1. Seviyenin kendisi açık olmalıdır.
    /// 2. İlk parça (index == 0) seviye açıksa her zaman açıktır.
    /// 3. Sonraki parçalar (index > 0), bir önceki parça (index - 1) tamamlanmışsa açılır.
    func isPartUnlocked(
        module: String,
        level: JLPTLevel,
        partIndex: Int,
        context: ModelContext
    ) -> Bool {
        guard isLevelUnlocked(level, context: context) else { return false }
        if partIndex == 0 { return true }

        let progress = getProgress(for: level, context: context)
        // Bir önceki parça tamamlanmış mı?
        return progress.isPartCompleted(module: module, index: partIndex - 1)
    }

    /// Bir parçayı (örn. Kanji Part 1 veya Vocab Part 3) tamamlandı olarak kaydeder.
    func markPartCompleted(
        module: String,
        level: JLPTLevel,
        partIndex: Int,
        context: ModelContext
    ) {
        let progress = getProgress(for: level, context: context)
        progress.markPartCompleted(module: module, index: partIndex)
        try? context.save()
    }

    // MARK: - İlerleme Hesaplama & Kapı Sınavı Uygunluğu

    /// Seviyedeki toplam ve tamamlanan parça sayılarına göre modül bazlı ve genel ilerleme yüzdesi döner.
    func calculateLevelProgress(
        level: JLPTLevel,
        context: ModelContext
    ) -> (kanji: Double, vocab: Double, grammar: Double, overall: Double) {
        let progress = getProgress(for: level, context: context)

        // Part sayıları (N5 için standart):
        let kanjiTotalParts = max(1, ContentStore.kanjiParts(level: level.rawValue).count)
        let vocabTotalParts = max(1, ContentStore.vocabularyParts(level: level.rawValue).count)
        let grammarTotalUnits = max(1, ContentStore.loadGrammarSyllabus(level: level.rawValue).count)

        let kanjiCompleted = progress.completedPartIndices(module: "kanji").count
        let vocabCompleted = progress.completedPartIndices(module: "vocab").count
        let grammarCompleted = progress.completedPartIndices(module: "grammar").count

        let kanjiRatio = min(1.0, Double(kanjiCompleted) / Double(kanjiTotalParts))
        let vocabRatio = min(1.0, Double(vocabCompleted) / Double(vocabTotalParts))
        let grammarRatio = min(1.0, Double(grammarCompleted) / Double(grammarTotalUnits))

        let overallRatio = (kanjiRatio + vocabRatio + grammarRatio) / 3.0

        return (kanji: kanjiRatio, vocab: vocabRatio, grammar: grammarRatio, overall: overallRatio)
    }

    /// Kullanıcının Seviye Kapı Sınavına (Gateway Exam) girip giremeyeceğini denetler.
    /// Kural: Seviye açık olmalı ve modüllerin genel ortalaması en az %85 olmalıdır.
    func canTakeGatewayExam(level: JLPTLevel, context: ModelContext) -> Bool {
        guard isLevelUnlocked(level, context: context) else { return false }
        let progress = calculateLevelProgress(level: level, context: context)
        return progress.overall >= level.completionThreshold
    }

    // MARK: - Seviye Atlama & Üst Seviyenin Kilidini Çözme

    /// Kapı Sınavı veya Test-Out sınavı başarıyla geçildiğinde çağrılır.
    /// Mevcut seviyeyi tamamlandı yapar ve bir sonraki seviyenin kilidini açar.
    func unlockNextLevel(
        afterCompleting currentLevel: JLPTLevel,
        score: Double,
        isTestOut: Bool,
        context: ModelContext
    ) {
        let currentProgress = getProgress(for: currentLevel, context: context)
        currentProgress.isCompleted = true
        currentProgress.hasPassedGatewayExam = true
        currentProgress.gatewayExamScore = score
        currentProgress.gatewayExamDate = .now
        if isTestOut {
            currentProgress.isSkippedViaPlacement = true
        }

        // Sonraki seviyenin kilidini aç
        if let next = currentLevel.nextLevel {
            let nextProgress = getProgress(for: next, context: context)
            nextProgress.isUnlocked = true
        }

        try? context.save()
    }
}
