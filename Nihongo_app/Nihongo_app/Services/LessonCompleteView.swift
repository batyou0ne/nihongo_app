//
//  LessonCompleteView.swift
//  "Zen Ring" başarı ekranı: halka dolar, checkmark çizilir, hafif parçacıklar
//  yayılır, XP sayılır, CTA belirir. Toplam ~1.75 sn. iOS 15+, bağımlılık yok.
//
//  Kullanım:
//    LessonCompleteView(xpGained: 20, streak: 7, streakIncreased: true) {
//        // sonraki adıma geç
//    }
//

import SwiftUI
import UIKit

// MARK: - Ana ekran

struct LessonCompleteView: View {
    var xpGained: Int = 20
    var streak: Int = 7
    var streakIncreased: Bool = true
    var title: String = L10n.lessonCompleteTitle
    var ctaTitle: String = L10n.continueButton
    var ringColor: Color = Theme.accent
    var onContinue: () -> Void

    @Environment(\.accessibilityReduceMotion) private var reduceMotion

    // Animasyon durumları
    @State private var scrim: Double = 0
    @State private var trackIn = false
    @State private var ringProgress: CGFloat = 0
    @State private var checkProgress: CGFloat = 0
    @State private var glow: CGFloat = 0
    @State private var breath: CGFloat = 1
    @State private var burst: CGFloat = 0
    @State private var titleIn = false
    @State private var xpValue: Double = 0
    @State private var badgeIn = false
    @State private var ctaIn = false

    private let ringSize: CGFloat = 160
    private let stroke: CGFloat = 6

    private var softColor: Color {
        ringColor.opacity(0.35)
    }

    var body: some View {
        ZStack {
            Theme.paper.ignoresSafeArea()

            VStack(spacing: 0) {
                Spacer()

                ZStack {
                    GlowPulse(progress: glow, color: ringColor)
                        .frame(width: ringSize, height: ringSize)

                    ParticleBurst(progress: burst, color: softColor)

                    ring
                        .scaleEffect(breath)
                }
                .frame(width: ringSize, height: ringSize)
                .padding(.bottom, 32)

                Text(title)
                    .font(Theme.heading(22))
                    .foregroundStyle(Theme.ink)
                    .multilineTextAlignment(.center)
                    .opacity(titleIn ? 1 : 0)
                    .offset(y: titleIn ? 0 : 12)
                    .padding(.bottom, 8)

                CountingText(value: xpValue)
                    .font(Theme.display(36))
                    .foregroundStyle(Theme.ink)
                    .padding(.bottom, 12)

                Text(L10n.streakText(streak))
                    .font(Theme.heading(14))
                    .foregroundStyle(ringColor)
                    .padding(.horizontal, 14)
                    .padding(.vertical, 7)
                    .background(Capsule().fill(ringColor.opacity(0.14)))
                    .opacity(badgeIn ? 1 : 0)
                    .scaleEffect(badgeIn ? 1 : 0.85)

                Spacer()

                Button {
                    UIImpactFeedbackGenerator(style: .light).impactOccurred()
                    onContinue()
                } label: {
                    Text(ctaTitle)
                        .font(Theme.heading(17))
                        .foregroundStyle(Theme.paper)
                        .frame(maxWidth: .infinity)
                        .frame(height: 54)
                        .background(
                            RoundedRectangle(cornerRadius: 16, style: .continuous)
                                .fill(Theme.ink)
                        )
                }
                .buttonStyle(.plain)
                .opacity(ctaIn ? 1 : 0)
                .offset(y: ctaIn ? 0 : 16)
                .scaleEffect(ctaIn ? 1 : 0.96)
                .allowsHitTesting(ctaIn)
                .padding(.bottom, 24)
            }
            .padding(.horizontal, 24)
            .opacity(scrim)
        }
        .onAppear(perform: run)
    }

    // MARK: Halka + checkmark

    private var ring: some View {
        ZStack {
            // Track
            Circle()
                .stroke(ringColor, lineWidth: stroke)
                .opacity(trackIn ? 0.15 : 0)
                .scaleEffect(trackIn ? 1 : 0.92)

            // İlerleme halkası
            Circle()
                .trim(from: 0, to: ringProgress)
                .stroke(ringColor, style: StrokeStyle(lineWidth: stroke, lineCap: .round))
                .rotationEffect(.degrees(-90))

            // Checkmark
            CheckShape()
                .trim(from: 0, to: checkProgress)
                .stroke(ringColor, style: StrokeStyle(lineWidth: stroke, lineCap: .round, lineJoin: .round))
                .frame(width: ringSize, height: ringSize)
                .opacity(min(1, Double(checkProgress) * 12))
        }
        .frame(width: ringSize - stroke, height: ringSize - stroke)
    }

    // MARK: Koreografi

    private func run() {
        if reduceMotion {
            runReduced()
            return
        }

        let impact = UIImpactFeedbackGenerator(style: .medium)
        let selection = UISelectionFeedbackGenerator()
        let notify = UINotificationFeedbackGenerator()
        impact.prepare(); selection.prepare(); notify.prepare()

        // 0 ms: scrim + track
        withAnimation(.timingCurve(0.4, 0, 0.2, 1, duration: 0.2)) { scrim = 1 }
        withAnimation(.timingCurve(0.22, 1, 0.36, 1, duration: 0.2)) { trackIn = true }

        // 100 → 700 ms: halka dolar
        after(100) {
            withAnimation(.timingCurve(0.65, 0, 0.35, 1, duration: 0.6)) { ringProgress = 1 }
        }

        // 560 → 860 ms: checkmark çizilir
        after(560) {
            withAnimation(.timingCurve(0.33, 1, 0.68, 1, duration: 0.3)) { checkProgress = 1 }
        }

        // 700 ms: halka kapanış anı (haptic + glow + nefes + parçacıklar)
        after(700) {
            impact.impactOccurred()
            withAnimation(.timingCurve(0.16, 1, 0.3, 1, duration: 0.45)) { glow = 1 }
            withAnimation(.easeOut(duration: 0.12)) { breath = 1.05 }
        }
        after(820) {
            withAnimation(.spring(response: 0.35, dampingFraction: 0.55)) { breath = 1 }
        }
        after(720) {
            withAnimation(.timingCurve(0.16, 1, 0.3, 1, duration: 0.46)) { burst = 1 }
        }

        // 900 ms: tebrik metni
        after(900) {
            withAnimation(.timingCurve(0.22, 1, 0.36, 1, duration: 0.35)) { titleIn = true }
        }

        // 1000 → 1500 ms: XP sayacı + hafif tikler
        after(1000) {
            withAnimation(.timingCurve(0.25, 1, 0.5, 1, duration: 0.5)) { xpValue = Double(xpGained) }
        }
        after(1100) { selection.selectionChanged() }
        after(1250) { selection.selectionChanged() }
        after(1400) { selection.selectionChanged() }

        // 1100 ms: streak rozeti (+ başarı haptic'i, sadece seri arttıysa)
        after(1100) {
            withAnimation(.spring(response: 0.4, dampingFraction: 0.7)) { badgeIn = true }
        }
        if streakIncreased {
            after(1450) { notify.notificationOccurred(.success) }
        }

        // 1300 ms: CTA
        after(1300) {
            withAnimation(.spring(response: 0.45, dampingFraction: 0.8)) { ctaIn = true }
        }
    }

    /// "Hareketi azalt" açıkken: her şey son hâlinde, sadece kısa fade.
    private func runReduced() {
        ringProgress = 1
        checkProgress = 1
        trackIn = true
        xpValue = Double(xpGained)
        withAnimation(.easeInOut(duration: 0.2)) {
            scrim = 1
            titleIn = true
            badgeIn = true
            ctaIn = true
        }
        UIImpactFeedbackGenerator(style: .medium).impactOccurred()
        if streakIncreased {
            after(250) { UINotificationFeedbackGenerator().notificationOccurred(.success) }
        }
    }

    private func after(_ ms: Int, _ work: @escaping () -> Void) {
        DispatchQueue.main.asyncAfter(deadline: .now() + .milliseconds(ms), execute: work)
    }
}

// MARK: - Checkmark şekli

private struct CheckShape: Shape {
    func path(in rect: CGRect) -> Path {
        // 160x160 referans alanına göre normalize edilmiş noktalar
        let w = rect.width, h = rect.height
        var p = Path()
        p.move(to: CGPoint(x: w * 0.350, y: h * 0.512))
        p.addLine(to: CGPoint(x: w * 0.450, y: h * 0.612))
        p.addLine(to: CGPoint(x: w * 0.662, y: h * 0.388))
        return p
    }
}

// MARK: - Glow halkası (dışa genişleyip kaybolur)

private struct GlowPulse: View, Animatable {
    var progress: CGFloat
    var color: Color

    var animatableData: CGFloat {
        get { progress }
        set { progress = newValue }
    }

    var body: some View {
        let p = Double(progress)
        let opacity = p < 0.4 ? (p / 0.4) * 0.45 : 0.45 * (1 - (p - 0.4) / 0.6)
        Circle()
            .fill(color)
            .opacity(opacity)
            .scaleEffect(1 + 0.35 * progress)
    }
}

// MARK: - Mikro parçacıklar

private struct ParticleBurst: View, Animatable {
    var progress: CGFloat
    var color: Color
    var count: Int = 10

    var animatableData: CGFloat {
        get { progress }
        set { progress = newValue }
    }

    var body: some View {
        let p = Double(progress)
        let opacity = p < 0.3 ? (p / 0.3) * 0.9 : 0.9 * (1 - (p - 0.3) / 0.7)

        ZStack {
            ForEach(0..<count, id: \.self) { i in
                let jitter = (i % 2 == 0) ? -0.14 : 0.14
                let angle = (Double(i) / Double(count)) * .pi * 2 + jitter
                let dist = 64.0 + Double(i % 3) * 10.0

                Circle()
                    .fill(color)
                    .frame(width: 6, height: 6)
                    .scaleEffect(1 - 0.7 * progress)
                    .offset(x: CGFloat(cos(angle) * dist * p),
                            y: CGFloat(sin(angle) * dist * p))
                    .opacity(opacity)
            }
        }
    }
}

// MARK: - Sayan XP metni

private struct CountingText: View, Animatable {
    var value: Double

    var animatableData: Double {
        get { value }
        set { value = newValue }
    }

    var body: some View {
        Text("+\(Int(value.rounded())) XP")
            .monospacedDigit()
    }
}

// MARK: - Preview

#Preview {
    LessonCompleteView(xpGained: 20, streak: 7, streakIncreased: true) {}
}
