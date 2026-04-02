package com.example.HumanResourceApplicationFrontend.service;


import com.example.HumanResourceApplicationFrontend.model.DepartmentLocationDTO;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.*;

@Slf4j
@Service
public class DepartmentLocationService extends BaseService {

    public DepartmentLocationService(RestTemplate restTemplate) {
        super(restTemplate);
    }

    public DepartmentLocationDTO buildDepartment(JsonNode n) {
        DepartmentLocationDTO d = new DepartmentLocationDTO();
        d.setDepartmentId(integer(n, "departmentId"));
        d.setDepartmentName(str(n, "departmentName"));

        JsonNode mgr = n.path("manager");
        if (!mgr.isMissingNode() && mgr.isObject() && !mgr.path("firstName").isMissingNode()) {
            DepartmentLocationDTO.ManagerDTO m = new DepartmentLocationDTO.ManagerDTO();
            m.setFirstName(str(mgr, "firstName"));
            m.setLastName(str(mgr, "lastName"));
            m.setEmail(str(mgr, "email"));
            m.setPhoneNumber(str(mgr, "phoneNumber"));
            d.setManager(m);
        }
        return d;
    }

    public List<DepartmentLocationDTO> getDepartmentsByLocation(Integer locationId) {
        String json = rawGet("/department/search/findByLocation_LocationId?locationId=" + locationId + "&projection=deptView");
        List<DepartmentLocationDTO> result = new ArrayList<>();
        for (JsonNode n : embedded(json, "departments"))
            result.add(buildDepartment(n));
        log.info("getDepartmentsByLocation({}) → {} depts", locationId, result.size());
        return result;
    }
}
