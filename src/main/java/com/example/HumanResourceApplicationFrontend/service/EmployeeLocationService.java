package com.example.HumanResourceApplicationFrontend.service;

import com.example.HumanResourceApplicationFrontend.model.EmployeeDTO;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import java.time.LocalDate;
import java.util.*;

@Slf4j
@Service
public class EmployeeLocationService extends BaseService {

    public EmployeeLocationService(RestTemplate restTemplate) {
        super(restTemplate);
    }



    public EmployeeDTO buildEmployee(JsonNode n) {
        EmployeeDTO e = new EmployeeDTO();
        e.setEmployeeId(integer(n, "employeeId"));
        e.setFirstName(str(n, "firstName"));
        e.setLastName(str(n, "lastName"));
        e.setEmail(str(n, "email"));
        e.setPhoneNumber(str(n, "phoneNumber"));

        // FIX HERE
        String hireDateStr = str(n, "hireDate");
        if (hireDateStr != null && !hireDateStr.isEmpty()) {
            e.setHireDate(LocalDate.parse(hireDateStr));
        }

        if (!n.path("salary").isMissingNode() && !n.path("salary").isNull())
            e.setSalary(n.path("salary").asDouble());

        e.setDepartmentName(str(n, "departmentName"));
        e.setJobTitle(str(n, "jobTitle"));
        return e;
    }

    public List<EmployeeDTO> getEmployeesByDepartment(String deptName) {
        try {
            String enc = java.net.URLEncoder.encode(deptName, "UTF-8").replace("+", "%20");
            String json = rawGet("/employees/search/by-department-name?departmentName=" + enc + "&size=200&projection=employeeView");
            List<EmployeeDTO> result = new ArrayList<>();
            for (JsonNode n : embedded(json, "employees"))
                result.add(buildEmployee(n));
            log.info("getEmployeesByDepartment('{}') → {} employees", deptName, result.size());
            return result;
        } catch (Exception e) {
            log.error("getEmployeesByDepartment '{}': {}", deptName, e.getMessage());
            return Collections.emptyList();
        }
    }
}
