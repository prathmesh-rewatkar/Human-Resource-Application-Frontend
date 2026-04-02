package com.example.HumanResourceApplicationFrontend.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;
import com.example.HumanResourceApplicationFrontend.model.JobDTO;

import java.math.BigDecimal;
import java.util.*;

@Service
public class JobService {

    private final RestTemplate restTemplate;

    @Value("${backend.api.url}")
    private String baseUrl;

    public JobService(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    public List<JobDTO> getAllJobs(int page) {
        try {
            String url = UriComponentsBuilder.fromHttpUrl(baseUrl + "/jobs")
                    .queryParam("page", page)
                    .queryParam("size", 5)
                    .queryParam("projection", "jobDetails")
                    .toUriString();
            Map response = restTemplate.getForObject(url, Map.class);
            return extractJobs(response);
        } catch (Exception e) {
            return Collections.emptyList();
        }
    }

    public JobDTO getJobById(String jobId) {
        try {
            String url = baseUrl + "/jobs/" + jobId + "?projection=jobDetails";
            Map job = restTemplate.getForObject(url, Map.class);
            return mapToJobDTO(job, jobId);
        } catch (Exception e) {
            return null;
        }
    }

    public List<JobDTO> searchByTitle(String title) {
        try {
            String url = baseUrl + "/jobs/search/findByJobTitleContainingIgnoreCase?jobTitle=" + title;
            Map response = restTemplate.getForObject(url, Map.class);
            return extractJobs(response);
        } catch (Exception e) {
            return Collections.emptyList();
        }
    }

    // --- Fixed: was missing, caused wrong salary search results ---
    public List<JobDTO> searchByMinSalary(BigDecimal minSalary) {
        try {
            String url = UriComponentsBuilder
                    .fromHttpUrl(baseUrl + "/jobs/search/findByMinSalaryGreaterThanEqual")
                    .queryParam("minSalary", minSalary)
                    .queryParam("size", 100)
                    .toUriString();
            Map response = restTemplate.getForObject(url, Map.class);
            return extractJobs(response);
        } catch (Exception e) {
            return Collections.emptyList();
        }
    }

    // --- Fixed: was missing, caused wrong salary search results ---
    public List<JobDTO> searchByMaxSalary(BigDecimal maxSalary) {
        try {
            String url = UriComponentsBuilder
                    .fromHttpUrl(baseUrl + "/jobs/search/findByMaxSalaryLessThanEqual")
                    .queryParam("maxSalary", maxSalary)
                    .queryParam("size", 100)
                    .toUriString();
            Map response = restTemplate.getForObject(url, Map.class);
            return extractJobs(response);
        } catch (Exception e) {
            return Collections.emptyList();
        }
    }

    public List<JobDTO> searchBySalaryRange(BigDecimal min, BigDecimal max) {
        try {
            String url = UriComponentsBuilder.fromHttpUrl(baseUrl + "/jobs/search/findByMinSalaryBetween")
                    .queryParam("min", min)
                    .queryParam("max", max)
                    .queryParam("size", 100)
                    .queryParam("projection", "jobDetails")
                    .toUriString();
            Map response = restTemplate.getForObject(url, Map.class);
            return extractJobs(response);
        } catch (Exception e) {
            return Collections.emptyList();
        }
    }

    private List<JobDTO> extractJobs(Map response) {
        if (response == null) return Collections.emptyList();
        Map embedded = (Map) response.get("_embedded");
        if (embedded == null) return Collections.emptyList();

        List<Map> rawJobs = (List<Map>) embedded.get("jobs");
        if (rawJobs == null) return Collections.emptyList();

        List<JobDTO> jobs = new ArrayList<>();
        for (Map raw : rawJobs) {
            String id = extractJobId(raw);
            jobs.add(mapToJobDTO(raw, id));
        }
        return jobs;
    }

    private String extractJobId(Map raw) {
        try {
            Map links = (Map) raw.get("_links");
            Map self = (Map) links.get("self");
            String href = (String) self.get("href");
            String id = href.split("/jobs/")[1];
            return id.replaceAll("\\{.*\\}", "").trim();
        } catch (Exception e) {
            return "";
        }
    }

    private JobDTO mapToJobDTO(Map raw, String jobId) {
        JobDTO dto = new JobDTO();
        dto.setJobId(jobId);
        dto.setJobTitle((String) raw.get("jobTitle"));
        if (raw.get("minSalary") != null) dto.setMinSalary(new BigDecimal(raw.get("minSalary").toString()));
        if (raw.get("maxSalary") != null) dto.setMaxSalary(new BigDecimal(raw.get("maxSalary").toString()));
        return dto;
    }
}