import Foundation

/// JLPT (Japanese-Language Proficiency Test) seviyeleri (N5 en temel, N1 en ileri).
enum JLPTLevel: String, CaseIterable, Codable, Comparable, Identifiable {
    case n5 = "N5"
    case n4 = "N4"
    case n3 = "N3"
    case n2 = "N2"
    case n1 = "N1"

    var id: String { rawValue }

    /// Karşılaştırma ve sıralama için sıra numarası (0: N5 -> 4: N1)
    var order: Int {
        switch self {
        case .n5: return 0
        case .n4: return 1
        case .n3: return 2
        case .n2: return 3
        case .n1: return 4
        }
    }

    static func < (lhs: JLPTLevel, rhs: JLPTLevel) -> Bool {
        lhs.order < rhs.order
    }

    /// Bir sonraki üst seviye (örn. N5 -> N4)
    var nextLevel: JLPTLevel? {
        switch self {
        case .n5: return .n4
        case .n4: return .n3
        case .n3: return .n2
        case .n2: return .n1
        case .n1: return nil
        }
    }

    /// Bir önceki alt seviye (örn. N4 -> N5)
    var previousLevel: JLPTLevel? {
        switch self {
        case .n5: return nil
        case .n4: return .n5
        case .n3: return .n4
        case .n2: return .n3
        case .n1: return .n2
        }
    }

    var title: String {
        "\(rawValue)"
    }

    var subtitle: String {
        switch self {
        case .n5: return "Temel Başlangıç (Beginner)"
        case .n4: return "Temel İleri (Elementary)"
        case .n3: return "Orta Seviye (Intermediate)"
        case .n2: return "İleri Seviye (Upper Intermediate)"
        case .n1: return "Ustalık (Advanced / Native)"
        }
    }

    /// Kapı sınavına (Gateway Exam) hak kazanmak için gereken asgari modül tamamlama oranı (%85)
    var completionThreshold: Double {
        0.85
    }

    /// Seviyeye ait toplam yaklaşık içerik sayıları (UI göstergeleri ve hedef takibi için)
    var targetCounts: (kanji: Int, vocab: Int, grammar: Int) {
        switch self {
        case .n5: return (kanji: 80, vocab: 675, grammar: 85)
        case .n4: return (kanji: 180, vocab: 800, grammar: 115)
        case .n3: return (kanji: 350, vocab: 1500, grammar: 140)
        case .n2: return (kanji: 400, vocab: 2500, grammar: 150)
        case .n1: return (kanji: 1000, vocab: 3500, grammar: 120)
        }
    }
}
