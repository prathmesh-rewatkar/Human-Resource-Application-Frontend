package com.example.HumanResourceApplicationFrontend.controller;

import com.example.HumanResourceApplicationFrontend.model.EmployeeDTO;
import com.example.HumanResourceApplicationFrontend.model.JobDTO;
import com.example.HumanResourceApplicationFrontend.service.JobEmployeeService;
import com.example.HumanResourceApplicationFrontend.service.JobService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Controller
public class JobController {

    private final JobService jobService;
    private final JobEmployeeService jobEmployeeService;
    private final RestTemplate restTemplate;

    @Value("${backend.api.url}")
    private String backendUrl;

    public JobController(JobService jobService, JobEmployeeService jobEmployeeService, RestTemplate restTemplate) {
        this.jobService = jobService;
        this.jobEmployeeService = jobEmployeeService;
        this.restTemplate = restTemplate;
    }

    // ── JOBS LIST PAGE ────────────────────────────────────────────
    @GetMapping("/jobs-page")
    public String jobsPage(
            @RequestParam(required = false) String title,
            @RequestParam(required = false) BigDecimal minSalary,
            @RequestParam(required = false) BigDecimal maxSalary,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(required = false) String successMsg,
            Model model) {

        List<JobDTO> jobs;
        // page size must match what backend returns (5 per page)
        int pageSize = 5;

        if (title != null && !title.isBlank()) {
            jobs = jobService.searchByTitle(title.trim());
            model.addAttribute("searchTitle", title);
            model.addAttribute("pageOffset", 0); // search results start at 1

        } else if (minSalary != null && maxSalary != null) {
            jobs = jobService.searchBySalaryRange(minSalary, maxSalary);
            model.addAttribute("searchMin", minSalary);
            model.addAttribute("searchMax", maxSalary);
            model.addAttribute("pageOffset", 0);

        } else if (minSalary != null) {
            jobs = jobService.searchByMinSalary(minSalary);
            model.addAttribute("searchMin", minSalary);
            model.addAttribute("pageOffset", 0);

        } else if (maxSalary != null) {
            jobs = jobService.searchByMaxSalary(maxSalary);
            model.addAttribute("searchMax", maxSalary);
            model.addAttribute("pageOffset", 0);

        } else {
            jobs = jobService.getAllJobs(page);
            // --- Fixed: pass offset so row # on page 2 starts at 6, page 3 at 11 etc ---
            model.addAttribute("pageOffset", page * pageSize);
        }

        model.addAttribute("jobs", jobs);
        model.addAttribute("currentPage", page);
        model.addAttribute("total", jobs.size());
        if (successMsg != null) model.addAttribute("successMsg", successMsg);

        return "jobs/jobsPage1";
    }

    // ── SHOW CREATE FORM ──────────────────────────────────────────
    @GetMapping("/jobs-page/create")
    public String showCreateForm() {
        return "jobs/createJob";
    }

    // ── CREATE JOB ────────────────────────────────────────────────
    @PostMapping("/jobs-page/create")
public String handleCreate(
        @RequestParam String jobId,
        @RequestParam String jobTitle,
        @RequestParam(required = false) BigDecimal minSalary,
        @RequestParam(required = false) BigDecimal maxSalary,
        Model model) {

    // --- Validation ---
    if (jobId == null || jobId.isBlank()) {
        model.addAttribute("errorMsg", "Job ID is required.");
        return "jobs/Createjob";
    }
    if (jobTitle == null || jobTitle.isBlank()) {
        model.addAttribute("errorMsg", "Job Title is required.");
        return "jobs/Createjob";
    }
    if (minSalary != null && maxSalary != null && minSalary.compareTo(maxSalary) > 0) {
        model.addAttribute("errorMsg", "Min Salary must be less than or equal to Max Salary.");
        // Send values back so user doesn't retype everything
        model.addAttribute("jobId", jobId);
        model.addAttribute("jobTitle", jobTitle);
        model.addAttribute("minSalary", minSalary);
        model.addAttribute("maxSalary", maxSalary);
        return "jobs/Createjob";
    }

        try {
            Map<String, Object> body = new HashMap<>();
            body.put("jobId", jobId.trim().toUpperCase());
            body.put("jobTitle", jobTitle.trim());
            if (minSalary != null) body.put("minSalary", minSalary);
            if (maxSalary != null) body.put("maxSalary", maxSalary);

            restTemplate.postForEntity(backendUrl + "/jobs", body, Void.class);
            return "redirect:/jobs-page?successMsg=Job+created+successfully";
        } catch (Exception e) {
            model.addAttribute("errorMsg", "Failed to create job.");
            return "jobs/Createjob";
        }
    }

    // ── SHOW EDIT FORM ────────────────────────────────────────────
    @GetMapping("/jobs-page/{jobId}/edit")
    public String showEditForm(@PathVariable String jobId, Model model) {
        JobDTO job = jobService.getJobById(jobId);
        model.addAttribute("job", job);
        return "jobs/EditJob";
    }

    // ── EDIT JOB ──────────────────────────────────────────────────
@PostMapping("/jobs-page/{jobId}/edit")
public String handleEdit(@PathVariable String jobId, @RequestParam String jobTitle, Model model) {

    // --- Validation ---
    if (jobTitle == null || jobTitle.isBlank()) {
        model.addAttribute("errorMsg", "Job Title cannot be empty.");
        JobDTO job = jobService.getJobById(jobId);
        model.addAttribute("job", job);
        return "jobs/EditJob";
    }
    try {
        Map<String, Object> body = new HashMap<>();
        body.put("jobTitle", jobTitle.trim());

        // RestTemplate sends PATCH to the backend internally — browser never sees it
        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(body);
        restTemplate.exchange(backendUrl + "/jobs/" + jobId, HttpMethod.PATCH, entity, Void.class);

        return "redirect:/jobs-page?successMsg=Job+updated+successfully";
    } catch (Exception e) {
        model.addAttribute("errorMsg", "Update failed.");
        JobDTO job = jobService.getJobById(jobId);
        model.addAttribute("job", job);
        return "jobs/EditJob";
    }
}

    // ── EMPLOYEES BY JOB PAGE ─────────────────────────────────────
    @GetMapping("/jobs-page/{jobId}/employees")
    public String employeesPage(
            @PathVariable String jobId,
            @RequestParam(defaultValue = "1") int page,
            Model model) {

        JobDTO job = jobService.getJobById(jobId);
        List<EmployeeDTO> allEmployees = jobEmployeeService.getEmployeesByJobTitle(job.getJobTitle());

        int pageSize = 10;
        int totalEmployees = allEmployees.size();
        int totalPages = (int) Math.ceil((double) totalEmployees / pageSize);
        if (page < 1) page = 1;
        if (totalPages > 0 && page > totalPages) page = totalPages;

        int start = (page - 1) * pageSize;
        int end = Math.min(start + pageSize, totalEmployees);
        List<EmployeeDTO> pageEmployees = totalEmployees > 0
                ? allEmployees.subList(start, end)
                : allEmployees;

        model.addAttribute("job", job);
        model.addAttribute("jobId", jobId);
        model.addAttribute("employees", pageEmployees);
        model.addAttribute("currentPage", page);
        model.addAttribute("totalPages", totalPages);
        model.addAttribute("totalEmployees", totalEmployees);
        // offset so row # on page 2 starts at 11, page 3 at 21 etc
        model.addAttribute("pageOffset", (page - 1) * pageSize);

        return "jobs/jobsPage2";
    }
}