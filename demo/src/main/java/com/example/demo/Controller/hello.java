package com.example.demo.Controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
@Slf4j
@RestController
public class hello {
    @RestController
    public class HelloController {

        @GetMapping("/hello")
        public String hello() {
            log.info("Hello, World!");
            return "Hello, World!";
        }
    }
}
