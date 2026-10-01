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
    private func sendToGemini() async throws -> String {
        let safeKey = Secrets.aiAPIKey.trimmingCharacters(in: .whitespacesAndNewlines)
            .addingPercentEncoding(withAllowedCharacters: .urlQueryAllowed) ?? Secrets.aiAPIKey
        let urlString = "https://generativelanguage.googleapis.com/v1beta/models/gemini-flash-latest:generateContent?key=\(safeKey)"
        guard let url = URL(string: urlString) else {
            throw AIError.apiError("Geçersiz URL: \(urlString)")
        }
        
        var request = URLRequest(url: url)
        request.httpMethod = "POST"
        request.addValue("application/json", forHTTPHeaderField: "Content-Type")
        
        // Gemini API beklentisine göre geçmişi dönüştür
        var geminiContents: [[String: Any]] = []
        
        // Sistem promptunu öne ekleyelim
        geminiContents.append([
            "role": "user",
            "parts": [["text": "SİSTEM TALİMATI: \(systemPrompt)"]]
        ])
        
        for msg in messageHistory where msg["role"] != "system" {
            let role = msg["role"] == "user" ? "user" : "model"
            let text = msg["content"] ?? ""
            geminiContents.append([
                "role": role,
                "parts": [["text": text]]
            ])
        }
        
        let body: [String: Any] = [
            "contents": geminiContents
        ]
        
        request.httpBody = try JSONSerialization.data(withJSONObject: body)
        
        let (data, response) = try await URLSession.shared.data(for: request)
        
        guard let httpResponse = response as? HTTPURLResponse, httpResponse.statusCode == 200 else {
            let errorText = String(data: data, encoding: .utf8) ?? ""
            throw AIError.apiError("Gemini Sunucu Hatası: \((response as? HTTPURLResponse)?.statusCode ?? 500). \(errorText)")
        }
        
        let json = try JSONSerialization.jsonObject(with: data) as? [String: Any]
        let candidates = json?["candidates"] as? [[String: Any]]
        let content = candidates?.first?["content"] as? [String: Any]
        let parts = content?["parts"] as? [[String: Any]]
        let text = parts?.first?["text"] as? String
        
        return text ?? "Cevap alınamadı."
    }
}
