package com.example.HumanResourceApplicationFrontend.service;

import com.example.HumanResourceApplicationFrontend.model.*;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.*;

@Service
@RequiredArgsConstructor
public class EmployeeService {

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    @Value("${backend.api.url}")
    private String baseUrl;

    private static final int PAGE_SIZE = 10;

    public PageResult<EmployeeDTO> getEmployees(int page, String search, String dept, String job) {
        String url = buildUrl(page, search, dept, job);
        try {
            JsonNode body = restTemplate.getForObject(url, JsonNode.class);
            return parseEmployeePage(body, page);
        } catch (Exception e) {
            return emptyPage();
        }
    }

    private String buildUrl(int page, String search, String dept, String job) {
        UriComponentsBuilder ub;

        if (!search.isBlank()) {
            ub = UriComponentsBuilder.fromHttpUrl(baseUrl + "/employees/search/search-by-name")
                    .queryParam("firstName", search)
                    .queryParam("lastName", search);
        } else if (!dept.isBlank()) {
            ub = UriComponentsBuilder.fromHttpUrl(baseUrl + "/employees/search/by-department-name")
                    .queryParam("departmentName", dept);
        } else if (!job.isBlank()) {
            ub = UriComponentsBuilder.fromHttpUrl(baseUrl + "/employees/search/by-job-title")
                    .queryParam("jobTitle", job);
        } else {
            ub = UriComponentsBuilder.fromHttpUrl(baseUrl + "/employees");
        }

        return ub.queryParam("page", page)
                .queryParam("size", PAGE_SIZE)
                .queryParam("projection", "employeeView")
                .toUriString();
    }

    private PageResult<EmployeeDTO> parseEmployeePage(JsonNode body, int page) {
        PageResult<EmployeeDTO> result = new PageResult<>();
        result.setCurrentPage(page);
        result.setPageSize(PAGE_SIZE);
        result.setTotalElements(body.path("page").path("totalElements").asLong(0));
        result.setTotalPages(body.path("page").path("totalPages").asInt(1));

        List<EmployeeDTO> list = new ArrayList<>();
        JsonNode arr = body.path("_embedded").path("employees");

        if (arr.isArray()) {
            for (JsonNode node : arr) {
                try {
                    list.add(parseEmployee(node));
                } catch (Exception ignored) {}
            }
        }

        result.setContent(list);
        return result;
    }

    private EmployeeDTO parseEmployee(JsonNode node) throws Exception {
        EmployeeDTO dto = objectMapper.treeToValue(node, EmployeeDTO.class);

        if (dto.getEmployeeId() == null) {
            String href = node.path("_links").path("self").path("href").asText(null);
            if (href != null && href.contains("/")) {
                dto.setEmployeeId(Integer.parseInt(href.substring(href.lastIndexOf("/") + 1)));
            }
        }

        dto.setDepartmentName(node.path("departmentName").asText(null));
        dto.setJobTitle(node.path("jobTitle").asText(null));
        dto.setManagerId(node.path("managerId").asInt(0));
        dto.setDepartmentId(node.path("departmentId").asInt());
        dto.setJobId(node.path("jobId").asText(null));

        return dto;
    }

    public boolean createEmployee(EmployeeFormDTO form) {
        return send(baseUrl + "/employees", form, HttpMethod.POST);
    }

    public boolean updateEmployee(Integer id, EmployeeFormDTO form) {
        return send(baseUrl + "/employees/" + id, form, HttpMethod.PATCH);
    }

    public boolean deleteEmployee(Integer id) {
        restTemplate.delete(baseUrl + "/employees/" + id);
        return true;
    }

    private boolean send(String url, EmployeeFormDTO form, HttpMethod method) {
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<Map<String, Object>> req = new HttpEntity<>(buildPayload(form), headers);
            restTemplate.exchange(url, method, req, String.class);
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    private Map<String, Object> buildPayload(EmployeeFormDTO form) {
        Map<String, Object> map = new HashMap<>();

        map.put("firstName", form.getFirstName());
        map.put("lastName", form.getLastName());
        map.put("email", form.getEmail());
        map.put("phoneNumber", form.getPhoneNumber());
        map.put("salary", form.getSalary());
        map.put("commissionPct", form.getCommissionPct());

        if (form.getHireDate() != null)
            map.put("hireDate", form.getHireDate().toString());

        if (form.getDepartmentId() != null)
            map.put("department", baseUrl + "/department/" + form.getDepartmentId());

        if (form.getJobId() != null)
            map.put("job", baseUrl + "/jobs/" + form.getJobId());

        if (form.getManagerId() != null)
            map.put("manager", baseUrl + "/employees/" + form.getManagerId());

        return map;
    }

    public List<DepartmentDTO> getDepartments() {
        return fetchList(baseUrl + "/department?size=100", DepartmentDTO.class);
    }

    public List<JobDTO> getJobs() {
        return fetchList(baseUrl + "/jobs?size=100", JobDTO.class);
    }

    public List<EmployeeDTO> getManagers() {
        return fetchList(baseUrl + "/employees?size=50", EmployeeDTO.class);
    }

    private <T> List<T> fetchList(String url, Class<T> clazz) {
        try {
            JsonNode body = restTemplate.getForObject(url, JsonNode.class);
            List<T> list = new ArrayList<>();
            JsonNode embedded = body.path("_embedded");

            if (embedded.isObject()) {
                for (Iterator<String> it = embedded.fieldNames(); it.hasNext(); ) {
                    String key = it.next();
                    JsonNode arr = embedded.path(key);
                    if (arr.isArray()) {
                        for (JsonNode node : arr) {
                            list.add(objectMapper.treeToValue(node, clazz));
                        }
                    }
                }
            }
            return list;
        } catch (Exception e) {
            return Collections.emptyList();
        }
    }

    private PageResult<EmployeeDTO> emptyPage() {
        PageResult<EmployeeDTO> r = new PageResult<>();
        r.setContent(Collections.emptyList());
        r.setCurrentPage(0);
        r.setTotalPages(0);
        r.setTotalElements(0);
        r.setPageSize(PAGE_SIZE);
        return r;
    }

    @Data
    public static class PageResult<T> {
        private List<T> content;
        private int currentPage;
        private int totalPages;
        private long totalElements;
        private int pageSize;

        public boolean hasPrevious() { return currentPage > 0; }
        public boolean hasNext() { return currentPage < totalPages - 1; }

        public List<Integer> getPageNumbers() {
            List<Integer> pages = new ArrayList<>();
            for (int i = 0; i < totalPages; i++) pages.add(i);
            return pages;
        }
    }

    public EmployeeFormDTO mapToForm(EmployeeDTO e) {
        EmployeeFormDTO f = new EmployeeFormDTO();
        f.setEmployeeId(e.getEmployeeId());
        f.setFirstName(e.getFirstName());
        f.setLastName(e.getLastName());
        f.setEmail(e.getEmail());
        f.setPhoneNumber(e.getPhoneNumber());
        f.setHireDate(e.getHireDate());
        f.setSalary(e.getSalary());
        f.setCommissionPct(e.getCommissionPct());
        f.setDepartmentId(e.getDepartmentId());
        f.setJobId(e.getJobId());
        f.setManagerId(e.getManagerId());
        return f;
    }

    public EmployeeDTO getEmployee(Integer id) {
        try {
            JsonNode node = restTemplate.getForObject(
                    baseUrl + "/employees/" + id + "?projection=employeeView",
                    JsonNode.class
            );

            EmployeeDTO dto = objectMapper.treeToValue(node, EmployeeDTO.class);

            if (dto.getEmployeeId() == null) {
                String href = node.path("_links").path("self").path("href").asText();
                if (href != null && href.contains("/")) {
                    dto.setEmployeeId(Integer.parseInt(href.substring(href.lastIndexOf("/") + 1)));
                }
            }

            int deptId = node.path("departmentId").asInt(0);
            if (deptId == 0) {
                deptId = node.path("department").path("departmentId").asInt(0);
            }
            dto.setDepartmentId(deptId);

            String jobId = node.path("jobId").asText(null);
            if (jobId == null || jobId.isBlank()) {
                jobId = node.path("job").path("jobId").asText(null);
            }
            dto.setJobId(jobId);

            int managerId = node.path("managerId").asInt(0);
            if (managerId == 0) {
                managerId = node.path("manager").path("employeeId").asInt(0);
            }
            dto.setManagerId(managerId);

            return dto;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }
}