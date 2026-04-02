package com.example.HumanResourceApplicationFrontend.model;


import lombok.Data;

import java.time.LocalDate;

@Data
public class EmployeeFormDTO {

    private Integer employeeId;

    private String firstName;
    private String lastName;
    private String email;
    private String phoneNumber;

    private LocalDate hireDate;
    private Double salary;
    private Double commissionPct;

    private Integer departmentId;
    private String jobId;

    private Integer managerId;
}