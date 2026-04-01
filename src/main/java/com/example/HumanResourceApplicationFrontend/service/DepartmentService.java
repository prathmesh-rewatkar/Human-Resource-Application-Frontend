package com.example.HumanResourceApplicationFrontend.service;

import com.example.HumanResourceApplicationFrontend.model.DepartmentDTO;
import com.example.HumanResourceApplicationFrontend.model.EmployeeDTO;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.beans.factory.annotation.Value;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class DepartmentService {
    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Value("${backend.api.url}")
    private String backendUrl;

    // ── Fetch all departments with pagination ──────────────────────
    public Map<String, Object> getDepartments(int page, int size,
                                              String search, String city) {
        Map<String, Object> result = new HashMap<>();
        List<DepartmentDTO> departments = new ArrayList<>();
        long totalElements = 0;
        int totalPages = 1;


        try {
            String url;
            if (search != null && !search.isBlank()) {
                url = backendUrl + "/department/search/findByDepartmentNameContainingIgnoreCase"
                        + "?name=" + search + "&page=" + page + "&size=" + size
                        + "&projection=deptView";
                page = 0;
            } else if (city != null && !city.isBlank()) {
                url = backendUrl + "/department/search/findByLocation_City"
                        + "?city=" + city + "&page=" + page + "&size=" + size
                        + "&projection=deptView";
                page = 0;
            } else {
                url = backendUrl + "/department?page=" + page
                        + "&size=" + size + "&projection=deptView";
            }

            String response = restTemplate.getForObject(url, String.class);
            JsonNode root = objectMapper.readTree(response);

            // Parse pagination info
            JsonNode pageNode = root.path("page");
            totalElements = pageNode.path("totalElements").asLong(0);
            totalPages    = pageNode.path("totalPages").asInt(1);

            // Parse departments
            JsonNode deptArray = root.path("_embedded").path("departments");
            if (deptArray.isArray()) {
                for (JsonNode node : deptArray) {
                    DepartmentDTO dept = parseDepartment(node);
                    departments.add(dept);
                }
            }

        } catch (Exception e) {
            e.printStackTrace();
            result.put("error", "Backend connection failed: " + e.getMessage());
        }

        result.put("departments",   departments);
        result.put("totalElements", totalElements);
        result.put("totalPages",    totalPages);
        result.put("currentPage",   page);
        result.put("pageSize",      size);
        return result;
    }

    // ── Fetch single department ────────────────────────────────────
    public DepartmentDTO getDepartmentById(Integer id) {
        try {
            String url = backendUrl + "/department/" + id + "?projection=deptView";
            String response = restTemplate.getForObject(url, String.class);
            JsonNode node = objectMapper.readTree(response);

            DepartmentDTO dept = new DepartmentDTO();
            dept.setDepartmentId(id);
            dept.setDepartmentName(node.path("departmentName").asText(null));

            // Location from _embedded
            JsonNode locEmbed = node.path("_embedded").path("location");
            if (!locEmbed.isMissingNode() && !locEmbed.isNull()) {
                dept.setLocation(parseLocation(locEmbed));
            } else {
                // fallback: fetch via link
                String locHref = node.path("_links").path("location").path("href").asText(null);
                if (locHref != null && !locHref.isBlank()) {
                    String cleanHref = locHref.replaceAll("\\{.*\\}", "");
                    try {
                        String locResp = restTemplate.getForObject(cleanHref, String.class);
                        JsonNode locNode = objectMapper.readTree(locResp);
                        dept.setLocation(parseLocation(locNode));
                    } catch (Exception ignored) {}
                }
            }

            // Manager via link
            String mgrHref = node.path("_links").path("manager").path("href").asText(null);
            if (mgrHref != null && !mgrHref.isBlank()) {
                String cleanHref = mgrHref.replaceAll("\\{.*\\}", "");
                try {
                    String mgrResp = restTemplate.getForObject(cleanHref, String.class);
                    JsonNode mgrNode = objectMapper.readTree(mgrResp);
                    dept.setManager(parseEmployee(mgrNode));
                } catch (Exception ignored) {}
            }

            return dept;

        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    // ── Fetch employees of a department ───────────────────────────
    public List<EmployeeDTO> getEmployeesByDepartment(Integer deptId) {
        List<EmployeeDTO> employees = new ArrayList<>();
        try {
            String url = backendUrl + "/department/" + deptId + "/employees";
            String response = restTemplate.getForObject(url, String.class);
            JsonNode root = objectMapper.readTree(response);
            JsonNode empArray = root.path("_embedded").path("employees");

            if (empArray.isArray()) {
                for (JsonNode empNode : empArray) {
                    employees.add(parseEmployee(empNode));
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return employees;
    }

    // Some more methods
//    public List<Map<String, Object>> getLocations() {
//        String url = backendUrl + "/locations";
//        Map<String, Object> response = restTemplate.getForObject(url, Map.class);
//        return (List<Map<String, Object>>) ((Map)response.get("_embedded")).get("locations");
//    }
//
//    public List<Map<String, Object>> getManagers() {
//        String url = backendUrl + "/employees/search/managersByDepartment?departmentId=50";
//        // OR simpler:
//        url = backendUrl + "/employees";
//        Map<String, Object> response = restTemplate.getForObject(url, Map.class);
//        return (List<Map<String, Object>>) ((Map)response.get("_embedded")).get("employees");
//    }

    // ── POST: Create department ────────────────────────────────────
    public ResponseEntity<String> createDepartment(String departmentName,
                                                   Integer locationId,
                                                   Integer managerId) {
        try {
            Map<String, Object> body = new HashMap<>();
            body.put("departmentName", departmentName);
            body.put("location", backendUrl + "/locations/" + locationId);
            if (managerId != null) {
                body.put("manager", backendUrl + "/employees/" + managerId);
            }

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(body, headers);

            ResponseEntity<String> response = restTemplate.postForEntity(
                    backendUrl + "/department", entity, String.class);
            return response;

        } catch (Exception e) {
            return ResponseEntity.status(500).body(e.getMessage());
        }
    }

    // ── PATCH: Update department ───────────────────────────────────
    public ResponseEntity<String> updateDepartment(Integer id,
                                                   String departmentName,
                                                   Integer locationId,
                                                   Integer managerId) {
        try {
            Map<String, Object> body = new HashMap<>();
            if (departmentName != null && !departmentName.isBlank()) {
                body.put("departmentName", departmentName);
            }
            if (locationId != null) {
                body.put("location", backendUrl + "/locations/" + locationId);
            }
            if (managerId != null) {
                body.put("manager", backendUrl + "/employees/" + managerId);
            }

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(body, headers);

            ResponseEntity<String> response = restTemplate.exchange(
                    backendUrl + "/department/" + id,
                    HttpMethod.PATCH, entity, String.class);
            return response;

        } catch (Exception e) {
            return ResponseEntity.status(500).body(e.getMessage());
        }
    }

    // ── DELETE department ──────────────────────────────────────────
    public ResponseEntity<String> deleteDepartment(Integer id) {
        try {
            restTemplate.delete(backendUrl + "/department/" + id);
            return ResponseEntity.noContent().build();
        } catch (Exception e) {
            return ResponseEntity.status(500).body(e.getMessage());
        }
    }

    // ── Parse helpers ──────────────────────────────────────────────

    private DepartmentDTO parseDepartment(JsonNode node) {
        DepartmentDTO dept = new DepartmentDTO();
        dept.setDepartmentName(node.path("departmentName").asText(null));

        // Extract ID from _links.self.href
        String selfHref = node.path("_links").path("self").path("href").asText("");
        if (!selfHref.isEmpty()) {
            try {
                String idStr = selfHref.replaceAll(".*/", "").replaceAll("\\?.*", "");
                dept.setDepartmentId(Integer.parseInt(idStr));
            } catch (NumberFormatException ignored) {}
        }

        // Location — try _embedded first, then link
        JsonNode locEmbed = node.path("_embedded").path("location");
        if (!locEmbed.isMissingNode() && !locEmbed.isNull()) {
            dept.setLocation(parseLocation(locEmbed));
        } else {
            String locHref = node.path("_links").path("location").path("href").asText(null);
            if (locHref != null && !locHref.isBlank()) {
                String cleanHref = locHref.replaceAll("\\{.*\\}", "");
                try {
                    String locResp = restTemplate.getForObject(cleanHref, String.class);
                    JsonNode locNode = objectMapper.readTree(locResp);
                    dept.setLocation(parseLocation(locNode));
                } catch (Exception ignored) {}
            }
        }

        // Manager — try _embedded first, then link
        JsonNode mgrEmbed = node.path("_embedded").path("manager");
        if (!mgrEmbed.isMissingNode() && !mgrEmbed.isNull()) {
            dept.setManager(parseEmployee(mgrEmbed));
        } else {
            String mgrHref = node.path("_links").path("manager").path("href").asText(null);
            if (mgrHref != null && !mgrHref.isBlank()) {
                String cleanHref = mgrHref.replaceAll("\\{.*\\}", "");
                try {
                    String mgrResp = restTemplate.getForObject(cleanHref, String.class);
                    JsonNode mgrNode = objectMapper.readTree(mgrResp);
                    dept.setManager(parseEmployee(mgrNode));
                } catch (Exception ignored) {}
            }
        }

        return dept;
    }

    private DepartmentDTO.LocationDTO parseLocation(JsonNode node) {
        DepartmentDTO.LocationDTO loc = new DepartmentDTO.LocationDTO();
        loc.setCity(node.path("city").asText(null));
        loc.setStreetAddress(node.path("streetAddress").asText(null));
        loc.setStateProvince(node.path("stateProvince").asText(null));
        loc.setPostalCode(node.path("postalCode").asText(null));
        return loc;
    }

    private EmployeeDTO parseEmployee(JsonNode node) {
        EmployeeDTO emp = new EmployeeDTO();
        emp.setFirstName(node.path("firstName").asText(null));
        emp.setLastName(node.path("lastName").asText(null));
        emp.setEmail(node.path("email").asText(null));
        emp.setPhoneNumber(node.path("phoneNumber").asText(null));

        if (!node.path("salary").isMissingNode()) {
            emp.setSalary(node.path("salary").asDouble(0));
        }

        return emp;
    }

    // ── Fetch all locations for dropdown ──────────────────────────
    public List<Map<String, Object>> getAllLocations() {
        List<Map<String, Object>> locations = new ArrayList<>();
        try {
            String url = backendUrl + "/locations?size=100";
            String response = restTemplate.getForObject(url, String.class);
            JsonNode root = objectMapper.readTree(response);
            JsonNode locArray = root.path("_embedded").path("locations");

            if (locArray.isArray()) {
                for (JsonNode node : locArray) {
                    Map<String, Object> loc = new HashMap<>();

                    // Extract ID from self link
                    String href = node.path("_links").path("self").path("href").asText("");
                    String idStr = href.replaceAll(".*/", "").replaceAll("\\?.*", "");
                    try { loc.put("id", Integer.parseInt(idStr)); }
                    catch (Exception ignored) {}

                    loc.put("city",          node.path("city").asText(""));
                    loc.put("streetAddress", node.path("streetAddress").asText(""));
                    loc.put("stateProvince", node.path("stateProvince").asText(""));
                    locations.add(loc);
                }
            }
        } catch (Exception e) { e.printStackTrace(); }
        return locations;
    }

    // ── Fetch all employees for manager dropdown ───────────────────
    public List<Map<String, Object>> getAllEmployees() {
        List<Map<String, Object>> employees = new ArrayList<>();
        try {
            String url = backendUrl + "/employees?size=300";
            String response = restTemplate.getForObject(url, String.class);
            JsonNode root = objectMapper.readTree(response);
            JsonNode empArray = root.path("_embedded").path("employees");

            if (empArray.isArray()) {
                for (JsonNode node : empArray) {
                    Map<String, Object> emp = new HashMap<>();

                    String href = node.path("_links").path("self").path("href").asText("");
                    String idStr = href.replaceAll(".*/", "").replaceAll("\\?.*", "");
                    try { emp.put("id", Integer.parseInt(idStr)); }
                    catch (Exception ignored) {}

                    emp.put("firstName", node.path("firstName").asText(""));
                    emp.put("lastName",  node.path("lastName").asText(""));
                    emp.put("email",     node.path("email").asText(""));
                    employees.add(emp);
                }
            }
        } catch (Exception e) { e.printStackTrace(); }
        return employees;
    }

}