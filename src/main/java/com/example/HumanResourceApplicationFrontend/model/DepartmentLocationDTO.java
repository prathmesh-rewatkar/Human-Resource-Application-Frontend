package com.example.HumanResourceApplicationFrontend.model;


import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class DepartmentLocationDTO {
    private Integer departmentId;
    private String  departmentName;
    private ManagerDTO manager;



    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class ManagerDTO {
        private String firstName;
        private String lastName;
        private String email;
        private String phoneNumber;
    }
}

