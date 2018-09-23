package com.example.groove

import com.example.groove.service.FFmpegService
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("track")
class TrackController(@Autowired val fFmpegService: FFmpegService) {

    @Transactional
    @GetMapping("/convert")
    fun getSup(): String {
        fFmpegService.test()
        return "donezo"
    }

}