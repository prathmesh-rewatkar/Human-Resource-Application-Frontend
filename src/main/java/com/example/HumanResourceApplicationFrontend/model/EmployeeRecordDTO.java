package com.example.HumanResourceApplicationFrontend.model;


import lombok.Data;

@Data
public class EmployeeRecordDTO {

    private Integer employeeId;
    private String firstName;
    private String lastName;
    private String email;

    private String job_JobTitle;
    private String department_DepartmentName;

    // getters setters
}
