package com.example.HumanResourceApplicationFrontend.controller;

import com.example.HumanResourceApplicationFrontend.model.*;
import com.example.HumanResourceApplicationFrontend.service.EmployeeService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.ArrayList;
import java.util.List;

@Controller
@RequestMapping("/employees")
@RequiredArgsConstructor
public class EmployeeController {

    private final EmployeeService employeeService;

    @GetMapping
    public String listEmployees(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "") String search,
            @RequestParam(defaultValue = "") String dept,
            @RequestParam(defaultValue = "") String job,
            Model model) {

        EmployeeService.PageResult<EmployeeDTO> result = employeeService.getEmployees(page, search, dept, job);

        model.addAttribute("page", result);
        model.addAttribute("employees", result.getContent());
        model.addAttribute("departments", employeeService.getDepartments());
        model.addAttribute("jobs", employeeService.getJobs());
        model.addAttribute("search", search);
        model.addAttribute("dept", dept);
        model.addAttribute("job", job);
        model.addAttribute("currentPage", page);

        return "employees/list";
    }

    @GetMapping("/add")
    public String addEmployeeForm(Model model) {
        model.addAttribute("form", new EmployeeFormDTO());
        model.addAttribute("departments", employeeService.getDepartments());
        model.addAttribute("jobs", employeeService.getJobs());
        model.addAttribute("managers", employeeService.getManagers());
        return "employees/add";
    }

    @PostMapping("/add")
    public String submitAddEmployee(
            @ModelAttribute EmployeeFormDTO form,
            RedirectAttributes redirect) {

        boolean ok = employeeService.createEmployee(form);
        if (ok) {
            redirect.addFlashAttribute("toastMsg", "Employee added successfully!");
            redirect.addFlashAttribute("toastType", "success");
        } else {
            String msg = employeeService.consumeLastError();
            if (msg == null || msg.isBlank()) {
                msg = "Failed to add employee. Check input and try again.";
            }
            redirect.addFlashAttribute("toastMsg", msg);
            redirect.addFlashAttribute("toastType", "error");
        }
        return "redirect:/employees";
    }

    @GetMapping("/edit/{id}")
    public String editEmployeeForm(@PathVariable Integer id, Model model) {
        EmployeeDTO e = employeeService.getEmployee(id);
        if (e == null) return "redirect:/employees";

        List<DepartmentDTO> departments = employeeService.getDepartments();
        List<JobDTO> jobs = employeeService.getJobs();
        List<EmployeeDTO> managers = employeeService.getManagers();

        EmployeeFormDTO form = new EmployeeFormDTO();
        form.setEmployeeId(e.getEmployeeId());
        form.setFirstName(e.getFirstName());
        form.setLastName(e.getLastName());
        form.setEmail(e.getEmail());
        form.setPhoneNumber(e.getPhoneNumber());
        form.setHireDate(e.getHireDate());
        form.setSalary(e.getSalary());
        form.setCommissionPct(e.getCommissionPct());
        form.setDepartmentId(e.getDepartmentId());
        form.setJobId(e.getJobId());
        form.setManagerId(e.getManagerId());

        if (form.getDepartmentId() == null && e.getDepartmentName() != null) {
            for (DepartmentDTO d : departments) {
                if (d.getDepartmentName() != null
                        && d.getDepartmentName().equalsIgnoreCase(e.getDepartmentName())) {
                    form.setDepartmentId(d.getDepartmentId());
                    break;
                }
            }
        }
        if ((form.getJobId() == null || form.getJobId().isBlank()) && e.getJobTitle() != null) {
            for (JobDTO j : jobs) {
                if (j.getJobTitle() != null && j.getJobTitle().equalsIgnoreCase(e.getJobTitle())) {
                    form.setJobId(j.getJobId());
                    break;
                }
            }
        }

        departments = new ArrayList<>(departments);
        jobs = new ArrayList<>(jobs);
        managers = new ArrayList<>(managers);

        if (form.getDepartmentId() != null) {
            boolean present = false;
            for (DepartmentDTO d : departments) {
                if (form.getDepartmentId().equals(d.getDepartmentId())) {
                    present = true;
                    break;
                }
            }
            if (!present) {
                DepartmentDTO d = new DepartmentDTO();
                d.setDepartmentId(form.getDepartmentId());
                d.setDepartmentName(e.getDepartmentName() != null ? e.getDepartmentName() : ("Department " + form.getDepartmentId()));
                departments.add(0, d);
            }
        }

        if (form.getJobId() != null && !form.getJobId().isBlank()) {
            boolean present = false;
            for (JobDTO j : jobs) {
                if (form.getJobId().equals(j.getJobId())) {
                    present = true;
                    break;
                }
            }
            if (!present) {
                JobDTO j = new JobDTO();
                j.setJobId(form.getJobId());
                j.setJobTitle(e.getJobTitle() != null ? e.getJobTitle() : form.getJobId());
                jobs.add(0, j);
            }
        }

        if (form.getManagerId() != null) {
            boolean present = false;
            for (EmployeeDTO m : managers) {
                if (form.getManagerId().equals(m.getEmployeeId())) {
                    present = true;
                    break;
                }
            }
            if (!present) {
                EmployeeDTO m = new EmployeeDTO();
                m.setEmployeeId(form.getManagerId());
                EmployeeDTO managerDetails = employeeService.getEmployee(form.getManagerId());
                if (managerDetails != null) {
                    m.setFirstName(managerDetails.getFirstName());
                    m.setLastName(managerDetails.getLastName());
                } else {
                    m.setFirstName("Manager");
                    m.setLastName("#" + form.getManagerId());
                }
                managers.add(0, m);
            }
        }

        model.addAttribute("form", form);
        model.addAttribute("departments", departments);
        model.addAttribute("jobs", jobs);
        model.addAttribute("managers", managers);
        return "employees/edit";
    }

    @PostMapping("/edit/{id}")
    public String submitEditEmployee(
            @PathVariable Integer id,
            @ModelAttribute EmployeeFormDTO form,
            RedirectAttributes redirect) {

        boolean ok = employeeService.updateEmployee(id, form);
        if (ok) {
            redirect.addFlashAttribute("toastMsg", "Employee updated successfully!");
            redirect.addFlashAttribute("toastType", "success");
        } else {
            String msg = employeeService.consumeLastError();
            if (msg == null || msg.isBlank()) {
                msg = "Failed to update employee.";
            }
            redirect.addFlashAttribute("toastMsg", msg);
            redirect.addFlashAttribute("toastType", "error");
        }
        return "redirect:/employees";
    }

    @PostMapping("/delete/{id}")
    public String deleteEmployee(
            @PathVariable Integer id,
            RedirectAttributes redirect) {

        boolean ok = employeeService.deleteEmployee(id);
        if (ok) {
            redirect.addFlashAttribute("toastMsg", "Employee deleted successfully.");
            redirect.addFlashAttribute("toastType", "success");
        } else {
            String msg = employeeService.consumeLastError();
            if (msg == null || msg.isBlank()) {
                msg = "Cannot delete employee.";
            }
            redirect.addFlashAttribute("toastMsg", msg);
            redirect.addFlashAttribute("toastType", "error");
        }
        return "redirect:/employees";
    }
}