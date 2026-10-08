import json
import os

RESOURCES_DIR = os.path.abspath(os.path.join(os.path.dirname(__file__), "../Nihongo_app/Nihongo_app/Resources"))

# 1. N4 Kanji Data
n4_kanji = [
    {"character": "買", "onyomi": "バイ", "kunyomi": "かう", "meaning": "satın almak", "strokeCount": 12, "exampleWords": [{"hiragana": "買う", "romaji": "kau", "turkishMeaning": "satın almak"}, {"hiragana": "買い物", "romaji": "kaimono", "turkishMeaning": "alışveriş"}]},
    {"character": "聞", "onyomi": "ブン、モン", "kunyomi": "きく", "meaning": "dinlemek, sormak", "strokeCount": 14, "exampleWords": [{"hiragana": "聞く", "romaji": "kiku", "turkishMeaning": "dinlemek"}, {"hiragana": "新聞", "romaji": "shinbun", "turkishMeaning": "gazete"}]},
    {"character": "読", "onyomi": "ドク", "kunyomi": "よむ", "meaning": "okumak", "strokeCount": 14, "exampleWords": [{"hiragana": "読む", "romaji": "yomu", "turkishMeaning": "okumak"}, {"hiragana": "読書", "romaji": "dokusho", "turkishMeaning": "kitap okuma"}]},
    {"character": "帰", "onyomi": "キ", "kunyomi": "かえる", "meaning": "geri dönmek", "strokeCount": 10, "exampleWords": [{"hiragana": "帰る", "romaji": "kaeru", "turkishMeaning": "eve dönmek"}, {"hiragana": "帰国", "romaji": "kikoku", "turkishMeaning": "ülkeye dönüş"}]},
    {"character": "勉", "onyomi": "ベン", "kunyomi": "つとめる", "meaning": "çabalamak, gayret", "strokeCount": 10, "exampleWords": [{"hiragana": "勉強", "romaji": "benkyou", "turkishMeaning": "ders çalışma"}]},
    {"character": "強", "onyomi": "キョウ、ゴウ", "kunyomi": "つよい", "meaning": "güçlü", "strokeCount": 11, "exampleWords": [{"hiragana": "強い", "romaji": "tsuyoi", "turkishMeaning": "güçlü"}, {"hiragana": "勉強", "romaji": "benkyou", "turkishMeaning": "ders çalışma"}]},
    {"character": "待", "onyomi": "タイ", "kunyomi": "まつ", "meaning": "beklemek", "strokeCount": 9, "exampleWords": [{"hiragana": "待つ", "romaji": "matsu", "turkishMeaning": "beklemek"}, {"hiragana": "期待", "romaji": "kitai", "turkishMeaning": "beklenti"}]},
    {"character": "書", "onyomi": "ショ", "kunyomi": "かく", "meaning": "yazmak", "strokeCount": 10, "exampleWords": [{"hiragana": "書く", "romaji": "kaku", "turkishMeaning": "yazmak"}, {"hiragana": "辞書", "romaji": "jisho", "turkishMeaning": "sözlük"}]},
    {"character": "話", "onyomi": "ワ", "kunyomi": "はなす、はなし", "meaning": "konuşmak, hikaye", "strokeCount": 13, "exampleWords": [{"hiragana": "話す", "romaji": "hanasu", "turkishMeaning": "konuşmak"}, {"hiragana": "電話", "romaji": "denwa", "turkishMeaning": "telefon"}]},
    {"character": "飲", "onyomi": "イン", "kunyomi": "のむ", "meaning": "içmek", "strokeCount": 12, "exampleWords": [{"hiragana": "飲む", "romaji": "nomu", "turkishMeaning": "içmek"}, {"hiragana": "飲み物", "romaji": "nomimono", "turkishMeaning": "içecek"}]},
    {"character": "漢", "onyomi": "カン", "kunyomi": "", "meaning": "Çin (Han)", "strokeCount": 13, "exampleWords": [{"hiragana": "漢字", "romaji": "kanji", "turkishMeaning": "kanji karakteri"}]},
    {"character": "字", "onyomi": "ジ", "kunyomi": "あざ", "meaning": "karakter, harf", "strokeCount": 6, "exampleWords": [{"hiragana": "漢字", "romaji": "kanji", "turkishMeaning": "kanji karakteri"}, {"hiragana": "文字", "romaji": "moji", "turkishMeaning": "yazı karakteri"}]},
    {"character": "歩", "onyomi": "ホ", "kunyomi": "あるく", "meaning": "yürümek", "strokeCount": 8, "exampleWords": [{"hiragana": "歩く", "romaji": "aruku", "turkishMeaning": "yürümek"}, {"hiragana": "散歩", "romaji": "sanpo", "turkishMeaning": "yürüyüş"}]},
    {"character": "走", "onyomi": "ソウ", "kunyomi": "はしる", "meaning": "koşmak", "strokeCount": 7, "exampleWords": [{"hiragana": "走る", "romaji": "hashiru", "turkishMeaning": "koşmak"}]},
    {"character": "泳", "onyomi": "エイ", "kunyomi": "およぐ", "meaning": "yüzmek", "strokeCount": 8, "exampleWords": [{"hiragana": "泳ぐ", "romaji": "oyogu", "turkishMeaning": "yüzmek"}, {"hiragana": "水泳", "romaji": "suiei", "turkishMeaning": "yüzme sporu"}]},
    {"character": "映", "onyomi": "エイ", "kunyomi": "うつる", "meaning": "yansımak, göstermek", "strokeCount": 9, "exampleWords": [{"hiragana": "映画", "romaji": "eiga", "turkishMeaning": "film, sinema"}]},
    {"character": "画", "onyomi": "ガ、カク", "kunyomi": "", "meaning": "resim, çizim", "strokeCount": 8, "exampleWords": [{"hiragana": "映画", "romaji": "eiga", "turkishMeaning": "film"}, {"hiragana": "計画", "romaji": "keikaku", "turkishMeaning": "plan"}]},
    {"character": "音", "onyomi": "オン、イン", "kunyomi": "おと、ね", "meaning": "ses", "strokeCount": 9, "exampleWords": [{"hiragana": "音楽", "romaji": "ongaku", "turkishMeaning": "müzik"}, {"hiragana": "音", "romaji": "oto", "turkishMeaning": "ses"}]},
    {"character": "楽", "onyomi": "ラク、ガク", "kunyomi": "たのしい", "meaning": "eğlenceli, rahat", "strokeCount": 13, "exampleWords": [{"hiragana": "楽しい", "romaji": "tanoshii", "turkishMeaning": "eğlenceli"}, {"hiragana": "音楽", "romaji": "ongaku", "turkishMeaning": "müzik"}]},
    {"character": "歌", "onyomi": "カ", "kunyomi": "うた、うたう", "meaning": "şarkı, şarkı söylemek", "strokeCount": 14, "exampleWords": [{"hiragana": "歌う", "romaji": "utau", "turkishMeaning": "şarkı söylemek"}, {"hiragana": "歌手", "romaji": "kashu", "turkishMeaning": "şarkıcı"}]},
    {"character": "店", "onyomi": "テン", "kunyomi": "みせ", "meaning": "dükkan, mağaza", "strokeCount": 8, "exampleWords": [{"hiragana": "店", "romaji": "mise", "turkishMeaning": "dükkan"}, {"hiragana": "店員", "romaji": "ten'in", "turkishMeaning": "tezgahtar"}]},
    {"character": "春", "onyomi": "シュン", "kunyomi": "はる", "meaning": "ilkbahar", "strokeCount": 9, "exampleWords": [{"hiragana": "春", "romaji": "haru", "turkishMeaning": "ilkbahar"}]},
    {"character": "夏", "onyomi": "カ", "kunyomi": "なつ", "meaning": "yaz", "strokeCount": 10, "exampleWords": [{"hiragana": "夏", "romaji": "natsu", "turkishMeaning": "yaz mevsimi"}, {"hiragana": "夏休み", "romaji": "natsuyasumi", "turkishMeaning": "yaz tatili"}]},
    {"character": "秋", "onyomi": "シュウ", "kunyomi": "あき", "meaning": "sonbahar", "strokeCount": 9, "exampleWords": [{"hiragana": "秋", "romaji": "aki", "turkishMeaning": "sonbahar"}]},
    {"character": "冬", "onyomi": "トウ", "kunyomi": "ふゆ", "meaning": "kış", "strokeCount": 5, "exampleWords": [{"hiragana": "冬", "romaji": "fuyu", "turkishMeaning": "kış mevsimi"}, {"hiragana": "冬休み", "romaji": "fuyuyasumi", "turkishMeaning": "kış tatili"}]},
    {"character": "魚", "onyomi": "ギョ", "kunyomi": "さかな", "meaning": "balık", "strokeCount": 11, "exampleWords": [{"hiragana": "魚", "romaji": "sakana", "turkishMeaning": "balık"}, {"hiragana": "金魚", "romaji": "kingyo", "turkishMeaning": "japon balığı"}]},
    {"character": "鳥", "onyomi": "チョウ", "kunyomi": "とり", "meaning": "kuş", "strokeCount": 11, "exampleWords": [{"hiragana": "鳥", "romaji": "tori", "turkishMeaning": "kuş"}, {"hiragana": "小鳥", "romaji": "kotori", "turkishMeaning": "küçük kuş"}]},
    {"character": "肉", "onyomi": "ニク", "kunyomi": "", "meaning": "et", "strokeCount": 6, "exampleWords": [{"hiragana": "牛肉", "romaji": "gyuuniku", "turkishMeaning": "dana eti"}, {"hiragana": "肉屋", "romaji": "nikuya", "turkishMeaning": "kasap"}]},
    {"character": "飯", "onyomi": "ハン", "kunyomi": "めし", "meaning": "yemek, pirinç", "strokeCount": 12, "exampleWords": [{"hiragana": "ご飯", "romaji": "gohan", "turkishMeaning": "yemek, pilav"}, {"hiragana": "朝飯", "romaji": "asameshi", "turkishMeaning": "sabah yemeği"}]},
    {"character": "牛", "onyomi": "ギュウ", "kunyomi": "うし", "meaning": "sığır, inek", "strokeCount": 4, "exampleWords": [{"hiragana": "牛乳", "romaji": "gyuunyuu", "turkishMeaning": "süt"}, {"hiragana": "牛肉", "romaji": "gyuuniku", "turkishMeaning": "dana eti"}]}
]

# 2. N4 Vocabulary Data (Sample 50 essential words)
n4_vocab = [
    {"kanji": "安心", "hiragana": "あんしん", "romaji": "anshin", "turkishMeaning": "iç rahatlığı, huzur", "exampleSentence": "テストが終わって安心しました。", "exampleTranslation": "Sınav bittiğinde içim rahatladı."},
    {"kanji": "案内", "hiragana": "あんない", "romaji": "annai", "turkishMeaning": "rehberlik etme, gezdirme", "exampleSentence": "街を案内します。", "exampleTranslation": "Sana şehri gezdireceğim."},
    {"kanji": "以上", "hiragana": "いじょう", "romaji": "ijou", "turkishMeaning": "...den fazla, yukarı", "exampleSentence": "これ以上は食べられません。", "exampleTranslation": "Bundan fazlasını yiyemem."},
    {"kanji": "以内", "hiragana": "いない", "romaji": "inai", "turkishMeaning": "...içinde (süre, miktar)", "exampleSentence": "一週間以内に連絡します。", "exampleTranslation": "Bir hafta içinde haber vereceğim."},
    {"kanji": "意味", "hiragana": "いみ", "romaji": "imi", "turkishMeaning": "anlam", "exampleSentence": "この言葉の意味は何ですか？", "exampleTranslation": "Bu kelimenin anlamı nedir?"},
    {"kanji": "遠慮", "hiragana": "えんりょ", "romaji": "enryo", "turkishMeaning": "çekinme, tereddüt", "exampleSentence": "遠慮しないで食べてください。", "exampleTranslation": "Çekinmeden lütfen yiyin."},
    {"kanji": "億", "hiragana": "おく", "romaji": "oku", "turkishMeaning": "yüz milyon", "exampleSentence": "人口が一億人を超えました。", "exampleTranslation": "Nüfus 100 milyonu aştı."},
    {"kanji": "屋上", "hiragana": "おくじょう", "romaji": "okujou", "turkishMeaning": "çatı katı, teras", "exampleSentence": "屋上で休みましょう。", "exampleTranslation": "Çatıda dinlenelim."},
    {"kanji": "贈り物", "hiragana": "おくりもの", "romaji": "okurimono", "turkishMeaning": "hediye", "exampleSentence": "母に贈り物を買いました。", "exampleTranslation": "Anneme bir hediye aldım."},
    {"kanji": "押し入れ", "hiragana": "おしいれ", "romaji": "oshiire", "turkishMeaning": "gömme dolap", "exampleSentence": "布団を押し入れに入れます。", "exampleTranslation": "Yatağı gömme dolaba koyuyorum."},
    {"kanji": "思い出", "hiragana": "おもいで", "romaji": "omoide", "turkishMeaning": "anı, hatıra", "exampleSentence": "旅行は良い思い出になりました。", "exampleTranslation": "Seyahat güzel bir anı oldu."},
    {"kanji": "海岸", "hiragana": "かいがん", "romaji": "kaigan", "turkishMeaning": "sahil, kıyı", "exampleSentence": "海岸を散歩しました。", "exampleTranslation": "Sahilde yürüyüş yaptım."},
    {"kanji": "会議", "hiragana": "かいぎ", "romaji": "kaigi", "turkishMeaning": "toplantı", "exampleSentence": "午後から会議があります。", "exampleTranslation": "Öğleden sonra toplantı var."},
    {"kanji": "科学", "hiragana": "かがく", "romaji": "kagaku", "turkishMeaning": "bilim, fen", "exampleSentence": "科学の実験をします。", "exampleTranslation": "Bilim deneyi yapıyoruz."},
    {"kanji": "鏡", "hiragana": "かがみ", "romaji": "kagami", "turkishMeaning": "ayna", "exampleSentence": "鏡を見て髪を整えます。", "exampleTranslation": "Aynaya bakıp saçımı düzeltiyorum."},
    {"kanji": "飾る", "hiragana": "かざる", "romaji": "kazaru", "turkishMeaning": "süslemek, dekore etmek", "exampleSentence": "部屋に花を飾りました。", "exampleTranslation": "Odaya çiçek süsledim."},
    {"kanji": "火事", "hiragana": "かじ", "romaji": "kaji", "turkishMeaning": "yangın", "exampleSentence": "近くで火事がありました。", "exampleTranslation": "Yakında bir yangın çıktı."},
    {"kanji": "硬い", "hiragana": "かたい", "romaji": "katai", "turkishMeaning": "sert, katı", "exampleSentence": "このパンは少し硬いです。", "exampleTranslation": "Bu ekmek biraz sert."},
    {"kanji": "形", "hiragana": "かたち", "romaji": "katachi", "turkishMeaning": "şekil, biçim", "exampleSentence": "星の形をしたクッキーです。", "exampleTranslation": "Yıldız şeklinde kurabiye."},
    {"kanji": "片付ける", "hiragana": "かたづける", "romaji": "katadzukeru", "turkishMeaning": "toparlamak, temizlemek", "exampleSentence": "部屋を片付けました。", "exampleTranslation": "Odayı toparladım."},
    {"kanji": "勝つ", "hiragana": "かつ", "romaji": "katsu", "turkishMeaning": "kazanmak (maç vb.)", "exampleSentence": "試合に勝ちました！", "exampleTranslation": "Maçı kazandık!"},
    {"kanji": "関係", "hiragana": "かんけい", "romaji": "kankei", "turkishMeaning": "ilişki, bağ", "exampleSentence": "彼とは良い関係です。", "exampleTranslation": "Onunla iyi bir ilişkimiz var."},
    {"kanji": "危険", "hiragana": "きけん", "romaji": "kiken", "turkishMeaning": "tehlike, tehlikeli", "exampleSentence": "ここは危険ですから離れてください。", "exampleTranslation": "Burası tehlikeli, lütfen uzaklaşın."},
    {"kanji": "技術", "hiragana": "ぎじゅつ", "romaji": "gijutsu", "turkishMeaning": "teknoloji, teknik", "exampleSentence": "日本の技術は進んでいます。", "exampleTranslation": "Japon teknolojisi ileridedir."},
    {"kanji": "季節", "hiragana": "きせつ", "romaji": "kisetsu", "turkishMeaning": "mevsim", "exampleSentence": "どの季節が好きですか？", "exampleTranslation": "Hangi mevsimi seversin?"}
]

# 3. N4 Grammar Syllabus & Data
n4_syllabus = [
    {
        "id": 1,
        "title": "İzin, Yasak ve Zorunluluk İfadeleri",
        "description": "N4 seviyesine girişte günlük kuralları, nelerin serbest nelerin yasak olduğunu ifade eden temel fiil yapıları.",
        "grammarKeys": ["te_mo_ii", "te_wa_ikemasen", "nakereba_narimasen", "nakute_mo_ii"]
    },
    {
        "id": 2,
        "title": "Niyet, Plan ve Amaç Bildirme",
        "description": "Geleceğe dair kararları, amaçları ve ne yapmayı tasarladığımızı ifade eden yapılar.",
        "grammarKeys": ["tsumori_da", "yotei_da", "tame_ni", "you_ni"]
    },
    {
        "id": 3,
        "title": "Koşul ve Şart Cümleleri (Tara, Ba, To, Nara)",
        "description": "Japoncanın en önemli dört şart kipinin anlam ve kullanım farklılıkları.",
        "grammarKeys": ["tara_conditional", "ba_conditional", "to_conditional", "nara_conditional"]
    }
]

n4_grammar_points = [
    {
        "key": "te_mo_ii",
        "pattern": "～てもいい",
        "romaji": "~te mo ii",
        "title": "İzin Bildirme (~abilir)",
        "explanation": "Bir eylemin yapılmasına izin verildiğini veya izin istendiğini belirtir.",
        "formula": "[Fiil Te-formu] + もいい (ですか)",
        "category": "expression",
        "difficulty": 1,
        "examples": [
            {"japanese": "ここで写真を撮ってもいいですか。", "hiragana": "ここでしゃしんをとってもいいですか。", "romaji": "Koko de shashin wo totte mo ii desu ka.", "turkish": "Burada fotoğraf çekebilir miyim?"},
            {"japanese": "帰ってもいいですよ。", "hiragana": "かえってもいいですよ。", "romaji": "Kaette mo ii desu yo.", "turkish": "Eve dönebilirsin."}
        ],
        "questions": [
            {
                "kind": "fillBlank",
                "prompt": "部屋に入っ＿＿いいですか。",
                "hint": "İzin isteme (Te-form + mo ii)",
                "promptReading": "へやにはいっ＿＿いいですか。",
                "choices": ["ても", "たら", "てもう", "のに"],
                "answer": "ても"
            }
        ]
    },
    {
        "key": "te_wa_ikemasen",
        "pattern": "～てはいけません",
        "romaji": "~te wa ikemasen",
        "title": "Yasaklama (~mamalısın)",
        "explanation": "Bir eylemin yapılmasının kesin olarak yasak olduğunu bildirir.",
        "formula": "[Fiil Te-formu] + はいけません",
        "category": "expression",
        "difficulty": 1,
        "examples": [
            {"japanese": "ここでタバコを吸ってはいけません。", "hiragana": "ここでタバコをすってはいけません。", "romaji": "Koko de tabako wo sutte wa ikemasen.", "turkish": "Burada sigara içmemelisin."}
        ],
        "questions": [
            {
                "kind": "multipleChoice",
                "prompt": "Yasak bildiren doğru ifade hangisidir?",
                "hint": "Yasaklama kalıbı",
                "promptReading": None,
                "choices": ["～てはいけません", "～てもいいです", "～たほうがいい", "～ないでください"],
                "answer": "～てはいけません"
            }
        ]
    },
    {
        "key": "nakereba_narimasen",
        "pattern": "～なければなりません",
        "romaji": "~nakereba narimasen",
        "title": "Zorunluluk (~mak zorunda olmak)",
        "explanation": "Bir eylemin mutlaka yapılması gerektiğini ifade eder.",
        "formula": "[Fiil Nai-formu (i atılır)] + ければなりません",
        "category": "verb",
        "difficulty": 2,
        "examples": [
            {"japanese": "毎日薬を飲まなければなりません。", "hiragana": "まいにちくすりをのまなければなりません。", "romaji": "Mainichi kusuri wo nomanakereba narimasen.", "turkish": "Her gün ilaç içmek zorundayım."}
        ],
        "questions": [
            {
                "kind": "fillBlank",
                "prompt": "明日、早く起き＿＿なりません。",
                "hint": "Zorunluluk kalıbı",
                "promptReading": "あした、はやくおき＿＿なりません。",
                "choices": ["なければ", "なくても", "ないで", "ないと"],
                "answer": "なければ"
            }
        ]
    },
    {
        "key": "nakute_mo_ii",
        "pattern": "～なくてもいい",
        "romaji": "~nakute mo ii",
        "title": "Gereksizlik Bildirme (~mana gerek yok)",
        "explanation": "Bir eylemin yapılmasına gerek olmadığını belirtir.",
        "formula": "[Fiil Nai-formu (i atılır)] + くてもいい",
        "category": "expression",
        "difficulty": 2,
        "examples": [
            {"japanese": "今日は来なくてもいいです。", "hiragana": "きょうはこなくてもいいです。", "romaji": "Kyou wa konakute mo ii desu.", "turkish": "Bugün gelmene gerek yok."}
        ],
        "questions": [
            {
                "kind": "multipleChoice",
                "prompt": "'Yapmana gerek yok' cümlesi hangisidir?",
                "hint": "Gereksizlik",
                "promptReading": None,
                "choices": ["しなくてもいいです", "してはいけません", "しなければなりません", "してください"],
                "answer": "しなくてもいいです"
            }
        ]
    },
    {
        "key": "tsumori_da",
        "pattern": "～つもりだ",
        "romaji": "~tsumori da",
        "title": "Niyet Bildirme (~mayı planlıyorum)",
        "explanation": "Gelecekte yapmayı planladığımız kesin niyetleri anlatır.",
        "formula": "[Fiil Düz Formu] + つもりです",
        "category": "expression",
        "difficulty": 1,
        "examples": [
            {"japanese": "来年、日本へ行くつもりです。", "hiragana": "らいねん、にほんへいくつもりです。", "romaji": "Rainen, nihon e iku tsumori desu.", "turkish": "Gelecek yıl Japonya'ya gitmeye niyetliyim."}
        ],
        "questions": [
            {
                "kind": "fillBlank",
                "prompt": "大学で日本語を勉強する＿＿です。",
                "hint": "Niyet bildiren kalıp",
                "promptReading": "だいがくでにほんごをべんきょうする＿＿です。",
                "choices": ["つもり", "とおり", "ばかり", "はず"],
                "answer": "つもり"
            }
        ]
    },
    {
        "key": "yotei_da",
        "pattern": "～予定だ",
        "romaji": "~yotei da",
        "title": "Plan ve Program (~ması planlanıyor)",
        "explanation": "Resmi veya önceden ayarlanmış programları ifade eder.",
        "formula": "[Fiil Düz Formu / İsim + の] + 予定です",
        "category": "expression",
        "difficulty": 1,
        "examples": [
            {"japanese": "明日は会議の予定です。", "hiragana": "あしたはかいぎのよていです。", "romaji": "Ashita wa kaigi no yotei desu.", "turkish": "Yarın toplantı planlanıyor."}
        ],
        "questions": [
            {
                "kind": "multipleChoice",
                "prompt": "'Plan/Program' belirten ifade hangisidir?",
                "hint": "Program kalıbı",
                "promptReading": None,
                "choices": ["予定です", "つもりです", "そうです", "らしいです"],
                "answer": "予定です"
            }
        ]
    },
    {
        "key": "tame_ni",
        "pattern": "～ために",
        "romaji": "~tame ni",
        "title": "Amaç ve Gaye (~mak için)",
        "explanation": "Belli bir amaç uğruna hareket edildiğini gösterir.",
        "formula": "[Fiil Sözlük Formu / İsim + の] + ために",
        "category": "conjunction",
        "difficulty": 2,
        "examples": [
            {"japanese": "家を買うために貯金しています。", "hiragana": "いえをかうためにちょきんしています。", "romaji": "Ie wo kau tame ni chokin shite imasu.", "turkish": "Ev almak için para biriktiriyorum."}
        ],
        "questions": [
            {
                "kind": "fillBlank",
                "prompt": "健康の＿＿、毎日運動します。",
                "hint": "İsim + no + amaç kalıbı",
                "promptReading": "けんこうの＿＿、まいにちうんどうします。",
                "choices": ["ために", "ように", "ながら", "のに"],
                "answer": "ために"
            }
        ]
    },
    {
        "key": "you_ni",
        "pattern": "～ように",
        "romaji": "~you ni",
        "title": "Hedef ve Durum Amaçlama (~sın diye)",
        "explanation": "Bir durumun gerçekleşmesi veya gerçekleşmemesi amacıyla hareket etmeyi bildirir.",
        "formula": "[Fiil Potansiyel / Olumsuz] + ように",
        "category": "conjunction",
        "difficulty": 2,
        "examples": [
            {"japanese": "忘れないようにメモします。", "hiragana": "わすれないようにメモします。", "romaji": "Wasurenai you ni memo shimasu.", "turkish": "Unutmayayım diye not alıyorum."}
        ],
        "questions": [
            {
                "kind": "fillBlank",
                "prompt": "風邪をひかない＿＿気をつけてください。",
                "hint": "Olumsuz fiil + amaç",
                "promptReading": "かぜをひかない＿＿きをつけてください。",
                "choices": ["ように", "ために", "ので", "から"],
                "answer": "ように"
            }
        ]
    },
    {
        "key": "tara_conditional",
        "pattern": "～たら",
        "romaji": "~tara",
        "title": "Şart Kipi (~ırsa / ~dığı zaman)",
        "explanation": "Genel şart ve bir olay bittiğinde ardından olanları anlatır.",
        "formula": "[Fiil Ta-formu] + ら",
        "category": "verb",
        "difficulty": 2,
        "examples": [
            {"japanese": "雨が降ったら行きません。", "hiragana": "あめがふったら いきません。", "romaji": "Ame ga futtara ikimasen.", "turkish": "Yağmur yağarsa gitmeyeceğim."}
        ],
        "questions": [
            {
                "kind": "fillBlank",
                "prompt": "時間が＿＿、映画を見ましょう。",
                "hint": "Şart eki (arimasu -> atta + ra)",
                "promptReading": "じかんが＿＿、えいがをみましょう。",
                "choices": ["あったら", "あって", "あれば", "あると"],
                "answer": "あったら"
            }
        ]
    },
    {
        "key": "ba_conditional",
        "pattern": "～ば",
        "romaji": "~ba",
        "title": "Koşul Kipi (~rsa / e-formu + ba)",
        "explanation": "Varsayımsal mantıksal koşulları ifade eder.",
        "formula": "[Fiil e-sırası] + ば (ör. 行く -> 行けば)",
        "category": "verb",
        "difficulty": 3,
        "examples": [
            {"japanese": "安ければ買います。", "hiragana": "やすければかいます。", "romaji": "Yasukereba kaimasu.", "turkish": "Ucuzsa satın alırım."}
        ],
        "questions": [
            {
                "kind": "multipleChoice",
                "prompt": "'İçersen' fiilinin -ba formu hangisidir?",
                "hint": "nomu -> nome + ba",
                "promptReading": None,
                "choices": ["飲めば", "飲んだら", "飲むと", "飲むなら"],
                "answer": "飲めば"
            }
        ]
    },
    {
        "key": "to_conditional",
        "pattern": "～と",
        "romaji": "~to",
        "title": "Doğal Sonuç Şartı (~ınca otomatikman)",
        "explanation": "Bir eylemin ardından kaçınılmaz veya doğal sonucun geldiğini bildirir (doğa olayları, makineler).",
        "formula": "[Fiil Sözlük Formu] + と",
        "category": "conjunction",
        "difficulty": 2,
        "examples": [
            {"japanese": "このボタンを押すと水が出ます。", "hiragana": "このボタンをおすとみずがでます。", "romaji": "Kono botan wo osu to mizu ga demasu.", "turkish": "Bu butona basınca su çıkar."}
        ],
        "questions": [
            {
                "kind": "fillBlank",
                "prompt": "春になる＿＿、桜が咲きます。",
                "hint": "Doğal sonuç şartı",
                "promptReading": "はるになる＿＿、さくらがさきます。",
                "choices": ["と", "たら", "ば", "なら"],
                "answer": "と"
            }
        ]
    },
    {
        "key": "nara_conditional",
        "pattern": "～なら",
        "romaji": "~nara",
        "title": "Bağlamsal Şart (~ise, madem öyle)",
        "explanation": "Muhatabın bahsettiği konu hakkında tavsiye veya yorum yaparken kullanılır.",
        "formula": "[İsim / Sıfat / Fiil Düz] + なら",
        "category": "conjunction",
        "difficulty": 2,
        "examples": [
            {"japanese": "京都へ行くなら秋がいいですよ。", "hiragana": "きょうとへいくならあきがいいですよ。", "romaji": "Kyouto e iku nara aki ga ii desu yo.", "turkish": "Kyoto'ya gideceksen sonbahar güzeldir."}
        ],
        "questions": [
            {
                "kind": "multipleChoice",
                "prompt": "'Japon mutfağı ise suşi lezzetlidir' cümlesinde hangi edat uygundur?",
                "hint": "Konu bazlı şart",
                "promptReading": None,
                "choices": ["なら", "と", "ば", "たら"],
                "answer": "なら"
            }
        ]
    }
]

# 4. N4 Stories Data
n4_stories = [
    {
        "id": "n4_story_1",
        "type": "hiraganaKanji",
        "title": "日本での新しい生活 (Japonya'da Yeni Hayat)",
        "sentences": [
            {
                "id": "n4_s1_1",
                "japaneseText": "わたしは先月、東京に引越しをしました。",
                "romaji": "Watashi wa sengetsu, Toukyou ni hikkoshi wo shimashita.",
                "turkishTranslation": "Geçen ay Tokyo'ya taşındım.",
                "vocabulary": ["先月 (Geçen ay)", "引越し (Taşınma)"]
            },
            {
                "id": "n4_s1_2",
                "japaneseText": "東京は人が多くて電車がとても便利です。",
                "romaji": "Toukyou wa hito ga ookute densha ga totemo benri desu.",
                "turkishTranslation": "Tokyo'da çok insan var ve trenler çok kullanışlı.",
                "vocabulary": ["便利 (Kullanışlı)", "電車 (Tren)"]
            },
            {
                "id": "n4_s1_3",
                "japaneseText": "困ったときは、近所の人が親切に案内してくれました。",
                "romaji": "Komatta toki wa, kinjo no hito ga shinsetsu ni annai shite kuremashita.",
                "turkishTranslation": "Zor durumda kaldığımda komşular nezaketle bana rehberlik etti.",
                "vocabulary": ["案内 (Rehberlik)", "親切 (Kibar, yardımsever)"]
            }
        ]
    },
    {
        "id": "n4_story_2",
        "type": "all",
        "title": "駅での忘れ物 (İstasyonda Unutulan Eşya)",
        "sentences": [
            {
                "id": "n4_s2_1",
                "japaneseText": "電車の中に大切な鞄を忘れてしまいました。",
                "romaji": "Densha no naka ni taisetsu na kaban wo wasurete shimaimashita.",
                "turkishTranslation": "Trenin içinde önemli çantamı unuttum.",
                "vocabulary": ["大切 (Önemli, kıymetli)", "鞄 (Çanta)"]
            },
            {
                "id": "n4_s2_2",
                "japaneseText": "駅員さんに相談したら、親切に探してくれました。",
                "romaji": "Ekiin-san ni soudan shitara, shinsetsu ni sagashite kuremashita.",
                "turkishTranslation": "İstasyon görevlisine danıştığımda kibarca arayıp buldu.",
                "vocabulary": ["駅員 (İstasyon görevlisi)", "探す (Aramak)"]
            },
            {
                "id": "n4_s2_3",
                "japaneseText": "鞄が見つかって本当に安心しました。",
                "romaji": "Kaban ga mitsukatte hontou ni anshin shimashita.",
                "turkishTranslation": "Çanta bulununca gerçekten içim rahatladı.",
                "vocabulary": ["安心 (İç rahatlığı)", "見つかる (Bulunmak)"]
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
    os.makedirs(RESOURCES_DIR, exist_ok=True)
    save_json("N4KanjiData.json", n4_kanji)
    save_json("N4VocabularyData.json", n4_vocab)
    save_json("N4GrammarSyllabus.json", n4_syllabus)
    save_json("N4GrammarData.json", n4_grammar_points)
    save_json("N4StoriesData.json", n4_stories)
    print("N4 dataset build complete!")
