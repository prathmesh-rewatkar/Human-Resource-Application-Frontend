package com.example.HumanResourceApplicationFrontend.service;

import com.example.HumanResourceApplicationFrontend.model.JobHistoryDTO;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Service
@RequiredArgsConstructor
public class JobHistoryService {

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    @Value("${backend.api.url}")
    private String baseUrl;

    public List<JobHistoryDTO> getJobHistory(Integer employeeId) {
        String url = baseUrl + "/job_history/search/by-employee?employeeId=" + employeeId
                + "&projection=jobHistoryView";
        try {
            JsonNode body = restTemplate.getForObject(url, JsonNode.class);
            List<JobHistoryDTO> list = new ArrayList<>();

            JsonNode embedded = body.path("_embedded");
            if (embedded.isObject() && embedded.fieldNames().hasNext()) {
                String key = embedded.fieldNames().next();
                JsonNode arr = embedded.path(key);
                if (arr.isArray()) {
                    for (JsonNode n : arr) {
                        try {
                            JobHistoryDTO dto = new JobHistoryDTO();
                            // startDate lives inside embedded id
                            if (n.has("startDate")) {
                                dto.setStartDate(java.time.LocalDate.parse(n.path("startDate").asText()));
                            } else if (n.has("id") && n.path("id").has("startDate")) {
                                dto.setStartDate(java.time.LocalDate.parse(n.path("id").path("startDate").asText()));
                            }
                            if (n.has("endDate"))        dto.setEndDate(java.time.LocalDate.parse(n.path("endDate").asText()));
                            if (n.has("jobTitle"))       dto.setJobTitle(n.path("jobTitle").asText("—"));
                            if (n.has("departmentName")) dto.setDepartmentName(n.path("departmentName").asText("—"));
                            if (n.has("employeeName"))   dto.setEmployeeName(n.path("employeeName").asText(""));
                            list.add(dto);
                        } catch (Exception ignored) {}
                    }
                }
            } else if (body.isArray()) {
                for (JsonNode n : body) {
                    try { list.add(objectMapper.treeToValue(n, JobHistoryDTO.class)); }
                    catch (Exception ignored) {}
                }
            }
            return list;
        } catch (Exception e) {
            return Collections.emptyList();
        }
    }
}
