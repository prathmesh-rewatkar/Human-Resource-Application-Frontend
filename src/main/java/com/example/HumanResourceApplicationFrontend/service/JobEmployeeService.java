package com.example.HumanResourceApplicationFrontend.service;

import com.example.HumanResourceApplicationFrontend.model.EmployeeDTO;
import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Service
public class JobEmployeeService {

    private final RestTemplate restTemplate;

    @Value("${backend.api.url}")
    private String baseUrl;

    public JobEmployeeService(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    public List<EmployeeDTO> getEmployeesByJobTitle(String jobTitle) {
        try {
            String url = baseUrl + "/employees/search/by-job-title?jobTitle=" + jobTitle
                       + "&projection=employeeView&size=100";

            JsonNode response = restTemplate.getForObject(url, JsonNode.class);

            if (response == null || !response.has("_embedded")) {
                return Collections.emptyList();
            }

            JsonNode employeesNode = response.path("_embedded").path("employees");
            List<EmployeeDTO> employees = new ArrayList<>();

            if (employeesNode.isArray()) {
                for (JsonNode node : employeesNode) {
                    EmployeeDTO dto = new EmployeeDTO();
                    dto.setFirstName(node.path("firstName").asText(null));
                    dto.setLastName(node.path("lastName").asText(null));
                    dto.setEmail(node.path("email").asText(null));
                    dto.setPhoneNumber(node.path("phoneNumber").asText(null));
                    dto.setSalary(node.path("salary").isNull() ? null : node.path("salary").asDouble());

                    // --- Fixed: these were missing, causing — in the table ---
                    dto.setJobTitle(node.path("jobTitle").asText(null));
                    dto.setDepartmentName(node.path("departmentName").asText(null));

                    employees.add(dto);
                }
            }
            return employees;
        } catch (Exception e) {
            e.printStackTrace();
            return Collections.emptyList();
        }
    }
}