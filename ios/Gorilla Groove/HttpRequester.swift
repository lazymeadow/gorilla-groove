import Foundation

class HttpRequester {
    
    static let baseUrl = "https://gorillagroove.net/api/"
    static let wsUrl = "wss://gorillagroove.net/api/socket"
    
    typealias ResponseHandler<T> = (_ data: T?, _ status: Int, _ err: String?) -> Void
    
    static func get<T: Codable>(
        _ url: String,
        _ type: T.Type,
        callback: @escaping ResponseHandler<T>
    ) {
        let session = URLSession(configuration: .default)
        guard let request = getBaseRequest("GET", url) else {
            return
        }
        
        print("Network - GET " + request.url!.absoluteString)
        let dataTask = session.dataTask(with: request) { data, response, error in
            handleResponse(data, type, response, error, callback)
        }
        dataTask.resume()
    }
    
    static func put<T: Codable>(
        _ url: String,
        _ type: T.Type,
        _ body: Codable?,
        asMultipartData: Bool = false,
        callback: ResponseHandler<T>? = nil
    ) {
        let session = URLSession(configuration: .default)
        guard let request = getBaseRequest("PUT", url, body: body, asMultipartData: asMultipartData) else {
            return
        }
        
        print("Network - PUT " + request.url!.absoluteString)
        let dataTask = session.dataTask(with: request) { data, response, error in
            handleResponse(data, type, response, error, callback)
        }
        dataTask.resume()
    }
    
    static func delete(
        _ url: String,
        _ body: Codable?,
        callback: @escaping ResponseHandler<EmptyResponse>
    ) {
        let session = URLSession(configuration: .default)
        guard let request = getBaseRequest("DELETE", url, body: body) else {
            return
        }
        
        print("Network - DELETE " + request.url!.absoluteString)
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
        
        print("Network - POST " + request.url!.absoluteString)
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
                print("An authenticated request was started to URL '\(url)' while the user was not logged in. This is likely a race condition with logging out, and this request will be aborted")
                return nil
            }
        }
        
        request.httpMethod = method
                
        if let body = body {
            if asMultipartData {
                let boundary = UUID().uuidString
                request.addValue("multipart/form-data; boundary=\(boundary)", forHTTPHeaderField: "Content-Type")
                request.httpBody = body.toMultipartFormData(boundary: boundary)
                
                print(String(decoding: request.httpBody!, as: UTF8.self))
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
        guard let httpResponse = response as? HTTPURLResponse
            else {
                print("error: not a valid http response")
                return
        }
        
        if (httpResponse.statusCode >= 300) {
            let dataError = data?.toString() ?? ""
            print("Non 2xx received! Code: \(httpResponse.statusCode). Error: \(dataError)")
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
    
    // This specifically targets the UpdateTrack request, which is a combination of json and image binary data.
    // This does not yet have support for the image binary data.
    // As the need arises, I will update this extension to be more generic and support said binary data
    func toMultipartFormData(boundary: String) -> Data? {
        var formData = Data()
        formData.append("\r\n--\(boundary)\r\n".data(using: .utf8)!)
        formData.append("Content-Disposition: form-data; name=\"updateTrackJson\"\r\n\r\n".data(using: .utf8)!)
        formData.append(self.toJSONData()!)
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
