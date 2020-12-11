import Foundation
import Combine

class WebSocketTaskConnection: NSObject, URLSessionWebSocketDelegate {
    private let logger = GGLogger(category: "socket")

    var webSocketTask: URLSessionWebSocketTask!
    let delegateQueue = OperationQueue()
    
    var onError: ((_ connection: WebSocketTaskConnection, _ message: String, _ error: NSError) -> Void)? = nil
    var onMessage: ((_ connection: WebSocketTaskConnection, _ string: String) -> Void)? = nil
    var onDataMessage: ((_ connection: WebSocketTaskConnection, _ string: Data) -> Void)? = nil

    init(_ url: String) {
        super.init()
        logger.info("WebSocket init with URL \(url)")

        onError = { _, message, error in
            self.logger.error("Got fatal error code \(error.code) from WebSocket message: \(message). Error: \(error.localizedDescription)")
            
            WebSocket.reset()
        }
        
        let token = FileState.read(LoginState.self)!.token
        
        var request = URLRequest(url: URL(string: url)!)
        request.addValue(token, forHTTPHeaderField: "Authorization")
        
        let session = URLSession(configuration: .default, delegate: self, delegateQueue: delegateQueue)
        webSocketTask = session.webSocketTask(with: request)
    }
    
    func urlSession(_ session: URLSession, webSocketTask: URLSessionWebSocketTask, didOpenWithProtocol protocol: String?) {
        logger.info("Websocket finished connecting")
    }
    
    func urlSession(_ session: URLSession, webSocketTask: URLSessionWebSocketTask, didCloseWith closeCode: URLSessionWebSocketTask.CloseCode, reason: Data?) {
        logger.info("Socket disconnected")
    }
    
    func connect() {
        logger.info("Connecting WebSocket...")
        webSocketTask.resume()
        
        listen()
    }
    
    func disconnect() {
        webSocketTask.cancel(with: .goingAway, reason: nil)
    }
    
    func listen()  {
        webSocketTask.receive { result in
            self.logger.info("Received websocket message")
            switch result {
            case .failure(let error):
                self.onError?(self, "", error as NSError)
            case .success(let message):
                switch message {
                case .string(let text):
                    self.onMessage?(self, text)
                case .data(let data):
                    self.onDataMessage?(self, data)
                @unknown default:
                    fatalError()
                }
                
                self.listen()
            }
        }
    }
    
    func send(_ text: String, _ retry: Int = 1) {
        logger.info("WebSocket Send (try \(retry) - \(text)")
        webSocketTask.send(URLSessionWebSocketTask.Message.string(text)) { error in
            if let error = error as NSError? {
                if error.code == 53 || error.code == 57 || error.code == 89 {
                    self.logger.info("Websocket was disconnected. About to reconnect")
                    WebSocket.reset()
                    
                    if retry < 3 {
                        sleep(1)
                        self.send(text, retry + 1)
                    } else {
                        self.logger.error("Failed to send WebSocket message \(text) after retry limit was reached")
                    }
                } else {
                    self.onError?(self, text, error)
                }
            }
        }
    }
    
    func send(_ data: Data) {
        webSocketTask.send(URLSessionWebSocketTask.Message.data(data)) { error in
            if let error = error {
                self.onError?(self, data.toString(), error as NSError)
            }
        }
    }
}

class WebSocket {
    static private var socket: WebSocketTaskConnection? = nil
    
    @SettingsBundleStorage(key: "offline_mode_enabled")
    private static var offlineModeEnabled: Bool
    
    static func sendMessage(_ message: Encodable) {
        let messageJson = message.toJSONData()!.toString()
        socket?.send(messageJson)
    }
    
    static func connect() {
        if socket != nil {
            return
        }
        if offlineModeEnabled {
            return
        }
        
        socket = WebSocketTaskConnection(HttpRequester.wsUrl)
        socket?.connect()
    }
    
    static func disconnect() {
        if socket != nil {
            socket!.disconnect()
            socket = nil
        }
    }
    
    static func reset() {
        socket = nil
        connect()
    }
}
