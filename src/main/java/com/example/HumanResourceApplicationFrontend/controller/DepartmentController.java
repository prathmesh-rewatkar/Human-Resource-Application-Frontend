package com.example.HumanResourceApplicationFrontend.controller;

import com.example.HumanResourceApplicationFrontend.model.DepartmentDTO;
import com.example.HumanResourceApplicationFrontend.model.EmployeeDTO;
//import com.example.HumanResourceApplicationFrontend.model.ManagerDTO;
import com.example.HumanResourceApplicationFrontend.service.DepartmentService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
//import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.Map;

@Controller
@RequiredArgsConstructor
public class DepartmentController {
    private final DepartmentService departmentService;

    // ── Page 1: Team page ──
//    @GetMapping("/")
//    public String page1() {
//        return "departments/page1";
//    }

    // ======================== DEPARTMENT ===========================

    // ── Page 2: Department list with pagination + filters ─────────
    @GetMapping("/departments")
    public String page2(
            @RequestParam(defaultValue = "0")  int    page,
            @RequestParam(defaultValue = "10") int    size,
            @RequestParam(required = false)    String search,
            @RequestParam(required = false)    String city,
            @RequestParam(required = false)    String street,
            @RequestParam(required = false)    String mgr,
            Model model) {

        Map<String, Object> result =
                departmentService.getDepartments(page, size, search, city);

        model.addAttribute("departments",   result.get("departments"));
        model.addAttribute("totalElements", result.get("totalElements"));
        model.addAttribute("totalPages",    result.get("totalPages"));
        model.addAttribute("currentPage",   result.get("currentPage"));
        model.addAttribute("pageSize",      result.get("pageSize"));
        model.addAttribute("search",        search);
        model.addAttribute("city",          city);
        model.addAttribute("streetFilter", street);
        model.addAttribute("mgrFilter",    mgr);
        model.addAttribute("locations", departmentService.getAllLocations());
        model.addAttribute("employees", departmentService.getAllEmployees());
        model.addAttribute("error",         result.get("error"));

        return "departments/list";
    }

    // ── Page 3: Employees in department ───────────────────────────
    @GetMapping("/departments/{id}/employees")
    public String page3(@PathVariable Integer id, Model model) {

        DepartmentDTO dept = departmentService.getDepartmentById(id);
        List<EmployeeDTO> employees = departmentService.getEmployeesByDepartment(id);

        if (dept == null) {
            dept = new DepartmentDTO();
            dept.setDepartmentName("Unknown Department");
        }

        model.addAttribute("department", dept);
        model.addAttribute("employees",  employees);
        return "departments/employees";
    }

    // ── REST endpoints called from JS (AJAX) ──────────────────────

    @PostMapping("/api/departments")
    @ResponseBody
    public ResponseEntity<String> createDepartment(
            @RequestParam String  departmentName,
            @RequestParam Integer locationId,
            @RequestParam(required = false) Integer managerId) {
        return departmentService.createDepartment(departmentName, locationId, managerId);
    }

    @PatchMapping("/api/departments/{id}")
    @ResponseBody
    public ResponseEntity<String> updateDepartment(
            @PathVariable Integer id,
            @RequestParam(required = false) String  departmentName,
            @RequestParam(required = false) Integer locationId,
            @RequestParam(required = false) Integer managerId) {
        return departmentService.updateDepartment(id, departmentName, locationId, managerId);
    }

    @DeleteMapping("/api/departments/{id}")
    @ResponseBody
    public ResponseEntity<String> deleteDepartment(@PathVariable Integer id) {
        return departmentService.deleteDepartment(id);
    }


}