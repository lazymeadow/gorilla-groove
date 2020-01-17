//
//  AudioPlayer.swift
//  Gorilla Groove
//
//  Created by mobius-mac on 1/16/20.
//  Copyright © 2020 mobius-mac. All rights reserved.
//

import AVFoundation
import Foundation

class AudioPlayer {
    
    static let shared = AudioPlayer()
    
    let player = AVPlayer()
    
    private init() { }
}
