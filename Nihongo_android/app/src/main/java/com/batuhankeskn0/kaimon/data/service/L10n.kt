package com.batuhankeskn0.kaimon.data.service

/**
 * Tüm arayüz string'lerini Türkçe / İngilizce olarak barındıran lokalizasyon katmanı.
 * Ekranlar `L10n.tabHome` gibi property'ler üzerinden erişir; `LanguageManager.shared.current`
 * değiştiğinde Compose snapshot state zinciri ile arayüz otomatik güncellenir.
 */
object L10n {
    private val lang: LanguageManager.AppLanguage
        get() = LanguageManager.shared.current

    // MARK: - Tab Bar
    val tabHome: String
        get() = if (lang == LanguageManager.AppLanguage.TURKISH) "Ana" else "Home"
    val tabAlphabet: String
        get() = if (lang == LanguageManager.AppLanguage.TURKISH) "Alfabe" else "Alphabet"
    val tabGrammar: String
        get() = if (lang == LanguageManager.AppLanguage.TURKISH) "Gramer" else "Grammar"
    val tabVocabulary: String
        get() = if (lang == LanguageManager.AppLanguage.TURKISH) "Kelime" else "Vocab"
    val tabReading: String
        get() = if (lang == LanguageManager.AppLanguage.TURKISH) "Okuma" else "Reading"
    val tabDictionary: String
        get() = if (lang == LanguageManager.AppLanguage.TURKISH) "Sözlük" else "Dictionary"
    val tabReference: String
        get() = if (lang == LanguageManager.AppLanguage.TURKISH) "Kütüphane" else "Library"

    // MARK: - Home
    fun streakText(count: Int): String =
        if (lang == LanguageManager.AppLanguage.TURKISH) "$count günlük seri" else "$count day streak"

    val noStreak: String
        get() = if (lang == LanguageManager.AppLanguage.TURKISH) "Seri yok — bugün başla" else "No streak — start today"
    val resumeLabel: String
        get() = if (lang == LanguageManager.AppLanguage.TURKISH) "KALDIĞIN YER" else "WHERE YOU LEFT OFF"
    val resumeButton: String
        get() = if (lang == LanguageManager.AppLanguage.TURKISH) "DEVAM ET  →" else "CONTINUE  →"

    // MARK: - Sections
    val sectionReview: String
        get() = if (lang == LanguageManager.AppLanguage.TURKISH) "TEKRAR & İLERLEME" else "REVIEW & PROGRESS"
    val sectionPractice: String
        get() = if (lang == LanguageManager.AppLanguage.TURKISH) "KONUŞMA PRATİĞİ" else "CONVERSATION PRACTICE"
    val sectionLearn: String
        get() = if (lang == LanguageManager.AppLanguage.TURKISH) "ÖĞREN" else "LEARN"

    // MARK: - Review & Progress cards
    val reviewTitle: String
        get() = if (lang == LanguageManager.AppLanguage.TURKISH) "Tekrar Çalış" else "Review"
    val reviewNoItems: String
        get() = if (lang == LanguageManager.AppLanguage.TURKISH) "Bekleyen öğe yok" else "No items pending"

    fun reviewItemsWaiting(count: Int): String =
        if (lang == LanguageManager.AppLanguage.TURKISH) "$count öğe seni bekliyor" else "$count items waiting"

    val progressTitle: String
        get() = if (lang == LanguageManager.AppLanguage.TURKISH) "İlerleme" else "Progress"
    val progressSubtitle: String
        get() = if (lang == LanguageManager.AppLanguage.TURKISH) "Öğrenilenleri görüntüle" else "View learned items"

    // MARK: - Practice
    val scenarioPractice: String
        get() = if (lang == LanguageManager.AppLanguage.TURKISH) "Sohbet Pratiği" else "Chat Practice"
    val aiChat: String
        get() = if (lang == LanguageManager.AppLanguage.TURKISH) "AI ile Sohbet" else "Chat with AI"

    // MARK: - Alphabet
    val alphabetTitle: String
        get() = if (lang == LanguageManager.AppLanguage.TURKISH) "Alfabe" else "Alphabet"
    val alphabetChartTitle: String
        get() = if (lang == LanguageManager.AppLanguage.TURKISH) "Alfabe Tablosu" else "Alphabet Chart"
    val alphabetChartSubtitle: String
        get() = if (lang == LanguageManager.AppLanguage.TURKISH) "Harfe dokunarak sesini dinle" else "Tap any character to listen"
    val basicAlphabetTab: String
        get() = if (lang == LanguageManager.AppLanguage.TURKISH) "Temel (46 Harf)" else "Basic (46)"
    val dakutenAlphabetTab: String
        get() = if (lang == LanguageManager.AppLanguage.TURKISH) "Tenten & Maru" else "Dakuten"
    val yoonAlphabetTab: String
        get() = if (lang == LanguageManager.AppLanguage.TURKISH) "Birleşik Heceler" else "Combo (Yoon)"
    val practiceCardsTab: String
        get() = if (lang == LanguageManager.AppLanguage.TURKISH) "Kartlarla Çalış" else "Practice Cards"
    val kanjiChartSubtitle: String
        get() = if (lang == LanguageManager.AppLanguage.TURKISH) "Kanji'ye dokunarak sesini ve anlamını incele" else "Tap any kanji to listen and inspect meaning"
    val writingSystemHiragana: String
        get() = "Hiragana (あ)"
    val writingSystemKatakana: String
        get() = "Katakana (ア)"
    val writingSystemKanji: String
        get() = "Kanji (漢)"
    val listenButton: String
        get() = if (lang == LanguageManager.AppLanguage.TURKISH) "Dinle" else "Listen"

    fun kanjiChartAllFilter(count: Int): String =
        if (lang == LanguageManager.AppLanguage.TURKISH) "Tümü ($count)" else "All ($count)"

    fun kanjiChartPartFilter(part: Int): String =
        if (lang == LanguageManager.AppLanguage.TURKISH) "Bölüm $part" else "Part $part"

    // MARK: - Grammar
    val grammarTitle: String
        get() = if (lang == LanguageManager.AppLanguage.TURKISH) "Gramer" else "Grammar"

    fun grammarLevel(level: String): String =
        if (lang == LanguageManager.AppLanguage.TURKISH) "$level Gramer" else "$level Grammar"

    fun grammarSubtitle(topics: Int, categories: Int): String =
        if (lang == LanguageManager.AppLanguage.TURKISH) "$topics konu · $categories kategori" else "$topics topics · $categories categories"

    fun grammarUnitTitle(unitId: Int, title: String): String =
        if (lang == LanguageManager.AppLanguage.TURKISH) "Ünite $unitId: $title" else "Unit $unitId: $title"

    val grammarUnitLockedTitle: String
        get() = if (lang == LanguageManager.AppLanguage.TURKISH) "Ünite Kilitli" else "Unit Locked"

    fun grammarUnitLockedMessage(unitId: Int): String =
        if (lang == LanguageManager.AppLanguage.TURKISH) {
            "Ünite $unitId kilidini açmak için lütfen önceki ünitedeki konuları tamamlayın."
        } else {
            "Please complete the topics in the previous unit to unlock Unit $unitId."
        }

    fun grammarAutoSessionTitle(level: String): String =
        if (lang == LanguageManager.AppLanguage.TURKISH) "$level Otomatik Gramer Oturumu" else "$level Auto Grammar Session"

    val grammarAutoSessionSubtitle: String
        get() = if (lang == LanguageManager.AppLanguage.TURKISH) {
            "Öğrenilmemiş 3 konuyu sırayla çalış ve toplu pratik yap."
        } else {
            "Study 3 unlearned topics in order and practice together."
        }

    val grammarStartAutoSessionButton: String
        get() = if (lang == LanguageManager.AppLanguage.TURKISH) "Oturumu Başlat" else "Start Session"

    fun grammarSyllabusSectionLabel(count: Int): String =
        if (lang == LanguageManager.AppLanguage.TURKISH) "ÖĞRENME YOLU ($count ÜNİTE)" else "LEARNING PATH ($count UNITS)"

    val grammarShortcutsSectionLabel: String
        get() = if (lang == LanguageManager.AppLanguage.TURKISH) "BAŞVURU & ARAÇLAR" else "REFERENCE & TOOLS"

    val verbConjugatorShortcutTitle: String
        get() = if (lang == LanguageManager.AppLanguage.TURKISH) "Fiil Çekimleyici" else "Verb Conjugator"

    val verbConjugatorShortcutSubtitle: String
        get() = if (lang == LanguageManager.AppLanguage.TURKISH) {
            "Seçilen veya aranan fiilin tüm çekim tabloları"
        } else {
            "All conjugation tables for any searched or selected verb"
        }

    val grammarLibraryShortcutTitle: String
        get() = if (lang == LanguageManager.AppLanguage.TURKISH) "Gramer Kütüphanesi" else "Grammar Library"

    val grammarLibraryShortcutSubtitle: String
        get() = if (lang == LanguageManager.AppLanguage.TURKISH) {
            "Tüm gramer konularına ve fiil kategorilerine kilitsiz göz at"
        } else {
            "Browse all grammar rules and verb categories without locks"
        }

    val grammarPracticeUnitButton: String
        get() = if (lang == LanguageManager.AppLanguage.TURKISH) "Ünite Pratiği" else "Unit Practice"

    val grammarAllLearnedTitle: String
        get() = if (lang == LanguageManager.AppLanguage.TURKISH) "Harika!" else "Awesome!"

    fun grammarAllLearnedMessage(level: String): String =
        if (lang == LanguageManager.AppLanguage.TURKISH) {
            "$level seviyesindeki tüm gramer konularını bitirdin."
        } else {
            "You finished all grammar topics in level $level."
        }

    val grammarGoBackButton: String
        get() = if (lang == LanguageManager.AppLanguage.TURKISH) "Geri Dön" else "Go Back"

    val grammarUnderstoodNextButton: String
        get() = if (lang == LanguageManager.AppLanguage.TURKISH) "Anladım, Devam Et →" else "Got It, Continue →"

    val grammarStartPracticeButton: String
        get() = if (lang == LanguageManager.AppLanguage.TURKISH) "Pratiğe geç →" else "Start Practice →"

    val grammarVerbConjugationCardTitle: String
        get() = if (lang == LanguageManager.AppLanguage.TURKISH) "Fiil Çekim Tablosu" else "Verb Conjugation Table"

    val grammarVerbConjugationCardSubtitle: String
        get() = if (lang == LanguageManager.AppLanguage.TURKISH) {
            "Geçmiş, Olumsuz, て, ます formlarını gör"
        } else {
            "See Past, Negative, て, ます forms"
        }

    val grammarExamplesTitle: String
        get() = if (lang == LanguageManager.AppLanguage.TURKISH) "Örnekler" else "Examples"

    val grammarSampleVerbMeaning: String
        get() = if (lang == LanguageManager.AppLanguage.TURKISH) "yemek (örnek fiil)" else "to eat (sample verb)"

    val grammarPracticeTitle: String
        get() = if (lang == LanguageManager.AppLanguage.TURKISH) "Pratik" else "Practice"

    fun grammarTopicProgress(current: Int, total: Int): String =
        if (lang == LanguageManager.AppLanguage.TURKISH) "Konu $current/$total" else "Topic $current/$total"

    val grammarFillBlankPromptLabel: String
        get() = if (lang == LanguageManager.AppLanguage.TURKISH) "Boşluğu doldur" else "Fill in the blank"

    val grammarMultipleChoicePromptLabel: String
        get() = if (lang == LanguageManager.AppLanguage.TURKISH) "Soru" else "Question"

    val grammarWordOrderPromptLabel: String
        get() = if (lang == LanguageManager.AppLanguage.TURKISH) "Kelimeleri sıraya diz" else "Arrange words in order"

    val grammarWordOrderEmptyHint: String
        get() = if (lang == LanguageManager.AppLanguage.TURKISH) {
            "Aşağıdaki kelimelere dokunarak diz."
        } else {
            "Tap the words below to arrange them."
        }

    val grammarCheckOrderButton: String
        get() = if (lang == LanguageManager.AppLanguage.TURKISH) "Kontrol Et" else "Check"

    fun grammarCorrectAnswerPrefix(answer: String): String =
        if (lang == LanguageManager.AppLanguage.TURKISH) "Doğrusu: $answer" else "Correct: $answer"

    val grammarPracticeFinishedTitle: String
        get() = if (lang == LanguageManager.AppLanguage.TURKISH) "Pratik Bitti" else "Practice Complete"

    val grammarStatCorrect: String
        get() = if (lang == LanguageManager.AppLanguage.TURKISH) "Doğru" else "Correct"

    val grammarStatWrong: String
        get() = if (lang == LanguageManager.AppLanguage.TURKISH) "Yanlış" else "Wrong"

    val grammarReviewAgainTitle: String
        get() = if (lang == LanguageManager.AppLanguage.TURKISH) "Tekrar bakılacaklar" else "Items to review"

    val grammarWrongAddedToReview: String
        get() = if (lang == LanguageManager.AppLanguage.TURKISH) {
            "Yanlış yaptığın konular \"Tekrar Çalış\" listesine eklendi."
        } else {
            "Topics you missed were added to the Review list."
        }

    // MARK: - Verb Conjugation & Reference Library
    val verbGroupPolite: String
        get() = if (lang == LanguageManager.AppLanguage.TURKISH) "Kibar" else "Polite"

    val verbGroupPlain: String
        get() = "Plain"

    val verbGroupSpecial: String
        get() = if (lang == LanguageManager.AppLanguage.TURKISH) "Özel" else "Special"

    val verbGroup1Label: String
        get() = if (lang == LanguageManager.AppLanguage.TURKISH) "Grup 1" else "Group 1"

    val verbGroup2Label: String
        get() = if (lang == LanguageManager.AppLanguage.TURKISH) "Grup 2" else "Group 2"

    val verbFullTableTitle: String
        get() = if (lang == LanguageManager.AppLanguage.TURKISH) "Tam Çekim Tablosu" else "Full Conjugation Table"

    val verbTableFormColumn: String
        get() = "Form"

    val verbTableJapaneseColumn: String
        get() = if (lang == LanguageManager.AppLanguage.TURKISH) "Japonca" else "Japanese"

    val verbSearchPlaceholder: String
        get() = if (lang == LanguageManager.AppLanguage.TURKISH) {
            "Fiil, romaji veya Türkçe anlam ara..."
        } else {
            "Search verb, romaji or meaning..."
        }

    val grammarCategorySearchPlaceholder: String
        get() = if (lang == LanguageManager.AppLanguage.TURKISH) {
            "Kural, romaji veya açıklama ara..."
        } else {
            "Search rule, romaji or explanation..."
        }

    val verbLegendIrregular: String
        get() = if (lang == LanguageManager.AppLanguage.TURKISH) "Düzensiz" else "Irregular"

    // MARK: - Vocabulary
    val vocabularyTitle: String
        get() = if (lang == LanguageManager.AppLanguage.TURKISH) "Kelimeler" else "Words"

    fun vocabularyLevel(level: String): String =
        if (lang == LanguageManager.AppLanguage.TURKISH) "$level Kelimeler" else "$level Words"

    fun vocabularySubtitle(words: Int, parts: Int): String =
        if (lang == LanguageManager.AppLanguage.TURKISH) "$words kelime · $parts bölüm" else "$words words · $parts parts"

    fun vocabularyPartTitle(level: String, part: Int): String =
        if (lang == LanguageManager.AppLanguage.TURKISH) "$level Part $part" else "$level Part $part"

    fun vocabularySessionTitle(level: String, part: Int): String =
        if (lang == LanguageManager.AppLanguage.TURKISH) "$level Kelimeler Part $part" else "$level Words Part $part"

    fun vocabularyContinueCardTitle(level: String, part: Int): String =
        if (lang == LanguageManager.AppLanguage.TURKISH) "$level Kelimeler: Part $part" else "$level Words: Part $part"

    fun vocabularyContinueCardSubtitle(count: Int = 25): String =
        if (lang == LanguageManager.AppLanguage.TURKISH) {
            "Hemen devam et ve bu bölümdeki $count kelimeyi tamamla."
        } else {
            "Continue right away and complete the $count words in this part."
        }

    val startStudyButton: String
        get() = if (lang == LanguageManager.AppLanguage.TURKISH) "Çalışmaya Başla" else "Start Studying"

    fun partFractionLabel(current: Int, total: Int): String = "Part $current / $total"

    fun allPartsSectionLabel(count: Int): String =
        if (lang == LanguageManager.AppLanguage.TURKISH) "TÜM BÖLÜMLER ($count PARÇA)" else "ALL PARTS ($count PARTS)"

    fun vocabPartCompletedSubtitle(count: Int): String =
        if (lang == LanguageManager.AppLanguage.TURKISH) "Tamamlandı • $count kelime" else "Completed • $count words"

    fun learnedPercentLabel(percent: Int): String =
        if (lang == LanguageManager.AppLanguage.TURKISH) "%$percent öğrenildi" else "$percent% learned"

    val wordListPreviewButton: String
        get() = if (lang == LanguageManager.AppLanguage.TURKISH) "Kelime Önizleme" else "Word Preview"

    val startFlashcardsButton: String
        get() = if (lang == LanguageManager.AppLanguage.TURKISH) "Kartlar" else "Flashcards"

    // MARK: - Gateway & Test-Out Exams
    fun gatewayExamCardTitle(level: String): String =
        if (lang == LanguageManager.AppLanguage.TURKISH) "$level Seviye Kapı Sınavı" else "$level Gateway Exam"

    fun gatewayExamReadySubtitle(nextLevel: String): String =
        if (lang == LanguageManager.AppLanguage.TURKISH) {
            "%85 ilerleme barajını geçtin! $nextLevel seviyesini açmak için 30 soruluk sınava gir."
        } else {
            "You passed the 85% threshold! Take the 30-question exam to unlock $nextLevel."
        }

    fun gatewayExamProgressSubtitle(currentPercent: Int): String =
        if (lang == LanguageManager.AppLanguage.TURKISH) {
            "Kapı Sınavı için %85 ilerleme gerekli (Şu an: %$currentPercent). Beklemeden geçmek için Test-Out sınavını deneyebilirsin."
        } else {
            "85% progress required for Gateway Exam (Current: $currentPercent%). Try the Test-Out exam to skip ahead."
        }

    val startGatewayExamButton: String
        get() = if (lang == LanguageManager.AppLanguage.TURKISH) "Kapı Sınavına Başla (30 Soru)" else "Start Gateway Exam (30 Qs)"

    val startTestOutChallengeButton: String
        get() = if (lang == LanguageManager.AppLanguage.TURKISH) "Test-Out Meydan Okuma (❤️❤️❤️)" else "Test-Out Challenge (❤️❤️❤️)"

    val examQuotaExhaustedTitle: String
        get() = if (lang == LanguageManager.AppLanguage.TURKISH) "Günlük Sınav Hakkı Doldu" else "Daily Exam Limit Reached"

    val examQuotaExhaustedMessage: String
        get() = if (lang == LanguageManager.AppLanguage.TURKISH) {
            "Ücretsiz planda günde 1 sınav hakkın var. Sınırsız JLPT seviye sınavı için Kaimon Pro'ya geçebilirsin."
        } else {
            "Free tier includes 1 exam per day. Upgrade to Kaimon Pro for unlimited JLPT level exams."
        }

    val examFailedTitle: String
        get() = if (lang == LanguageManager.AppLanguage.TURKISH) "Sınav Tamamlanamadı" else "Exam Not Completed"

    val examFailedTestOutMessage: String
        get() = if (lang == LanguageManager.AppLanguage.TURKISH) {
            "3 hata sınırına ulaştın. Eksik olduğun konuları çalışıp tekrar deneyebilirsin."
        } else {
            "You reached the 3-mistake limit. Study the topics you missed and try again."
        }

    fun examFailedGatewayMessage(correct: Int, total: Int): String =
        if (lang == LanguageManager.AppLanguage.TURKISH) {
            "%80 başarı barajına ulaşılamadı. (Doğru: $correct / $total)\nBiraz daha pratik yaparak tekrar deneyebilirsin."
        } else {
            "Did not reach the 80% passing score. (Correct: $correct / $total)\nPractice a bit more and try again."
        }

    val backToLessonsButton: String
        get() = if (lang == LanguageManager.AppLanguage.TURKISH) "Derslere Geri Dön" else "Back to Lessons"

    fun levelUnlockedTitle(level: String): String =
        if (lang == LanguageManager.AppLanguage.TURKISH) "$level Seviyesi Açıldı!" else "$level Level Unlocked!"

    fun examPassedSummarySubtitle(level: String): String =
        if (lang == LanguageManager.AppLanguage.TURKISH) "$level sınavını başarıyla tamamladın." else "You successfully passed the $level exam."

    // MARK: - Reading
    val readingTitle: String
        get() = if (lang == LanguageManager.AppLanguage.TURKISH) "Okuma" else "Reading"

    fun sentenceCount(count: Int): String =
        if (lang == LanguageManager.AppLanguage.TURKISH) "$count cümle" else "$count sentences"

    // MARK: - Dictionary
    val dictionaryTitle: String
        get() = if (lang == LanguageManager.AppLanguage.TURKISH) "Sözlük" else "Dictionary"
    val dictionarySearch: String
        get() = if (lang == LanguageManager.AppLanguage.TURKISH) "Kelime veya anlam ara..." else "Search word or meaning..."
    val dictionaryVocabTab: String
        get() = if (lang == LanguageManager.AppLanguage.TURKISH) "Kelime" else "Words"
    val dictionaryKanjiTab: String
        get() = if (lang == LanguageManager.AppLanguage.TURKISH) "Kanji" else "Kanji"
    val dictionaryGrammarTab: String
        get() = if (lang == LanguageManager.AppLanguage.TURKISH) "Gramer" else "Grammar"
    val grammarLibraryTitle: String
        get() = if (lang == LanguageManager.AppLanguage.TURKISH) "Dilbilgisi Kütüphanesi" else "Grammar Library"
    val grammarLibrarySubtitle: String
        get() = if (lang == LanguageManager.AppLanguage.TURKISH) "Bir kategoriye tıkla, kuralları ve formları incele." else "Tap a category to explore rules and forms."
    val allFilterLabel: String
        get() = if (lang == LanguageManager.AppLanguage.TURKISH) "Tümü" else "All"
    val verbLibrarySubtitle: String
        get() = if (lang == LanguageManager.AppLanguage.TURKISH) "Çekim tabloları · N5 fiilleri" else "Conjugation tables · N5 verbs"
    val grammarReferenceTitle: String
        get() = if (lang == LanguageManager.AppLanguage.TURKISH) "Gramer Referansı" else "Grammar Reference"
    val difficultyLabel: String
        get() = if (lang == LanguageManager.AppLanguage.TURKISH) "Zorluk" else "Difficulty"
    val formulaLabel: String
        get() = if (lang == LanguageManager.AppLanguage.TURKISH) "Formül" else "Formula"
    val exampleSentencesLabel: String
        get() = if (lang == LanguageManager.AppLanguage.TURKISH) "Örnek Cümleler" else "Example Sentences"
    val exampleWordsLabel: String
        get() = if (lang == LanguageManager.AppLanguage.TURKISH) "Örnek Kelimeler" else "Example Words"
    val meaningLabel: String
        get() = if (lang == LanguageManager.AppLanguage.TURKISH) "Anlam" else "Meaning"
    val readingLabel: String
        get() = if (lang == LanguageManager.AppLanguage.TURKISH) "Okunuş" else "Reading"
    val onyomiLabel: String
        get() = "On'yomi"
    val kunyomiLabel: String
        get() = "Kun'yomi"
    val verbConjugationTitle: String
        get() = if (lang == LanguageManager.AppLanguage.TURKISH) "Fiil Çekimleri" else "Verb Conjugations"
    val listenPronunciation: String
        get() = if (lang == LanguageManager.AppLanguage.TURKISH) "Telaffuzu Dinle" else "Listen Pronunciation"

    fun strokeCountLabel(count: Int): String =
        if (lang == LanguageManager.AppLanguage.TURKISH) "$count çizgi" else "$count strokes"

    fun grammarRuleCountSubtitle(count: Int, level: String = "N5"): String =
        if (lang == LanguageManager.AppLanguage.TURKISH) "$count kural · $level" else "$count rules · $level"

    fun grammarLessonHint(title: String): String =
        if (lang == LanguageManager.AppLanguage.TURKISH) {
            "Bu konuyu öğrenmek için Gramer bölümündeki \"$title\" dersine git."
        } else {
            "Go to the \"$title\" lesson in the Grammar tab to practice this topic."
        }

    fun noSearchResults(query: String): String =
        if (lang == LanguageManager.AppLanguage.TURKISH) "'$query' için sonuç yok" else "No results for '$query'"

    fun verbCountLabel(count: Int): String =
        if (lang == LanguageManager.AppLanguage.TURKISH) "$count fiil" else "$count verbs"

    // MARK: - Soon / locked
    val comingSoon: String
        get() = if (lang == LanguageManager.AppLanguage.TURKISH) "Yakında" else "Coming soon"

    fun levelLockedTitle(level: String): String =
        if (lang == LanguageManager.AppLanguage.TURKISH) "$level Seviyesi Kilitli" else "$level Level Locked"

    fun levelLockedDescription(previousLevel: String): String =
        if (lang == LanguageManager.AppLanguage.TURKISH) {
            "Bu seviyenin derslerine erişebilmek için $previousLevel seviyesini tamamlamalı veya Seviye Atlama Sınavı'nı (Test-Out) geçmelisin."
        } else {
            "Complete $previousLevel or pass the Test-Out exam to unlock this level's lessons."
        }

    fun levelProgressStatus(previousLevel: String): String =
        if (lang == LanguageManager.AppLanguage.TURKISH) "$previousLevel İlerleme Durumu" else "$previousLevel Progress Status"

    val kanjiPartsMetric: String
        get() = if (lang == LanguageManager.AppLanguage.TURKISH) "Kanji Parçaları" else "Kanji Parts"

    val vocabPartsMetric: String
        get() = if (lang == LanguageManager.AppLanguage.TURKISH) "Kelime Parçaları" else "Vocabulary Parts"

    val grammarUnitsMetric: String
        get() = if (lang == LanguageManager.AppLanguage.TURKISH) "Gramer Üniteleri" else "Grammar Units"

    val testOutButton: String
        get() = if (lang == LanguageManager.AppLanguage.TURKISH) "Seviyeyi Sınavla Atla (Test-Out)" else "Skip Level via Test-Out Exam"

    val completeMissingLessonsButton: String
        get() = if (lang == LanguageManager.AppLanguage.TURKISH) "Eksik Dersleri Tamamla" else "Complete Missing Lessons"

    fun lockedLevelSubtitle(subtitle: String): String =
        if (lang == LanguageManager.AppLanguage.TURKISH) "Kilitli • $subtitle" else "Locked • $subtitle"

    fun topicCount(count: Int): String =
        if (lang == LanguageManager.AppLanguage.TURKISH) "$count konu" else "$count topics"

    fun wordCount(count: Int): String =
        if (lang == LanguageManager.AppLanguage.TURKISH) "$count kelime" else "$count words"

    val noStoriesForLevel: String
        get() = if (lang == LanguageManager.AppLanguage.TURKISH) "Bu seviye için henüz hikaye eklenmedi." else "No stories added for this level yet."

    // MARK: - Kanji
    fun kanjiLevel(level: String): String = "${level} Kanji's"

    fun kanjiSubtitle(count: Int, parts: Int): String =
        if (lang == LanguageManager.AppLanguage.TURKISH) "$count kanji · $parts bölüm" else "$count kanji · $parts parts"

    fun kanjiPartTitle(level: String, part: Int): String = "${level} Kanji's Part $part"

    fun kanjiCount(count: Int): String = "$count kanji"

    fun kanjiPartCompletedSubtitle(count: Int): String =
        if (lang == LanguageManager.AppLanguage.TURKISH) "Tamamlandı • $count kanji" else "Completed • $count kanji"

    val partLockedTitle: String
        get() = if (lang == LanguageManager.AppLanguage.TURKISH) "Bölüm Kilitli" else "Part Locked"

    val partLockedSubtitle: String
        get() = if (lang == LanguageManager.AppLanguage.TURKISH) "Kilitli • Önceki bölümü tamamla" else "Locked • Complete previous part"

    fun partLockedMessage(partNumber: Int): String =
        if (lang == LanguageManager.AppLanguage.TURKISH) {
            "Part $partNumber kilidini açmak için lütfen önceki bölümü tamamlayın."
        } else {
            "Please complete the previous part to unlock Part $partNumber."
        }

    val okButton: String
        get() = if (lang == LanguageManager.AppLanguage.TURKISH) "Tamam" else "OK"

    // MARK: - Flashcard / Quiz
    val quizTitle: String
        get() = "Quiz"
    val quickQuiz: String
        get() = if (lang == LanguageManager.AppLanguage.TURKISH) "Hızlı Quiz" else "Quick Quiz"
    val flashcardKnownButton: String
        get() = if (lang == LanguageManager.AppLanguage.TURKISH) "Öğrendim" else "I Know"
    val flashcardRepeatButton: String
        get() = if (lang == LanguageManager.AppLanguage.TURKISH) "Tekrar Et" else "Repeat"
    val swipeHint: String
        get() = if (lang == LanguageManager.AppLanguage.TURKISH) "← Tekrar Et   ·   Öğrendim →" else "← Repeat   ·   I Know →"

    fun learnedCount(learned: Int, total: Int): String =
        if (lang == LanguageManager.AppLanguage.TURKISH) "$learned/$total öğrenildi" else "$learned/$total learned"

    val hintButton: String
        get() = if (lang == LanguageManager.AppLanguage.TURKISH) "İpucu" else "Hint"
    val tapToContinue: String
        get() = if (lang == LanguageManager.AppLanguage.TURKISH) "Devam etmek için karta dokun" else "Tap card to continue"
    val continueButton: String
        get() = if (lang == LanguageManager.AppLanguage.TURKISH) "Devam Et" else "Continue"
    val closeButton: String
        get() = if (lang == LanguageManager.AppLanguage.TURKISH) "Kapat" else "Close"
    val finishButton: String
        get() = if (lang == LanguageManager.AppLanguage.TURKISH) "Bitir" else "Finish"

    fun scoreResult(score: Int, total: Int): String =
        if (lang == LanguageManager.AppLanguage.TURKISH) "$score / $total doğru" else "$score / $total correct"

    // MARK: - Lesson Complete
    val lessonCompleteTitle: String
        get() = if (lang == LanguageManager.AppLanguage.TURKISH) "Harika iş, ders tamamlandı" else "Great job, lesson complete"

    // MARK: - Session Summary
    val sessionSummary: String
        get() = if (lang == LanguageManager.AppLanguage.TURKISH) "Oturum Özeti" else "Session Summary"
    val cardsSeenLabel: String
        get() = if (lang == LanguageManager.AppLanguage.TURKISH) "Gördüğün kart" else "Cards seen"
    val wrongAnswersLabel: String
        get() = if (lang == LanguageManager.AppLanguage.TURKISH) "Yanlış cevap" else "Wrong answers"
    val perfectScore: String
        get() = if (lang == LanguageManager.AppLanguage.TURKISH) "Hiç yanlışın yok — mükemmel! 🎉" else "No mistakes — perfect! 🎉"
    val wrongItemsTitle: String
        get() = if (lang == LanguageManager.AppLanguage.TURKISH) "Yanlış yaptıkların" else "Items you got wrong"
    val addedToReview: String
        get() = if (lang == LanguageManager.AppLanguage.TURKISH) "Bunlar \"Tekrar Çalış\" listesine eklendi." else "These were added to your review list."

    // MARK: - Review List
    val reviewNavTitle: String
        get() = if (lang == LanguageManager.AppLanguage.TURKISH) "Tekrar Çalış" else "Review"
    val reviewEmptyTitle: String
        get() = if (lang == LanguageManager.AppLanguage.TURKISH) "Harika!" else "Great!"
    val reviewEmptyMessage: String
        get() = if (lang == LanguageManager.AppLanguage.TURKISH) {
            "Tekrar çalışman gereken bir şey yok. Yanlış yaptığın kartlar burada birikir."
        } else {
            "Nothing to review. Cards you get wrong will appear here."
        }

    fun practiceWithThese(): String =
        if (lang == LanguageManager.AppLanguage.TURKISH) "Bunlarla çalış →" else "Practice these →"

    val learnedButton: String
        get() = if (lang == LanguageManager.AppLanguage.TURKISH) "Öğrendim ✓" else "Learned ✓"

    fun reviewPrefix(name: String): String =
        if (lang == LanguageManager.AppLanguage.TURKISH) "Tekrar: $name" else "Review: $name"

    // MARK: - Progress Overview
    val progressNavTitle: String
        get() = if (lang == LanguageManager.AppLanguage.TURKISH) "İlerleme" else "Progress"
    val learnedSection: String
        get() = if (lang == LanguageManager.AppLanguage.TURKISH) "Öğrenilenler" else "Learned"
    val streakSection: String
        get() = if (lang == LanguageManager.AppLanguage.TURKISH) "Seri" else "Streak"

    fun longestStreak(count: Int): String =
        if (lang == LanguageManager.AppLanguage.TURKISH) "En uzun: $count" else "Longest: $count"

    fun levelLabel(level: Int): String =
        if (lang == LanguageManager.AppLanguage.TURKISH) "Seviye $level" else "Level $level"

    val studyTime: String
        get() = if (lang == LanguageManager.AppLanguage.TURKISH) "Çalışma Süresi" else "Study Time"

    fun permanentlyLearned(count: Int): String =
        if (lang == LanguageManager.AppLanguage.TURKISH) {
            "🏆 $count tanesi kalıcı öğrenildi (4+ doğru tekrar)"
        } else {
            "🏆 $count permanently learned (4+ correct reviews)"
        }

    fun timeFormatted(hours: Int, minutes: Int): String =
        if (lang == LanguageManager.AppLanguage.TURKISH) {
            if (hours > 0) "$hours sa $minutes dk" else "$minutes dk"
        } else {
            if (hours > 0) "${hours}h ${minutes}m" else "${minutes}m"
        }

    val chartDay: String
        get() = if (lang == LanguageManager.AppLanguage.TURKISH) "Gün" else "Day"
    val chartMinutes: String
        get() = if (lang == LanguageManager.AppLanguage.TURKISH) "Dakika" else "Minutes"
    val chartXpEarned: String
        get() = if (lang == LanguageManager.AppLanguage.TURKISH) "Kazanılan XP" else "XP Earned"
    val totalXpLabel: String
        get() = if (lang == LanguageManager.AppLanguage.TURKISH) "Toplam XP" else "Total XP"
    val totalLearnedLabel: String
        get() = if (lang == LanguageManager.AppLanguage.TURKISH) "Öğrenilen Öğe" else "Items Learned"
    val srsDistributionSection: String
        get() = if (lang == LanguageManager.AppLanguage.TURKISH) "SRS Kademe Dağılımı" else "SRS Stage Distribution"
    val jlptLevelStatusSection: String
        get() = if (lang == LanguageManager.AppLanguage.TURKISH) "JLPT Seviye Durumu" else "JLPT Level Status"
    val srsStageNew: String
        get() = if (lang == LanguageManager.AppLanguage.TURKISH) "Yeni" else "New"
    val srsStageApprentice: String
        get() = if (lang == LanguageManager.AppLanguage.TURKISH) "Çırak" else "Apprentice"
    val srsStageGuru: String
        get() = if (lang == LanguageManager.AppLanguage.TURKISH) "Kalfa" else "Guru"
    val srsStageMaster: String
        get() = if (lang == LanguageManager.AppLanguage.TURKISH) "Usta" else "Master"
    val srsStageEnlightened: String
        get() = if (lang == LanguageManager.AppLanguage.TURKISH) "Aydınlanmış" else "Enlightened"
    val srsStageBurned: String
        get() = if (lang == LanguageManager.AppLanguage.TURKISH) "Kalıcı" else "Burned"

    fun readingModuleSubtitle(storyCount: Int): String =
        if (lang == LanguageManager.AppLanguage.TURKISH) "N5 · $storyCount hikaye" else "N5 · $storyCount stories"

    fun kanjiModuleSubtitle(parts: Int = 4): String =
        if (lang == LanguageManager.AppLanguage.TURKISH) "N5 · $parts bölüm" else "N5 · $parts parts"

    fun vocabModuleSubtitle(parts: Int = 27): String =
        if (lang == LanguageManager.AppLanguage.TURKISH) "N5 · $parts bölüm" else "N5 · $parts parts"

    fun grammarModuleSubtitle(categories: Int = 5): String =
        if (lang == LanguageManager.AppLanguage.TURKISH) "N5 · $categories kategori" else "N5 · $categories categories"

    // MARK: - Auth / Account
    val accountTitle: String
        get() = if (lang == LanguageManager.AppLanguage.TURKISH) "Hesabım" else "My Account"

    fun ageLabel(age: Int): String =
        if (lang == LanguageManager.AppLanguage.TURKISH) "Yaş: $age" else "Age: $age"

    val accountSyncInfo: String
        get() = if (lang == LanguageManager.AppLanguage.TURKISH) {
            "İlerlemen bu hesaba bağlı. Başka bir cihazda aynı hesapla giriş yaptığında kaldığın yerden devam edersin."
        } else {
            "Your progress is linked to this account. Sign in on another device to pick up where you left off."
        }
    val signOutButton: String
        get() = if (lang == LanguageManager.AppLanguage.TURKISH) "Çıkış Yap" else "Sign Out"

    // MARK: - Sign In
    val signUpTitle: String
        get() = if (lang == LanguageManager.AppLanguage.TURKISH) "Hesap oluştur" else "Create Account"
    val signInTitle: String
        get() = if (lang == LanguageManager.AppLanguage.TURKISH) "Giriş yap" else "Sign In"
    val signInDescription: String
        get() = if (lang == LanguageManager.AppLanguage.TURKISH) {
            "İlerlemen hesabına kaydedilir; başka bir cihazdan giriş yaptığında kaldığın yerden devam edersin."
        } else {
            "Your progress is saved to your account. Sign in from another device to continue where you left off."
        }
    val firstNamePlaceholder: String
        get() = if (lang == LanguageManager.AppLanguage.TURKISH) "Ad" else "First Name"
    val lastNamePlaceholder: String
        get() = if (lang == LanguageManager.AppLanguage.TURKISH) "Soyad" else "Last Name"
    val agePlaceholder: String
        get() = if (lang == LanguageManager.AppLanguage.TURKISH) "Yaş (opsiyonel)" else "Age (optional)"
    val emailPlaceholder: String
        get() = "Email"
    val passwordPlaceholder: String
        get() = if (lang == LanguageManager.AppLanguage.TURKISH) "Şifre (en az 6 karakter)" else "Password (min 6 chars)"
    val signUpButton: String
        get() = if (lang == LanguageManager.AppLanguage.TURKISH) "Kayıt Ol" else "Sign Up"
    val signInButton: String
        get() = if (lang == LanguageManager.AppLanguage.TURKISH) "Giriş Yap" else "Sign In"
    val alreadyHaveAccount: String
        get() = if (lang == LanguageManager.AppLanguage.TURKISH) "Zaten hesabın var mı? Giriş yap" else "Already have an account? Sign in"
    val noAccount: String
        get() = if (lang == LanguageManager.AppLanguage.TURKISH) "Hesabın yok mu? Kayıt ol" else "Don't have an account? Sign up"
    val forgotPassword: String
        get() = if (lang == LanguageManager.AppLanguage.TURKISH) "Şifremi unuttum" else "Forgot password"
    val orDivider: String
        get() = if (lang == LanguageManager.AppLanguage.TURKISH) "veya" else "or"
    val googleSignIn: String
        get() = if (lang == LanguageManager.AppLanguage.TURKISH) "Google ile devam et" else "Continue with Google"
    val continueAsGuest: String
        get() = if (lang == LanguageManager.AppLanguage.TURKISH) "Şimdilik misafir olarak devam et" else "Continue as guest for now"
    val emptyEmailPassword: String
        get() = if (lang == LanguageManager.AppLanguage.TURKISH) "Email ve şifre boş bırakılamaz." else "Email and password cannot be empty."
    val emptyName: String
        get() = if (lang == LanguageManager.AppLanguage.TURKISH) "Ad ve soyad boş bırakılamaz." else "First and last name cannot be empty."
    val passwordResetPrompt: String
        get() = if (lang == LanguageManager.AppLanguage.TURKISH) "Şifre sıfırlama için üstteki alana email adresini yaz." else "Enter your email above to reset your password."

    fun passwordResetSent(email: String): String =
        if (lang == LanguageManager.AppLanguage.TURKISH) {
            "Şifre sıfırlama bağlantısı $email adresine gönderildi."
        } else {
            "Password reset link sent to $email."
        }

    // MARK: - Chat
    val aiPracticeTitle: String
        get() = if (lang == LanguageManager.AppLanguage.TURKISH) "AI Pratik" else "AI Practice"
    val chatInputPlaceholder: String
        get() = if (lang == LanguageManager.AppLanguage.TURKISH) "Japonca veya Türkçe yaz..." else "Write in Japanese or English..."

    // MARK: - Scenarios
    val scenariosTitle: String
        get() = if (lang == LanguageManager.AppLanguage.TURKISH) "Senaryolar" else "Scenarios"
    val howToRespond: String
        get() = if (lang == LanguageManager.AppLanguage.TURKISH) "Nasıl cevap verirsin?" else "How would you respond?"
    val correctFeedback: String
        get() = if (lang == LanguageManager.AppLanguage.TURKISH) "Doğru!" else "Correct!"
    val wrongFeedback: String
        get() = if (lang == LanguageManager.AppLanguage.TURKISH) "Yanlış, tekrar dene!" else "Wrong, try again!"
    val typingIndicator: String
        get() = if (lang == LanguageManager.AppLanguage.TURKISH) "Yazıyor..." else "Typing..."
    val congratulations: String
        get() = if (lang == LanguageManager.AppLanguage.TURKISH) "Tebrikler!" else "Congratulations!"
    val scenarioComplete: String
        get() = if (lang == LanguageManager.AppLanguage.TURKISH) {
            "Bu konuşma senaryosunu başarıyla tamamladın."
        } else {
            "You successfully completed this conversation scenario."
        }

    // MARK: - Story Reader
    val storyVocabulary: String
        get() = if (lang == LanguageManager.AppLanguage.TURKISH) "Hikayenin Kelimeleri" else "Story Vocabulary"
    val completeStory: String
        get() = if (lang == LanguageManager.AppLanguage.TURKISH) "Hikayeyi Tamamla" else "Complete Story"
    val backButton: String
        get() = if (lang == LanguageManager.AppLanguage.TURKISH) "Geri" else "Back"
    val sentenceReview: String
        get() = if (lang == LanguageManager.AppLanguage.TURKISH) "Cümle İncelemesi" else "Sentence Review"
    val translationLabel: String
        get() = if (lang == LanguageManager.AppLanguage.TURKISH) "Türkçe Çeviri" else "Translation"
    val keyVocabulary: String
        get() = if (lang == LanguageManager.AppLanguage.TURKISH) "Anahtar Kelimeler" else "Key Vocabulary"
    val noNewVocab: String
        get() = if (lang == LanguageManager.AppLanguage.TURKISH) "Bu cümle için yeni kelime yok." else "No new vocabulary for this sentence."
    val readingModeContinuous: String
        get() = if (lang == LanguageManager.AppLanguage.TURKISH) "Tam Metin" else "Full Text"
    val readingModeSentenceBySentence: String
        get() = if (lang == LanguageManager.AppLanguage.TURKISH) "Cümle Cümle" else "By Sentence"
    val toggleFuriganaLabel: String
        get() = "Furigana"
    val toggleRomajiLabel: String
        get() = "Romaji"
    val toggleTranslationLabel: String
        get() = if (lang == LanguageManager.AppLanguage.TURKISH) "Çeviri" else "Translation"
    val storyTapSentenceHint: String
        get() = if (lang == LanguageManager.AppLanguage.TURKISH) {
            "Cümleye dokunarak sesli dinleyebilir ve kelime incelemesini açabilirsin."
        } else {
            "Tap any sentence to listen and inspect vocabulary."
        }

    // MARK: - Story Types
    fun storyTypeName(type: String): String =
        when (type) {
            "hiragana" -> if (lang == LanguageManager.AppLanguage.TURKISH) "Sadece Hiragana" else "Hiragana Only"
            "hiraganaKatakana" -> "Hiragana + Katakana"
            "hiraganaKanji" -> "Hiragana + Kanji"
            "all" -> "Hiragana + Katakana + Kanji"
            else -> type
        }

    // MARK: - LearnableItemKind display names
    fun itemKindName(kind: String): String =
        when (kind) {
            "hiraganaCharacter" -> "Hiragana"
            "katakanaCharacter" -> "Katakana"
            "vocabularyWord" -> if (lang == LanguageManager.AppLanguage.TURKISH) "Kelimeler" else "Words"
            "kanji" -> "Kanji"
            "grammar" -> if (lang == LanguageManager.AppLanguage.TURKISH) "Gramer" else "Grammar"
            else -> kind
        }

    // MARK: - Grammar Categories
    fun grammarCategoryName(category: String): String =
        when (category) {
            "particle" -> if (lang == LanguageManager.AppLanguage.TURKISH) "Edatlar" else "Particles"
            "verb" -> if (lang == LanguageManager.AppLanguage.TURKISH) "Fiiller" else "Verbs"
            "adjective" -> if (lang == LanguageManager.AppLanguage.TURKISH) "Sıfatlar" else "Adjectives"
            "conjunction" -> if (lang == LanguageManager.AppLanguage.TURKISH) "Bağlaçlar" else "Conjunctions"
            "expression" -> if (lang == LanguageManager.AppLanguage.TURKISH) "İfadeler" else "Expressions"
            else -> category
        }

    // MARK: - Language Settings
    val languageSettingsTitle: String
        get() = if (lang == LanguageManager.AppLanguage.TURKISH) "Uygulama Dili" else "App Language"
    val languageSettingsSubtitle: String
        get() = if (lang == LanguageManager.AppLanguage.TURKISH) "Arayüz dilini değiştir" else "Change interface language"

    // MARK: - Notifications
    val notificationsTitle: String
        get() = if (lang == LanguageManager.AppLanguage.TURKISH) "Bildirimler" else "Notifications"
    val dailyReminderToggle: String
        get() = if (lang == LanguageManager.AppLanguage.TURKISH) "Günlük Hatırlatıcı" else "Daily Reminder"
    val reminderTimeLabel: String
        get() = if (lang == LanguageManager.AppLanguage.TURKISH) "Hatırlatma Saati" else "Reminder Time"

    // MARK: - Paywall / Pro
    val proName: String
        get() = "Kaimon Pro"
    val paywallHeadline: String
        get() = if (lang == LanguageManager.AppLanguage.TURKISH) "Japoncaya\ntam erişim." else "Full access\nto Japanese."
    val paywallBenefitContent: String
        get() = if (lang == LanguageManager.AppLanguage.TURKISH) "Tüm N5 kanji, kelime ve gramer bölümleri" else "All N5 kanji, vocabulary and grammar parts"
    val paywallBenefitAI: String
        get() = if (lang == LanguageManager.AppLanguage.TURKISH) "Sınırsız AI sohbet partneri" else "Unlimited AI chat partner"
    val paywallBenefitExams: String
        get() = if (lang == LanguageManager.AppLanguage.TURKISH) "Sınırsız JLPT deneme sınavı" else "Unlimited JLPT practice exams"
    val paywallBenefitNoAds: String
        get() = if (lang == LanguageManager.AppLanguage.TURKISH) "Reklamsız, kesintisiz çalışma" else "Ad-free, uninterrupted study"
    val proUnlockHint: String
        get() = if (lang == LanguageManager.AppLanguage.TURKISH) "Pro ile aç" else "Unlock with Pro"

    fun aiQuotaRemaining(count: Int): String =
        if (lang == LanguageManager.AppLanguage.TURKISH) "Bugün $count ücretsiz mesaj hakkın kaldı" else "$count free messages left today"

    val aiQuotaExhausted: String
        get() = if (lang == LanguageManager.AppLanguage.TURKISH) {
            "Günlük ücretsiz mesaj hakkın bitti. Sınırsız sohbet için Pro'ya geç."
        } else {
            "You've used today's free messages. Go Pro for unlimited chat."
        }
    val planYearly: String
        get() = if (lang == LanguageManager.AppLanguage.TURKISH) "Yıllık" else "Yearly"
    val planMonthly: String
        get() = if (lang == LanguageManager.AppLanguage.TURKISH) "Aylık" else "Monthly"
    val planLifetime: String
        get() = if (lang == LanguageManager.AppLanguage.TURKISH) "Ömür Boyu" else "Lifetime"
    val planBestValue: String
        get() = if (lang == LanguageManager.AppLanguage.TURKISH) "EN AVANTAJLI" else "BEST VALUE"
    val planOneTime: String
        get() = if (lang == LanguageManager.AppLanguage.TURKISH) "Tek seferlik ödeme" else "One-time payment"
    val planPerYear: String
        get() = if (lang == LanguageManager.AppLanguage.TURKISH) "/ yıl" else "/ year"
    val planPerMonth: String
        get() = if (lang == LanguageManager.AppLanguage.TURKISH) "/ ay" else "/ month"

    fun planPerMonthEquivalent(price: String): String =
        if (lang == LanguageManager.AppLanguage.TURKISH) "ayda $price" else "$price per month"

    val startTrial: String
        get() = if (lang == LanguageManager.AppLanguage.TURKISH) "7 GÜN ÜCRETSİZ BAŞLAT" else "START 7-DAY FREE TRIAL"
    val subscribeButton: String
        get() = if (lang == LanguageManager.AppLanguage.TURKISH) "PRO'YA GEÇ" else "GO PRO"
    val buyLifetimeButton: String
        get() = if (lang == LanguageManager.AppLanguage.TURKISH) "ÖMÜR BOYU SATIN AL" else "BUY LIFETIME"
    val restorePurchases: String
        get() = if (lang == LanguageManager.AppLanguage.TURKISH) "Satın Alımları Geri Yükle" else "Restore Purchases"
    val termsOfUse: String
        get() = if (lang == LanguageManager.AppLanguage.TURKISH) "Kullanım Koşulları" else "Terms of Use"
    val privacyPolicy: String
        get() = if (lang == LanguageManager.AppLanguage.TURKISH) "Gizlilik Politikası" else "Privacy Policy"
    val productsUnavailable: String
        get() = if (lang == LanguageManager.AppLanguage.TURKISH) "Planlar şu an yüklenemedi." else "Plans couldn't be loaded right now."
    val retry: String
        get() = if (lang == LanguageManager.AppLanguage.TURKISH) "Tekrar Dene" else "Try Again"
    val purchasePending: String
        get() = if (lang == LanguageManager.AppLanguage.TURKISH) "Satın alma onay bekliyor." else "Purchase is awaiting approval."
    val proActiveTitle: String
        get() = if (lang == LanguageManager.AppLanguage.TURKISH) "Pro aktif" else "Pro is active"
    val proActiveSubtitle: String
        get() = if (lang == LanguageManager.AppLanguage.TURKISH) "Tüm içeriğe erişimin var. Teşekkürler!" else "You have access to everything. Thank you!"
    val manageSubscription: String
        get() = if (lang == LanguageManager.AppLanguage.TURKISH) "Aboneliği Yönet" else "Manage Subscription"

    fun renewsOn(date: String): String =
        if (lang == LanguageManager.AppLanguage.TURKISH) "Yenilenme/bitiş: $date" else "Renews/ends: $date"

    val lifetimeActive: String
        get() = if (lang == LanguageManager.AppLanguage.TURKISH) "Ömür boyu erişim" else "Lifetime access"

    fun paywallFinePrint(trial: Boolean): String {
        if (lang == LanguageManager.AppLanguage.TURKISH) {
            return (if (trial) "Ücretsiz deneme bitince seçtiğin plan otomatik başlar. " else "") +
                "Abonelik, dönem bitiminden en az 24 saat önce iptal edilmezse otomatik yenilenir. Ödeme Apple Kimliği hesabından alınır; iptal için Ayarlar > Apple Kimliği > Abonelikler."
        }
        return (if (trial) "Your plan starts automatically when the free trial ends. " else "") +
            "Subscriptions renew automatically unless cancelled at least 24 hours before the period ends. Payment is charged to your Apple ID; manage in Settings > Apple ID > Subscriptions."
    }
}
