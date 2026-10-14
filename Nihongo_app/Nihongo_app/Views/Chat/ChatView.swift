import SwiftUI

struct ChatView: View {
    @State private var viewModel = ChatViewModel()
    @State private var inputText = ""
    @Environment(\.dismiss) private var dismiss
    @State private var showPaywall = false
    private let gate = FeatureGate.shared
    
    var body: some View {
        VStack(spacing: 0) {
            ScrollViewReader { proxy in
                ScrollView {
                    LazyVStack(spacing: 12) {
                        ForEach(viewModel.messages) { message in
                            MessageBubble(message: message) {
                                viewModel.retryLastMessage()
                            }
                            .id(message.id)
                        }
                        
                        if viewModel.isTyping {
                            HStack {
                                TypingIndicator()
                                    .padding(.horizontal, 16)
                                    .padding(.vertical, 12)
                                    .background(Theme.paper)
                                    .clipShape(RoundedRectangle(cornerRadius: 16))
                                    .overlay(RoundedRectangle(cornerRadius: 16).strokeBorder(Theme.ink, lineWidth: 1.5))
                                Spacer()
                            }
                            .padding(.horizontal)
                            .padding(.top, 4)
                            .id("TypingIndicator")
                        }
                    }
                    .padding(.vertical)
                }
                .background(Theme.paper.opacity(0.5))
                .onChange(of: viewModel.messages.count) { _, _ in
                    withAnimation {
                        proxy.scrollTo(viewModel.messages.last?.id, anchor: .bottom)
                    }
                }
                .onChange(of: viewModel.isTyping) { _, isTyping in
                    if isTyping {
                        withAnimation {
                            proxy.scrollTo("TypingIndicator", anchor: .bottom)
                        }
                    }
                }
            }
            
            // Input area
            VStack(spacing: 0) {
                Divider().background(Theme.ink)
                if let remaining = gate.remainingAIMessages {
                    AIQuotaBanner(remaining: remaining) { showPaywall = true }
                    Divider().background(Theme.ink.opacity(0.2))
                }
                HStack(spacing: 12) {
                    TextField(L10n.chatInputPlaceholder, text: $inputText)
                        .padding(.horizontal, 16)
                        .padding(.vertical, 10)
                        .background(Theme.paper)
                        .overlay(RoundedRectangle(cornerRadius: 20).strokeBorder(Theme.ink, lineWidth: 1.5))
                    
                    Button {
                        guard gate.canSendAIMessage else {
                            showPaywall = true
                            return
                        }
                        gate.recordAIMessage()
                        viewModel.sendMessage(inputText)
                        inputText = ""
                    } label: {
                        Image(systemName: "arrow.up.circle.fill")
                            .font(.system(size: 32))
                            .foregroundStyle(inputText.isEmpty ? Theme.secondaryInk : Theme.accent)
                    }
                    .disabled(inputText.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty)
                }
                .padding(.horizontal)
                .padding(.vertical, 12)
                .background(Theme.paper)
            }
        }
        .sheet(isPresented: $showPaywall) {
            PaywallView()
        }
        .navigationTitle(L10n.aiPracticeTitle)
        .navigationBarTitleDisplayMode(.inline)
        .toolbar {
            ToolbarItem(placement: .cancellationAction) {
                Button(L10n.closeButton) {
                    dismiss()
                }
            }
            ToolbarItem(placement: .primaryAction) {
                Button {
                    viewModel.clearMessages()
                } label: {
                    Image(systemName: "arrow.counterclockwise")
                        .font(.system(size: 15, weight: .medium))
                        .foregroundStyle(Theme.secondaryInk)
                }
            }
        }
        .background(Theme.paper)
    }
}

struct MessageBubble: View {
    let message: ChatMessage
    var onRetry: () -> Void = {}
    
    var body: some View {
        if message.isError {
            ErrorBubble(message: message, onRetry: onRetry)
        } else if message.isUser {
            UserBubble(message: message)
        } else {
            BotBubble(message: message)
        }
    }
}

struct UserBubble: View {
    let message: ChatMessage
    
    var body: some View {
        HStack {
            Spacer(minLength: 40)
            
            Text(message.text)
                .font(.system(size: 16, weight: .regular))
                .foregroundStyle(Theme.paper)
                .padding(.horizontal, 16)
                .padding(.vertical, 12)
                .background(Theme.accent)
                .clipShape(RoundedRectangle(cornerRadius: 18))
        }
        .padding(.horizontal)
    }
}

struct BotBubble: View {
    let message: ChatMessage
    
    private var parsed: (japanese: String, translation: String?) {
        parseMessage(message.text)
    }
    
    var body: some View {
        HStack {
            VStack(alignment: .leading, spacing: 10) {
                // Japanese content & Audio button
                HStack(alignment: .top, spacing: 10) {
                    FuriganaText(
                        text: parsed.japanese,
                        font: .system(size: 16, weight: .regular),
                        readingFont: .system(size: 11, weight: .regular),
                        color: Theme.ink
                    )
                    
                    Spacer(minLength: 4)
                    
                    Button {
                        AudioService.shared.speak(parsed.japanese)
                    } label: {
                        Image(systemName: "speaker.wave.2.fill")
                            .font(.system(size: 13))
                            .foregroundStyle(Theme.accent)
                            .padding(7)
                            .background(Theme.accent.opacity(0.12))
                            .clipShape(Circle())
                    }
                    .buttonStyle(.plain)
                }
                
                // Turkish Translation (if available)
                if let translation = parsed.translation {
                    VStack(alignment: .leading, spacing: 4) {
                        Divider()
                            .background(Theme.ink.opacity(0.12))
                        Text(translation)
                            .font(.system(size: 14, weight: .regular))
                            .foregroundStyle(Theme.secondaryInk)
                            .fixedSize(horizontal: false, vertical: true)
                    }
                }
            }
            .padding(.horizontal, 16)
            .padding(.vertical, 12)
            .background(Theme.paper)
            .clipShape(RoundedRectangle(cornerRadius: 18))
            .overlay(
                RoundedRectangle(cornerRadius: 18)
                    .strokeBorder(Theme.ink, lineWidth: 1.5)
            )
            
            Spacer(minLength: 40)
        }
        .padding(.horizontal)
    }
    
    private func parseMessage(_ text: String) -> (japanese: String, translation: String?) {
        let pattern = "^([\\s\\S]*?)[\\s\\n]*\\(([^()]+)\\)[\\s\\n]*$"
        if let regex = try? NSRegularExpression(pattern: pattern, options: []) {
            let nsString = text as NSString
            let matches = regex.matches(in: text, options: [], range: NSRange(location: 0, length: nsString.length))
            if let match = matches.first, match.numberOfRanges >= 3 {
                let jp = nsString.substring(with: match.range(at: 1)).trimmingCharacters(in: .whitespacesAndNewlines)
                let tr = nsString.substring(with: match.range(at: 2)).trimmingCharacters(in: .whitespacesAndNewlines)
                if !jp.isEmpty && !tr.isEmpty {
                    return (jp, tr)
                }
            }
        }
        return (text, nil)
    }
}

struct ErrorBubble: View {
    let message: ChatMessage
    let onRetry: () -> Void
    
    var body: some View {
        HStack {
            VStack(alignment: .leading, spacing: 8) {
                HStack(alignment: .top, spacing: 8) {
                    Image(systemName: "exclamationmark.triangle.fill")
                        .foregroundStyle(Color.orange)
                        .font(.system(size: 16))
                    Text(message.text)
                        .font(.system(size: 14))
                        .foregroundStyle(Theme.ink)
                        .fixedSize(horizontal: false, vertical: true)
                }
                
                Button(action: onRetry) {
                    HStack(spacing: 6) {
                        Image(systemName: "arrow.clockwise")
                        Text("Tekrar Dene")
                    }
                    .font(.system(size: 13, weight: .semibold))
                    .foregroundStyle(Theme.accent)
                    .padding(.horizontal, 12)
                    .padding(.vertical, 6)
                    .background(Theme.accent.opacity(0.12))
                    .clipShape(Capsule())
                }
            }
            .padding(.horizontal, 16)
            .padding(.vertical, 12)
            .background(Theme.paper)
            .clipShape(RoundedRectangle(cornerRadius: 18))
            .overlay(
                RoundedRectangle(cornerRadius: 18)
                    .strokeBorder(Color.orange.opacity(0.7), lineWidth: 1.5)
            )
            
            Spacer(minLength: 40)
        }
        .padding(.horizontal)
    }
}

struct TypingIndicator: View {
    @State private var offset1 = 0.0
    @State private var offset2 = 0.0
    @State private var offset3 = 0.0
    
    var body: some View {
        HStack(spacing: 4) {
            Circle().frame(width: 8, height: 8).offset(y: offset1)
            Circle().frame(width: 8, height: 8).offset(y: offset2)
            Circle().frame(width: 8, height: 8).offset(y: offset3)
        }
        .foregroundStyle(Theme.secondaryInk)
        .onAppear {
            let baseAnim = Animation.easeInOut(duration: 0.5).repeatForever()
            withAnimation(baseAnim) { offset1 = -5 }
            withAnimation(baseAnim.delay(0.15)) { offset2 = -5 }
            withAnimation(baseAnim.delay(0.3)) { offset3 = -5 }
        }
    }
}

#Preview {
    ChatView()
}
