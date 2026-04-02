package com.example.HumanResourceApplicationFrontend.service;

import com.example.HumanResourceApplicationFrontend.model.*;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpStatusCodeException;
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
    private final ThreadLocal<String> lastError = new ThreadLocal<>();

    public String consumeLastError() {
        String msg = lastError.get();
        lastError.remove();
        return msg;
    }

    private void clearLastError() {
        lastError.remove();
    }

    private void setLastError(String msg) {
        if (msg != null && !msg.isBlank()) lastError.set(msg.trim());
    }

    public PageResult<EmployeeDTO> getEmployees(int page, String search, String dept, String job) {
        int safePage = Math.max(page, 0);
        String safeSearch = search != null ? search.trim() : "";
        String safeDept = dept != null ? dept.trim() : "";
        String safeJob = job != null ? job.trim() : "";

        List<String> candidateUrls = buildEmployeesSearchCandidates(safePage, safeSearch, safeDept, safeJob);
        for (String url : candidateUrls) {
            try {
                ResponseEntity<JsonNode> resp = restTemplate.getForEntity(url, JsonNode.class);
                JsonNode body = resp.getBody();
                if (body != null) return parseEmployeePage(body, safePage);
            } catch (Exception ignored) {
            }
        }
        return emptyPage();
    }

    private List<String> buildEmployeesSearchCandidates(int page, String search, String dept, String job) {
        List<String> urls = new ArrayList<>();
        urls.add(buildEmployeesUrl(page, search, dept, job)); 

        boolean hasSearch = search != null && !search.isBlank();
        boolean hasDept = dept != null && !dept.isBlank();
        boolean hasJob = job != null && !job.isBlank();

        if (hasSearch && hasDept && hasJob) {
            urls.add(buildEmployeesUrl(page, search, dept, ""));
            urls.add(buildEmployeesUrl(page, search, "", job));
            urls.add(buildEmployeesUrl(page, "", dept, job));
        }
        if (hasSearch) urls.add(buildEmployeesUrl(page, search, "", ""));
        if (hasDept) urls.add(buildEmployeesUrl(page, "", dept, ""));
        if (hasJob) urls.add(buildEmployeesUrl(page, "", "", job));
        urls.add(buildEmployeesUrl(page, "", "", ""));

        return new ArrayList<>(new LinkedHashSet<>(urls));
    }

    private String buildEmployeesUrl(int page, String search, String dept, String job) {

        boolean hasSearch = search != null && !search.isBlank();
        boolean hasDept   = dept   != null && !dept.isBlank();
        boolean hasJob    = job    != null && !job.isBlank();

        String firstName = search != null ? search.trim() : "";
        String lastName  = search != null ? search.trim() : "";
        if (hasSearch && search.trim().contains(" ")) {
            String[] parts = search.trim().split("\\s+", 2);
            firstName = parts[0];
            lastName  = parts[1];
        }

        UriComponentsBuilder ub;

        if (hasSearch && hasDept && hasJob) {
            ub = UriComponentsBuilder.fromHttpUrl(baseUrl + "/employees/search/by-name-dept-and-job")
                    .queryParam("dept1", dept).queryParam("job1", job).queryParam("firstName", firstName)
                    .queryParam("dept2", dept).queryParam("job2", job).queryParam("lastName",  lastName);
        } else if (hasSearch) {
            ub = UriComponentsBuilder.fromHttpUrl(baseUrl + "/employees/search/search-by-name")
                    .queryParam("firstName", firstName).queryParam("lastName", lastName);
        } else if (hasDept) {
            ub = UriComponentsBuilder.fromHttpUrl(baseUrl + "/employees/search/by-department-name")
                    .queryParam("departmentName", dept);
        } else if (hasJob) {
            ub = UriComponentsBuilder.fromHttpUrl(baseUrl + "/employees/search/by-job-title")
                    .queryParam("jobTitle", job);
        } else {
            ub = UriComponentsBuilder.fromHttpUrl(baseUrl + "/employees");
        }

        ub.queryParam("page", page)
                .queryParam("size", PAGE_SIZE)
                .queryParam("projection", "employeeView");

        return ub.toUriString();
    }

    private PageResult<EmployeeDTO> parseEmployeePage(JsonNode body, int page) {
        PageResult<EmployeeDTO> result = new PageResult<>();
        result.setPageSize(PAGE_SIZE);
        result.setCurrentPage(page);
        result.setTotalElements(body.path("page").path("totalElements").asLong(0));
        result.setTotalPages(body.path("page").path("totalPages").asInt(1));

        List<EmployeeDTO> list = new ArrayList<>();
        JsonNode arr = body.path("_embedded").path("employees");

        if (arr.isArray()) {
            for (JsonNode node : arr) {
                try { list.add(parseEmployeeNode(node)); }
                catch (Exception ignored) {}
            }
        } else if (body.isArray()) {
            for (JsonNode node : body) {
                try { list.add(parseEmployeeNode(node)); }
                catch (Exception ignored) {}
            }
        } else if (body.isObject() && body.has("employeeId")) {
            try { list.add(parseEmployeeNode(body)); }
            catch (Exception ignored) {}
        }

        if (body.path("page").isMissingNode() || body.path("page").isNull()) {
            result.setTotalElements(list.size());
            result.setTotalPages(list.isEmpty() ? 0 : 1);
        }

        result.setContent(list);
        return result;
    }

    public EmployeeDTO getEmployee(Integer id) {
        try {
            String primaryUrl = baseUrl + "/employees/" + id + "?projection=employeeView";
            JsonNode primary = restTemplate.getForObject(primaryUrl, JsonNode.class);
            if (primary != null) return parseEmployeeNode(primary);
        } catch (Exception e) {
            System.out.println("Primary employee fetch failed: " + e.getMessage());
        }

        try {
            String fallbackUrl = baseUrl + "/employees/search/search-by-id?empId=" + id
                    + "&projection=employeeView";
            JsonNode fallback = restTemplate.getForObject(fallbackUrl, JsonNode.class);
            if (fallback != null) return parseEmployeeNode(fallback);
        } catch (Exception e) {
            System.out.println("Fallback employee fetch failed: " + e.getMessage());
        }
        return null;
    }

    private EmployeeDTO parseEmployeeNode(JsonNode node) throws Exception {
        EmployeeDTO dto = objectMapper.treeToValue(node, EmployeeDTO.class);

        if (dto.getEmployeeId() == null) {
            String selfHref = node.path("_links").path("self").path("href").asText(null);
            String id = extractLastPathSegment(selfHref);
            if (id != null) {
                try { dto.setEmployeeId(Integer.parseInt(id)); }
                catch (NumberFormatException ignored) {}
            }
        }

        if (dto.getJobTitle() == null) {
            String jt = node.path("_embedded").path("job").path("jobTitle").asText(null);
            if (jt != null && !jt.isEmpty()) dto.setJobTitle(jt);
        }

        if (dto.getDepartmentName() == null) {
            String dn = node.path("_embedded").path("department").path("departmentName").asText(null);
            if (dn != null && !dn.isEmpty()) dto.setDepartmentName(dn);
        }

        if (dto.getCommissionPct() == null) {
            Double c = firstDouble(node,
                    "commissionPct", "commission_pct", "commissionPCT", "commission");
            if (c != null) dto.setCommissionPct(c);
        }

        if (dto.getDepartmentId() == null) {
            Integer idVal = firstInt(node,
                    "departmentId", "department_id", "department_DepartmentId", "department_departmentId");
            if (idVal == null) {
                JsonNode deptNode = node.path("department");
                if (deptNode.isObject() && deptNode.has("departmentId")) {
                    idVal = deptNode.path("departmentId").asInt();
                }
            }
            if (idVal != null) dto.setDepartmentId(idVal);
        }

        if (dto.getJobId() == null || dto.getJobId().isBlank()) {
            String idVal = firstText(node, "jobId", "job_id", "job_JobId", "job_jobId");
            if (idVal == null) {
                JsonNode jobNode = node.path("job");
                if (jobNode.isObject() && jobNode.has("jobId")) {
                    idVal = jobNode.path("jobId").asText(null);
                }
            }
            if (idVal != null && !idVal.isBlank()) dto.setJobId(idVal);
        }

        if (dto.getManagerId() == null) {
            JsonNode mgr = node.path("_embedded").path("manager");
            if (!mgr.isMissingNode() && !mgr.isNull() && mgr.has("employeeId")) {
                dto.setManagerId(mgr.path("employeeId").asInt());
            }
        }

        if (dto.getManagerId() == null) {
            Integer idVal = firstInt(node,
                    "managerId", "manager_id", "manager_EmployeeId", "manager_employeeId");
            if (idVal == null) {
                JsonNode mgrNode = node.path("manager");
                if (mgrNode.isObject() && mgrNode.has("employeeId")) {
                    idVal = mgrNode.path("employeeId").asInt();
                }
            }
            if (idVal != null) dto.setManagerId(idVal);
        }

        String deptHref = node.path("_links").path("department").path("href").asText(null);
        if (dto.getDepartmentId() == null && deptHref != null && deptHref.contains("/department/")) {
            String deptId = extractLastPathSegment(deptHref);
            if (deptId != null) {
                try { dto.setDepartmentId(Integer.parseInt(deptId)); }
                catch (Exception ignored) {}
            }
        }

        String jobHref = node.path("_links").path("job").path("href").asText(null);
        if ((dto.getJobId() == null || dto.getJobId().isBlank()) && jobHref != null && jobHref.contains("/jobs/")) {
            String jobId = extractLastPathSegment(jobHref);
            if (jobId != null && !jobId.isBlank()) {
                dto.setJobId(jobId);
            }
        }

        if (dto.getManagerId() == null) {
            String managerHref = node.path("_links").path("manager").path("href").asText(null);
            Integer managerIdFromHref = extractTerminalEmployeeId(managerHref);
            if (managerIdFromHref != null) {
                dto.setManagerId(managerIdFromHref);
            }
        }

        return dto;
    }

    private String extractLastPathSegment(String href) {
        if (href == null || href.isBlank()) return null;

        String clean = href.trim();
        int brace = clean.indexOf('{');
        if (brace >= 0) clean = clean.substring(0, brace);
        int query = clean.indexOf('?');
        if (query >= 0) clean = clean.substring(0, query);
        if (clean.endsWith("/")) clean = clean.substring(0, clean.length() - 1);

        int slash = clean.lastIndexOf('/');
        if (slash < 0 || slash == clean.length() - 1) return null;

        String last = clean.substring(slash + 1).trim();
        return last.isEmpty() ? null : last;
    }

    private Integer firstInt(JsonNode node, String... keys) {
        for (String key : keys) {
            JsonNode n = node.path(key);
            if (!n.isMissingNode() && !n.isNull()) {
                if (n.isInt() || n.isLong()) return n.asInt();
                String t = n.asText(null);
                if (t != null && !t.isBlank()) {
                    try { return Integer.parseInt(t.trim()); }
                    catch (NumberFormatException ignored) {}
                }
            }
        }
        return null;
    }

    private String firstText(JsonNode node, String... keys) {
        for (String key : keys) {
            JsonNode n = node.path(key);
            if (!n.isMissingNode() && !n.isNull()) {
                String t = n.asText(null);
                if (t != null && !t.isBlank()) return t.trim();
            }
        }
        return null;
    }

    private Double firstDouble(JsonNode node, String... keys) {
        for (String key : keys) {
            JsonNode n = node.path(key);
            if (!n.isMissingNode() && !n.isNull()) {
                if (n.isNumber()) return n.asDouble();
                String t = n.asText(null);
                if (t != null && !t.isBlank()) {
                    try { return Double.parseDouble(t.trim()); }
                    catch (NumberFormatException ignored) {}
                }
            }
        }
        return null;
    }

    public List<DepartmentDTO> getDepartments() {
        return fetchList(baseUrl + "/department?size=100", DepartmentDTO.class);
    }

    public List<JobDTO> getJobs() {
        try {
            JsonNode body = restTemplate.getForObject(baseUrl + "/jobs?size=100&projection=jobDetails", JsonNode.class);
            return parseJobListBody(body);
        } catch (Exception e) {
            return Collections.emptyList();
        }
    }

    public List<EmployeeDTO> getAllEmployees() {
        return fetchList(baseUrl + "/employees?size=500&projection=employeeView", EmployeeDTO.class);
    }

    public List<EmployeeDTO> getManagers() {
        List<String> urls = List.of(baseUrl + "/managers/?size=20");

        for (String url : urls) {
            try {
                JsonNode body = restTemplate.getForObject(url, JsonNode.class);
                List<EmployeeDTO> managers = parseEmployeeListBody(body);
                if (!managers.isEmpty()) return managers;
            } catch (Exception e) {
                System.out.println("Managers fetch failed for URL " + url + ": " + e.getMessage());
            }
        }
        List<EmployeeDTO> employees = getAllEmployees();
        return employees.size() > 20 ? employees.subList(0, 20) : employees;
    }

    private List<EmployeeDTO> parseEmployeeListBody(JsonNode body) {
        if (body == null) return Collections.emptyList();

        List<EmployeeDTO> list = new ArrayList<>();

        if (body.isArray()) {
            for (JsonNode n : body) {
                try { list.add(objectMapper.treeToValue(n, EmployeeDTO.class)); }
                catch (Exception ignored) {}
            }
            return list;
        }

        JsonNode content = body.path("content");
        if (content.isArray()) {
            for (JsonNode n : content) {
                try { list.add(objectMapper.treeToValue(n, EmployeeDTO.class)); }
                catch (Exception ignored) {}
            }
            return list;
        }

        JsonNode emb = body.path("_embedded");
        if (emb.isObject() && emb.fieldNames().hasNext()) {
            String key = emb.fieldNames().next();
            for (JsonNode n : emb.path(key)) {
                try { list.add(objectMapper.treeToValue(n, EmployeeDTO.class)); }
                catch (Exception ignored) {}
            }
        }

        return list;
    }

    private List<JobDTO> parseJobListBody(JsonNode body) {
        if (body == null) return Collections.emptyList();

        List<JobDTO> jobs = new ArrayList<>();
        JsonNode arr = body.path("_embedded").path("jobs");

        if (arr.isArray()) {
            for (JsonNode node : arr) {
                JobDTO job = parseJobNode(node);
                if (job != null) jobs.add(job);
            }
            return jobs;
        }

        JsonNode content = body.path("content");
        if (content.isArray()) {
            for (JsonNode node : content) {
                JobDTO job = parseJobNode(node);
                if (job != null) jobs.add(job);
            }
        }

        return jobs;
    }

    private JobDTO parseJobNode(JsonNode node) {
        if (node == null || node.isNull()) return null;

        JobDTO dto = new JobDTO();
        dto.setJobTitle(firstText(node, "jobTitle", "job_title", "title"));

        String jobId = firstText(node, "jobId", "job_id", "job_JobId", "job_jobId");
        if (jobId == null) {
            String selfHref = node.path("_links").path("self").path("href").asText(null);
            if (selfHref != null && selfHref.contains("/jobs/")) {
                jobId = extractLastPathSegment(selfHref);
            }
        }
        dto.setJobId(jobId);

        JsonNode minSalary = node.path("minSalary");
        if (!minSalary.isMissingNode() && !minSalary.isNull()) {
            try { dto.setMinSalary(minSalary.decimalValue()); }
            catch (Exception ignored) {}
        }

        JsonNode maxSalary = node.path("maxSalary");
        if (!maxSalary.isMissingNode() && !maxSalary.isNull()) {
            try { dto.setMaxSalary(maxSalary.decimalValue()); }
            catch (Exception ignored) {}
        }

        if ((dto.getJobId() == null || dto.getJobId().isBlank())
                && (dto.getJobTitle() == null || dto.getJobTitle().isBlank())) {
            return null;
        }

        return dto;
    }

    private <T> List<T> fetchList(String url, Class<T> clazz) {
        try {
            JsonNode body = restTemplate.getForObject(url, JsonNode.class);
            List<T> list  = new ArrayList<>();
            JsonNode emb  = body.path("_embedded");
            if (emb.isObject() && emb.fieldNames().hasNext()) {
                String key = emb.fieldNames().next();
                for (JsonNode n : emb.path(key)) {
                    try { list.add(objectMapper.treeToValue(n, clazz)); }
                    catch (Exception ignored) {}
                }
            }
            return list;
        } catch (Exception e) {
            return Collections.emptyList();
        }
    }

    public boolean createEmployee(EmployeeFormDTO form) {
        clearLastError();
        return sendRequest(baseUrl + "/employees", buildPayload(form), HttpMethod.POST);
    }

    public boolean updateEmployee(Integer id, EmployeeFormDTO form) {
        clearLastError();
        EmployeeFormDTO safeForm = mergeMissingFieldsFromCurrent(id, form);
        return sendRequest(baseUrl + "/employees/" + id, buildPayload(safeForm), HttpMethod.PATCH);
    }

    private EmployeeFormDTO mergeMissingFieldsFromCurrent(Integer id, EmployeeFormDTO form) {
        if (form == null) return null;

        EmployeeDTO current = getEmployee(id);
        if (current == null) return form;

        if (isBlank(form.getFirstName()) && current.getFirstName() != null) {
            form.setFirstName(current.getFirstName());
        }
        if (isBlank(form.getLastName()) && current.getLastName() != null) {
            form.setLastName(current.getLastName());
        }
        if (isBlank(form.getEmail()) && current.getEmail() != null) {
            form.setEmail(current.getEmail());
        }
        if (isBlank(form.getPhoneNumber()) && current.getPhoneNumber() != null) {
            form.setPhoneNumber(current.getPhoneNumber());
        }
        if (form.getHireDate() == null && current.getHireDate() != null) {
            form.setHireDate(current.getHireDate());
        }
        if (form.getSalary() == null && current.getSalary() != null) {
            form.setSalary(current.getSalary());
        }
        if (form.getCommissionPct() == null && current.getCommissionPct() != null) {
            form.setCommissionPct(current.getCommissionPct());
        }
        if (form.getDepartmentId() == null && current.getDepartmentId() != null) {
            form.setDepartmentId(current.getDepartmentId());
        }
        if (isBlank(form.getJobId()) && current.getJobId() != null && !current.getJobId().isBlank()) {
            form.setJobId(current.getJobId());
        }
        if (isBlank(form.getJobId())) {
            String currentJobId = fetchEmployeeRelationId(id, "job");
            if (currentJobId != null && !currentJobId.isBlank()) {
                form.setJobId(currentJobId);
            }
        }
        if (form.getDepartmentId() == null) {
            String currentDeptId = fetchEmployeeRelationId(id, "department");
            if (currentDeptId != null) {
                try { form.setDepartmentId(Integer.parseInt(currentDeptId)); }
                catch (NumberFormatException ignored) {}
            }
        }

        if (form.getManagerId() == null && current.getManagerId() != null) {
            form.setManagerId(current.getManagerId());
        }
        return form;
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }

    private String fetchEmployeeRelationId(Integer id, String relation) {
        try {
            EmployeeDTO current = getEmployee(id);
            if (current == null) return null;

            if ("job".equalsIgnoreCase(relation)) {
                return current.getJobId();
            }
            if ("department".equalsIgnoreCase(relation)) {
                return current.getDepartmentId() != null ? String.valueOf(current.getDepartmentId()) : null;
            }
            if ("manager".equalsIgnoreCase(relation)) {
                return current.getManagerId() != null ? String.valueOf(current.getManagerId()) : null;
            }
            return null;
        } catch (Exception ignored) {
            return null;
        }
    }

    private Integer extractTerminalEmployeeId(String href) {
        if (href == null || href.isBlank()) return null;
        String clean = href.trim();
        int brace = clean.indexOf('{');
        if (brace >= 0) clean = clean.substring(0, brace);
        int query = clean.indexOf('?');
        if (query >= 0) clean = clean.substring(0, query);
        if (clean.endsWith("/")) clean = clean.substring(0, clean.length() - 1);
        if (!clean.matches(".*/employees/\\d+$")) return null;
        String last = extractLastPathSegment(clean);
        if (last == null) return null;
        try { return Integer.parseInt(last); } catch (NumberFormatException ignored) { return null; }
    }

    public boolean deleteEmployee(Integer id) {
        clearLastError();
        try {
            restTemplate.exchange(baseUrl + "/managers/" + id, HttpMethod.DELETE, null, String.class);
            return true;
        } catch (org.springframework.web.client.HttpClientErrorException e) {

            try {
                restTemplate.exchange(baseUrl + "/employees/" + id, HttpMethod.DELETE, null, String.class);
                return true;
            } catch (org.springframework.web.client.HttpClientErrorException ex) {
                System.out.println("DELETE failed [" + ex.getStatusCode() + "]: " + ex.getResponseBodyAsString());
                setLastError(extractBackendErrorMessage(ex, "Delete failed."));
                return false;
            } catch (Exception ex) {
                ex.printStackTrace();
                setLastError("Delete failed due to an unexpected server error.");
                return false;
            }
        } catch (Exception e) {
            e.printStackTrace();
            setLastError("Delete failed due to an unexpected server error.");
            return false;
        }
    }

    private Map<String, Object> buildPayload(EmployeeFormDTO form) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("firstName",     form.getFirstName());
        payload.put("lastName",      form.getLastName());
        payload.put("email",         form.getEmail());
        payload.put("phoneNumber",   form.getPhoneNumber());
        payload.put("salary",        form.getSalary());
        payload.put("commissionPct", form.getCommissionPct());

        if (form.getHireDate() != null) {
            payload.put("hireDate", form.getHireDate().toString());
        }
        if (form.getDepartmentId() != null) {
            payload.put("department", baseUrl + "/department/" + form.getDepartmentId());
        }
        if (form.getJobId() != null && !form.getJobId().isBlank()) {
            payload.put("job", baseUrl + "/jobs/" + form.getJobId());
        }
        if (form.getManagerId() != null) {
            payload.put("manager", baseUrl + "/employees/" + form.getManagerId());
        }
        return payload;
    }

    private boolean sendRequest(String url, Map<String, Object> payload, HttpMethod method) {
        try {
            ensureRequiredEmployeeRelations(url, payload, method);
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.setAccept(Collections.singletonList(MediaType.APPLICATION_JSON));
            HttpEntity<Map<String, Object>> req = new HttpEntity<>(payload, headers);
            restTemplate.exchange(url, method, req, String.class);
            return true;
        } catch (org.springframework.web.client.HttpClientErrorException e) {
            System.out.println("REQUEST FAILED [" + e.getStatusCode() + "]: " + e.getResponseBodyAsString());
            setLastError(extractBackendErrorMessage(e, "Request failed."));
            return false;
        } catch (Exception e) {
            e.printStackTrace();
            setLastError("Request failed due to an unexpected server error.");
            return false;
        }
    }

    private String extractBackendErrorMessage(HttpStatusCodeException ex, String fallback) {
        try {
            String body = ex.getResponseBodyAsString();
            if (body != null && !body.isBlank()) {
                String trimmed = body.trim();
                if (trimmed.startsWith("{")) {
                    JsonNode n = objectMapper.readTree(trimmed);
                    String msg = firstText(n, "message", "error", "details", "title");
                    if (msg != null && !msg.isBlank()) return msg;
                }
                return trimmed;
            }
        } catch (Exception ignored) {}

        String statusText = ex.getStatusText();
        if (statusText != null && !statusText.isBlank()) return statusText;
        return fallback;
    }

    private void ensureRequiredEmployeeRelations(String url, Map<String, Object> payload, HttpMethod method) {
        if (method != HttpMethod.PATCH || payload == null || url == null) return;
        if (!url.contains("/employees/")) return;

        Integer employeeId = extractEmployeeIdFromUrl(url);
        if (employeeId == null) return;

        if (isBlankPayloadValue(payload.get("job"))) {
            String currentJobId = fetchEmployeeRelationId(employeeId, "job");
            if (currentJobId != null && !currentJobId.isBlank()) {
                payload.put("job", baseUrl + "/jobs/" + currentJobId);
            }
        }

        if (isBlankPayloadValue(payload.get("department"))) {
            String currentDeptId = fetchEmployeeRelationId(employeeId, "department");
            if (currentDeptId != null && !currentDeptId.isBlank()) {
                payload.put("department", baseUrl + "/department/" + currentDeptId);
            }
        }
    }

    private Integer extractEmployeeIdFromUrl(String url) {
        try {
            int idx = url.indexOf("/employees/");
            if (idx < 0) return null;
            String tail = url.substring(idx + "/employees/".length());
            int slash = tail.indexOf('/');
            if (slash >= 0) tail = tail.substring(0, slash);
            int q = tail.indexOf('?');
            if (q >= 0) tail = tail.substring(0, q);
            if (tail.isBlank()) return null;
            return Integer.parseInt(tail.trim());
        } catch (Exception ignored) {
            return null;
        }
    }

    private boolean isBlankPayloadValue(Object value) {
        if (value == null) return true;
        if (!(value instanceof String s)) return false;
        return s.trim().isEmpty();
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
            if (totalPages <= 7) {
                for (int i = 0; i < totalPages; i++) pages.add(i);
                return pages;
            }
            pages.add(0);
            if (currentPage > 2) pages.add(-1);
            for (int i = Math.max(1, currentPage - 1); i <= Math.min(totalPages - 2, currentPage + 1); i++) {
                pages.add(i);
            }
            if (currentPage < totalPages - 3) pages.add(-1);
            pages.add(totalPages - 1);
            return pages;
        }
    }
}
