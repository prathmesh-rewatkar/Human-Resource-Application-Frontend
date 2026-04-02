package com.example.HumanResourceApplicationFrontend.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.math.BigDecimal;

@JsonIgnoreProperties(ignoreUnknown = true)
public class JobDTO {

    private String jobId;
    private String jobTitle;
    private BigDecimal minSalary;
    private BigDecimal maxSalary;

    public JobDTO() {}

    public JobDTO(String jobId, String jobTitle, BigDecimal minSalary, BigDecimal maxSalary) {
        this.jobId = jobId;
        this.jobTitle = jobTitle;
        this.minSalary = minSalary;
        this.maxSalary = maxSalary;
    }

    public String getJobId() { return jobId; }
    public void setJobId(String jobId) { this.jobId = jobId; }

    public String getJobTitle() { return jobTitle; }
    public void setJobTitle(String jobTitle) { this.jobTitle = jobTitle; }

    public BigDecimal getMinSalary() { return minSalary; }
    public void setMinSalary(BigDecimal minSalary) { this.minSalary = minSalary; }

    public BigDecimal getMaxSalary() { return maxSalary; }
    public void setMaxSalary(BigDecimal maxSalary) { this.maxSalary = maxSalary; }

    public String getMinSalaryFormatted() {
        return minSalary != null ? "$" + String.format("%,.0f", minSalary) : "—";
    }

    public String getMaxSalaryFormatted() {
        return maxSalary != null ? "$" + String.format("%,.0f", maxSalary) : "—";
    }
}