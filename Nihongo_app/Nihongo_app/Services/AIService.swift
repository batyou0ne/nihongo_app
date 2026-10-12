import Foundation

enum AIError: LocalizedError {
    case invalidURL
    case invalidResponse
    case apiError(String)
    
    var errorDescription: String? {
        switch self {
        case .invalidURL: return "Geçersiz URL."
        case .invalidResponse: return "Sunucudan geçersiz bir yanıt geldi."
        case .apiError(let message): return message
        }
    }
}

final class AIService {
    static let shared = AIService()
    
    private let systemPrompt = """
    Sen bir Japonca dil partnerisin. Görevin öğrencinin Japonca pratik yapmasına yardımcı olmak.
    Kullanıcı sana Japonca veya Türkçe yazabilir. 
    Sen her zaman DOĞAL ve GÜNLÜK JAPONCA (Desu/Masu formu, N5/N4 seviyesine uygun) ile cevap vermelisin.
    Ayrıca her Japonca cümlenin yanına mutlaka parantez içinde (Türkçe Çevirisini) yazmalısın.
    Kullanıcı hatalı bir Japonca kurarsa, nazikçe düzelt.
    Çok uzun cevaplar verme, kısa ve sohbeti devam ettirecek sorular sor.
    Örnek Cevap Formatı:
    こんにちは！お元気ですか？ (Merhaba! Nasılsın?)
    """
    
    // Mesaj geçmişini tutmak (basit bir hafıza)
    private var messageHistory: [[String: String]] = []
    
    private init() {
        // Sistemi başlatırken system promptunu ekle
        messageHistory.append(["role": "system", "content": systemPrompt])
    }
    
    func sendMessage(_ userText: String) async throws -> String {
        guard !Secrets.aiAPIKey.isEmpty, Secrets.aiAPIKey != "API_KEY_BURAYA_GELECEK" else {
            throw AIError.apiError("Lütfen Config/Secrets.swift dosyasına API anahtarını girin.")
        }
        
        messageHistory.append(["role": "user", "content": userText])
        
        do {
            let response: String
            if Secrets.aiProvider.lowercased() == "gemini" {
                response = try await sendToGemini()
            } else {
                response = try await sendToOpenAI()
            }
            messageHistory.append(["role": "assistant", "content": response])
            return response
        } catch {
            // Başarısız olursa son mesajı geçmişten sil
            messageHistory.removeLast()
            throw error
        }
    }
    
    // MARK: - OpenAI Implementation
    private func sendToOpenAI() async throws -> String {
        guard let url = URL(string: "https://api.openai.com/v1/chat/completions") else {
            throw AIError.invalidURL
        }
        
        var request = URLRequest(url: url)
        request.httpMethod = "POST"
        let safeKey = Secrets.aiAPIKey.trimmingCharacters(in: .whitespacesAndNewlines)
        request.addValue("Bearer \(safeKey)", forHTTPHeaderField: "Authorization")
        request.addValue("application/json", forHTTPHeaderField: "Content-Type")
        
        let body: [String: Any] = [
            "model": "gpt-4o-mini", // Veya gpt-3.5-turbo
            "messages": messageHistory,
            "temperature": 0.7
        ]
        
        request.httpBody = try JSONSerialization.data(withJSONObject: body)
        
        let (data, response) = try await URLSession.shared.data(for: request)
        
        guard let httpResponse = response as? HTTPURLResponse, httpResponse.statusCode == 200 else {
            throw AIError.apiError("OpenAI Sunucu Hatası: \((response as? HTTPURLResponse)?.statusCode ?? 500)")
        }
        
        let json = try JSONSerialization.jsonObject(with: data) as? [String: Any]
        let choices = json?["choices"] as? [[String: Any]]
        let message = choices?.first?["message"] as? [String: Any]
        let content = message?["content"] as? String
        
        return content ?? "Cevap alınamadı."
    }
    
    // MARK: - Gemini Implementation
    private let geminiModels = [
        "gemini-3.5-flash-lite",
        "gemini-3.5-flash",
        "gemini-3.8-flash",
        "gemini-flash-latest"
    ]
    
    func resetConversation() {
        messageHistory = [["role": "system", "content": systemPrompt]]
    }
    
    private func sendToGemini() async throws -> String {
        let safeKey = Secrets.aiAPIKey.trimmingCharacters(in: .whitespacesAndNewlines)
            .addingPercentEncoding(withAllowedCharacters: .urlQueryAllowed) ?? Secrets.aiAPIKey
        
        // Gemini API beklentisine göre geçmişi dönüştür (sadece user ve model)
        var geminiContents: [[String: Any]] = []
        for msg in messageHistory where msg["role"] != "system" {
            let role = msg["role"] == "user" ? "user" : "model"
            let text = msg["content"] ?? ""
            geminiContents.append([
                "role": role,
                "parts": [["text": text]]
            ])
        }
        
        let body: [String: Any] = [
            "systemInstruction": [
                "parts": [["text": systemPrompt]]
            ],
            "contents": geminiContents
        ]
        
        let bodyData = try JSONSerialization.data(withJSONObject: body)
        var lastError: Error?
        var lastStatusCode: Int = 0
        var lastErrorMessage: String = ""
        
        // Model fallback zincirini sırayla dene (örn. 503 veya 429 gelirse diğer modele geç)
        for (index, model) in geminiModels.enumerated() {
            let urlString = "https://generativelanguage.googleapis.com/v1beta/models/\(model):generateContent?key=\(safeKey)"
            guard let url = URL(string: urlString) else { continue }
            
            var request = URLRequest(url: url)
            request.httpMethod = "POST"
            request.addValue("application/json", forHTTPHeaderField: "Content-Type")
            request.timeoutInterval = 25
            request.httpBody = bodyData
            
            do {
                let (data, response) = try await URLSession.shared.data(for: request)
                guard let httpResponse = response as? HTTPURLResponse else { continue }
                
                if httpResponse.statusCode == 200 {
                    let json = try JSONSerialization.jsonObject(with: data) as? [String: Any]
                    let candidates = json?["candidates"] as? [[String: Any]]
                    let content = candidates?.first?["content"] as? [String: Any]
                    let parts = content?["parts"] as? [[String: Any]]
                    if let text = parts?.first?["text"] as? String, !text.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty {
                        return text
                    }
                }
                
                lastStatusCode = httpResponse.statusCode
                if let json = try? JSONSerialization.jsonObject(with: data) as? [String: Any],
                   let errorObj = json["error"] as? [String: Any],
                   let msg = errorObj["message"] as? String {
                    lastErrorMessage = msg
                } else {
                    lastErrorMessage = String(data: data, encoding: .utf8) ?? ""
                }
                
                // 503 (yoğunluk), 429 (oran aşımı) veya 404 (model sürümü kapalı) durumunda diğer modele geç
                if (httpResponse.statusCode == 503 || httpResponse.statusCode == 429 || httpResponse.statusCode == 404) && index < geminiModels.count - 1 {
                    try? await Task.sleep(nanoseconds: 600_000_000)
                    continue
                }
            } catch {
                lastError = error
                if index < geminiModels.count - 1 {
                    continue
                }
            }
        }
        
        // Tüm modeller başarısız olduysa kullanıcı dostu Türkçe hata döndür
        if lastStatusCode == 503 {
            throw AIError.apiError("Gemini sunucularında şu anda yoğunluk yaşanıyor. Lütfen birkaç saniye sonra tekrar deneyin.")
        } else if lastStatusCode == 429 {
            throw AIError.apiError("İstek limiti aşıldı. Lütfen bir süre bekleyip tekrar deneyin.")
        } else if lastStatusCode == 401 || lastStatusCode == 403 {
            throw AIError.apiError("Gemini API anahtarı geçersiz veya yetkisiz.")
        } else if !lastErrorMessage.isEmpty {
            throw AIError.apiError("Gemini yanıt veremedi: \(lastErrorMessage)")
        } else if let error = lastError {
            throw error
        } else {
            throw AIError.apiError("Sunucuya ulaşılamadı. Lütfen internet bağlantınızı kontrol edip tekrar deneyin.")
        }
    }
}
