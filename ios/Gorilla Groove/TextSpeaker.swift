import Foundation
import AVFoundation

class TextSpeaker: NSObject {
    private static let synth = AVSpeechSynthesizer()
    
    private static let delegate = TextSpeaker()
    
    // You can get a list of voices with AVSpeechSynthesisVoice.speechVoices()
    @SettingsBundleStorage(key: "text_speech_voice_identifier")
    private static var voiceIdentifier: String
    
    @SettingsBundleStorage(key: "text_speech_voice_enabled")
    private static var voiceEnabled: Bool
    
    override init() {
        super.init()
    }
    
    private static var onFinishFun: (() -> Void)? = nil
    static func speak(_ string: String, onFinish: (() -> Void)? = nil) {
        // I noticed a delay on first time startup of this. May as well just run it on a background thread as there's no reason not to.
        DispatchQueue.global().async {
            if !voiceEnabled {
                onFinish?()
                return
            }
            
            if synth.delegate == nil {
                synth.delegate = delegate
            }
            onFinishFun = onFinish
            
            let speechUtterance = AVSpeechUtterance(string: string)
            speechUtterance.rate = AVSpeechUtteranceMaximumSpeechRate / 2.2
            speechUtterance.voice = AVSpeechSynthesisVoice(identifier: voiceIdentifier)
            synth.speak(speechUtterance)
            
            GGLog.info("Speaking the line: '\(string)'")
        }
    }
}

extension TextSpeaker: AVSpeechSynthesizerDelegate {
    func speechSynthesizer(_ synthesizer: AVSpeechSynthesizer, didFinish utterance: AVSpeechUtterance) {
        GGLog.debug("Voice line finished speaking")
        TextSpeaker.onFinishFun?()
    }
}
