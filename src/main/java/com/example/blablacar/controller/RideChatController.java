package com.example.blablacar.controller;

import org.springframework.messaging.Message;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import java.security.Principal;
import java.time.Instant;
import java.util.Map;

@Controller
public class RideChatController {


    private final SimpMessagingTemplate template;

    public RideChatController(SimpMessagingTemplate template) {
        this.template = template;
    }


    @MessageMapping("/sendMessage")
    @SendTo("/queue/user.{userId}")
    public Message send(Message message) {
        return message;
    }
}
