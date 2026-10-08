import json
import os

RESOURCES_DIR = os.path.abspath(os.path.join(os.path.dirname(__file__), "../Nihongo_app/Nihongo_app/Resources"))

# --- N3 DATA ---
n3_kanji = [
    {"character": "政", "onyomi": "セイ、ショウ", "kunyomi": "まつりごと", "meaning": "siyaset, hükümet", "strokeCount": 9, "exampleWords": [{"hiragana": "政治", "romaji": "seiji", "turkishMeaning": "siyaset"}]},
    {"character": "経", "onyomi": "ケイ、キョウ", "kunyomi": "へる", "meaning": "geçmek, yönetmek", "strokeCount": 11, "exampleWords": [{"hiragana": "経済", "romaji": "keizai", "turkishMeaning": "ekonomi"}, {"hiragana": "経験", "romaji": "keiken", "turkishMeaning": "deneyim"}]},
    {"character": "済", "onyomi": "サイ、ザイ", "kunyomi": "すむ", "meaning": "tamamlanmak, kurtarmak", "strokeCount": 11, "exampleWords": [{"hiragana": "経済", "romaji": "keizai", "turkishMeaning": "ekonomi"}]},
    {"character": "歴", "onyomi": "レキ", "kunyomi": "", "meaning": "tarih, geçmiş", "strokeCount": 14, "exampleWords": [{"hiragana": "歴史", "romaji": "rekishi", "turkishMeaning": "tarih"}]},
    {"character": "史", "onyomi": "シ", "kunyomi": "", "meaning": "tarih, kronik", "strokeCount": 5, "exampleWords": [{"hiragana": "歴史", "romaji": "rekishi", "turkishMeaning": "tarih"}]},
    {"character": "育", "onyomi": "イク", "kunyomi": "そだつ、そだてる", "meaning": "büyümek, yetiştirmek", "strokeCount": 8, "exampleWords": [{"hiragana": "教育", "romaji": "kyouiku", "turkishMeaning": "eğitim"}]},
    {"character": "化", "onyomi": "カ、ケ", "kunyomi": "ばける", "meaning": "dönüşmek, kimya", "strokeCount": 4, "exampleWords": [{"hiragana": "文化", "romaji": "bunka", "turkishMeaning": "kültür"}, {"hiragana": "化学", "romaji": "kagaku", "turkishMeaning": "kimya"}]},
    {"character": "科", "onyomi": "カ", "kunyomi": "", "meaning": "bölüm, ders, bilim", "strokeCount": 9, "exampleWords": [{"hiragana": "科学", "romaji": "kagaku", "turkishMeaning": "bilim"}, {"hiragana": "教科書", "romaji": "kyoukasho", "turkishMeaning": "ders kitabı"}]},
    {"character": "理", "onyomi": "リ", "kunyomi": "ことわり", "meaning": "mantık, akıl, düzen", "strokeCount": 11, "exampleWords": [{"hiragana": "理由", "romaji": "riyuu", "turkishMeaning": "neden, sebep"}, {"hiragana": "料理", "romaji": "ryouri", "turkishMeaning": "yemek pişirme"}]},
    {"character": "由", "onyomi": "ユ、ユウ", "kunyomi": "よし", "meaning": "neden, köken", "strokeCount": 5, "exampleWords": [{"hiragana": "理由", "romaji": "riyuu", "turkishMeaning": "sebep"}, {"hiragana": "自由", "romaji": "jiyuu", "turkishMeaning": "özgürlük"}]}
]

n3_vocab = [
    {"kanji": "愛情", "hiragana": "あいじょう", "romaji": "aijou", "turkishMeaning": "sevgi, şefkat", "exampleSentence": "子供に深い愛情を注ぐ。", "exampleTranslation": "Çocuğa derin bir sevgi gösterir."},
    {"kanji": "挨拶", "hiragana": "あいさつ", "romaji": "aisatsu", "turkishMeaning": "selamlaşma", "exampleSentence": "毎朝笑顔で挨拶します。", "exampleTranslation": "Her sabah gülümseyerek selam veririm."},
    {"kanji": "相手", "hiragana": "あいて", "romaji": "aite", "turkishMeaning": "muhatap, ortak, rakip", "exampleSentence": "話の相手をする。", "exampleTranslation": "Sohbetine eşlik etmek."},
    {"kanji": "曖昧", "hiragana": "あいまい", "romaji": "aimai", "turkishMeaning": "belirsiz, muğlak", "exampleSentence": "曖昧な返事は避けてください。", "exampleTranslation": "Belirsiz yanıtlardan kaçının."},
    {"kanji": "扇ぐ", "hiragana": "あおぐ", "romaji": "aogu", "turkishMeaning": "yelpazelenmek", "exampleSentence": "うちわで風を扇ぐ。", "exampleTranslation": "Yelpazeyle rüzgar yapmak."}
]

n3_syllabus = [
    {
        "id": 1,
        "title": "Nüans ve Mantıksal Açıklama Kalıpları",
        "description": "N3'ün temel yapı taşları: olayların neden öyle olduğunu ve doğal sonuçları anlatan kalıplar.",
        "grammarKeys": ["wake_da", "wake_ga_nai", "koto_ni_natte_iru"]
    }
]

n3_grammar = [
    {
        "key": "wake_da",
        "pattern": "～わけだ",
        "romaji": "~wake da",
        "title": "Doğal Sonuç (~bu yüzden / demek ki öyle)",
        "explanation": "Öğrenilen yeni bir bilgiyle durumun nedeninin anlaşıldığını belirtir.",
        "formula": "[Düz Form] + わけだ",
        "category": "expression",
        "difficulty": 1,
        "examples": [
            {"japanese": "暑いわけだ。気温が35度もある。", "hiragana": "あついわけだ。きおんがさんじゅうごどもある。", "romaji": "Atsui wake da. Kion ga 35-do mo aru.", "turkish": "Sıcak olması çok normal. Hava sıcaklığı tam 35 derece."}
        ],
        "questions": [
            {
                "kind": "fillBlank",
                "prompt": "彼が日本に5年住んでいたなら、日本語が上手な＿＿だ。",
                "hint": "Doğal mantıksal sonuç kalıbı",
                "promptReading": None,
                "choices": ["わけ", "はず", "もの", "こと"],
                "answer": "わけ"
            }
        ]
    },
    {
        "key": "wake_ga_nai",
        "pattern": "～わけがない",
        "romaji": "~wake ga nai",
        "title": "İmkansızlık Bildirme (~olması imkansız)",
        "explanation": "Mantıken bir durumun gerçekleşmesinin kesinlikle imkansız olduğunu vurgular.",
        "formula": "[Düz Form] + わけがない",
        "category": "expression",
        "difficulty": 2,
        "examples": [
            {"japanese": "そんな嘘を信じるわけがない。", "hiragana": "そんなうそをしんじるわけがない。", "romaji": "Sonna uso wo shinjiru wake ga nai.", "turkish": "Böyle bir yalana inanması imkansız."}
        ],
        "questions": [
            {
                "kind": "multipleChoice",
                "prompt": "'İmkansızlık / Asla öyle olamaz' anlamı veren kalıp hangisidir?",
                "hint": "İmkansızlık kalıbı",
                "promptReading": None,
                "choices": ["～わけがない", "～わけだ", "～かもしれない", "～にちがいない"],
                "answer": "～わけがない"
            }
        ]
    },
    {
        "key": "koto_ni_natte_iru",
        "pattern": "～ことになっている",
        "romaji": "~koto ni natte iru",
        "title": "Kural ve Kararlaştırılmış Durum (~ması kararlaştırıldı)",
        "explanation": "Bir kural veya önceden alınmış karara göre bir şeyin yapılmasının beklendiğini belirtir.",
        "formula": "[Fiil Sözlük Formu] + ことになっている",
        "category": "expression",
        "difficulty": 2,
        "examples": [
            {"japanese": "法律でシートベルトを締めることになっている。", "hiragana": "ほうりつでシートベルトをしめることになっている。", "romaji": "Houritsu de shiito beruto wo shimeru koto ni natte iru.", "turkish": "Kanunen emniyet kemeri takılması gerekiyor."}
        ],
        "questions": [
            {
                "kind": "fillBlank",
                "prompt": "校則で髪を染めてはいけない＿＿になっている。",
                "hint": "Kural bildiren yapı",
                "promptReading": None,
                "choices": ["こと", "もの", "わけ", "よう"],
                "answer": "こと"
            }
        ]
    }
]

n3_stories = [
    {
        "id": "n3_story_1",
        "type": "all",
        "title": "地球温暖化と私たちの未来 (Küresel Isınma ve Geleceğimiz)",
        "sentences": [
            {
                "id": "n3_s1_1",
                "japaneseText": "最近、世界中で異常気象が増加しています。",
                "romaji": "Saikin, sekaijuu de ijoukishou ga zouka shite imasu.",
                "turkishTranslation": "Son zamanlarda tüm dünyada anormal hava olayları artmaktadır.",
                "vocabulary": ["異常気象 (Anormal hava olayları)", "増加 (Artış)"]
            },
            {
                "id": "n3_s1_2",
                "japaneseText": "一人ひとりが環境問題に関心を持つことが重要です。",
                "romaji": "Hitori hitori ga kankyou mondai ni kanshin wo motsu koto ga juuyou desu.",
                "turkishTranslation": "Her bireyin çevre sorunlarına ilgi duyması önemlidir.",
                "vocabulary": ["環境問題 (Çevre sorunları)", "重要 (Önemli)"]
            }
        ]
    }
]

# --- N2 DATA ---
n2_kanji = [
    {"character": "党", "onyomi": "トウ", "kunyomi": "なかま", "meaning": "siyasi parti, hizip", "strokeCount": 10, "exampleWords": [{"hiragana": "政党", "romaji": "seitou", "turkishMeaning": "siyasi parti"}]},
    {"character": "協", "onyomi": "キョウ", "kunyomi": "", "meaning": "işbirliği", "strokeCount": 8, "exampleWords": [{"hiragana": "協力", "romaji": "kyouryoku", "turkishMeaning": "işbirliği"}]},
    {"character": "総", "onyomi": "ソウ", "kunyomi": "すべて", "meaning": "genel, toplam", "strokeCount": 14, "exampleWords": [{"hiragana": "総理大臣", "romaji": "souridaijin", "turkishMeaning": "başbakan"}]}
]

n2_vocab = [
    {"kanji": "把握", "hiragana": "はあく", "romaji": "haaku", "turkishMeaning": "kavramak, tam anlamak", "exampleSentence": "現状を正確に把握する。", "exampleTranslation": "Mevcut durumu doğru bir şekilde kavramak."},
    {"kanji": "反映", "hiragana": "はんえい", "romaji": "han'ei", "turkishMeaning": "yansıtmak (fikir, durum)", "exampleSentence": "市民の意見を政策に反映させる。", "exampleTranslation": "Vatandaşların görüşlerini politikalara yansıtmak."}
]

n2_syllabus = [
    {
        "id": 1,
        "title": "Resmi ve Yazılı Dil İfadeleri (N2)",
        "description": "Gazete, makale ve resmi yazışmalarda sık kullanılan edebi ve ileri yapılar.",
        "grammarKeys": ["ni_saishi", "wo_keiki_ni"]
    }
]

n2_grammar = [
    {
        "key": "ni_saishi",
        "pattern": "～に際して",
        "romaji": "~ni saishite",
        "title": "Önemli Bir Olay / Başlangıç Anında (~ırken)",
        "explanation": "Özel ve resmi bir başlangıç veya tören anında kullanılır.",
        "formula": "[İsim / Fiil Sözlük Formu] + に際して",
        "category": "conjunction",
        "difficulty": 2,
        "examples": [
            {"japanese": "新学期を迎えるに際して、一言申し上げます。", "hiragana": "しんがっきをむかえるにさいして、ひとこともうしあげます。", "romaji": "Shingakki wo mukaeru ni saishite, hitokoto moushiagemasu.", "turkish": "Yeni dönemi karşılarken birkaç söz söylemek isterim."}
        ],
        "questions": [
            {
                "kind": "multipleChoice",
                "prompt": "'Açılış vesilesiyle / anında' anlamına gelen resmi kalıp hangisidir?",
                "hint": "Resmi başlangıç kalıbı",
                "promptReading": None,
                "choices": ["～に際して", "～につれて", "～に伴って", "～にしたがって"],
                "answer": "～に際して"
            }
        ]
    },
    {
        "key": "wo_keiki_ni",
        "pattern": "～を契機に",
        "romaji": "~wo keiki ni",
        "title": "Dönüm Noktası Olarak (~fırsat bilerek / vesilesiyle)",
        "explanation": "Büyük bir değişimin tetikleyicisi olan olayı anlatır.",
        "formula": "[İsim] + を契機に",
        "category": "conjunction",
        "difficulty": 2,
        "examples": [
            {"japanese": "留学を契機に、世界観が大きく変わった。", "hiragana": "りゅうがくをけいきに、せかいかんがおおきくかわった。", "romaji": "Ryuugaku wo keiki ni, sekaikan ga ookiku kawatta.", "turkish": "Yurtdışı eğitimini dönüm noktası alarak dünya görüşüm büyük ölçüde değişti."}
        ],
        "questions": [
            {
                "kind": "fillBlank",
                "prompt": "事件を＿＿に、法律が見直された。",
                "hint": "Dönüm noktası kalıbı",
                "promptReading": None,
                "choices": ["契機", "際", "ため", "中心"],
                "answer": "契機"
            }
        ]
    }
]

n2_stories = [
    {
        "id": "n2_story_1",
        "type": "all",
        "title": "人工知能の進化と倫理的課題 (Yapay Zekanın Evrimi ve Etik Meseleler)",
        "sentences": [
            {
                "id": "n2_s1_1",
                "japaneseText": "AI技術の急速な発展は、利便性をもたらす一方で新たな倫理的課題を提起している。",
                "romaji": "AI gijutsu no kyuusoku na hatten wa, ribensei wo motarasu ippou de arata na rinriteki kadai wo teiki shite iru.",
                "turkishTranslation": "Yapay zeka teknolojisinin hızlı gelişimi kolaylık sağlarken öte yandan yeni etik meseleler ortaya koymaktadır.",
                "vocabulary": ["急速 (Hızlı)", "倫理的課題 (Etik meseleler)"]
            }
        ]
    }
]

# --- N1 DATA ---
n1_kanji = [
    {"character": "憂", "onyomi": "ユウ", "kunyomi": "うれえる、うれい", "meaning": "keder, kaygı", "strokeCount": 15, "exampleWords": [{"hiragana": "憂慮", "romaji": "yuuryo", "turkishMeaning": "derin endişe"}]},
    {"character": "鬱", "onyomi": "ウツ", "kunyomi": "ふさぐ", "meaning": "melankoli, kasvet", "strokeCount": 29, "exampleWords": [{"hiragana": "憂鬱", "romaji": "yuuutsu", "turkishMeaning": "melankoli, bunalım"}]}
]

n1_vocab = [
    {"kanji": "圧倒", "hiragana": "あっとう", "romaji": "attou", "turkishMeaning": "ezici üstünlük, büyüleme", "exampleSentence": "雄大な自然に圧倒される。", "exampleTranslation": "Görkemli doğa karşısında büyülenmek."},
    {"kanji": "糾弾", "hiragana": "きゅうだん", "romaji": "kyuudan", "turkishMeaning": "sertçe suçlama, kınama", "exampleSentence": "不正行為を厳しく糾弾する。", "exampleTranslation": "Yolsuzluğu sertçe kınamak."}
]

n1_syllabus = [
    {
        "id": 1,
        "title": "Edebi ve Arkaik Nüanslar (N1)",
        "description": "En üst düzey Japonca edebi ve resmi anlatım biçimleri.",
        "grammarKeys": ["ya_ina_ya", "wo_kawaikiri_ni"]
    }
]

n1_grammar = [
    {
        "key": "ya_ina_ya",
        "pattern": "～や否や",
        "romaji": "~ya ina ya",
        "title": "Yapar Yapmaz (~anında)",
        "explanation": "Bir eylem biter bitmez hemen bir sonrakinin gerçekleştiğini edebi dille anlatır.",
        "formula": "[Fiil Sözlük Formu] + や否や",
        "category": "conjunction",
        "difficulty": 3,
        "examples": [
            {"japanese": "ベルが鳴るや否や、生徒たちは教室を飛び出した。", "hiragana": "ベルがなるやいなや、せいとたちはきょうしつをとびだした。", "romaji": "Beru ga naru ya ina ya, seitotachi wa kyoushitsu wo tobidashita.", "turkish": "Zil çalar çalmaz öğrenciler sınıftan dışarı fırladı."}
        ],
        "questions": [
            {
                "kind": "multipleChoice",
                "prompt": "'Yapar yapmaz anında' anlamına gelen edebi N1 yapısı hangisidir?",
                "hint": "ya ina ya",
                "promptReading": None,
                "choices": ["～や否や", "～なりに", "～まじき", "～がてら"],
                "answer": "～や否や"
            }
        ]
    },
    {
        "key": "wo_kawaikiri_ni",
        "pattern": "～を皮切りに",
        "romaji": "~wo kawaikiri ni",
        "title": "İlk Adım Olarak (~başlangıç alarak ardı ardına)",
        "explanation": "Bir dizi benzer olayın ilki olarak başlamasını ifade eder.",
        "formula": "[İsim] + を皮切りに(して)",
        "category": "conjunction",
        "difficulty": 3,
        "examples": [
            {"japanese": "東京公演を皮切りに、全国ツアーが始まる。", "hiragana": "とうきょうこうえんをかわきりに、ぜんこくツアーがはじまる。", "romaji": "Toukyou kouen wo kawaikiri ni, zenkoku tsuaa ga hajimaru.", "turkish": "Tokyo gösterisi başlangıç olmak üzere ulusal turne başlıyor."}
        ],
        "questions": [
            {
                "kind": "fillBlank",
                "prompt": "会長の挨拶を＿＿に、式典が厳かに執り行われた。",
                "hint": "Başlangıç adımı kalıbı",
                "promptReading": None,
                "choices": ["皮切り", "きっかけ", "手始め", "最初"],
                "answer": "皮切り"
            }
        ]
    }
]

n1_stories = [
    {
        "id": "n1_story_1",
        "type": "all",
        "title": "古典文学に見る自然観の変遷 (Klasik Edebiyatta Doğa Anlayışının Dönüşümü)",
        "sentences": [
            {
                "id": "n1_s1_1",
                "japaneseText": "日本の古典文学において、自然は単なる背景にとどまらず、人間の心情を映し出す鏡としての役割を果たしてきた。",
                "romaji": "Nihon no koten bungaku ni oite, shizen wa tannaru haikei ni todomarazu, ningen no shinjou wo utsushidasu kagami to shite no yakuwari wo hatashite kita.",
                "turkishTranslation": "Japon klasik edebiyatında doğa yalnızca bir arka plan olarak kalmamış, insanın iç dünyasını yansıtan bir ayna işlevi görmüştür.",
                "vocabulary": ["古典文学 (Klasik edebiyat)", "心情 (Duygusal durum, iç dünya)"]
            }
        ]
    }
]

def save_json(filename, data):
    path = os.path.join(RESOURCES_DIR, filename)
    with open(path, "w", encoding="utf-8") as f:
        json.dump(data, f, ensure_ascii=False, indent=2)
    print(f"Saved: {path} ({len(data)} items)")

if __name__ == "__main__":
    save_json("N3KanjiData.json", n3_kanji)
    save_json("N3VocabularyData.json", n3_vocab)
    save_json("N3GrammarSyllabus.json", n3_syllabus)
    save_json("N3GrammarData.json", n3_grammar)
    save_json("N3StoriesData.json", n3_stories)

    save_json("N2KanjiData.json", n2_kanji)
    save_json("N2VocabularyData.json", n2_vocab)
    save_json("N2GrammarSyllabus.json", n2_syllabus)
    save_json("N2GrammarData.json", n2_grammar)
    save_json("N2StoriesData.json", n2_stories)

    save_json("N1KanjiData.json", n1_kanji)
    save_json("N1VocabularyData.json", n1_vocab)
    save_json("N1GrammarSyllabus.json", n1_syllabus)
    save_json("N1GrammarData.json", n1_grammar)
    save_json("N1StoriesData.json", n1_stories)

    print("N3, N2, N1 datasets successfully created!")
