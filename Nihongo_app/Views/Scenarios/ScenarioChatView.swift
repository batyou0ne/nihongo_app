import SwiftUI

struct ScenarioChatView: View {
    let scenario: ConversationScenario
    
    @Environment(\.dismiss) private var dismiss
    
    @State private var currentStepIndex: Int = 0
    @State private var chatHistory: [ChatMessageItem] = []
    
    @State private var selectedOptionID: String?
    @State private var showFeedback: Bool = false
    @State private var isBotTyping: Bool = false
    
    struct ChatMessageItem: Identifiable {
        let id = UUID()
        let isBot: Bool
        let text: String
        let translation: String?
        let isSuccessFeedback: Bool?
    }
    
    var body: some View {
        VStack(spacing: 0) {
            ScrollViewReader { proxy in
                ScrollView {
                    VStack(spacing: 16) {
                        ForEach(chatHistory) { msg in
                            chatBubble(msg)
                                .id(msg.id)
                        }
                    }
                    .padding()
                    .padding(.bottom, 20)
                }
                .scrollIndicators(.hidden)
                .onChange(of: chatHistory.count) { _, _ in
                    if let lastId = chatHistory.last?.id {
                        withAnimation {
                            proxy.scrollTo(lastId, anchor: .bottom)
                        }
                    }
                }
            }
            .background(Theme.paper)
            
            if currentStepIndex < scenario.steps.count {
                if isBotTyping {
                    typingIndicator
                } else {
                    optionsView(step: scenario.steps[currentStepIndex])
                }
            } else {
                completionView
            }
        }
        .navigationTitle(scenario.title)
        .navigationBarTitleDisplayMode(.inline)
        .toolbar {
            ToolbarItem(placement: .topBarLeading) {
                Button {
                    dismiss()
                } label: {
                    Image(systemName: "xmark.circle.fill")
                        .foregroundStyle(Theme.secondaryInk)
                        .font(.title3)
                }
            }
        }
        .onAppear {
            if chatHistory.isEmpty {
                startStep(index: 0)
            }
        }
    }
    
    private func chatBubble(_ msg: ChatMessageItem) -> some View {
        HStack {
            if msg.isBot {
                Image(systemName: "robot")
                    .font(.title2)
                    .foregroundStyle(Theme.accent)
                    .frame(width: 40, height: 40)
                    .background(Theme.accent.opacity(0.15))
                    .clipShape(Circle())
            } else {
                Spacer(minLength: 40)
            }
            
            VStack(alignment: msg.isBot ? .leading : .trailing, spacing: 4) {
                FuriganaText(text: msg.text, font: .system(size: 16, weight: .medium), color: msg.isBot ? Theme.ink : Theme.paper)
                
                if let trans = msg.translation {
                    Text(trans)
                        .font(.caption)
                        .foregroundStyle(msg.isBot ? Theme.secondaryInk : Theme.paper.opacity(0.8))
                }
                
                if let isSuccess = msg.isSuccessFeedback {
                    if isSuccess {
                        Text("Doğru!")
                            .font(.caption.weight(.bold))
                            .foregroundStyle(.green)
                    } else {
                        Text("Yanlış, tekrar dene!")
                            .font(.caption.weight(.bold))
                            .foregroundStyle(.red)
                    }
                }
            }
            .padding(12)
            .background(
                RoundedRectangle(cornerRadius: 16)
                    .fill(msg.isBot ? Theme.paper : Theme.ink)
            )
            .overlay(
                RoundedRectangle(cornerRadius: 16)
                    .strokeBorder(msg.isBot ? Theme.ink.opacity(0.2) : .clear, lineWidth: 1)
            )
            
            if msg.isBot {
                Spacer(minLength: 40)
            } else {
                Image(systemName: "person.fill")
                    .font(.title2)
                    .foregroundStyle(Theme.ink)
                    .frame(width: 40, height: 40)
                    .background(Theme.ink.opacity(0.15))
                    .clipShape(Circle())
            }
        }
    }
    
    private var typingIndicator: some View {
        HStack(spacing: 8) {
            Circle().fill(Theme.secondaryInk).frame(width: 8, height: 8)
            Circle().fill(Theme.secondaryInk).frame(width: 8, height: 8).opacity(0.7)
            Circle().fill(Theme.secondaryInk).frame(width: 8, height: 8).opacity(0.4)
            Text("Yazıyor...")
                .font(.caption)
                .foregroundStyle(Theme.secondaryInk)
        }
        .padding()
        .frame(maxWidth: .infinity, alignment: .leading)
        .padding(.horizontal)
    }
    
    private func optionsView(step: ScenarioStep) -> some View {
        VStack(spacing: 12) {
            Text("Nasıl cevap verirsin?")
                .font(.subheadline.weight(.bold))
                .foregroundStyle(Theme.secondaryInk)
                .frame(maxWidth: .infinity, alignment: .leading)
            
            ForEach(step.options) { option in
                let isSelected = selectedOptionID == option.id
                let isWrong = isSelected && showFeedback
                
                Button {
                    handleOptionSelection(option, in: step)
                } label: {
                    HStack {
                        VStack(alignment: .leading, spacing: 2) {
                            FuriganaText(text: option.japanese, font: .system(size: 16, weight: .bold), color: (isWrong || isSelected) ? .white : Theme.ink)
                        }
                        Spacer()
                    }
                    .padding(14)
                    .frame(maxWidth: .infinity)
                    .background(isWrong ? .red : (isSelected ? Theme.accent : Theme.paper))
                    .foregroundStyle(isWrong || isSelected ? .white : Theme.ink)
                    .overlay(
                        RoundedRectangle(cornerRadius: 12)
                            .strokeBorder(isWrong ? .red : (isSelected ? Theme.accent : Theme.ink), lineWidth: 1.5)
                    )
                    .clipShape(RoundedRectangle(cornerRadius: 12))
                }
                .disabled(showFeedback && isSelected)
            }
        }
        .padding()
        .background(
            UnevenRoundedRectangle(topLeadingRadius: 24, bottomLeadingRadius: 0, bottomTrailingRadius: 0, topTrailingRadius: 24)
                .fill(Theme.paper)
                .shadow(color: Theme.ink.opacity(0.1), radius: 10, y: -5)
        )
    }
    
    private var completionView: some View {
        VStack(spacing: 16) {
            Text("🎉")
                .font(.system(size: 44))
            Text("Tebrikler!")
                .font(Theme.display(24))
                .foregroundStyle(Theme.ink)
            Text("Bu konuşma senaryosunu başarıyla tamamladın.")
                .font(.subheadline)
                .foregroundStyle(Theme.secondaryInk)
                .multilineTextAlignment(.center)
            
            Button {
                dismiss()
            } label: {
                Text("Bitir")
                    .font(.system(size: 17, weight: .bold))
                    .foregroundStyle(Theme.paper)
                    .frame(maxWidth: .infinity)
                    .padding()
                    .background(Theme.ink)
                    .clipShape(RoundedRectangle(cornerRadius: 12))
            }
            .padding(.top, 8)
        }
        .padding(24)
        .background(
            UnevenRoundedRectangle(topLeadingRadius: 24, bottomLeadingRadius: 0, bottomTrailingRadius: 0, topTrailingRadius: 24)
                .fill(Theme.paper)
                .shadow(color: Theme.ink.opacity(0.1), radius: 10, y: -5)
        )
    }
    
    private func startStep(index: Int) {
        guard index < scenario.steps.count else { return }
        
        isBotTyping = true
        
        // Simulate typing delay based on message length
        DispatchQueue.main.asyncAfter(deadline: .now() + 1.2) {
            let step = scenario.steps[index]
            
            // Add bot message to history
            chatHistory.append(ChatMessageItem(
                isBot: true,
                text: step.botMessage.japanese,
                translation: step.botMessage.turkish,
                isSuccessFeedback: nil
            ))
            
            isBotTyping = false
        }
    }
    
    private func handleOptionSelection(_ option: ScenarioOption, in step: ScenarioStep) {
        selectedOptionID = option.id
        showFeedback = true
        
        if option.isCorrect {
            FeedbackManager.shared.playSuccess()
            
            // Add user message to history
            chatHistory.append(ChatMessageItem(
                isBot: false,
                text: option.japanese,
                translation: option.turkish,
                isSuccessFeedback: true
            ))
            
            // Wait for 1 second to show success color, then start next step (which shows typing indicator)
            DispatchQueue.main.asyncAfter(deadline: .now() + 0.8) {
                selectedOptionID = nil
                showFeedback = false
                currentStepIndex += 1
                startStep(index: currentStepIndex)
            }
        } else {
            FeedbackManager.shared.playError()
            
            // Just shake or show red.
            // Reset after 1 second so they can try again.
            DispatchQueue.main.asyncAfter(deadline: .now() + 1.0) {
                selectedOptionID = nil
                showFeedback = false
            }
        }
    }
}
