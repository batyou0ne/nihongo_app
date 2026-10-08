import Foundation

/// Tüm arayüz string'lerini Türkçe / İngilizce olarak barındıran lokalizasyon katmanı.
/// View'lar `L10n.tabHome` gibi statik property'ler üzerinden erişir; LanguageManager.shared
/// değiştiğinde @Observable zinciri ile arayüz otomatik güncellenir.
enum L10n {
    private static var lang: LanguageManager.AppLanguage {
        LanguageManager.shared.current
    }

    // MARK: - Tab Bar
    static var tabHome: String { lang == .turkish ? "Ana" : "Home" }
    static var tabAlphabet: String { lang == .turkish ? "Alfabe" : "Alphabet" }
    static var tabGrammar: String { lang == .turkish ? "Gramer" : "Grammar" }
    static var tabVocabulary: String { lang == .turkish ? "Kelime" : "Vocab" }
    static var tabReading: String { lang == .turkish ? "Okuma" : "Reading" }
    static var tabDictionary: String { lang == .turkish ? "Sözlük" : "Dictionary" }
    static var tabReference: String { lang == .turkish ? "Kütüphane" : "Library" }

    // MARK: - Home
    static func streakText(_ count: Int) -> String {
        lang == .turkish ? "\(count) günlük seri" : "\(count) day streak"
    }
    static var noStreak: String { lang == .turkish ? "Seri yok — bugün başla" : "No streak — start today" }
    static var resumeLabel: String { lang == .turkish ? "KALDIĞIN YER" : "WHERE YOU LEFT OFF" }
    static var resumeButton: String { lang == .turkish ? "DEVAM ET  →" : "CONTINUE  →" }

    // MARK: - Sections
    static var sectionReview: String { lang == .turkish ? "TEKRAR & İLERLEME" : "REVIEW & PROGRESS" }
    static var sectionPractice: String { lang == .turkish ? "KONUŞMA PRATİĞİ" : "CONVERSATION PRACTICE" }
    static var sectionLearn: String { lang == .turkish ? "ÖĞREN" : "LEARN" }

    // MARK: - Review & Progress cards
    static var reviewTitle: String { lang == .turkish ? "Tekrar Çalış" : "Review" }
    static var reviewNoItems: String { lang == .turkish ? "Bekleyen öğe yok" : "No items pending" }
    static func reviewItemsWaiting(_ count: Int) -> String {
        lang == .turkish ? "\(count) öğe seni bekliyor" : "\(count) items waiting"
    }
    static var progressTitle: String { lang == .turkish ? "İlerleme" : "Progress" }
    static var progressSubtitle: String { lang == .turkish ? "Öğrenilenleri görüntüle" : "View learned items" }

    // MARK: - Practice
    static var scenarioPractice: String { lang == .turkish ? "Sohbet Pratiği" : "Chat Practice" }
    static var aiChat: String { lang == .turkish ? "AI ile Sohbet" : "Chat with AI" }

    // MARK: - Alphabet
    static var alphabetTitle: String { lang == .turkish ? "Alfabe" : "Alphabet" }
    static var alphabetChartTitle: String { lang == .turkish ? "Alfabe Tablosu" : "Alphabet Chart" }
    static var alphabetChartSubtitle: String { lang == .turkish ? "Harfe dokunarak sesini dinle" : "Tap any character to listen" }
    static var basicAlphabetTab: String { lang == .turkish ? "Temel (46 Harf)" : "Basic (46)" }
    static var dakutenAlphabetTab: String { lang == .turkish ? "Tenten & Maru" : "Dakuten" }
    static var practiceCardsTab: String { lang == .turkish ? "Kartlarla Çalış" : "Practice Cards" }
    static var kanjiChartSubtitle: String { lang == .turkish ? "Kanji'ye dokunarak sesini ve anlamını incele" : "Tap any kanji to listen and inspect meaning" }

    // MARK: - Grammar
    static var grammarTitle: String { lang == .turkish ? "Gramer" : "Grammar" }
    static func grammarLevel(_ level: String) -> String {
        lang == .turkish ? "\(level) Gramer" : "\(level) Grammar"
    }
    static func grammarSubtitle(_ topics: Int, _ categories: Int) -> String {
        lang == .turkish ? "\(topics) konu · \(categories) kategori" : "\(topics) topics · \(categories) categories"
    }

    // MARK: - Vocabulary
    static var vocabularyTitle: String { lang == .turkish ? "Kelimeler" : "Words" }
    static func vocabularyLevel(_ level: String) -> String {
        lang == .turkish ? "\(level) Kelimeler" : "\(level) Words"
    }
    static func vocabularySubtitle(_ words: Int, _ parts: Int) -> String {
        lang == .turkish ? "\(words) kelime · \(parts) bölüm" : "\(words) words · \(parts) parts"
    }

    // MARK: - Reading
    static var readingTitle: String { lang == .turkish ? "Okuma" : "Reading" }
    static func sentenceCount(_ count: Int) -> String {
        lang == .turkish ? "\(count) cümle" : "\(count) sentences"
    }

    // MARK: - Dictionary
    static var dictionaryTitle: String { lang == .turkish ? "Sözlük" : "Dictionary" }
    static var dictionarySearch: String { lang == .turkish ? "Kelime veya anlam ara..." : "Search word or meaning..." }

    // MARK: - Soon / locked
    static var comingSoon: String { lang == .turkish ? "Yakında" : "Coming soon" }

    // MARK: - Kanji
    static func kanjiLevel(_ level: String) -> String {
        "\(level) Kanji's"
    }
    static func kanjiSubtitle(_ count: Int, _ parts: Int) -> String {
        lang == .turkish ? "\(count) kanji · \(parts) bölüm" : "\(count) kanji · \(parts) parts"
    }
    static func kanjiPartTitle(_ level: String, _ part: Int) -> String {
        "\(level) Kanji's Part \(part)"
    }
    static func kanjiCount(_ count: Int) -> String {
        "\(count) kanji"
    }

    // MARK: - Flashcard / Quiz
    static var quickQuiz: String { lang == .turkish ? "Hızlı Quiz" : "Quick Quiz" }
    static func learnedCount(_ learned: Int, _ total: Int) -> String {
        lang == .turkish ? "\(learned)/\(total) öğrenildi" : "\(learned)/\(total) learned"
    }
    static var hintButton: String { lang == .turkish ? "İpucu" : "Hint" }
    static var tapToContinue: String { lang == .turkish ? "Devam etmek için karta dokun" : "Tap card to continue" }
    static var continueButton: String { lang == .turkish ? "Devam Et" : "Continue" }
    static var closeButton: String { lang == .turkish ? "Kapat" : "Close" }
    static var finishButton: String { lang == .turkish ? "Bitir" : "Finish" }
    static func scoreResult(_ score: Int, _ total: Int) -> String {
        lang == .turkish ? "\(score) / \(total) doğru" : "\(score) / \(total) correct"
    }

    // MARK: - Lesson Complete
    static var lessonCompleteTitle: String { lang == .turkish ? "Harika iş, ders tamamlandı" : "Great job, lesson complete" }

    // MARK: - Session Summary
    static var sessionSummary: String { lang == .turkish ? "Oturum Özeti" : "Session Summary" }
    static var cardsSeenLabel: String { lang == .turkish ? "Gördüğün kart" : "Cards seen" }
    static var wrongAnswersLabel: String { lang == .turkish ? "Yanlış cevap" : "Wrong answers" }
    static var perfectScore: String { lang == .turkish ? "Hiç yanlışın yok — mükemmel! 🎉" : "No mistakes — perfect! 🎉" }
    static var wrongItemsTitle: String { lang == .turkish ? "Yanlış yaptıkların" : "Items you got wrong" }
    static var addedToReview: String { lang == .turkish ? "Bunlar \"Tekrar Çalış\" listesine eklendi." : "These were added to your review list." }

    // MARK: - Review List
    static var reviewNavTitle: String { lang == .turkish ? "Tekrar Çalış" : "Review" }
    static var reviewEmptyTitle: String { lang == .turkish ? "Harika!" : "Great!" }
    static var reviewEmptyMessage: String { lang == .turkish ? "Tekrar çalışman gereken bir şey yok. Yanlış yaptığın kartlar burada birikir." : "Nothing to review. Cards you get wrong will appear here." }
    static func practiceWithThese() -> String { lang == .turkish ? "Bunlarla çalış →" : "Practice these →" }
    static var learnedButton: String { lang == .turkish ? "Öğrendim ✓" : "Learned ✓" }
    static func reviewPrefix(_ name: String) -> String {
        lang == .turkish ? "Tekrar: \(name)" : "Review: \(name)"
    }

    // MARK: - Progress Overview
    static var progressNavTitle: String { lang == .turkish ? "İlerleme" : "Progress" }
    static var learnedSection: String { lang == .turkish ? "Öğrenilenler" : "Learned" }
    static var streakSection: String { lang == .turkish ? "Seri" : "Streak" }
    static func longestStreak(_ count: Int) -> String {
        lang == .turkish ? "En uzun: \(count)" : "Longest: \(count)"
    }
    static func levelLabel(_ level: Int) -> String {
        lang == .turkish ? "Seviye \(level)" : "Level \(level)"
    }
    static var studyTime: String { lang == .turkish ? "Çalışma Süresi" : "Study Time" }
    static func permanentlyLearned(_ count: Int) -> String {
        lang == .turkish ? "🏆 \(count) tanesi kalıcı öğrenildi (4+ doğru tekrar)" : "🏆 \(count) permanently learned (4+ correct reviews)"
    }
    static func timeFormatted(hours: Int, minutes: Int) -> String {
        if lang == .turkish {
            return hours > 0 ? "\(hours) sa \(minutes) dk" : "\(minutes) dk"
        } else {
            return hours > 0 ? "\(hours)h \(minutes)m" : "\(minutes)m"
        }
    }
    static var chartDay: String { lang == .turkish ? "Gün" : "Day" }
    static var chartMinutes: String { lang == .turkish ? "Dakika" : "Minutes" }

    // MARK: - Auth / Account
    static var accountTitle: String { lang == .turkish ? "Hesabım" : "My Account" }
    static func ageLabel(_ age: Int) -> String {
        lang == .turkish ? "Yaş: \(age)" : "Age: \(age)"
    }
    static var accountSyncInfo: String {
        lang == .turkish
        ? "İlerlemen bu hesaba bağlı. Başka bir cihazda aynı hesapla giriş yaptığında kaldığın yerden devam edersin."
        : "Your progress is linked to this account. Sign in on another device to pick up where you left off."
    }
    static var signOutButton: String { lang == .turkish ? "Çıkış Yap" : "Sign Out" }

    // MARK: - Sign In
    static var signUpTitle: String { lang == .turkish ? "Hesap oluştur" : "Create Account" }
    static var signInTitle: String { lang == .turkish ? "Giriş yap" : "Sign In" }
    static var signInDescription: String {
        lang == .turkish
        ? "İlerlemen hesabına kaydedilir; başka bir cihazdan giriş yaptığında kaldığın yerden devam edersin."
        : "Your progress is saved to your account. Sign in from another device to continue where you left off."
    }
    static var firstNamePlaceholder: String { lang == .turkish ? "Ad" : "First Name" }
    static var lastNamePlaceholder: String { lang == .turkish ? "Soyad" : "Last Name" }
    static var agePlaceholder: String { lang == .turkish ? "Yaş (opsiyonel)" : "Age (optional)" }
    static var emailPlaceholder: String { "Email" }
    static var passwordPlaceholder: String { lang == .turkish ? "Şifre (en az 6 karakter)" : "Password (min 6 chars)" }
    static var signUpButton: String { lang == .turkish ? "Kayıt Ol" : "Sign Up" }
    static var signInButton: String { lang == .turkish ? "Giriş Yap" : "Sign In" }
    static var alreadyHaveAccount: String { lang == .turkish ? "Zaten hesabın var mı? Giriş yap" : "Already have an account? Sign in" }
    static var noAccount: String { lang == .turkish ? "Hesabın yok mu? Kayıt ol" : "Don't have an account? Sign up" }
    static var forgotPassword: String { lang == .turkish ? "Şifremi unuttum" : "Forgot password" }
    static var orDivider: String { lang == .turkish ? "veya" : "or" }
    static var googleSignIn: String { lang == .turkish ? "Google ile devam et" : "Continue with Google" }
    static var continueAsGuest: String { lang == .turkish ? "Şimdilik misafir olarak devam et" : "Continue as guest for now" }
    static var emptyEmailPassword: String { lang == .turkish ? "Email ve şifre boş bırakılamaz." : "Email and password cannot be empty." }
    static var emptyName: String { lang == .turkish ? "Ad ve soyad boş bırakılamaz." : "First and last name cannot be empty." }
    static var passwordResetPrompt: String { lang == .turkish ? "Şifre sıfırlama için üstteki alana email adresini yaz." : "Enter your email above to reset your password." }
    static func passwordResetSent(_ email: String) -> String {
        lang == .turkish ? "Şifre sıfırlama bağlantısı \(email) adresine gönderildi." : "Password reset link sent to \(email)."
    }

    // MARK: - Chat
    static var aiPracticeTitle: String { lang == .turkish ? "AI Pratik" : "AI Practice" }
    static var chatInputPlaceholder: String { lang == .turkish ? "Japonca veya Türkçe yaz..." : "Write in Japanese or English..." }

    // MARK: - Scenarios
    static var scenariosTitle: String { lang == .turkish ? "Senaryolar" : "Scenarios" }
    static var howToRespond: String { lang == .turkish ? "Nasıl cevap verirsin?" : "How would you respond?" }
    static var correctFeedback: String { lang == .turkish ? "Doğru!" : "Correct!" }
    static var wrongFeedback: String { lang == .turkish ? "Yanlış, tekrar dene!" : "Wrong, try again!" }
    static var typingIndicator: String { lang == .turkish ? "Yazıyor..." : "Typing..." }
    static var congratulations: String { lang == .turkish ? "Tebrikler!" : "Congratulations!" }
    static var scenarioComplete: String {
        lang == .turkish ? "Bu konuşma senaryosunu başarıyla tamamladın." : "You successfully completed this conversation scenario."
    }

    // MARK: - Story Reader
    static var storyVocabulary: String { lang == .turkish ? "Hikayenin Kelimeleri" : "Story Vocabulary" }
    static var completeStory: String { lang == .turkish ? "Hikayeyi Tamamla" : "Complete Story" }
    static var backButton: String { lang == .turkish ? "Geri" : "Back" }
    static var sentenceReview: String { lang == .turkish ? "Cümle İncelemesi" : "Sentence Review" }
    static var translationLabel: String { lang == .turkish ? "Türkçe Çeviri" : "Translation" }
    static var keyVocabulary: String { lang == .turkish ? "Anahtar Kelimeler" : "Key Vocabulary" }
    static var noNewVocab: String { lang == .turkish ? "Bu cümle için yeni kelime yok." : "No new vocabulary for this sentence." }

    // MARK: - Story Types
    static func storyTypeName(_ type: String) -> String {
        switch type {
        case "hiragana":
            return lang == .turkish ? "Sadece Hiragana" : "Hiragana Only"
        case "hiraganaKatakana":
            return lang == .turkish ? "Hiragana + Katakana" : "Hiragana + Katakana"
        case "hiraganaKanji":
            return lang == .turkish ? "Hiragana + Kanji" : "Hiragana + Kanji"
        case "all":
            return lang == .turkish ? "Hiragana + Katakana + Kanji" : "Hiragana + Katakana + Kanji"
        default:
            return type
        }
    }

    // MARK: - LearnableItemKind display names
    static func itemKindName(_ kind: String) -> String {
        switch kind {
        case "hiraganaCharacter":
            return "Hiragana"
        case "katakanaCharacter":
            return "Katakana"
        case "vocabularyWord":
            return lang == .turkish ? "Kelimeler" : "Words"
        case "kanji":
            return "Kanji"
        case "grammar":
            return lang == .turkish ? "Gramer" : "Grammar"
        default:
            return kind
        }
    }

    // MARK: - Grammar Categories
    static func grammarCategoryName(_ category: String) -> String {
        switch category {
        case "particle":
            return lang == .turkish ? "Edatlar" : "Particles"
        case "verb":
            return lang == .turkish ? "Fiiller" : "Verbs"
        case "adjective":
            return lang == .turkish ? "Sıfatlar" : "Adjectives"
        case "conjunction":
            return lang == .turkish ? "Bağlaçlar" : "Conjunctions"
        case "expression":
            return lang == .turkish ? "İfadeler" : "Expressions"
        default:
            return category
        }
    }

    // MARK: - Language Settings
    static var languageSettingsTitle: String { lang == .turkish ? "Uygulama Dili" : "App Language" }
    static var languageSettingsSubtitle: String {
        lang == .turkish ? "Arayüz dilini değiştir" : "Change interface language"
    }

    // MARK: - Notifications
    static var notificationsTitle: String { lang == .turkish ? "Bildirimler" : "Notifications" }
    static var dailyReminderToggle: String { lang == .turkish ? "Günlük Hatırlatıcı" : "Daily Reminder" }
    static var reminderTimeLabel: String { lang == .turkish ? "Hatırlatma Saati" : "Reminder Time" }
}
