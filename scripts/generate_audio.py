#!/usr/bin/env python3
"""
Nihongo App — Google Cloud TTS ile Japonca ses dosyaları üretici.

Kullanım:
    export GOOGLE_APPLICATION_CREDENTIALS="./gcloud-key.json"
    python3 scripts/generate_audio.py

Üretilen ses dosyaları:  Nihongo_app/Nihongo_app/Resources/Audio/
Dosya adlandırma:         Japonca metnin SHA-256 hash'inin ilk 16 karakteri + .mp3
                          (AudioService.swift'teki lookup mantığıyla aynı)
"""

import json
import hashlib
import os
import sys
import time
from pathlib import Path

# ─── Yapılandırma ─────────────────────────────────────────────────────────────

VOICE_NAME = "ja-JP-Neural2-B"     # Kadın, en doğal Neural2 ses
LANGUAGE_CODE = "ja-JP"
SPEAKING_RATE = 0.9                 # Biraz yavaş — öğrenme amaçlı
PITCH = 0.0                        # Normal pitch
AUDIO_ENCODING = "MP3"

# Yollar
SCRIPT_DIR = Path(__file__).resolve().parent
PROJECT_ROOT = SCRIPT_DIR.parent
RESOURCES_DIR = PROJECT_ROOT / "Nihongo_app" / "Nihongo_app" / "Resources"
OUTPUT_DIR = RESOURCES_DIR / "Audio"

# ─── Yardımcı fonksiyonlar ────────────────────────────────────────────────────

def text_to_filename(text: str) -> str:
    """Japonca metni dosya adına dönüştürür (SHA-256 hash tabanlı)."""
    hash_hex = hashlib.sha256(text.encode("utf-8")).hexdigest()[:16]
    return f"{hash_hex}.mp3"


def load_json(filename: str) -> list:
    """Resources klasöründen JSON dosyası yükler."""
    path = RESOURCES_DIR / filename
    with open(path, "r", encoding="utf-8") as f:
        return json.load(f)


def collect_all_texts() -> dict[str, str]:
    """
    Tüm JSON verilerinden ses gerektiren Japonca metinleri toplar.
    Döndürülen dict: { metin: kategori }
    """
    texts: dict[str, str] = {}

    # 1) Hiragana karakterleri
    for char in load_json("HiraganaData.json"):
        texts[char["character"]] = "hiragana"

    # 2) Katakana karakterleri
    for char in load_json("KatakanaData.json"):
        texts[char["character"]] = "katakana"

    # 3) N5 Kelime bilgisi — hiragana okunuşu
    for word in load_json("N5VocabularyData.json"):
        texts[word["hiragana"]] = "vocabulary"

    # 4) N5 Gramer örnekleri
    for lesson in load_json("N5GrammarData.json"):
        for example in lesson.get("examples", []):
            # Hiragana varsa onu, yoksa kanji'li japanese'i al
            text = example.get("hiragana") or example.get("japanese", "")
            if text:
                texts[text] = "grammar"

    # 5) Hikaye cümleleri
    for story in load_json("N5StoriesData.json"):
        for sentence in story.get("sentences", []):
            text = sentence.get("japaneseText", "")
            if text:
                texts[text] = "story"

    return texts


def generate_audio_files(texts: dict[str, str]):
    """Google Cloud TTS ile ses dosyalarını üretir."""
    try:
        from google.cloud import texttospeech
    except ImportError:
        print("❌ google-cloud-texttospeech kütüphanesi bulunamadı!")
        print("   Kur: pip3 install google-cloud-texttospeech")
        sys.exit(1)

    # API istemcisini oluştur
    client = texttospeech.TextToSpeechClient()

    # Ses yapılandırması
    voice = texttospeech.VoiceSelectionParams(
        language_code=LANGUAGE_CODE,
        name=VOICE_NAME,
    )
    audio_config = texttospeech.AudioConfig(
        audio_encoding=texttospeech.AudioEncoding.MP3,
        speaking_rate=SPEAKING_RATE,
        pitch=PITCH,
    )

    # Çıktı klasörünü oluştur
    OUTPUT_DIR.mkdir(parents=True, exist_ok=True)

    total = len(texts)
    skipped = 0
    generated = 0
    errors = 0

    print(f"\n🎌 Nihongo App — Ses Dosyası Üretici")
    print(f"   Ses: {VOICE_NAME}")
    print(f"   Toplam metin: {total}")
    print(f"   Çıktı: {OUTPUT_DIR}\n")
    print("─" * 60)

    for i, (text, category) in enumerate(texts.items(), 1):
        filename = text_to_filename(text)
        filepath = OUTPUT_DIR / filename

        # Zaten varsa atla
        if filepath.exists():
            skipped += 1
            continue

        try:
            synthesis_input = texttospeech.SynthesisInput(text=text)
            response = client.synthesize_speech(
                input=synthesis_input,
                voice=voice,
                audio_config=audio_config,
            )

            with open(filepath, "wb") as out:
                out.write(response.audio_content)

            generated += 1
            # İlerleme göstergesi
            progress = i / total * 100
            print(f"  [{i:4d}/{total}] ({progress:5.1f}%) ✅ {category:12s} │ {text[:30]:<30s} → {filename}")

            # Rate limiting — Google Cloud TTS'de dakikada 300 istek limiti var
            # Her 250 istekte bir kısa duraklama
            if generated % 250 == 0:
                print(f"\n  ⏳ Rate limit koruması — 10 saniye bekleniyor...\n")
                time.sleep(10)

        except Exception as e:
            errors += 1
            print(f"  [{i:4d}/{total}]          ❌ {category:12s} │ {text[:30]:<30s} → HATA: {e}")

    print("─" * 60)
    print(f"\n📊 Sonuç:")
    print(f"   ✅ Üretilen:  {generated}")
    print(f"   ⏭️  Atlanan:   {skipped} (zaten mevcuttu)")
    print(f"   ❌ Hata:      {errors}")
    print(f"   📁 Toplam dosya: {len(list(OUTPUT_DIR.glob('*.mp3')))}")
    print()


def generate_manifest(texts: dict[str, str]):
    """
    AudioService'in kullanacağı text→filename eşleme dosyasını üretir.
    Bu dosya Xcode projesine eklenmeli.
    """
    manifest = {}
    for text in texts:
        manifest[text] = text_to_filename(text)

    manifest_path = OUTPUT_DIR / "audio_manifest.json"
    with open(manifest_path, "w", encoding="utf-8") as f:
        json.dump(manifest, f, ensure_ascii=False, indent=2)

    print(f"📋 Manifest oluşturuldu: {manifest_path}")
    print(f"   Toplam giriş: {len(manifest)}")


# ─── Ana akış ─────────────────────────────────────────────────────────────────

if __name__ == "__main__":
    if not os.environ.get("GOOGLE_APPLICATION_CREDENTIALS"):
        print("⚠️  GOOGLE_APPLICATION_CREDENTIALS ortam değişkeni ayarlanmamış!")
        print("   export GOOGLE_APPLICATION_CREDENTIALS=\"./gcloud-key.json\"")
        print()

    texts = collect_all_texts()
    print(f"📝 Toplanan benzersiz metin sayısı: {len(texts)}")

    # Kategori bazlı özet
    categories: dict[str, int] = {}
    for category in texts.values():
        categories[category] = categories.get(category, 0) + 1
    for cat, count in sorted(categories.items()):
        print(f"   • {cat}: {count}")

    # Ses dosyalarını üret
    generate_audio_files(texts)

    # Manifest oluştur
    generate_manifest(texts)

    print("\n🎉 Tamamlandı! Şimdi Xcode'da Resources/Audio klasörünü projeye ekle.")
