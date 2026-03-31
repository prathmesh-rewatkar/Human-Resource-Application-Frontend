package com.example.HumanResourceApplicationFrontend.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

import java.time.LocalDate;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class EmployeeDTO {
    private Integer employeeId;
    private String firstName;
    private String lastName;
    private String email;
    private String phoneNumber;
    private LocalDate hireDate;
    private Double salary;
    private Double commissionPct;
    private String departmentName;
    private String jobTitle;
    // For add-form transient fields
    private String department;
    private String job;
    private Integer managerId;
}