import SwiftUI

struct StoryReaderView: View {
    let story: Story
    @State private var selectedSentence: StorySentence?
    @Environment(\.dismiss) private var dismiss
    
    var continuousText: AttributedString {
        var result = AttributedString()
        for sentence in story.sentences {
            var attr = AttributedString(sentence.japaneseText + " ")
            attr.link = URL(string: "sentence://\(sentence.id)")
            attr.foregroundColor = (sentence.id == selectedSentence?.id) ? Theme.accent : Theme.ink
            result.append(attr)
        }
        return result
    }
    
    var allVocabulary: [String] {
        let allVocab = story.sentences.flatMap { $0.vocabulary }
        var uniqueVocab = [String]()
        var seen = Set<String>()
        for v in allVocab {
            if !seen.contains(v) {
                seen.insert(v)
                uniqueVocab.append(v)
            }
        }
        return uniqueVocab
    }

    var body: some View {
        ScrollView {
            VStack(alignment: .leading, spacing: 32) {
                Text(story.title)
                    .font(Theme.display(28))
                    .foregroundStyle(Theme.ink)
                
                Text(continuousText)
                    .font(Theme.heading(22))
                    .lineSpacing(12)
                    .tint(Theme.ink)
                    .environment(\.openURL, OpenURLAction { url in
                        if url.scheme == "sentence" {
                            if let s = story.sentences.first(where: { $0.id == url.host }) {
                                selectedSentence = s
                                return .handled
                            }
                        }
                        return .systemAction
                    })
                
                Divider()
                    .background(Theme.secondaryInk.opacity(0.3))
                
                VStack(alignment: .leading, spacing: 12) {
                    Text("Hikayenin Kelimeleri")
                        .font(Theme.heading(18))
                        .foregroundStyle(Theme.secondaryInk)
                    
                    ForEach(allVocabulary, id: \.self) { vocab in
                        Text("• \(vocab)")
                            .font(.body)
                            .foregroundStyle(Theme.ink)
                    }
                }
            }
            .padding(24)
            .padding(.bottom, 80)
        }
        .background(Theme.paper)
        .navigationTitle("")
        .navigationBarTitleDisplayMode(.inline)
        .navigationBarBackButtonHidden(true)
        .toolbar {
            ToolbarItem(placement: .topBarLeading) {
                Button {
                    dismiss()
                } label: {
                    HStack(spacing: 4) {
                        Image(systemName: "chevron.left")
                            .font(.body.weight(.bold))
                        Text("Geri")
                    }
                    .foregroundStyle(Theme.ink)
                }
            }
        }
        .sheet(item: $selectedSentence) { sentence in
            SentenceDetailPopup(sentence: sentence)
                .presentationDetents([.medium, .large])
                .presentationDragIndicator(.visible)
        }
    }
}

struct SentenceDetailPopup: View {
    let sentence: StorySentence
    @Environment(\.dismiss) private var dismiss
    
    var body: some View {
        NavigationStack {
            ScrollView {
                VStack(alignment: .leading, spacing: 24) {
                    // Orijinal Metin & Ses
                    HStack(alignment: .top) {
                        Text(sentence.japaneseText)
                            .font(Theme.heading(26))
                            .foregroundStyle(Theme.ink)
                            .lineSpacing(8)
                        
                        Spacer()
                        
                        Button {
                            AudioService.shared.speak(sentence.japaneseText)
                        } label: {
                            Image(systemName: "speaker.wave.2.circle.fill")
                                .font(.system(size: 32))
                                .foregroundStyle(Theme.accent)
                        }
                    }
                    
                    Divider()
                        .background(Theme.secondaryInk.opacity(0.3))
                    
                    // Romaji & Çeviri
                    VStack(alignment: .leading, spacing: 12) {
                        Text("Romaji")
                            .font(.caption.weight(.bold))
                            .foregroundStyle(Theme.secondaryInk)
                        Text(sentence.romaji)
                            .font(.body)
                            .foregroundStyle(Theme.ink)
                        
                        Text("Türkçe Çeviri")
                            .font(.caption.weight(.bold))
                            .foregroundStyle(Theme.secondaryInk)
                            .padding(.top, 8)
                        Text(sentence.turkishTranslation)
                            .font(.body)
                            .foregroundStyle(Theme.ink)
                    }
                    
                    Divider()
                        .background(Theme.secondaryInk.opacity(0.3))
                    
                    // Kelimeler
                    VStack(alignment: .leading, spacing: 12) {
                        Text("Anahtar Kelimeler")
                            .font(Theme.heading(18))
                            .foregroundStyle(Theme.ink)
                        
                        if sentence.vocabulary.isEmpty {
                            Text("Bu cümle için yeni kelime yok.")
                                .font(.body)
                                .foregroundStyle(Theme.secondaryInk)
                        } else {
                            ForEach(sentence.vocabulary, id: \.self) { vocab in
                                Text("• \(vocab)")
                                    .font(.body)
                                    .foregroundStyle(Theme.ink)
                            }
                        }
                    }
                }
                .padding(24)
            }
            .background(Theme.paper)
            .navigationTitle("Cümle İncelemesi")
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .topBarTrailing) {
                    Button("Kapat") {
                        dismiss()
                    }
                    .font(.body.weight(.bold))
                    .foregroundStyle(Theme.accent)
                }
            }
            .onDisappear {
                AudioService.shared.stop()
            }
        }
    }
}
