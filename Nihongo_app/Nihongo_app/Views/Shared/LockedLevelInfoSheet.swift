import SwiftUI
import SwiftData

/// Kilitli bir JLPT seviyesine tıklandığında açılan bilgilendirme ve yönlendirme sayfası.
/// N5 tamamlama yüzdesini gösterir ve kullanıcıya 'Eksik Dersleri Tamamla' veya
/// 'Test-Out Challenge' (Seviye Atlama Sınavı) seçeneklerini sunar.
struct LockedLevelInfoSheet: View {
    let level: JLPTLevel
    var onStartTestOut: (() -> Void)? = nil

    @Environment(\.dismiss) private var dismiss
    @Environment(\.modelContext) private var modelContext

    private var previousLevel: JLPTLevel {
        level.previousLevel ?? .n5
    }

    private var progressData: (kanji: Double, vocab: Double, grammar: Double, overall: Double) {
        LevelProgressionService.shared.calculateLevelProgress(level: previousLevel, context: modelContext)
    }

    var body: some View {
        NavigationStack {
            VStack(spacing: 24) {
                // Kilit İkonu ve Başlık
                VStack(spacing: 12) {
                    ZStack {
                        Circle()
                            .fill(Theme.accent.opacity(0.12))
                            .frame(width: 80, height: 80)

                        Image(systemName: "lock.fill")
                            .font(.system(size: 36))
                            .foregroundStyle(Theme.accent)
                    }

                    Text("\(level.rawValue) Seviyesi Kilitli")
                        .font(Theme.display(28))
                        .foregroundStyle(Theme.ink)

                    Text("Bu seviyenin derslerine erişebilmek için \(previousLevel.rawValue) seviyesini tamamlamalı veya Seviye Atlama Sınavı'nı (Test-Out) geçmelisin.")
                        .font(.subheadline)
                        .foregroundStyle(Theme.secondaryInk)
                        .multilineTextAlignment(.center)
                        .padding(.horizontal, 16)
                }
                .padding(.top, 16)

                // Ön Seviye İlerleme Kartı
                VStack(alignment: .leading, spacing: 14) {
                    HStack {
                        Text("\(previousLevel.rawValue) İlerleme Durumu")
                            .font(Theme.heading(17))
                            .foregroundStyle(Theme.ink)
                        Spacer()
                        Text("%\(Int(progressData.overall * 100)) / %85")
                            .font(.system(size: 14, weight: .bold))
                            .foregroundStyle(progressData.overall >= 0.85 ? .green : Theme.accent)
                    }

                    ProgressBar(fraction: progressData.overall)

                    Divider()
                        .padding(.vertical, 2)

                    progressMetricRow(title: "Kanji Parçaları", fraction: progressData.kanji)
                    progressMetricRow(title: "Kelime Parçaları", fraction: progressData.vocab)
                    progressMetricRow(title: "Gramer Üniteleri", fraction: progressData.grammar)
                }
                .padding(18)
                .inkBordered()

                Spacer()

                // Aksiyon Butonları
                VStack(spacing: 12) {
                    if let onStartTestOut {
                        Button {
                            dismiss()
                            onStartTestOut()
                        } label: {
                            HStack {
                                Image(systemName: "bolt.fill")
                                Text("Seviyeyi Sınavla Atla (Test-Out)")
                            }
                            .font(Theme.heading(16))
                            .foregroundStyle(Theme.paper)
                            .frame(maxWidth: .infinity)
                            .frame(height: 52)
                            .background(Theme.accent)
                            .clipShape(RoundedRectangle(cornerRadius: 14))
                        }
                    }

                    Button {
                        dismiss()
                    } label: {
                        Text("Eksik Dersleri Tamamla")
                            .font(Theme.heading(16))
                            .foregroundStyle(Theme.ink)
                            .frame(maxWidth: .infinity)
                            .frame(height: 52)
                            .background(Theme.paper)
                            .clipShape(RoundedRectangle(cornerRadius: 14))
                            .overlay(
                                RoundedRectangle(cornerRadius: 14)
                                    .strokeBorder(Theme.ink, lineWidth: 2)
                            )
                    }
                }
                .padding(.bottom, 16)
            }
            .padding(20)
            .background(Theme.paper)
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .cancellationAction) {
                    Button(L10n.closeButton) { dismiss() }
                        .foregroundStyle(Theme.ink)
                }
            }
        }
    }

    private func progressMetricRow(title: String, fraction: Double) -> some View {
        HStack {
            Text(title)
                .font(.subheadline)
                .foregroundStyle(Theme.ink)
            Spacer()
            Text("%\(Int(fraction * 100))")
                .font(.system(size: 13, weight: .bold))
                .foregroundStyle(Theme.secondaryInk)
            Image(systemName: fraction >= 0.85 ? "checkmark.circle.fill" : "circle")
                .foregroundStyle(fraction >= 0.85 ? .green : Theme.secondaryInk)
        }
    }
}

#Preview {
    LockedLevelInfoSheet(level: .n4)
        .modelContainer(for: [UserLevelProgress.self, LearningItemProgress.self, UserProgress.self], inMemory: true)
}
