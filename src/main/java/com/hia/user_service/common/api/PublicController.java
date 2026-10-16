package com.hia.user_service.common.api;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/public")
public class PublicController {
//    공개 HTTP API Controller

    @GetMapping("/ping")
    public PingResponse ping(){
        return new PingResponse("ok");
    }

    public record PingResponse(String status) {
    }
}
