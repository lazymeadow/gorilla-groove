import Foundation

class HttpRequester {
    
    static let baseUrl = "https://gorillagroove.net/api/"
    static let wsUrl = "wss://gorillagroove.net/api/socket"
    
    static let STATUS_ABORTED = 666
    
    typealias ResponseHandler<T> = (_ data: T?, _ status: Int, _ err: String?) -> Void
    
    @SettingsBundleStorage(key: "offline_mode_enabled")
    private static var offlineModeEnabled: Bool
    
    private static let logger = GGLogger(category: "network")
    
    static func get<T: Codable>(
        _ url: String,
        _ type: T.Type,
        callback: @escaping ResponseHandler<T>
    ) {
        let session = URLSession(configuration: .default)
        guard let request = getBaseRequest("GET", url) else {
            return callback(nil, STATUS_ABORTED, nil)
        }
        
        logger.debug("GET \(request.url!.absoluteString)")
        let dataTask = session.dataTask(with: request) { data, response, error in
            handleResponse(data, type, response, error, callback)
        }
        dataTask.resume()
    }
    
    static func getSync<T: Codable>(
        _ url: String,
        _ type: T.Type
    ) -> (T?, Int, String?) {
        let session = URLSession(configuration: .default)
        guard let request = getBaseRequest("GET", url) else {
            return (nil, STATUS_ABORTED, nil)
        }
        
        logger.debug("GET \(request.url!.absoluteString)")
        let semaphore = DispatchSemaphore(value: 0)
        var rval: (T?, Int, String?) = (nil, -1, nil)
                
        let dataTask = session.dataTask(with: request) { data, response, error in
            rval = handleResponse(data, type, response, error)
            semaphore.signal()
        }
        dataTask.resume()
        semaphore.wait()
        
        return rval
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
            callback?(nil, STATUS_ABORTED, nil)
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
        _ body: Encodable? = nil,
        callback: @escaping ResponseHandler<EmptyResponse>
    ) {
        let session = URLSession(configuration: .default)
        guard let request = getBaseRequest("DELETE", url, body: body) else {
            return callback(nil, STATUS_ABORTED, nil)
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
            callback?(nil, STATUS_ABORTED, nil)
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
            callback?(nil, STATUS_ABORTED, nil)
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
        _ urlPart: String,
        body: Encodable? = nil,
        authenticated: Bool = true,
        asMultipartData: Bool = false
    ) -> URLRequest? {
        
        if offlineModeEnabled {
            logger.debug("Offline mode is enabled. Not making http request to \(urlPart)")
            return nil
        }
        
        let encodedPart = urlPart.addingPercentEncoding(withAllowedCharacters: .urlQueryAllowed)!
        
        guard let url = URL(string: self.baseUrl + encodedPart) else {
            GGLog.critical("Could not create URL! \(self.baseUrl + encodedPart)")
            return nil
        }
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
        _ error: Error?
    ) -> (T?, Int, String?) {
        guard let httpResponse = response as? HTTPURLResponse else {
            logger.error("error: not a valid http response")
            return (nil, -1, error?.localizedDescription)
        }
        
        if (httpResponse.statusCode >= 300 || httpResponse.statusCode < 200) {
            let dataError = data?.toString() ?? ""
            logger.error("Non 2xx received! Code: \(httpResponse.statusCode). Error: \(dataError)")
            return (nil, httpResponse.statusCode, error as! String?)
        }
        
        // We aren't expecting the server to give us anything, so don't bother decoding
        if (type == EmptyResponse.self) {
            return (nil, httpResponse.statusCode, error as! String?)
        }
        
        let decoder = JSONDecoder()
        let dateFormatter = DateFormatter()
        dateFormatter.dateFormat = "yyyy-MM-dd'T'HH:mm:ss.SSSZ"
        decoder.dateDecodingStrategy = .formatted(dateFormatter)
        
        do {
            let decodedData = try decoder.decode(T.self, from: data!)
            return (decodedData, httpResponse.statusCode, error as! String?)
        } catch {
            let loggedData = String(data: data!, encoding: .utf8) ?? "--Unparsable--"
            let errorString = "Could not parse HTTP response into expected type \(type)! Response: \(loggedData)"
            logger.error("Parse exception: \(error.localizedDescription)")
            logger.critical(errorString)
            
            return (nil, httpResponse.statusCode, errorString)
        }
    }
    
    static private func handleResponse<T: Codable>(
        _ data: Data?,
        _ type: T.Type,
        _ response: URLResponse?,
        _ error: Error?,
        _ callback: ResponseHandler<T>?
    ) {
        let (parsedData, responseCode, error) = handleResponse(data, type, response, error)
        callback?(parsedData, responseCode, error)
    }
    
    static func download(
        _ stringUrl: String,
        downloadFinishedHandler: @escaping (_ outputUrl: URL?) -> Void
    ) {
        if offlineModeEnabled {
            logger.debug("Offline mode is enabled. Not making download request to \(stringUrl)")
            downloadFinishedHandler(nil)
            return
        }
        
        guard let url = URL(string: stringUrl) else {
            GGLog.error("Could not parse URL \(stringUrl)!")
            downloadFinishedHandler(nil)
            return
        }
        
        let downloadTask = URLSession.shared.downloadTask(with: url) { outputUrl, response, error in
            guard let httpResponse = response as? HTTPURLResponse else {
                logger.error("error: not a valid http response")
                downloadFinishedHandler(nil)
                return
            }
            
            if (httpResponse.statusCode >= 300 || httpResponse.statusCode < 200) {
                logger.error("Non 2xx received! Code: \(httpResponse.statusCode), \(error?.localizedDescription ?? "--no error--")")
                downloadFinishedHandler(nil)
                return
            }
            
            downloadFinishedHandler(outputUrl)
        }
        downloadTask.resume()
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
