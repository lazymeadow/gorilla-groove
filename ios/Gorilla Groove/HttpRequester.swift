import Foundation
import os

class HttpRequester {
    
    static let baseUrl = "https://gorillagroove.net/api/"
    static let wsUrl = "wss://gorillagroove.net/api/socket"
    
    typealias ResponseHandler<T> = (_ data: T?, _ status: Int, _ err: String?) -> Void
    
    static let logger = Logger(subsystem: Bundle.main.bundleIdentifier!, category: "network")
    
    static func get<T: Codable>(
        _ url: String,
        _ type: T.Type,
        callback: @escaping ResponseHandler<T>
    ) {
        let session = URLSession(configuration: .default)
        guard let request = getBaseRequest("GET", url) else {
            return
        }
        
        logger.debug("GET \(request.url!.absoluteString)")
        let dataTask = session.dataTask(with: request) { data, response, error in
            handleResponse(data, type, response, error, callback)
        }
        dataTask.resume()
    }
    
    static func put<T: Codable>(
        _ url: String,
        _ type: T.Type,
        _ body: Encodable?,
        asMultipartData: Bool = false,
        callback: ResponseHandler<T>? = nil
    ) {
        let session = URLSession(configuration: .default)
        guard let request = getBaseRequest("PUT", url, body: body, asMultipartData: asMultipartData) else {
            return
        }
        
        logger.debug("PUT \(request.url!.absoluteString)")
        let dataTask = session.dataTask(with: request) { data, response, error in
            handleResponse(data, type, response, error, callback)
        }
        dataTask.resume()
    }
    
    static func delete(
        _ url: String,
        _ body: Encodable?,
        callback: @escaping ResponseHandler<EmptyResponse>
    ) {
        let session = URLSession(configuration: .default)
        guard let request = getBaseRequest("DELETE", url, body: body) else {
            return
        }
        
        logger.debug("DELETE \(request.url!.absoluteString)")
        let dataTask = session.dataTask(with: request) { data, response, error in
            handleResponse(data, EmptyResponse.self, response, error, callback)
        }
        dataTask.resume()
    }
    
    static func post<T: Codable>(
        _ url: String,
        _ type: T.Type,
        _ body: Encodable?,
        asMultipartData: Bool = false,
        authenticated: Bool = true,
        callback: ResponseHandler<T>? = nil
    ) {
        let session = URLSession(configuration: .default)
        guard let request = getBaseRequest("POST", url, body: body, authenticated: authenticated, asMultipartData: asMultipartData) else {
            return
        }
        
        logger.debug("POST (upload) \(request.url!.absoluteString)")
        let dataTask = session.dataTask(with: request) { data, response, error in
            handleResponse(data, type, response, error, callback)
        }
        dataTask.resume()
    }
    
    static func upload<T: Codable>(
        _ url: String,
        _ type: T.Type,
        _ body: Data,
        authenticated: Bool = true,
        callback: ResponseHandler<T>? = nil
    ) {
        let session = URLSession(configuration: .default)
        guard let request = getBaseRequest("POST", url, body: body, authenticated: authenticated, asMultipartData: true) else {
            return
        }
        
        logger.debug("POST \(request.url!.absoluteString)")
        let dataTask = session.dataTask(with: request) { data, response, error in
            handleResponse(data, type, response, error, callback)
        }
        dataTask.resume()
    }
    
    static private func getBaseRequest(
        _ method: String,
        _ url: String,
        body: Encodable? = nil,
        authenticated: Bool = true,
        asMultipartData: Bool = false
    ) -> URLRequest? {
        
        let url = URL(string: self.baseUrl + url)!
        var request : URLRequest = URLRequest(url: url)
        
        if authenticated {
            if UserState.isLoggedIn {
                let token = FileState.read(LoginState.self)!.token
                request.setValue(token, forHTTPHeaderField: "Authorization")
            } else {
                logger.warning("An authenticated request was started to URL '\(url)' while the user was not logged in. This is likely a race condition with logging out, and this request will be aborted")
                return nil
            }
        }
        
        request.httpMethod = method
                
        if let body = body {
            if asMultipartData {
                let boundary = UUID().uuidString
                request.addValue("multipart/form-data; boundary=\(boundary)", forHTTPHeaderField: "Content-Type")
                request.httpBody = body.toMultipartFormData(boundary: boundary)
            } else {
                request.addValue("application/json", forHTTPHeaderField: "Content-Type")
                request.httpBody = body.toJSONData()
            }
        }
        
        return request
    }
    
    static private func handleResponse<T: Codable>(
        _ data: Data?,
        _ type: T.Type,
        _ response: URLResponse?,
        _ error: Error?,
        _ callback: ResponseHandler<T>?
    ) {
        guard let httpResponse = response as? HTTPURLResponse else {
            logger.error("error: not a valid http response")
            return
        }
        
        if (httpResponse.statusCode >= 300) {
            let dataError = data?.toString() ?? ""
            logger.error("Non 2xx received! Code: \(httpResponse.statusCode). Error: \(dataError)")
            callback?(nil, httpResponse.statusCode, error as! String?)
            return
        }
        
        // We aren't expecting the server to give us anything, so don't bother decoding
        if (type == EmptyResponse.self) {
            callback?(nil, httpResponse.statusCode, error as! String?)
            return
        }
        
        let decoder = JSONDecoder()
        let dateFormatter = DateFormatter()
        dateFormatter.dateFormat = "yyyy-MM-dd'T'HH:mm:ss.SSSZ"
        decoder.dateDecodingStrategy = .formatted(dateFormatter)
        
        let decodedData = try! decoder.decode(T.self, from: data!)
        
        callback?(decodedData, httpResponse.statusCode, error as! String?)
    }
}

extension Encodable {
    func toJSONData() -> Data? {
        return try! JSONEncoder().encode(self)
    }
    
    // This currently is hard coded for the crash log. I don't THINK the "filename" matters. But these parts could be parameterized
    func toMultipartFormData(boundary: String) -> Data? {
        var formData = Data()
        formData.append("\r\n--\(boundary)\r\n".data(using: .utf8)!)
        formData.append("Content-Disposition: form-data; name=\"file\"; filename=\"crashlog.zip\"\r\n\r\n".data(using: .utf8)!)
        formData.append(self as! Data)
        formData.append("\r\n--\(boundary)--\r\n".data(using: .utf8)!)
        
        return formData
    }
}

extension Int {
    func isSuccessful() -> Bool {
        return self >= 200 && self < 300
    }
}

struct EmptyResponse: Codable { }

extension Data {
    func toString() -> String {
        return String(decoding: self, as: UTF8.self)
    }
}
