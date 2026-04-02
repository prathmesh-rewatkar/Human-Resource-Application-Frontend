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

    private Integer departmentId;
    private String jobId;

    private Integer managerId;

    public String getFullName() {
        return (firstName != null ? firstName : "") + " " + (lastName != null ? lastName : "");
    }

    public String getSalaryFormatted() {
        return salary != null ? "$" + String.format("%,.0f", salary) : "—";
    }

    public String getInitials() {
        String f = firstName != null && !firstName.isEmpty() ? String.valueOf(firstName.charAt(0)) : "";
        String l = lastName != null && !lastName.isEmpty() ? String.valueOf(lastName.charAt(0)) : "";
        return f + l;
    }
}