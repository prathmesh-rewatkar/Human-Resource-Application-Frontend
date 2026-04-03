package com.example.HumanResourceApplicationFrontend.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

import java.time.LocalDate;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class JobHistoryDTO {
    private LocalDate startDate;
    private LocalDate endDate;
    private String jobTitle;
    private String departmentName;
    private String employeeName;
}
