package com.example.HumanResourceApplicationFrontend.controller;

import com.example.HumanResourceApplicationFrontend.model.EmployeeDTO;
import com.example.HumanResourceApplicationFrontend.model.JobHistoryDTO;
import com.example.HumanResourceApplicationFrontend.service.EmployeeService;
import com.example.HumanResourceApplicationFrontend.service.JobHistoryService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;

@Controller
@RequestMapping("/job-history")
@RequiredArgsConstructor
public class JobHistoryController {

    private final EmployeeService    employeeService;
    private final JobHistoryService  jobHistoryService;

    @GetMapping
    public String jobHistory(@RequestParam Integer employeeId, Model model) {

        EmployeeDTO employee = employeeService.getEmployee(employeeId);
        if (employee == null) {
            model.addAttribute("error", "Employee not found (ID: " + employeeId + ")");
            return "employees/job-history";
        }

        List<JobHistoryDTO> histories = jobHistoryService.getJobHistory(employeeId);

        long tenureMonths = 0;
        if (employee.getHireDate() != null) {
            tenureMonths = ChronoUnit.MONTHS.between(employee.getHireDate(), LocalDate.now());
        }
        long tenureYears  = tenureMonths / 12;
        long tenureRemain = tenureMonths % 12;

        String initials = "";
        if (employee.getFirstName() != null && !employee.getFirstName().isEmpty())
            initials += employee.getFirstName().charAt(0);
        if (employee.getLastName() != null && !employee.getLastName().isEmpty())
            initials += employee.getLastName().charAt(0);

        model.addAttribute("employee",     employee);
        model.addAttribute("histories",    histories);
        model.addAttribute("histCount",    histories.size());
        model.addAttribute("tenureYears",  tenureYears);
        model.addAttribute("tenureMonths", tenureRemain);
        model.addAttribute("initials",     initials.toUpperCase());

        return "employees/job-history";
    }
}

