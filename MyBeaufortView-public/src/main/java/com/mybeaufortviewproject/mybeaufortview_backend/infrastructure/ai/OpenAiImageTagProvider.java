package com.mybeaufortviewproject.mybeaufortview_backend.infrastructure.ai;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mybeaufortviewproject.mybeaufortview_backend.common.config.AppProperties;

@Service
public class OpenAiImageTagProvider implements AiImageTagProvider {

    private final AppProperties appProperties;

    private final RestTemplate restTemplate;

    private final ObjectMapper objectMapper;

    private static final Logger log = LoggerFactory.getLogger(OpenAiImageTagProvider.class);

    public OpenAiImageTagProvider(AppProperties appProperties, RestTemplate restTemplate, ObjectMapper objectMapper) {
        this.appProperties = appProperties;
        this.restTemplate = restTemplate;
        this.objectMapper = objectMapper;
    }

    @Override
    public ImageTagResult generateTags(String imageUrl) {
        try {
            List<String> tags = callOpenAi(imageUrl);
            return new ImageTagResult(tags, appProperties.getOpenai().getModel(), true);
        } catch (Exception e) {
            log.warn("OpenAI image tagging failed for imageUrl={}", imageUrl, e);
            return new ImageTagResult(List.of(), appProperties.getOpenai().getModel(), false);
        }
    }

    private List<String> callOpenAi(String imageUrl) {

        String apiKey = appProperties.getOpenai().getApiKey();

        if (apiKey == null || apiKey.isBlank()) {
            throw new IllegalStateException("OpenAI API key is not configured");
        }

        String url = appProperties.getOpenai().getBaseUrl() + "/responses";

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(apiKey);

        String prompt = """
                Return only a raw JSON array of 5 to 8 lowercase tags describing visible objects in this image.
                Do not use markdown.
                Do not use code fences.
                Only include concrete, physical elements.
                """;

        Map<String, Object> requestBody = Map.of(
                "model", appProperties.getOpenai().getModel(),
                "input", List.of(
                        Map.of(
                                "role", "user",
                                "content", List.of(
                                        Map.of("type", "input_text", "text", prompt),
                                        Map.of("type", "input_image", "image_url", imageUrl)
                                    )
                            )
                        ),
                        "max_output_tokens", 60
                );

        HttpEntity<Map<String, Object>> request = new HttpEntity<>(requestBody, headers);

        ResponseEntity<Map<String, Object>> response = restTemplate.exchange(
                url,
                HttpMethod.POST,
                request,
                new ParameterizedTypeReference<Map<String, Object>>() {}
        );

        log.debug("OpenAI raw response: {}", response.getBody());

        return extractTagsFromResponse(response.getBody());

    }

    private List<String> extractTagsFromResponse(Map<String, Object> body) {
        if (body == null) {
            return List.of();
        }

        try {
            Object outputObj = body.get("output");
            if (!(outputObj instanceof List<?> output) || output.isEmpty()) {
                return List.of();
            }

            for (Object outputItemObj : output) {
                if (!(outputItemObj instanceof Map<?, ?> outputItem)) {
                    continue;
                }

                Object contentObj = outputItem.get("content");
                if (!(contentObj instanceof List<?> contentList)) {
                    continue;
                }

                for (Object contentItemObj : contentList) {
                    if (!(contentItemObj instanceof Map<?, ?> contentItem)) {
                        continue;
                    }

                    Object textObj = contentItem.get("text");
                    if (textObj instanceof String text && !text.isBlank()) {
                        return parseJsonArray(text);
                    }
                }
            }

            return List.of();
        } catch (Exception e) {
            log.warn("Failed to extract tags from OpenAI response: {}", body, e);
            return List.of();
        }
    }

    private List<String> parseJsonArray(String text) {
        if (text == null || text.isBlank()) {
            return List.of();
        }

        String cleaned = text.trim();

        if (cleaned.startsWith("```")) {
            cleaned = cleaned.replaceFirst("^```json\\s*", "");
            cleaned = cleaned.replaceFirst("^```\\s*", "");
            cleaned = cleaned.replaceFirst("\\s*```$", "");
            cleaned = cleaned.trim();
        }

        try {
            JsonNode root = objectMapper.readTree(cleaned);

            if (!root.isArray()) {
                return List.of();
            }

            List<String> tags = new ArrayList<>();

            for (JsonNode node : root) {
                if (!node.isTextual()) {
                    continue;
                }

                String value = node.asText().trim();
                if (!value.isBlank()) {
                    tags.add(value);
                }
            }

            return List.copyOf(tags);
        } catch (Exception e) {
            log.warn("Failed to parse OpenAI tag JSON: {}", text, e);
            return List.of();
        }
    }


}
