package com.example.groove

import com.example.groove.service.UserService
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RestController

@RestController
class TestController(@Autowired val userService: UserService) {

    @Transactional
    @GetMapping("/sup")
    fun getSup(): String {
        userService.goodThings()
        return "hello"
    }

//    @GetMapping("/sup")
//    fun getSup2(): String {
//        return "hello"
//    }

}