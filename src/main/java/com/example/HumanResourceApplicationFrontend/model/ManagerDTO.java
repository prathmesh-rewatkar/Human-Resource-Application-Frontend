package com.example.HumanResourceApplicationFrontend.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true )
public class ManagerDTO {
    private Integer employeeId;
    private String firstName;
    private String lastName;
    private String email;
    private Double salary;
    private String phoneNumber;
    @JsonProperty("department_DepartmentName")
    private String departmentName;
}
