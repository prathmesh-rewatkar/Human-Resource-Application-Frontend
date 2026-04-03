package com.example.HumanResourceApplicationFrontend.service;


import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.web.client.RestTemplate;

import java.util.*;

@Slf4j
public abstract class BaseService {

    protected final RestTemplate restTemplate;
    protected final ObjectMapper objectMapper = new ObjectMapper();

    @Value("${backend.api.url}")
    protected String baseUrl;

    protected BaseService(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    // ── HTTP helpers ──────────────────────────────────────────────────

    protected String rawGet(String path) {
        try {
            ResponseEntity<String> resp = restTemplate.getForEntity(baseUrl + path, String.class);
            if (!resp.getStatusCode().is2xxSuccessful())
                throw new RuntimeException("Backend error " + resp.getStatusCode() + " for GET " + path);
            return resp.getBody();
        } catch (Exception e) {
            log.error("rawGet '{}' failed: {}", path, e.getMessage());
            throw new RuntimeException("Backend call failed for " + path + ": " + e.getMessage(), e);
        }
    }

    protected void post(String path, Map<String, Object> body) {
        HttpHeaders h = new HttpHeaders();
        h.setContentType(MediaType.APPLICATION_JSON);
        ResponseEntity<String> resp = restTemplate.postForEntity(baseUrl + path,
                new HttpEntity<>(body, h), String.class);
        if (!resp.getStatusCode().is2xxSuccessful())
            throw new RuntimeException("POST failed: " + resp.getBody());
    }

    protected void put(String path, Map<String, Object> body) {
        HttpHeaders h = new HttpHeaders();
        h.setContentType(MediaType.APPLICATION_JSON);
        restTemplate.put(baseUrl + path, new HttpEntity<>(body, h));
    }

    protected void delete(String path) {
        restTemplate.delete(baseUrl + path);
    }

    // ── JSON helpers ──────────────────────────────────────────────────

    protected List<JsonNode> embedded(String json, String key) {
        if (json == null) return Collections.emptyList();
        try {
            JsonNode node = objectMapper.readTree(json).path("_embedded").path(key);
            if (node.isMissingNode()) { log.warn("_embedded.{} missing", key); return Collections.emptyList(); }
            List<JsonNode> list = new ArrayList<>();
            node.forEach(list::add);
            return list;
        } catch (Exception e) {
            log.error("embedded('{}') failed: {}", key, e.getMessage());
            return Collections.emptyList();
        }
    }

    protected JsonNode parseJson(String json) {
        try { return objectMapper.readTree(json); }
        catch (Exception e) { return objectMapper.createObjectNode(); }
    }

    protected String str(JsonNode n, String field) {
        String v = n.path(field).asText("");
        return v.isEmpty() ? null : v;
    }

    protected Integer integer(JsonNode n, String field) {
        return n.path(field).isMissingNode() ? null : n.path(field).asInt();
    }
}

