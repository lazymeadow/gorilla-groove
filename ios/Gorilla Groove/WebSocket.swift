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
        logger.debug("WebSocket init with URL \(url)")

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
        logger.debug("Websocket finished connecting")
    }
    
    func urlSession(_ session: URLSession, webSocketTask: URLSessionWebSocketTask, didCloseWith closeCode: URLSessionWebSocketTask.CloseCode, reason: Data?) {
        logger.debug("Socket disconnected")
    }
    
    func connect() {
        logger.debug("Connecting WebSocket...")
        webSocketTask.resume()
        
        listen()
    }
    
    func disconnect() {
        webSocketTask.cancel(with: .goingAway, reason: nil)
    }
    
    func listen()  {
        webSocketTask.receive { [weak self] result in
            guard let this = self else { return }
            this.logger.debug("Received websocket message")
            
            switch result {
            case .failure(let error):
                this.onError?(this, "", error as NSError)
            case .success(let message):
                switch message {
                case .string(let text):
                    this.onMessage?(this, text)
                case .data(let data):
                    this.onDataMessage?(this, data)
                @unknown default:
                    fatalError()
                }
                
                this.listen()
            }
        }
    }
    
    func send(_ text: String, _ retry: Int = 1) {
        logger.debug("WebSocket Send (try \(retry) - \(text)")
        webSocketTask.send(URLSessionWebSocketTask.Message.string(text)) { [weak self] error in
            guard let this = self else { return }

            if let error = error as NSError? {
                if error.code == 53 || error.code == 57 || error.code == 89 {
                    this.logger.info("Websocket was disconnected. About to reconnect")
                    WebSocket.reset()
                    
                    if retry < 3 {
                        sleep(1)
                        this.send(text, retry + 1)
                    } else {
                        this.logger.error("Failed to send WebSocket message \(text) after retry limit was reached")
                    }
                } else {
                    this.onError?(this, text, error)
                }
            }
        }
    }
    
    func send(_ data: Data) {
        webSocketTask.send(URLSessionWebSocketTask.Message.data(data)) { [weak self] error in
            guard let this = self else { return }

            if let error = error {
                this.onError?(this, data.toString(), error as NSError)
            }
        }
    }
}

class WebSocket {
    static private var socket: WebSocketTaskConnection? = nil
    
    private static let this = WebSocket()
    
    private init() {}
    
    static func sendMessage(_ message: Encodable) {
        let messageJson = message.toJSONData()!.toString()
        socket?.send(messageJson)
    }
    
    static func connect() {
        synchronized(this) {
            if socket != nil {
                return
            }
            if OfflineStorageService.offlineModeEnabled {
                return
            }
            
            socket = WebSocketTaskConnection(HttpRequester.wsUrl)
        }

        socket?.connect()
    }
    
    static func disconnect() {
        synchronized(this) {
            if let liveSocket = socket {
                liveSocket.disconnect()
                socket = nil
            }
        }
    }
    
    static func reset() {
        synchronized(this) {
            socket = nil
        }
        
        connect()
    }
}
