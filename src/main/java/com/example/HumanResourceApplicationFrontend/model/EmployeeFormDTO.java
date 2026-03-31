package com.example.HumanResourceApplicationFrontend.model;

import lombok.Data;
import java.time.LocalDate;

@Data
public class EmployeeFormDTO {
    private String firstName;
    private String lastName;
    private String email;
    private String phoneNumber;
    private LocalDate hireDate;
    private Double salary;
    private Double commissionPct;
    private String departmentName;
    private String jobTitle;
    private Integer managerId;
}