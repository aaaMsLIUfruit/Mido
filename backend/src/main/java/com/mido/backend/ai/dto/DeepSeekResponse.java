package com.mido.backend.ai.dto;

import java.util.List;
import lombok.Data;

@Data
public class DeepSeekResponse {

    private List<Choice> choices;

    @Data
    public static class Choice {
        private Message message;
    }

    @Data
    public static class Message {
        private String role;
        private String content;
    }
}

