import SwiftUI
import StoreKit

/// Yasal bağlantılar. Gizlilik politikası yayınlandığında URL buraya girilecek;
/// nil iken bağlantı gösterilmez. (App Store yayını için zorunludur.)
enum PaywallLinks {
    static let termsOfUse = URL(string: "https://www.apple.com/legal/internet-services/itunes/dev/stdeula/")
    static let privacyPolicy: URL? = nil
}

/// Kaimon Pro satın alma ekranı. Sheet olarak sunulur.
struct PaywallView: View {
    @Environment(\.dismiss) private var dismiss
    @State private var subscription = SubscriptionManager.shared

    @State private var selectedPlan: SubscriptionPlan = .yearly
    @State private var trialEligible = false
    @State private var infoMessage: String?
    @State private var showManageSubscriptions = false

    var body: some View {
        ZStack(alignment: .topTrailing) {
            Theme.paper.ignoresSafeArea()

            ScrollView {
                if subscription.isPro {
                    activeContent
                } else {
                    purchaseContent
                }
            }
            .scrollIndicators(.hidden)

            Button {
                dismiss()
            } label: {
                Image(systemName: "xmark")
                    .font(.system(size: 15, weight: .heavy))
                    .foregroundStyle(Theme.ink)
                    .frame(width: 36, height: 36)
                    .background(Circle().fill(Theme.paper))
                    .overlay(Circle().strokeBorder(Theme.ink, lineWidth: 2))
            }
            .padding(16)
            .accessibilityLabel(L10n.closeButton)
        }
        .task {
            await subscription.loadProducts()
            await updateTrialEligibility()
        }
        .onChange(of: subscription.products.count) { _, _ in
            Task { await updateTrialEligibility() }
        }
        .manageSubscriptionsSheet(isPresented: $showManageSubscriptions)
        .tint(Theme.accent)
    }

    // MARK: - Satın alma içeriği

    private var purchaseContent: some View {
        VStack(alignment: .leading, spacing: 24) {
            header
            benefits

            if subscription.products.isEmpty {
                unavailableView
            } else {
                VStack(spacing: 12) {
                    ForEach(SubscriptionPlan.allCases.sorted { $0.sortOrder < $1.sortOrder }) { plan in
                        if let product = subscription.product(for: plan) {
                            planCard(plan: plan, product: product)
                        }
                    }
                }
                purchaseButton
            }

            if let message = infoMessage ?? subscription.lastError {
                Text(message)
                    .font(.footnote.weight(.semibold))
                    .foregroundStyle(Theme.accent)
                    .frame(maxWidth: .infinity, alignment: .center)
            }

            footer
        }
        .padding(.horizontal, 20)
        .padding(.top, 56)
        .padding(.bottom, 28)
    }

    private var header: some View {
        VStack(alignment: .leading, spacing: 10) {
            Text("開門")
                .font(Theme.display(48))
                .foregroundStyle(Theme.accent)
            Text(L10n.proName.uppercased())
                .font(.caption.weight(.heavy))
                .tracking(2)
                .foregroundStyle(Theme.secondaryInk)
            Text(L10n.paywallHeadline)
                .font(Theme.display(34))
                .foregroundStyle(Theme.ink)
                .fixedSize(horizontal: false, vertical: true)
        }
    }

    private var benefits: some View {
        VStack(alignment: .leading, spacing: 14) {
            benefitRow("books.vertical.fill", L10n.paywallBenefitContent)
            benefitRow("bubble.left.and.bubble.right.fill", L10n.paywallBenefitAI)
            benefitRow("checkmark.seal.fill", L10n.paywallBenefitExams)
            benefitRow("nosign", L10n.paywallBenefitNoAds)
        }
        .padding(18)
        .frame(maxWidth: .infinity, alignment: .leading)
        .inkBordered()
    }

    private func benefitRow(_ icon: String, _ text: String) -> some View {
        HStack(spacing: 14) {
            Image(systemName: icon)
                .font(.system(size: 16, weight: .bold))
                .foregroundStyle(Theme.accent)
                .frame(width: 26)
            Text(text)
                .font(.system(size: 16, weight: .semibold, design: .rounded))
                .foregroundStyle(Theme.ink)
                .fixedSize(horizontal: false, vertical: true)
        }
    }

    // MARK: - Plan kartı

    private func planCard(plan: SubscriptionPlan, product: Product) -> some View {
        let isSelected = selectedPlan == plan

        return Button {
            withAnimation(.easeOut(duration: 0.15)) { selectedPlan = plan }
        } label: {
            HStack(spacing: 14) {
                Image(systemName: isSelected ? "largecircle.fill.circle" : "circle")
                    .font(.system(size: 22))
                    .foregroundStyle(isSelected ? Theme.accent : Theme.secondaryInk)

                VStack(alignment: .leading, spacing: 3) {
                    HStack(spacing: 8) {
                        Text(planTitle(plan))
                            .font(Theme.heading(18))
                            .foregroundStyle(Theme.ink)
                        if plan == .yearly {
                            Text(L10n.planBestValue)
                                .font(.system(size: 10, weight: .heavy))
                                .tracking(0.5)
                                .padding(.horizontal, 7)
                                .padding(.vertical, 3)
                                .background(Capsule().fill(Theme.accent))
                                .foregroundStyle(.white)
                        }
                    }
                    Text(planSubtitle(plan, product: product))
                        .font(.footnote)
                        .foregroundStyle(Theme.secondaryInk)
                }

                Spacer(minLength: 8)

                VStack(alignment: .trailing, spacing: 2) {
                    Text(product.displayPrice)
                        .font(Theme.heading(18))
                        .foregroundStyle(Theme.ink)
                    Text(planSuffix(plan))
                        .font(.caption)
                        .foregroundStyle(Theme.secondaryInk)
                }
            }
            .padding(.horizontal, 16)
            .padding(.vertical, 14)
            .background(RoundedRectangle(cornerRadius: 16).fill(isSelected ? Theme.accent.opacity(0.08) : Theme.paper))
            .overlay(
                RoundedRectangle(cornerRadius: 16)
                    .strokeBorder(isSelected ? Theme.accent : Theme.ink.opacity(0.35), lineWidth: isSelected ? 3 : 1.5)
            )
        }
        .buttonStyle(.plain)
    }

    private func planTitle(_ plan: SubscriptionPlan) -> String {
        switch plan {
        case .yearly: return L10n.planYearly
        case .monthly: return L10n.planMonthly
        case .lifetime: return L10n.planLifetime
        }
    }

    private func planSuffix(_ plan: SubscriptionPlan) -> String {
        switch plan {
        case .yearly: return L10n.planPerYear
        case .monthly: return L10n.planPerMonth
        case .lifetime: return ""
        }
    }

    private func planSubtitle(_ plan: SubscriptionPlan, product: Product) -> String {
        switch plan {
        case .yearly:
            let monthly = product.price / 12
            return L10n.planPerMonthEquivalent(monthly.formatted(product.priceFormatStyle))
        case .monthly:
            return " "
        case .lifetime:
            return L10n.planOneTime
        }
    }

    // MARK: - Satın alma butonu

    private var selectedHasTrial: Bool {
        selectedPlan == .yearly && trialEligible
    }

    private var purchaseButton: some View {
        Button {
            Task { await purchaseSelected() }
        } label: {
            ZStack {
                if subscription.isPurchasing {
                    ProgressView().tint(Theme.paper)
                } else {
                    Text(buttonTitle)
                }
            }
        }
        .buttonStyle(PrimaryButtonStyle())
        .disabled(subscription.isPurchasing)
    }

    private var buttonTitle: String {
        if selectedHasTrial { return L10n.startTrial }
        return selectedPlan == .lifetime ? L10n.buyLifetimeButton : L10n.subscribeButton
    }

    private var unavailableView: some View {
        VStack(spacing: 12) {
            if subscription.isLoadingProducts {
                ProgressView()
            } else {
                Text(L10n.productsUnavailable)
                    .font(.subheadline.weight(.semibold))
                    .foregroundStyle(Theme.secondaryInk)
                Button(L10n.retry) {
                    Task { await subscription.loadProducts() }
                }
                .buttonStyle(PrimaryButtonStyle())
            }
        }
        .frame(maxWidth: .infinity)
        .padding(.vertical, 12)
    }

    // MARK: - Alt bilgi

    private var footer: some View {
        VStack(spacing: 12) {
            Button(L10n.restorePurchases) {
                Task {
                    infoMessage = nil
                    await subscription.restorePurchases()
                }
            }
            .font(.system(size: 15, weight: .bold))
            .foregroundStyle(Theme.ink)

            Text(L10n.paywallFinePrint(trial: selectedHasTrial))
                .font(.caption2)
                .foregroundStyle(Theme.secondaryInk)
                .multilineTextAlignment(.center)

            HStack(spacing: 18) {
                if let terms = PaywallLinks.termsOfUse {
                    Link(L10n.termsOfUse, destination: terms)
                }
                if let privacy = PaywallLinks.privacyPolicy {
                    Link(L10n.privacyPolicy, destination: privacy)
                }
            }
            .font(.caption.weight(.semibold))
            .foregroundStyle(Theme.secondaryInk)
        }
        .frame(maxWidth: .infinity)
    }

    // MARK: - Pro aktif içeriği

    private var activeContent: some View {
        VStack(spacing: 20) {
            Text("開門")
                .font(Theme.display(64))
                .foregroundStyle(Theme.accent)
            Text(L10n.proActiveTitle)
                .font(Theme.display(30))
                .foregroundStyle(Theme.ink)
            Text(L10n.proActiveSubtitle)
                .font(.body)
                .foregroundStyle(Theme.secondaryInk)
                .multilineTextAlignment(.center)

            if case .pro(let plan, let expiresAt) = subscription.entitlement {
                VStack(spacing: 4) {
                    Text(planTitle(plan))
                        .font(Theme.heading(18))
                    if plan == .lifetime {
                        Text(L10n.lifetimeActive)
                            .font(.footnote)
                            .foregroundStyle(Theme.secondaryInk)
                    } else if let expiresAt {
                        Text(L10n.renewsOn(expiresAt.formatted(date: .abbreviated, time: .omitted)))
                            .font(.footnote)
                            .foregroundStyle(Theme.secondaryInk)
                    }
                }
                .padding(16)
                .frame(maxWidth: .infinity)
                .inkBordered()

                if plan.isAutoRenewing {
                    Button(L10n.manageSubscription) {
                        showManageSubscriptions = true
                    }
                    .buttonStyle(PrimaryButtonStyle())
                }
            }
        }
        .padding(.horizontal, 20)
        .padding(.top, 90)
    }

    // MARK: - İşlemler

    private func purchaseSelected() async {
        guard let product = subscription.product(for: selectedPlan) else { return }
        infoMessage = nil
        switch await subscription.purchase(product) {
        case .success:
            dismiss()
        case .pending:
            infoMessage = L10n.purchasePending
        case .cancelled:
            break
        }
    }

    private func updateTrialEligibility() async {
        guard let yearly = subscription.product(for: .yearly),
              let sub = yearly.subscription else {
            trialEligible = false
            return
        }
        trialEligible = await sub.isEligibleForIntroOffer
    }
}

#Preview {
    PaywallView()
}
