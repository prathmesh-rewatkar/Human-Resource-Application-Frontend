package com.example.HumanResourceApplicationFrontend.service;

import com.example.HumanResourceApplicationFrontend.model.DepartmentDTO;
import com.example.HumanResourceApplicationFrontend.model.EmployeeDTO;
import com.example.HumanResourceApplicationFrontend.model.EmployeeRecordDTO;
import com.example.HumanResourceApplicationFrontend.model.ManagerDTO;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.*;

@Service
public class ManagerService {

    private final RestTemplate restTemplate;

    @Value("${backend.api.url}")
    private String baseUrl;

    public ManagerService(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    public List<ManagerDTO> getManagers(
            String email,
            String firstname,
            String lastname,
            Integer departmentId,
            int page,
            int size) {

        try {

            if (email != null && !email.isBlank()) {
                String url = baseUrl + "/managers/by-email?email=" + email;
                ManagerDTO manager = restTemplate.getForObject(url, ManagerDTO.class);
                return (manager != null) ? List.of(manager) : List.of();
            }

            if (firstname != null && !firstname.isBlank()) {
                String url = UriComponentsBuilder
                        .fromHttpUrl(baseUrl + "/managers/by-firstname")
                        .queryParam("firstname", firstname.trim())
                        .toUriString();
                return Arrays.asList(restTemplate.getForObject(url, ManagerDTO[].class));
            }

            if (lastname != null && !lastname.isBlank()) {
                String url = baseUrl + "/managers/by-lastname?lastname=" + lastname.trim();
                return Arrays.asList(restTemplate.getForObject(url, ManagerDTO[].class));
            }

            if (departmentId != null) {
                String url = baseUrl + "/managers/by-department?departmentId=" + departmentId;
                return Arrays.asList(restTemplate.getForObject(url, ManagerDTO[].class));
            }

            String url = baseUrl + "/managers/?page=" + page + "&size=" + size;
            return Arrays.asList(restTemplate.getForObject(url, ManagerDTO[].class));

        } catch (Exception ex) {
            return List.of();
        }
    }
    public List<DepartmentDTO> getAllDepartments() {
        try {
            String url = baseUrl + "/department?size=50";

            ObjectMapper mapper = new ObjectMapper();

            String response = restTemplate.getForObject(url, String.class);

            JsonNode root = mapper.readTree(response);
            JsonNode departments = root.path("_embedded").path("departments");

            List<DepartmentDTO> list = new ArrayList<>();

            for (JsonNode node : departments) {
                DepartmentDTO dto = new DepartmentDTO();

                dto.setDepartmentName(node.path("departmentName").asText());

                // 🔥 Extract ID from _links.self.href
                String href = node.path("_links").path("self").path("href").asText();
                String id = href.substring(href.lastIndexOf("/") + 1);

                dto.setDepartmentId(Integer.parseInt(id));

                list.add(dto);
            }

            return list;

        } catch (Exception e) {
            e.printStackTrace();
            return List.of();
        }
    }

    public List<EmployeeRecordDTO> getSubordinates(Integer id) {

        String url = baseUrl+"/managers/" + id + "/subordinates";

        ResponseEntity<EmployeeRecordDTO[]> response =
                restTemplate.getForEntity(url, EmployeeRecordDTO[].class);

        return Arrays.asList(response.getBody());
    }



    public void updateManager(Integer employeeId, Integer newManagerId) {

        String url = baseUrl+"/managers/update-manager";

        Map<String, Object> dto = new HashMap<>();
        dto.put("employeeId", employeeId);
        dto.put("newManagerId", newManagerId);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        HttpEntity<Map<String, Object>> request = new HttpEntity<>(dto, headers);

        ResponseEntity<String> response = restTemplate.exchange(
                url,
                HttpMethod.PATCH,
                request,
                String.class
        );

        System.out.println("STATUS: " + response.getStatusCode());
        System.out.println("BODY: " + response.getBody());
    }



    public List<EmployeeDTO> getHierarchy(Integer id) {

        String url = baseUrl+"/managers/" + id + "/hierarchy";

        ResponseEntity<EmployeeDTO[]> response =
                restTemplate.getForEntity(url, EmployeeDTO[].class);

        return Arrays.asList(response.getBody());
    }
}