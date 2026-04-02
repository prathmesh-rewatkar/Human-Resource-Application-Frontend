//package com.example.HumanResourceApplicationFrontend.model;
//
//import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
//
//@JsonIgnoreProperties(ignoreUnknown = true)
//public class DepartmentDTO {
//    private Integer departmentId;
//    private String departmentName;
//    private LocationDTO location;
//    private EmployeeDTO manager;
//
//    // Getters and Setters
//    public Integer getDepartmentId() { return departmentId; }
//    public void setDepartmentId(Integer departmentId) { this.departmentId = departmentId; }
//
//    public String getDepartmentName() { return departmentName; }
//    public void setDepartmentName(String departmentName) { this.departmentName = departmentName; }
//
//    public LocationDTO getLocation() { return location; }
//    public void setLocation(LocationDTO location) { this.location = location; }
//
//    public EmployeeDTO getManager() { return manager; }
//    public void setManager(EmployeeDTO manager) { this.manager = manager; }
//}



package com.example.HumanResourceApplicationFrontend.model;

import com.example.HumanResourceApplicationFrontend.model.EmployeeDTO;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class DepartmentDTO {
    private Integer departmentId;
    private String departmentName;
    private LocationDTO location;
    private EmployeeDTO manager;

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class LocationDTO {
        private Integer locationId;
        private String city;
        private String streetAddress;
        private String stateProvince;
        private String postalCode;
    }
}
