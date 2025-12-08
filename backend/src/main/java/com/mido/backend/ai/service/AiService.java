package com.mido.backend.ai.service;

import com.mido.backend.ai.dto.AiMessageDto;
import com.mido.backend.ai.dto.DeepSeekResponse;
import com.mido.backend.config.AiProperties;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.util.UriComponentsBuilder;

@Service
@RequiredArgsConstructor
public class AiService {

    private final AiProperties aiProperties;
    private final RestTemplate restTemplate = new RestTemplate();

    public String chat(List<AiMessageDto> messages) {
        if (!StringUtils.hasText(aiProperties.getApiKey())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "未配置 AI 密钥，无法调用");
        }
        if (messages == null || messages.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "消息内容不能为空");
        }
        String url = UriComponentsBuilder.fromHttpUrl(aiProperties.getBaseUrl())
                .path(aiProperties.getChatPath())
                .toUriString();

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(aiProperties.getApiKey());

        Map<String, Object> payload = new HashMap<>();
        payload.put("model", aiProperties.getModel());
        payload.put("messages", messages);
        payload.put("temperature", aiProperties.getTemperature());

        try {
            ResponseEntity<DeepSeekResponse> response =
                    restTemplate.postForEntity(url, new HttpEntity<>(payload, headers), DeepSeekResponse.class);
            DeepSeekResponse body = response.getBody();
            String content = body != null
                    ? body.getChoices()
                            .stream()
                            .map(DeepSeekResponse.Choice::getMessage)
                            .filter(message -> message != null && StringUtils.hasText(message.getContent()))
                            .map(DeepSeekResponse.Message::getContent)
                            .findFirst()
                            .orElse(null)
                    : null;
            if (!StringUtils.hasText(content)) {
                throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "AI 未返回内容");
            }
            return content.trim();
        } catch (RestClientException ex) {
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "调用 AI 服务失败", ex);
        }
    }
}

