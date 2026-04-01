package com.example.HumanResourceApplicationFrontend.controller;

import com.example.HumanResourceApplicationFrontend.model.DepartmentDTO;
import com.example.HumanResourceApplicationFrontend.model.EmployeeDTO;
import com.example.HumanResourceApplicationFrontend.model.EmployeeRecordDTO;
import com.example.HumanResourceApplicationFrontend.model.ManagerDTO;
import com.example.HumanResourceApplicationFrontend.service.ManagerService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;

@Controller
@RequestMapping("/managers")
public class ManagerController {

    private final ManagerService managerService;

    public ManagerController(ManagerService managerService) {
        this.managerService = managerService;
    }

    @GetMapping
    public String listManagers(
            @RequestParam(required = false) String email,
            @RequestParam(required = false) String firstname,
            @RequestParam(required = false) String lastname,
            @RequestParam(required = false) Integer departmentId,
            @RequestParam(defaultValue = "0") int page,
            Model model) {

        //Integer deptId = null;

//        if (departmentId != null && !departmentId.isBlank()) {
//            deptId = Integer.parseInt(departmentId);
//        }

        List<ManagerDTO> managers = managerService.getManagers(
                email, firstname, lastname, departmentId, page, 5
        );
        model.addAttribute("pageSize", 5);
        model.addAttribute("departments", managerService.getAllDepartments());
        model.addAttribute("managers", managers);
        model.addAttribute("currentPage", page);
        model.addAttribute("departmentId", departmentId); // for selected dropdown

        return "Manager/managers";
    }

    @GetMapping("/{id}")
    public String getManagerDetails(@PathVariable Integer id, Model model) {


        List<EmployeeRecordDTO> subordinates = managerService.getSubordinates(id);

        //List<EmployeeDTO> hierarchy = managerService.getHierarchy(id);

        model.addAttribute("subordinates", subordinates);
        //model.addAttribute("hierarchy",hierarchy);


        return "Manager/manager-detail";
    }

    @PostMapping("/update")
    public String updateManager(@RequestParam Integer employeeId,
                                @RequestParam Integer newManagerId,
                                RedirectAttributes redirectAttributes) {

        try {

            System.out.println(employeeId);
            System.out.println(newManagerId);
            managerService.updateManager(employeeId, newManagerId);
            redirectAttributes.addFlashAttribute("success", "Manager updated successfully");

        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }

        return "redirect:/Manager/managers";
    }
}