import SwiftUI

struct ChatView: View {
    @State private var viewModel = ChatViewModel()
    @State private var inputText = ""
    @Environment(\.dismiss) private var dismiss
    
    var body: some View {
        VStack(spacing: 0) {
            ScrollViewReader { proxy in
                ScrollView {
                    LazyVStack(spacing: 12) {
                        ForEach(viewModel.messages) { message in
                            MessageBubble(message: message)
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
                HStack(spacing: 12) {
                    TextField(L10n.chatInputPlaceholder, text: $inputText)
                        .padding(.horizontal, 16)
                        .padding(.vertical, 10)
                        .background(Theme.paper)
                        .overlay(RoundedRectangle(cornerRadius: 20).strokeBorder(Theme.ink, lineWidth: 1.5))
                    
                    Button {
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
        .navigationTitle(L10n.aiPracticeTitle)
        .navigationBarTitleDisplayMode(.inline)
        .toolbar {
            ToolbarItem(placement: .cancellationAction) {
                Button(L10n.closeButton) {
                    dismiss()
                }
            }
        }
        .background(Theme.paper)
    }
}

struct MessageBubble: View {
    let message: ChatMessage
    
    var body: some View {
        HStack {
            if message.isUser {
                Spacer(minLength: 40)
            }
            
            FuriganaText(text: message.text, font: .system(size: 16, weight: .regular), color: message.isUser ? Theme.paper : Theme.ink)
                .padding(.horizontal, 16)
                .padding(.vertical, 12)
                .background(message.isUser ? Theme.accent : Theme.paper)
                .clipShape(RoundedRectangle(cornerRadius: 18))
                .overlay(
                    RoundedRectangle(cornerRadius: 18)
                        .strokeBorder(Theme.ink, lineWidth: message.isUser ? 0 : 1.5)
                )
            
            if !message.isUser {
                Spacer(minLength: 40)
            }
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
