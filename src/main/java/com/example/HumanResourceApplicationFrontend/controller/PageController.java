package com.example.HumanResourceApplicationFrontend.controller;

import com.example.HumanResourceApplicationFrontend.model.DepartmentDTO;
import com.example.HumanResourceApplicationFrontend.model.EmployeeDTO;
import com.example.HumanResourceApplicationFrontend.model.LocationDTO;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
import java.util.List;

@Controller
public class PageController {
    @Value("${backend.api.url}")
    private String backendUrl;

    private final RestTemplate restTemplate = new RestTemplate();
    private final ObjectMapper objectMapper = new ObjectMapper();

    // ── Page 1: Team page ──
    @GetMapping("/")
    public String page1() {
        return "page1";
    }

    // ── Page 2: Department list ──
    @GetMapping("/departments")
    public String page2(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String city,
            Model model) {

        List<DepartmentDTO> departments = new ArrayList<>();
        long totalCount = 0;

        try {
            String url;

            // Build URL based on filter
            if (search != null && !search.isEmpty()) {
                url = backendUrl + "/department/search/" +
                        "findByDepartmentNameContainingIgnoreCase?name=" + search;
            } else if (city != null && !city.isEmpty()) {
                url = backendUrl + "/department/search/findByLocation_City?city=" + city;
            } else {
                url = backendUrl + "/department?size=100&projection=deptView";
            }

            // Call backend API
            String response = restTemplate.getForObject(url, String.class);
            JsonNode root = objectMapper.readTree(response);

            // Parse departments from _embedded.departments
            JsonNode deptArray = root.path("_embedded").path("departments");
            if (deptArray.isArray()) {
                for (JsonNode node : deptArray) {
                    DepartmentDTO dept = new DepartmentDTO();
                    dept.setDepartmentName(node.path("departmentName").asText());

                    // Get ID from _links.self.href
                    String selfHref = node.path("_links").path("self").path("href").asText();
                    if (!selfHref.isEmpty()) {
                        String idStr = selfHref.replaceAll(".*/", "");
                        try {
                            dept.setDepartmentId(Integer.parseInt(idStr));
                        } catch (NumberFormatException ignored) {}
                    }

                    // Parse nested location
                    JsonNode locNode = node.path("_embedded").path("location");
                    if (!locNode.isMissingNode()) {
                        LocationDTO loc =
                                new LocationDTO();
                        loc.setCity(locNode.path("city").asText(null));
                        loc.setStreetAddress(locNode.path("streetAddress").asText(null));
                        loc.setStateProvince(locNode.path("stateProvince").asText(null));
                        dept.setLocation(loc);
                    }

                    // Parse nested manager
//                    JsonNode mgrNode = node.path("manager");
//                    if (!mgrNode.isMissingNode() && !mgrNode.isNull()) {
//                        com.example.HumanResourceApplicationFrontend.model.Manager mgr =
//                                new com.example.HumanResourceApplicationFrontend.model.Manager();
//                        mgr.setFirstName(mgrNode.path("firstName").asText(null));
//                        mgr.setLastName(mgrNode.path("lastName").asText(null));
//                        mgr.setEmail(mgrNode.path("email").asText(null));
//                        dept.setManager(mgr);
//                    }

                    departments.add(dept);
                }
            }

            // Get total count
            totalCount = root.path("page").path("totalElements").asLong(departments.size());

        } catch (Exception e) {
            e.printStackTrace();
            model.addAttribute("error", "Could not connect to backend: " + e.getMessage());
        }

        model.addAttribute("departments", departments);
        model.addAttribute("totalCount", totalCount);
        model.addAttribute("search", search);
        model.addAttribute("city", city);
        return "DepartmentPage2";
    }

    // ── Page 3: Employees in department ──
    @GetMapping("/departments/{id}/employees")
    public String page3(@PathVariable Integer id, Model model) {

        DepartmentDTO dept = new DepartmentDTO();
        List<EmployeeDTO> employees = new ArrayList<>();

        try {
            // Get department details
            String deptUrl = backendUrl + "/department/" + id + "?projection=deptView";
            String deptResponse = restTemplate.getForObject(deptUrl, String.class);
            JsonNode deptNode = objectMapper.readTree(deptResponse);

            dept.setDepartmentId(id);
            dept.setDepartmentName(deptNode.path("departmentName").asText());

            // Parse location from _embedded
            JsonNode locNode = deptNode.path("_embedded").path("location");
            if (!locNode.isMissingNode()) {
                LocationDTO loc =
                        new LocationDTO();
                loc.setCity(locNode.path("city").asText(null));
                loc.setStreetAddress(locNode.path("streetAddress").asText(null));
                loc.setStateProvince(locNode.path("stateProvince").asText(null));
                dept.setLocation(loc);
            }

            // Get manager
//            try {
//                String mgrUrl = backendUrl + "/department/" + id + "/manager";
//                String mgrResponse = restTemplate.getForObject(mgrUrl, String.class);
//                JsonNode mgrNode = objectMapper.readTree(mgrResponse);
//                com.example.HRFrontend.model.Manager mgr =
//                        new com.example.HRFrontend.model.Manager();
//                mgr.setFirstName(mgrNode.path("firstName").asText(null));
//                mgr.setLastName(mgrNode.path("lastName").asText(null));
//                mgr.setEmail(mgrNode.path("email").asText(null));
//                dept.setManager(mgr);
//            } catch (Exception ignored) {}

            // Get employees
            String empUrl = backendUrl + "/department/" + id + "/employees";
            String empResponse = restTemplate.getForObject(empUrl, String.class);
            JsonNode empRoot = objectMapper.readTree(empResponse);
            JsonNode empArray = empRoot.path("_embedded").path("employees");

            if (empArray.isArray()) {
                for (JsonNode empNode : empArray) {
                    EmployeeDTO emp = new EmployeeDTO();
                    emp.setFirstName(empNode.path("firstName").asText(null));
                    emp.setLastName(empNode.path("lastName").asText(null));
                    emp.setEmail(empNode.path("email").asText(null));
                    emp.setPhoneNumber(empNode.path("phoneNumber").asText(null));
                    emp.setSalary(empNode.path("salary").asDouble(0));

                    // Parse job
//                    JsonNode jobNode = empNode.path("job");
//                    if (!jobNode.isMissingNode() && !jobNode.isNull()) {
//                        Job job = new Job();
//                        job.setJobTitle(jobNode.path("jobTitle").asText(null));
//                        emp.setJob(job);
//                    }
                    employees.add(emp);
                }
            }

        } catch (Exception e) {
            e.printStackTrace();
            model.addAttribute("error", "Could not connect to backend: " + e.getMessage());
        }

        model.addAttribute("department", dept);
        model.addAttribute("employees", employees);
        return "DepartmentPage3";
    }

}
