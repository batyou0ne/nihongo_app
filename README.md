# Kaimon (開門)

JLPT N5 seviyesinde Japonca öğrenmek için bir iOS uygulaması. "Kaimon" =
"kapıyı açmak" — Japoncaya açılan kapı (torii).

## Modüller

- **Hiragana / Katakana** — 46'şar karakter, flashcard + 4 şıklı seçim
- **Kanji (N5)** — 80 kanji, 4 bölüm
- **Kelimeler (N5)** — ~675 kelime, 25'erlik bölümler
- **Tekrar Çalış** — yanlış yapılan öğeler burada birikir, "Öğrendim" ile temizlenir
- **İlerleme** — günlük seri (streak) ve modül bazlı öğrenilen oranı

Tekrar zamanlaması basitleştirilmiş bir SM-2 (spaced repetition) uygulamasıyla
yapılır; oturumlar kaldığı yerden devam eder ve tüm kartlar art arda doğru
bilinene kadar yanlışları tekrar turu açılır.

## Teknoloji

SwiftUI · SwiftData (yerel ilerleme) · Firebase Auth + Firestore (hesap ve
bulut senkronizasyonu) · Google ile giriş · iOS yerleşik TTS ile telaffuz

## Kurulum

1. `Nihongo_app.xcodeproj` dosyasını Xcode ile aç (Swift Package bağımlılıkları
   otomatik çözülür).
2. Firebase konsolundan alınan `GoogleService-Info.plist` dosyasını
   `Nihongo_app/App/` altına koy (repoya dahil değildir).
3. Bir simülatör ya da cihaz seçip çalıştır.

## Tasarım

Beyaz zemin, ağır siyah tipografi, tek vurgu rengi olarak vermilyon kırmızısı
ve keskin köşeler — brutalist/editoryal stil (`Theme/Theme.swift`).
