#!/usr/bin/env python3
"""
Nihongo App — edge-tts ile Japonca ses dosyaları üretici.
Kayıt / kredi kartı gerektirmez. Microsoft Edge'in Neural sesini kullanır.

Kullanım:
    pip3 install edge-tts
    python3 scripts/generate_audio.py

Üretilen ses dosyaları:  Nihongo_app/Nihongo_app/Resources/Audio/
Dosya adlandırma:         Japonca metnin SHA-256 hash'inin ilk 16 karakteri + .mp3
                          (AudioService.swift'teki lookup mantığıyla birebir aynı)
"""

import asyncio
import json
import hashlib
import os
import sys
from pathlib import Path

# ─── Yapılandırma ─────────────────────────────────────────────────────────────

VOICE = "ja-JP-NanamiNeural"   # Kadın, en doğal ücretsiz Neural ses
RATE  = "-10%"                  # Biraz yavaş — öğrenme amaçlı (%-oran veya +/- ms)
PITCH = "+0Hz"                  # Normal pitch

# Yollar
SCRIPT_DIR   = Path(__file__).resolve().parent
PROJECT_ROOT = SCRIPT_DIR.parent
RESOURCES_DIR = PROJECT_ROOT / "Nihongo_app" / "Nihongo_app" / "Resources"
OUTPUT_DIR    = RESOURCES_DIR / "Audio"

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

    # 3) N5 Kelime bilgisi — hiragana okunuşu (TTS kanji'yi yanlış okuyabilir)
    for word in load_json("N5VocabularyData.json"):
        texts[word["hiragana"]] = "vocabulary"

    # 4) N5 Gramer örnekleri
    for lesson in load_json("N5GrammarData.json"):
        for example in lesson.get("examples", []):
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


async def generate_one(text: str, filepath: Path, semaphore: asyncio.Semaphore) -> bool:
    """Tek bir metni seslendirip dosyaya yazar. Eşzamanlılığı semaphore ile sınırlar."""
    async with semaphore:
        try:
            import edge_tts
            communicate = edge_tts.Communicate(text, voice=VOICE, rate=RATE, pitch=PITCH)
            await communicate.save(str(filepath))
            return True
        except Exception as e:
            print(f"  ❌ HATA: {text[:30]!r} → {e}")
            return False


async def generate_all(texts: dict[str, str]):
    """Tüm ses dosyalarını eşzamanlı olarak üretir."""
    try:
        import edge_tts  # noqa: F401
    except ImportError:
        print("❌ edge-tts kütüphanesi bulunamadı!")
        print("   Kur: pip3 install edge-tts")
        sys.exit(1)

    OUTPUT_DIR.mkdir(parents=True, exist_ok=True)

    # Neyin üretilmesi gerektiğini belirle
    tasks_needed: list[tuple[str, str, Path]] = []
    skipped = 0
    for text, category in texts.items():
        filename = text_to_filename(text)
        filepath = OUTPUT_DIR / filename
        if filepath.exists():
            skipped += 1
        else:
            tasks_needed.append((text, category, filepath))

    total     = len(texts)
    to_gen    = len(tasks_needed)
    generated = 0
    errors    = 0

    print(f"\n🎌 Nihongo App — Ses Dosyası Üretici (edge-tts)")
    print(f"   Ses:             {VOICE}")
    print(f"   Hız:             {RATE}")
    print(f"   Toplam metin:    {total}")
    print(f"   ⏭️  Zaten mevcut: {skipped}")
    print(f"   🔄 Üretilecek:   {to_gen}")
    print(f"   📁 Çıktı:        {OUTPUT_DIR}\n")
    print("─" * 65)

    if not tasks_needed:
        print("✅ Tüm sesler zaten mevcut, atlandı.")
        return

    # Eşzamanlı istek sayısını sınırla (edge-tts'de flood sorunlarını önler)
    semaphore = asyncio.Semaphore(5)

    for i, (text, category, filepath) in enumerate(tasks_needed, 1):
        success = await generate_one(text, filepath, semaphore)
        if success:
            generated += 1
            progress = i / to_gen * 100
            print(f"  [{i:4d}/{to_gen}] ({progress:5.1f}%) ✅ {category:12s} │ {text[:28]:<28s}")
        else:
            errors += 1

        # Her 100 dosyada bir küçük bir nefes
        if i % 100 == 0:
            await asyncio.sleep(1)

    print("─" * 65)
    print(f"\n📊 Sonuç:")
    print(f"   ✅ Üretilen:      {generated}")
    print(f"   ⏭️  Atlandı:       {skipped}")
    print(f"   ❌ Hata:          {errors}")
    print(f"   📁 Toplam dosya:  {len(list(OUTPUT_DIR.glob('*.mp3')))}")
    print()


def generate_manifest(texts: dict[str, str]):
    """AudioService'in kullanacağı text→filename eşleme dosyasını üretir."""
    manifest = {text: text_to_filename(text) for text in texts}
    manifest_path = OUTPUT_DIR / "audio_manifest.json"
    with open(manifest_path, "w", encoding="utf-8") as f:
        json.dump(manifest, f, ensure_ascii=False, indent=2)
    print(f"📋 Manifest oluşturuldu: {manifest_path}")
    print(f"   Toplam giriş: {len(manifest)}\n")


# ─── Ana akış ─────────────────────────────────────────────────────────────────

if __name__ == "__main__":
    texts = collect_all_texts()

    print(f"📝 Toplanan benzersiz metin: {len(texts)}")
    categories: dict[str, int] = {}
    for cat in texts.values():
        categories[cat] = categories.get(cat, 0) + 1
    for cat, count in sorted(categories.items()):
        print(f"   • {cat}: {count}")

    asyncio.run(generate_all(texts))
    generate_manifest(texts)

    print("🎉 Tamamlandı!")
    print("   Şimdi Xcode'da Resources/Audio klasörünü projeye ekle.")
    print("   (Sürükle-bırak → 'Create folder references' seçili olsun)")
