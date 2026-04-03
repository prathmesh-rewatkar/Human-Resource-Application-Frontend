package com.example.HumanResourceApplicationFrontend.controller;

import com.example.HumanResourceApplicationFrontend.model.*;
import com.example.HumanResourceApplicationFrontend.service.EmployeeService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/employees")
@RequiredArgsConstructor
public class EmployeeController {

    private final EmployeeService service;

    @GetMapping
    public String listEmployees(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "") String search,
            @RequestParam(defaultValue = "") String dept,
            @RequestParam(defaultValue = "") String job,
            Model model) {

        var result = service.getEmployees(page, search, dept, job);

        model.addAttribute("page", result);
        model.addAttribute("employees", result.getContent());
        loadDropdowns(model);

        model.addAttribute("search", search);
        model.addAttribute("dept", dept);
        model.addAttribute("job", job);
        model.addAttribute("currentPage", page);

        return "employees/list";
    }

    @GetMapping("/add")
    public String addForm(Model model) {
        model.addAttribute("form", new EmployeeFormDTO());
        loadDropdowns(model);
        return "employees/add";
    }

    @PostMapping("/add")
    public String addEmployee(@ModelAttribute EmployeeFormDTO form,
                              RedirectAttributes redirect) {

        boolean success = service.createEmployee(form);
        handleRedirect(success, redirect, "Employee added successfully!", "Failed to add employee");

        return "redirect:/employees";
    }

    @GetMapping("/edit/{id}")
    public String editForm(@PathVariable Integer id, Model model) {
        EmployeeDTO emp = service.getEmployee(id);
        if (emp == null) return "redirect:/employees";

        model.addAttribute("form", service.mapToForm(emp));
        loadDropdowns(model);

        return "employees/edit";
    }

    @PostMapping("/edit/{id}")
    public String updateEmployee(@PathVariable Integer id,
                                 @ModelAttribute EmployeeFormDTO form,
                                 RedirectAttributes redirect) {

        boolean success = service.updateEmployee(id, form);

        if (!success) {
            System.out.println("Update failed for ID: " + id);
        }

        handleRedirect(success, redirect,
                "Employee updated successfully!",
                "Failed to update employee");

        return "redirect:/employees";
    }

    @PostMapping("/delete/{id}")
    public String deleteEmployee(@PathVariable Integer id,
                                 RedirectAttributes redirect) {

        boolean success = service.deleteEmployee(id);
        handleRedirect(success, redirect, "Employee deleted successfully.", "Failed to delete employee");

        return "redirect:/employees";
    }

    private void loadDropdowns(Model model) {
        model.addAttribute("departments", service.getDepartments());
        model.addAttribute("jobs", service.getJobs());
        model.addAttribute("managers", service.getManagers());
    }

    private void handleRedirect(boolean success, RedirectAttributes redirect,
                                String successMsg, String errorMsg) {

        if (success) {
            redirect.addFlashAttribute("toastMsg", successMsg);
            redirect.addFlashAttribute("toastType", "success");
        } else {
            redirect.addFlashAttribute("toastMsg", errorMsg);
            redirect.addFlashAttribute("toastType", "error");
        }
    }
}