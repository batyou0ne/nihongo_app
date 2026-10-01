import json
import uuid

def gen_id():
    return str(uuid.uuid4())

def make_option(jap, rom, tur, is_corr):
    return {
        "id": gen_id(),
        "japanese": jap,
        "romaji": rom,
        "turkish": tur,
        "isCorrect": is_corr
    }

def make_step(bot_jap, bot_rom, bot_tur, options):
    return {
        "id": gen_id(),
        "botMessage": {
            "japanese": bot_jap,
            "romaji": bot_rom,
            "turkish": bot_tur
        },
        "options": options
    }

def make_scenario(s_id, title, desc, level, steps):
    return {
        "id": s_id,
        "title": title,
        "description": desc,
        "level": level,
        "steps": steps
    }

scenarios = []

# Scenario 1: Self Introduction
s1 = make_scenario("s1", "Tanışma", "Yeni biriyle karşılaştığınızda kendinizi tanıtın.", "N5", [
    make_step("はじめまして。わたしは田中[たなか]です。", "Hajimemashite. Watashi wa Tanaka desu.", "Memnun oldum. Ben Tanaka.", [
        make_option("はじめまして。よろしくお願[ねが]いします。", "Hajimemashite. Yoroshiku onegaishimasu.", "Memnun oldum. Lütfen bana iyi davranın.", True),
        make_option("さようなら。", "Sayounara.", "Hoşça kal.", False),
        make_option("ごめんなさい。", "Gomennasai.", "Özür dilerim.", False),
        make_option("ありがとう。", "Arigatou.", "Teşekkür ederim.", False)
    ]),
    make_step("お仕事[しごと]はなんですか？", "Oshigoto wa nan desu ka?", "Mesleğiniz nedir?", [
        make_option("わたしは学生[がくせい]です。", "Watashi wa gakusei desu.", "Ben öğrenciyim.", True),
        make_option("これはペンです。", "Kore wa pen desu.", "Bu bir kalemdir.", False),
        make_option("はい、そうです。", "Hai, sou desu.", "Evet, öyle.", False),
        make_option("おいしいです。", "Oishii desu.", "Lezzetlidir.", False)
    ]),
    make_step("そうですか。どこから来[き]ましたか？", "Sou desu ka. Doko kara kimashita ka?", "Öyle mi? Nereden geldiniz?", [
        make_option("トルコから来[き]ました。", "Toruko kara kimashita.", "Türkiye'den geldim.", True),
        make_option("学校[がっこう]へ行[い]きます。", "Gakkou e ikimasu.", "Okula gidiyorum.", False),
        make_option("明日[あした]来[き]ます。", "Ashita kimasu.", "Yarın geleceğim.", False),
        make_option("東京[とうきょう]にあります。", "Toukyou ni arimasu.", "Tokyo'da bulunuyor.", False)
    ]),
    make_step("トルコですね。いいですね。", "Toruko desu ne. Ii desu ne.", "Türkiye demek. Çok güzel.", [
        make_option("はい、とてもいい国[くに]です。", "Hai, totemo ii kuni desu.", "Evet, çok güzel bir ülkedir.", True),
        make_option("いいえ、ちがいます。", "Iie, chigaimasu.", "Hayır, yanılıyorsunuz.", False),
        make_option("まだ食[た]べていません。", "Mada tabete imasen.", "Henüz yemedim.", False),
        make_option("わかりません。", "Wakarimasen.", "Bilmiyorum.", False)
    ]),
    make_step("これからもよろしくお願[ねが]いします。", "Korekara mo yoroshiku onegaishimasu.", "Bundan sonra da görüşmek üzere (memnun oldum).", [
        make_option("こちらこそ、よろしくお願[ねが]いします。", "Kochirakoso, yoroshiku onegaishimasu.", "Asıl ben memnun oldum.", True),
        make_option("どういたしまして。", "Douitashimashite.", "Rica ederim.", False),
        make_option("すみません。", "Sumimasen.", "Afedersiniz.", False),
        make_option("いただきます。", "Itadakimasu.", "Afiyet olsun.", False)
    ])
])
scenarios.append(s1)

# Scenario 2: Restaurant
s2 = make_scenario("s2", "Restoranda Sipariş", "Bir restoranda yemek siparişi verin.", "N5", [
    make_step("いらっしゃいませ。何名様[なんめいさま]ですか？", "Irasshaimase. Nanmei-sama desu ka?", "Hoş geldiniz. Kaç kişisiniz?", [
        make_option("二人[ふたり]です。", "Futari desu.", "İki kişiyiz.", True),
        make_option("二[ふた]つです。", "Futatsu desu.", "İki tane.", False),
        make_option("二時[にじ]です。", "Niji desu.", "Saat iki.", False),
        make_option("二階[にかい]です。", "Nikai desu.", "İkinci kat.", False)
    ]),
    make_step("こちらへどうぞ。メニューです。", "Kochira e douzo. Menyuu desu.", "Buradan buyrun. Menünüz.", [
        make_option("ありがとうございます。", "Arigatou gozaimasu.", "Teşekkür ederim.", True),
        make_option("ごめんなさい。", "Gomennasai.", "Özür dilerim.", False),
        make_option("おやすみなさい。", "Oyasuminasai.", "İyi geceler.", False),
        make_option("おはようございます。", "Ohayou gozaimasu.", "Günaydın.", False)
    ]),
    make_step("ご注[ちゅう]文[もん]はお決[き]まりですか？", "Gochuumon wa okimari desu ka?", "Siparişinize karar verdiniz mi?", [
        make_option("はい、ラーメンをお願[ねが]いします。", "Hai, raamen o onegaishimasu.", "Evet, ramen lütfen.", True),
        make_option("いいえ、食[た]べません。", "Iie, tabemasen.", "Hayır, yemem.", False),
        make_option("おいしいです。", "Oishii desu.", "Lezzetlidir.", False),
        make_option("水[みず]が好[す]きです。", "Mizu ga suki desu.", "Suyu severim.", False)
    ]),
    make_step("お飲[の]み物[もの]はいかがですか？", "Onomimono wa ikaga desu ka?", "İçecek bir şey ister misiniz?", [
        make_option("お茶[ちゃ]をください。", "Ocha o kudasai.", "Çay verin lütfen.", True),
        make_option("りんごをください。", "Ringo o kudasai.", "Elma verin lütfen.", False),
        make_option("肉[にく]をお願[ねが]いします。", "Niku o onegaishimasu.", "Et lütfen.", False),
        make_option("いりません。", "Irimasen.", "İstemiyorum. (Kaba)", False)
    ]),
    make_step("かしこまりました。少々[しょうしょう]お待[ま]ちください。", "Kashikomarimashita. Shoushou omachi kudasai.", "Anlaşıldı. Lütfen biraz bekleyin.", [
        make_option("はい、わかりました。", "Hai, wakarimashita.", "Evet, anladım.", True),
        make_option("早[はや]くしてください。", "Hayaku shite kudasai.", "Acele edin lütfen.", False),
        make_option("ごちそうさまでした。", "Gochisousama deshita.", "Ellerinize sağlık (Yemekten sonra).", False),
        make_option("さようなら。", "Sayounara.", "Hoşça kal.", False)
    ])
])
scenarios.append(s2)

# Scenario 3: Shopping
s3 = make_scenario("s3", "Alışveriş", "Bir mağazada kıyafet alırken geçen diyalog.", "N5", [
    make_step("いらっしゃいませ。何[なに]かお探[さが]しですか？", "Irasshaimase. Nanika osagashi desu ka?", "Hoş geldiniz. Bir şey mi arıyordunuz?", [
        make_option("シャツを探[さが]しています。", "Shatsu o sagashite imasu.", "Gömlek arıyorum.", True),
        make_option("寝[ね]たいです。", "Netai desu.", "Uyumak istiyorum.", False),
        make_option("道[みち]に迷[まよ]いました。", "Michi ni mayoimashita.", "Yolumu kaybettim.", False),
        make_option("本[ほん]を読[よ]みます。", "Hon o yomimasu.", "Kitap okuyacağım.", False)
    ]),
    make_step("こちらのシャツはいかがですか？", "Kochira no shatsu wa ikaga desu ka?", "Bu gömlek nasıl?", [
        make_option("いいですね。試着[しちゃく]してもいいですか？", "Ii desu ne. Shichaku shite mo ii desu ka?", "Güzelmiş. Deneyebilir miyim?", True),
        make_option("美味[おい]しくないです。", "Oishikunai desu.", "Lezzetli değil.", False),
        make_option("はい、捨[す]ててください。", "Hai, sutete kudasai.", "Evet, çöpe atın.", False),
        make_option("あそこです。", "Asoko desu.", "Oradadır.", False)
    ]),
    make_step("はい、試着室[しちゃくしつ]はあちらです。", "Hai, shichakushitsu wa achira desu.", "Evet, deneme kabini oradadır.", [
        make_option("ありがとうございます。", "Arigatou gozaimasu.", "Teşekkür ederim.", True),
        make_option("どういたしまして。", "Douitashimashite.", "Rica ederim.", False),
        make_option("ただいま。", "Tadaima.", "Ben geldim.", False),
        make_option("いただきます。", "Itadakimasu.", "Afiyet olsun.", False)
    ]),
    make_step("いかがでしたか？", "Ikaga deshita ka?", "Nasıldı? (Üzerinize oldu mu?)", [
        make_option("ちょうどいいです。これを買[か]います。", "Choudo ii desu. Kore o kaimasu.", "Tam oldu. Bunu satın alıyorum.", True),
        make_option("食[た]べすぎました。", "Tabesugimashita.", "Çok fazla yedim.", False),
        make_option("まだ終[お]わっていません。", "Mada owatte imasen.", "Henüz bitmedi.", False),
        make_option("分[わ]かりません。", "Wakarimasen.", "Bilmiyorum.", False)
    ]),
    make_step("ありがとうございます。三千円[さんぜんえん]になります。", "Arigatou gozaimasu. Sanzen-en ni narimasu.", "Teşekkür ederim. Üç bin yen tutuyor.", [
        make_option("カードで払[はら]えますか？", "Kaado de haraemasu ka?", "Kartla ödeyebilir miyim?", True),
        make_option("お金[かね]がありません。", "Okane ga arimasen.", "Param yok.", False),
        make_option("安[やす]すぎます。", "Yasusugimasu.", "Çok ucuz.", False),
        make_option("明日[あした]払[はら]います。", "Ashita haraimasu.", "Yarın ödeyeceğim.", False)
    ])
])
scenarios.append(s3)

# Scenario 4: Directions
s4 = make_scenario("s4", "Yol Tarifi", "İstasyona nasıl gideceğinizi sorun.", "N5", [
    make_step("すみません、駅[えき]はどこですか？", "Sumimasen, eki wa doko desu ka?", "Afedersiniz, istasyon nerede?", [
        make_option("駅[えき]はこの道[みち]をまっすぐです。", "Eki wa kono michi o massugu desu.", "İstasyon bu yoldan dümdüz ileride.", True),
        make_option("駅[えき]は私[わたし]の家[いえ]です。", "Eki wa watashi no ie desu.", "İstasyon benim evim.", False),
        make_option("駅[えき]を食[た]べました。", "Eki o tabemashita.", "İstasyonu yedim.", False),
        make_option("はい、駅[えき]です。", "Hai, eki desu.", "Evet, istasyon.", False)
    ]),
    make_step("歩[ある]いてどのくらいかかりますか？", "Aruite dono kurai kakarimasu ka?", "Yürüyerek ne kadar sürer?", [
        make_option("十分[じゅっぷん]くらいかかります。", "Juppun kurai kakarimasu.", "Yaklaşık 10 dakika sürer.", True),
        make_option("十円[じゅうえん]くらいかかります。", "Juuen kurai kakarimasu.", "Yaklaşık 10 yen tutar.", False),
        make_option("十個[じゅっこ]くらいあります。", "Jukko kurai arimasu.", "Yaklaşık 10 tane var.", False),
        make_option("十人[じゅうにん]くらいいます。", "Juunin kurai imasu.", "Yaklaşık 10 kişi var.", False)
    ]),
    make_step("遠[とお]いですか？", "Tooi desu ka?", "Uzak mı?", [
        make_option("いいえ、近[ちか]くですよ。", "Iie, chikaku desu yo.", "Hayır, yakındır.", True),
        make_option("はい、安[やす]いです。", "Hai, yasui desu.", "Evet, ucuzdur.", False),
        make_option("いいえ、甘[あま]くないです。", "Iie, amakunai desu.", "Hayır, tatlı değil.", False),
        make_option("はい、遠[とお]かったです。", "Hai, tookatta desu.", "Evet, uzaktı. (Geçmiş zaman)", False)
    ]),
    make_step("バスでも行[い]けますか？", "Basu de mo ikemasu ka?", "Otobüsle de gidilebilir mi?", [
        make_option("はい、あそこにバス停[てい]があります。", "Hai, asoko ni basutei ga arimasu.", "Evet, şurada otobüs durağı var.", True),
        make_option("はい、バスを食[た]べます。", "Hai, basu o tabemasu.", "Evet, otobüsü yerim.", False),
        make_option("いいえ、行[い]きませんでした。", "Iie, ikimasen deshita.", "Hayır, gitmedim.", False),
        make_option("はい、車[くるま]です。", "Hai, kuruma desu.", "Evet, araba.", False)
    ]),
    make_step("どうもありがとうございます。", "Doumo arigatou gozaimasu.", "Çok teşekkür ederim.", [
        make_option("気[き]をつけてくださいね。", "Ki o tsukete kudasai ne.", "Dikkatli olun lütfen.", True),
        make_option("ごちそうさまでした。", "Gochisousama deshita.", "Ellerinize sağlık.", False),
        make_option("おやすみなさい。", "Oyasuminasai.", "İyi geceler.", False),
        make_option("いただきます。", "Itadakimasu.", "Afiyet olsun.", False)
    ])
])
scenarios.append(s4)

# Scenario 5: Hotel Check-in
s5 = make_scenario("s5", "Otel Check-in", "Bir otele giriş yapın ve işlemlerinizi tamamlayın.", "N4", [
    make_step("いらっしゃいませ。ご予約[よやく]のお客様[きゃくさま]ですか？", "Irasshaimase. Goyoyaku no okyakusama desu ka?", "Hoş geldiniz. Rezervasyonunuz var mıydı?", [
        make_option("はい、予約[よやく]したアリです。", "Hai, yoyaku shita Ari desu.", "Evet, rezervasyon yapan Ali'yim.", True),
        make_option("いいえ、帰[かえ]りたかったです。", "Iie, kaeritakatta desu.", "Hayır, dönmek istiyordum.", False),
        make_option("はい、ホテルです。", "Hai, hoteru desu.", "Evet, otel.", False),
        make_option("部屋[へや]が広[ひろ]いです。", "Heya ga hiroi desu.", "Oda geniş.", False)
    ]),
    make_step("アリ様[さま]ですね。パスポートをお願[ねが]いします。", "Ari-sama desu ne. Pasupooto o onegaishimasu.", "Ali Bey, değil mi? Pasaportunuzu alabilir miyim?", [
        make_option("はい、どうぞ。", "Hai, douzo.", "Evet, buyrun.", True),
        make_option("いいえ、ちがいます。", "Iie, chigaimasu.", "Hayır, yanlış.", False),
        make_option("パスポートが好[す]きです。", "Pasupooto ga suki desu.", "Pasaportu severim.", False),
        make_option("これは本[ほん]です。", "Kore wa hon desu.", "Bu kitaptır.", False)
    ]),
    make_step("ありがとうございます。三泊[さんぱく]ですね。", "Arigatou gozaimasu. Sanpaku desu ne.", "Teşekkür ederim. Üç gece kalacaksınız değil mi?", [
        make_option("はい、そうです。", "Hai, sou desu.", "Evet, öyle.", True),
        make_option("いいえ、三個[さんこ]です。", "Iie, sanko desu.", "Hayır, üç tane.", False),
        make_option("はい、三回[さんかい]です。", "Hai, sankai desu.", "Evet, üç kere.", False),
        make_option("いいえ、三歳[さんさい]です。", "Iie, sansai desu.", "Hayır, üç yaşında.", False)
    ]),
    make_step("朝食[ちょうしょく]は朝[あさ]７時[じ]から９時[じ]までです。", "Choushoku wa asa shichiji kara kuji made desu.", "Kahvaltı sabah 7'den 9'a kadardır.", [
        make_option("わかりました。レストランはどこですか？", "Wakarimashita. Resutoran wa doko desu ka?", "Anladım. Restoran nerede?", True),
        make_option("朝食[ちょうしょく]は食[た]べました。", "Choushoku wa tabemashita.", "Kahvaltıyı yedim.", False),
        make_option("朝食[ちょうしょく]を作[つく]ります。", "Choushoku o tsukurimasu.", "Kahvaltı hazırlayacağım.", False),
        make_option("９時[じ]に寝[ね]ます。", "Kuji ni nemasu.", "9'da uyurum.", False)
    ]),
    make_step("一階[いっかい]のロビーの隣[となり]にあります。こちらが鍵[かぎ]です。", "Ikkai no robii no tonari ni arimasu. Kochira ga kagi desu.", "Birinci kattaki lobinin yanındadır. Bu da anahtarınız.", [
        make_option("ありがとうございます。部屋[へや]に行[い]きます。", "Arigatou gozaimasu. Heya ni ikimasu.", "Teşekkür ederim. Odaya gidiyorum.", True),
        make_option("鍵[かぎ]を開[あ]けます。", "Kagi o akemasu.", "Anahtarı açacağım.", False),
        make_option("鍵[かぎ]をなくしました。", "Kagi o nakushimashita.", "Anahtarı kaybettim.", False),
        make_option("ロビーで寝[ね]ます。", "Robii de nemasu.", "Lobide uyuyacağım.", False)
    ])
])
scenarios.append(s5)

# Scenario 6: Doctor
s6 = make_scenario("s6", "Doktorda", "Kendinizi kötü hissediyorsunuz, hastaneye gittiniz.", "N4", [
    make_step("今日[きょう]はどうしましたか？", "Kyou wa dou shimashita ka?", "Bugün şikayetiniz nedir?", [
        make_option("熱[ねつ]があって、頭[あたま]が痛[いた]いです。", "Netsu ga atte, atama ga itai desu.", "Ateşim var ve başım ağrıyor.", True),
        make_option("元気[げんき]です。", "Genki desu.", "İyiyim.", False),
        make_option("今日[きょう]はいい天気[てんき]です。", "Kyou wa ii tenki desu.", "Bugün hava güzel.", False),
        make_option("足[あし]が長[なが]いです。", "Ashi ga nagai desu.", "Bacaklarım uzun.", False)
    ]),
    make_step("いつからですか？", "Itsu kara desu ka?", "Ne zamandan beri?", [
        make_option("昨日[きのう]の夜[よる]からです。", "Kinou no yoru kara desu.", "Dün geceden beri.", True),
        make_option("来週[らいしゅう]からです。", "Raishuu kara desu.", "Gelecek haftadan itibaren.", False),
        make_option("まだです。", "Mada desu.", "Henüz değil.", False),
        make_option("今[いま]からです。", "Ima kara desu.", "Şu andan itibaren.", False)
    ]),
    make_step("咳[せき]や鼻水[はなみず]は出[で]ますか？", "Seki ya hanamizu wa demasu ka?", "Öksürük veya burun akıntısı var mı?", [
        make_option("はい、少[すこ]し咳[せき]が出[で]ます。", "Hai, sukoshi seki ga demasu.", "Evet, biraz öksürüyorum.", True),
        make_option("いいえ、水[みず]が出[で]ます。", "Iie, mizu ga demasu.", "Hayır, su akıyor.", False),
        make_option("はい、出[で]かけます。", "Hai, dekakemasu.", "Evet, dışarı çıkacağım.", False),
        make_option("いいえ、何[なに]もありません。", "Iie, nanimo arimasen.", "Hayır, hiçbir şeyim yok.", False)
    ]),
    make_step("風邪[かぜ]ですね。薬[くすり]を出[だ]しておきます。", "Kaze desu ne. Kusuri o dashite okimasu.", "Soğuk algınlığı. Size ilaç yazıyorum.", [
        make_option("一日[いちにち]に何回[なんかい]飲[の]みますか？", "Ichinichi ni nankai nomimasu ka?", "Günde kaç kere içeceğim?", True),
        make_option("薬[くすり]は好[す]きです。", "Kusuri wa suki desu.", "İlacı severim.", False),
        make_option("薬[くすり]を作[つく]りましょうか？", "Kusuri o tsukurimashou ka?", "İlaç yapayım mı?", False),
        make_option("薬[くすり]は高[たか]いですか？", "Kusuri wa takai desu ka?", "İlaç pahalı mı?", False)
    ]),
    make_step("食後[しょくご]に一日三回[いちにちさんかい]飲[の]んでください。お大事[だいじ]に。", "Shokugo ni ichinichi sankai nonde kudasai. Odaiji ni.", "Yemeklerden sonra günde 3 kere için. Geçmiş olsun.", [
        make_option("はい、ありがとうございました。", "Hai, arigatou gozaimashita.", "Peki, çok teşekkür ederim.", True),
        make_option("いただきます。", "Itadakimasu.", "Afiyet olsun.", False),
        make_option("おかえりなさい。", "Okaerinasai.", "Evine hoş geldin.", False),
        make_option("ごちそうさまでした。", "Gochisousama deshita.", "Ellerinize sağlık.", False)
    ])
])
scenarios.append(s6)

# Scenario 7: Weekend Plans
s7 = make_scenario("s7", "Hafta Sonu Planları", "Bir arkadaşınızla hafta sonu planları hakkında konuşun.", "N5", [
    make_step("週末[しゅうまつ]は何[なに]か予定[よてい]がありますか？", "Shuumatsu wa nanika yotei ga arimasu ka?", "Hafta sonu için planın var mı?", [
        make_option("いいえ、特[とく]にありません。", "Iie, tokuni arimasen.", "Hayır, özellikle bir planım yok.", True),
        make_option("はい、週末[しゅうまつ]です。", "Hai, shuumatsu desu.", "Evet, hafta sonu.", False),
        make_option("いいえ、昨日[きのう]でした。", "Iie, kinou deshita.", "Hayır, dündü.", False),
        make_option("はい、予定[よてい]を知[し]りません。", "Hai, yotei o shirimasen.", "Evet, planı bilmiyorum.", False)
    ]),
    make_step("じゃあ、一緒[いっしょ]に映画[えいが]を見[み]に行[い]きませんか？", "Jaa, issho ni eiga o mi ni ikimasen ka?", "Öyleyse, birlikte film izlemeye gidelim mi?", [
        make_option("いいですね。何[なに]を見[み]ますか？", "Ii desu ne. Nani o mimasu ka?", "Olur, ne izleyeceğiz?", True),
        make_option("映画[えいが]は嫌[きら]いです。", "Eiga wa kirai desu.", "Filmlerden nefret ederim.", False),
        make_option("一人[ひとり]で行[い]きます。", "Hitori de ikimasu.", "Tek başıma gideceğim.", False),
        make_option("私[わたし]は映画[えいが]です。", "Watashi wa eiga desu.", "Ben filmim.", False)
    ]),
    make_step("新[あたら]しいアニメの映画[えいが]はどうですか？", "Atarashii anime no eiga wa dou desu ka?", "Yeni anime filmi nasıl olur?", [
        make_option("面白[おもしろ]そうですね。そうしましょう。", "Omoshirosou desu ne. Sou shimashou.", "İlginç görünüyor. Öyle yapalım.", True),
        make_option("おいしそうですね。", "Oishisou desu ne.", "Lezzetli görünüyor.", False),
        make_option("悲[かな]しいです。", "Kanashii desu.", "Üzgünüm.", False),
        make_option("アニメは見[み]ません。", "Anime wa mimasen.", "Anime izlemem.", False)
    ]),
    make_step("土曜日[どようび]の午後[ごご]はどうですか？", "Doyoubi no gogo wa dou desu ka?", "Cumartesi öğleden sonraya ne dersin?", [
        make_option("はい、大丈夫[だいじょうぶ]です。", "Hai, daijoubu desu.", "Evet, bana uyar.", True),
        make_option("土曜日[どようび]は昨日[きのう]でした。", "Doyoubi wa kinou deshita.", "Cumartesi dündü.", False),
        make_option("午後[ごご]は朝[あさ]です。", "Gogo wa asa desu.", "Öğleden sonra sabahtır.", False),
        make_option("いいえ、時計[とけい]がありません。", "Iie, tokei ga arimasen.", "Hayır, saatim yok.", False)
    ]),
    make_step("じゃあ、駅[えき]の前[まえ]で２時[じ]に待[ま]ち合[あ]わせしましょう。", "Jaa, eki no mae de niji ni machiawase shimashou.", "Tamam, istasyonun önünde saat 2'de buluşalım.", [
        make_option("わかりました。楽[たの]しみにしています。", "Wakarimashita. Tanoshimi ni shite imasu.", "Anlaştık. Dört gözle bekliyorum.", True),
        make_option("駅[えき]はどこですか。", "Eki wa doko desu ka.", "İstasyon nerede?", False),
        make_option("待[ま]ち合[あ]わせはしません。", "Machiawase wa shimasen.", "Buluşmayacağız.", False),
        make_option("さようなら。", "Sayounara.", "Hoşça kal.", False)
    ])
])
scenarios.append(s7)

# Scenario 8: Job Interview
s8 = make_scenario("s8", "İş Görüşmesi", "Bir ofiste yarı zamanlı iş (baito) görüşmesindesiniz.", "N4", [
    make_step("どうぞ、座[すわ]ってください。", "Douzo, suwatte kudasai.", "Buyrun, oturun.", [
        make_option("失礼[しつれい]します。", "Shitsurei shimasu.", "İzninizle.", True),
        make_option("ただいま。", "Tadaima.", "Ben geldim.", False),
        make_option("いってきます。", "Ittekimasu.", "Gidip döneceğim.", False),
        make_option("ごめんください。", "Gomen kudasai.", "Kimse var mı? / Özür dilerim.", False)
    ]),
    make_step("どうしてこのアルバイトに応募[おうぼ]したんですか？", "Doushite kono arubaito ni oubo shitan desu ka?", "Neden bu yarı zamanlı işe başvurdunuz?", [
        make_option("日本語[にほんご]を使[つか]う仕事[しごと]がしたいからです。", "Nihongo o tsukau shigoto ga shitai kara desu.", "Japonca kullanacağım bir iş yapmak istediğim için.", True),
        make_option("お金[かね]が好[す]きだからです。", "Okane ga suki dakara desu.", "Parayı sevdiğim için.", False),
        make_option("仕事[しごと]をしたくないからです。", "Shigoto o shitakunai kara desu.", "Çalışmak istemediğim için.", False),
        make_option("家[いえ]が近[ちか]いからです。", "Ie ga chikai kara desu.", "Ev yakın olduğu için.", False)
    ]),
    make_step("週[しゅう]に何日[なんにち]働[はたら]けますか？", "Shuu ni nannichi hatarakemasu ka?", "Haftada kaç gün çalışabilirsiniz?", [
        make_option("週[しゅう]に三日[みっか]働[はたら]けます。", "Shuu ni mikka hatarakemasu.", "Haftada 3 gün çalışabilirim.", True),
        make_option("三週間[さんしゅうかん]です。", "Sanshuukan desu.", "3 haftadır.", False),
        make_option("働[はたら]きません。", "Hatarakimasen.", "Çalışmam.", False),
        make_option("日曜日[にちようび]です。", "Nichiyoubi desu.", "Pazar günüdür.", False)
    ]),
    make_step("夜遅[よるおそ]くても大丈夫[だいじょうぶ]ですか？", "Yoru osokute mo daijoubu desu ka?", "Gece geç saatlere kadar olsa da sorun olur mu?", [
        make_option("はい、夜[よる]１１時[じ]までなら大丈夫[だいじょうぶ]です。", "Hai, yoru juuichiji made nara daijoubu desu.", "Evet, gece 11'e kadarsa sorun olmaz.", True),
        make_option("夜[よる]は寝[ね]ます。", "Yoru wa nemasu.", "Gece uyurum.", False),
        make_option("暗[くら]いのが怖[こわ]いです。", "Kurai no ga kowai desu.", "Karanlıktan korkarım.", False),
        make_option("遅刻[ちこく]します。", "Chikoku shimasu.", "Geç kalırım.", False)
    ]),
    make_step("結果[けっか]は来週[らいしゅう]電話[でんわ]で知[し]らせます。", "Kekka wa raishuu denwa de shirasemasu.", "Sonucu haftaya telefonla bildireceğiz.", [
        make_option("はい、よろしくお願[ねが]いします。", "Hai, yoroshiku onegaishimasu.", "Anladım, sizden haber bekliyorum.", True),
        make_option("電話[でんわ]を持[も]っていません。", "Denwa o motte imasen.", "Telefonum yok.", False),
        make_option("今日[きょう]教[おし]えてください。", "Kyou oshiete kudasai.", "Bugün söyleyin.", False),
        make_option("どういたしまして。", "Douitashimashite.", "Rica ederim.", False)
    ])
])
scenarios.append(s8)

# Scenario 9: Lost Item
s9 = make_scenario("s9", "Kayıp Eşya", "İstasyonda çantanızı unuttunuz ve görevliye soruyorsunuz.", "N4", [
    make_step("どうしましたか？", "Dou shimashita ka?", "Size nasıl yardımcı olabilirim?", [
        make_option("電車[でんしゃ]にカバンを忘[わす]れました。", "Densha ni kaban o wasuremashita.", "Trende çantamı unuttum.", True),
        make_option("電車[でんしゃ]が好[す]きです。", "Densha ga suki desu.", "Trenleri severim.", False),
        make_option("カバンを買[か]いたいです。", "Kaban o kaitai desu.", "Çanta satın almak istiyorum.", False),
        make_option("道[みち]に迷[まよ]いました。", "Michi ni mayoimashita.", "Yolumu kaybettim.", False)
    ]),
    make_step("どんなカバンですか？", "Donna kaban desu ka?", "Nasıl bir çantaydı?", [
        make_option("黒[くろ]くて、大[おお]きいです。", "Kurokute, ookii desu.", "Siyah ve büyüktür.", True),
        make_option("高[たか]かったです。", "Takakatta desu.", "Pahalıydı.", False),
        make_option("カバンは重[おも]いです。", "Kaban wa omoi desu.", "Çanta ağırdır.", False),
        make_option("私[わたし]が作[つく]りました。", "Watashi ga tsukurimashita.", "Ben yaptım.", False)
    ]),
    make_step("何線[なにせん]に乗[の]っていましたか？", "Nansen ni notte imashita ka?", "Hangi hatta biniyordunuz?", [
        make_option("山手線[やまのてせん]です。", "Yamanotesen desu.", "Yamanote hattındaydım.", True),
        make_option("新幹線[しんかんせん]を見[み]ました。", "Shinkansen o mimashita.", "Hızlı treni gördüm.", False),
        make_option("自転車[じてんしゃ]です。", "Jitensha desu.", "Bisikletti.", False),
        make_option("線[せん]は引[ひ]きません。", "Sen wa hikimasen.", "Çizgi çizmem.", False)
    ]),
    make_step("カバンの中[なか]に何[なに]が入[はい]っていますか？", "Kaban no naka ni nani ga haitte imasu ka?", "Çantanın içinde neler var?", [
        make_option("財布[さいふ]とパスポートが入[はい]っています。", "Saifu to pasupooto ga haitte imasu.", "Cüzdanım ve pasaportum var.", True),
        make_option("夢[ゆめ]が入[はい]っています。", "Yume ga haitte imasu.", "Hayallerim var.", False),
        make_option("何[なに]も食[た]べません。", "Nanimo tabemasen.", "Hiçbir şey yemem.", False),
        make_option("空[から]っぽです。", "Karappo desu.", "Bomboş.", False)
    ]),
    make_step("見[み]つかったら電話[でんわ]します。電話番号[でんわばんごう]をお願[ねが]いします。", "Mitsukattara denwa shimasu. Denwabangou o onegaishimasu.", "Bulunursa arayacağız. Telefon numaranızı alabilir miyim?", [
        make_option("はい、０９０ー...です。お願[ねが]いします。", "Hai, zero-kyuu-zero... desu. Onegaishimasu.", "Evet, 090-... Lütfen bana yardımcı olun.", True),
        make_option("電話[でんわ]は嫌[きら]いです。", "Denwa wa kirai desu.", "Telefondan nefret ederim.", False),
        make_option("電話[でんわ]をください。", "Denwa o kudasai.", "Bana telefon verin.", False),
        make_option("見[み]つかりません。", "Mitsukarimasen.", "Bulunamaz.", False)
    ])
])
scenarios.append(s9)

# Scenario 10: Apology
s10 = make_scenario("s10", "Geç Kalma Özrü", "Arkadaşınızla buluşmaya geç kaldınız ve özür diliyorsunuz.", "N5", [
    make_step("遅[おそ]いよ！三十分[さんじゅっぷん]も待[ま]ったよ。", "Osoi yo! Sanjuppun mo matta yo.", "Çok geciktin! 30 dakika bekledim.", [
        make_option("本当[ほんとう]にごめんなさい！", "Hontou ni gomennasai!", "Gerçekten çok özür dilerim!", True),
        make_option("ありがとうございます。", "Arigatou gozaimasu.", "Teşekkür ederim.", False),
        make_option("私[わたし]も待[ま]ちました。", "Watashi mo machimashita.", "Ben de bekledim.", False),
        make_option("おはようございます。", "Ohayou gozaimasu.", "Günaydın.", False)
    ]),
    make_step("どうして遅[おく]れたの？", "Doushite okureta no?", "Neden geç kaldın?", [
        make_option("電車[でんしゃ]が遅[おく]れました。", "Densha ga okuremashita.", "Tren gecikti.", True),
        make_option("走[はし]りました。", "Hashirimashita.", "Koştum.", False),
        make_option("時計[とけい]を買[か]いました。", "Tokei o kaimashita.", "Saat satın aldım.", False),
        make_option("早[はや]く来[き]ました。", "Hayaku kimashita.", "Erken geldim.", False)
    ]),
    make_step("そうなんだ。連絡[れんらく]してほしかったな。", "Sou nanda. Renraku shite hoshikatta na.", "Öyle mi. Haber vermeni isterdim.", [
        make_option("スマホを忘[わす]れてしまって...", "Sumaho o wasurete shimatte...", "Akıllı telefonumu unuttum da...", True),
        make_option("連絡[れんらく]は嫌[きら]いです。", "Renraku wa kirai desu.", "İletişimi sevmem.", False),
        make_option("スマホが欲[ほ]しいです。", "Sumaho ga hoshii desu.", "Akıllı telefon istiyorum.", False),
        make_option("電話[でんわ]をしましたよ。", "Denwa o shimashita yo.", "Telefon ettim ya.", False)
    ]),
    make_step("次[つぎ]からは気[き]をつけてね。", "Tsugi kara wa ki o tsukete ne.", "Bir dahakine dikkat et olur mu?", [
        make_option("うん、本当[ほんとう]に気[き]をつける。", "Un, hontou ni ki o tsukeru.", "Evet, gerçekten dikkat edeceğim.", True),
        make_option("気[き]をつけません。", "Ki o tsukemasen.", "Dikkat etmeyeceğim.", False),
        make_option("次[つぎ]はありません。", "Tsugi wa arimasen.", "Bir dahakisi yok.", False),
        make_option("私[わたし]もそう思[おも]います。", "Watashi mo sou omoimasu.", "Ben de öyle düşünüyorum.", False)
    ]),
    make_step("じゃあ、行[い]こうか。お腹空[なかす]いた？", "Jaa, ikou ka. Onaka suita?", "Öyleyse gidelim mi? Karnın aç mı?", [
        make_option("うん、すごく空[す]いた！おごるよ。", "Un, sugoku suita! Ogoru yo.", "Evet, çok açım! Yemeği ben ısmarlıyorum.", True),
        make_option("いいえ、食[た]べません。", "Iie, tabemasen.", "Hayır, yemem.", False),
        make_option("お腹[なか]が痛[いた]いです。", "Onaka ga itai desu.", "Karnım ağrıyor.", False),
        make_option("行[い]きません。", "Ikimasen.", "Gitmiyorum.", False)
    ])
])
scenarios.append(s10)

with open("/Users/batu/Desktop/Projects/nihongo_app/Nihongo_app/Nihongo_app/Resources/ScenariosData.json", "w", encoding="utf-8") as f:
    json.dump(scenarios, f, ensure_ascii=False, indent=2)

print("Created ScenariosData.json with Furigana")
