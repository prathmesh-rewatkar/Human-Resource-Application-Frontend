package com.example.HumanResourceApplicationFrontend.controller;

import com.example.HumanResourceApplicationFrontend.model.*;
import com.example.HumanResourceApplicationFrontend.service.CountryService;
import com.example.HumanResourceApplicationFrontend.service.DepartmentLocationService;
import com.example.HumanResourceApplicationFrontend.service.EmployeeLocationService;
import com.example.HumanResourceApplicationFrontend.service.LocationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.*;

@Slf4j
@Controller
@RequiredArgsConstructor
public class LocationController {

    private final LocationService   locationService;
    private final CountryService    countryService;
    private final DepartmentLocationService departmentLocationService;
    private final EmployeeLocationService employeeLocationService;

    @Value("${backend.api.url}")
    private String backendBaseUrl;

    @GetMapping({"/locations", "/locations/"})
    public String listLocations(Model model) {
        List<LocationNDTO> locations = locationService.getAllLocations();
        List<CountryDTO>  countries = countryService.getAllCountries();

        // FIX: use countryName and regionName as distinct keys because
        // the locationDetail projection does NOT expose countryId/regionId.
        // countryId IS exposed via exposeIdsFor — so we use it when available,
        // and fall back to countryName as a string key.
        long uniqueCountries = locations.stream()
                .map(l -> {
                    if (l.getCountry() == null) return null;
                    // prefer countryId (available when exposeIdsFor is set)
                    String id = l.getCountry().getCountryId();
                    return id != null ? id : l.getCountry().getCountryName();
                })
                .filter(Objects::nonNull).distinct().count();

        long uniqueRegions = locations.stream()
                .map(l -> {
                    if (l.getCountry() == null || l.getCountry().getRegion() == null) return null;
                    Integer rid = l.getCountry().getRegion().getRegionId();
                    if (rid != null) return rid.toString();
                    return l.getCountry().getRegion().getRegionName();
                })
                .filter(Objects::nonNull).distinct().count();

        model.addAttribute("locations",       locations);
        model.addAttribute("countries",       countries);
        model.addAttribute("totalLocations",  locations.size());
        model.addAttribute("uniqueCountries", uniqueCountries);
        model.addAttribute("uniqueRegions",   uniqueRegions);
        model.addAttribute("activePage",      "locations");
        return "locations/list";
    }

    @GetMapping("/locations/{id}")
    public String locationDetail(@PathVariable Integer id, Model model) {
        log.info("locationDetail id={}", id);
        System.out.println("hi before location service");
        LocationNDTO location = locationService.getLocationById(id);
        System.out.println("hi after location service");
        if (location == null) {
            model.addAttribute("errorMsg", "Location " + id + " not found.");
            model.addAttribute("locations", Collections.emptyList());
            model.addAttribute("countries", countryService.getAllCountries());
            model.addAttribute("totalLocations", 0);
            model.addAttribute("uniqueCountries", 0);
            model.addAttribute("uniqueRegions", 0);
            model.addAttribute("activePage", "locations");
            return "locations/list";
        }

        List<DepartmentLocationDTO> allDepartments = departmentLocationService.getDepartmentsByLocation(id);
        System.out.println("hi after all departments");
        Map<String, List<EmployeeDTO>> employeesByDept = new LinkedHashMap<>();
        List<DepartmentLocationDTO> deptsWithEmployees = new ArrayList<>();

        for (DepartmentLocationDTO dept : allDepartments) {
            try {
                List<EmployeeDTO> emps = employeeLocationService.getEmployeesByDepartment(dept.getDepartmentName());
                if (!emps.isEmpty()) {
                    employeesByDept.put(dept.getDepartmentName(), emps);
                    deptsWithEmployees.add(dept);
                }
            } catch (Exception e) {
                log.error("Failed to load employees for dept '{}': {}", dept.getDepartmentName(), e.getMessage());
            }
        }
        System.out.println("befor model");

        model.addAttribute("location",        location);
        model.addAttribute("departments",     deptsWithEmployees);
        model.addAttribute("allDeptCount",    allDepartments.size());
        model.addAttribute("employeesByDept", employeesByDept);
        model.addAttribute("activePage",      "locations");
        return "locations/detail";
    }

    @PostMapping("/locations/add")
    public String addLocation(@RequestParam String city,
                              @RequestParam(required = false) String streetAddress,
                              @RequestParam(required = false) String postalCode,
                              @RequestParam(required = false) String stateProvince,
                              @RequestParam String countryId,
                              RedirectAttributes ra) {
        try {
            Map<String, Object> p = new LinkedHashMap<>();
            p.put("city",          city);
            p.put("streetAddress", streetAddress);
            p.put("postalCode",    postalCode);
            p.put("stateProvince", stateProvince);
            p.put("country",       backendBaseUrl + "/countries/" + countryId);
            locationService.createLocation(p);
            ra.addFlashAttribute("successMsg", "Location '" + city + "' added successfully!");
        } catch (Exception e) {
            ra.addFlashAttribute("errorMsg", "Failed to add location: " + extractError(e));
        }
        return "redirect:/locations";
    }

    @PostMapping("/locations/{id}/edit")
    public String editLocation(@PathVariable Integer id,
                               @RequestParam String city,
                               @RequestParam(required = false) String streetAddress,
                               @RequestParam(required = false) String postalCode,
                               @RequestParam(required = false) String stateProvince,
                               @RequestParam String countryId,
                               RedirectAttributes ra) {
        try {
            Map<String, Object> p = new LinkedHashMap<>();
            p.put("city",          city);
            p.put("streetAddress", streetAddress);
            p.put("postalCode",    postalCode);
            p.put("stateProvince", stateProvince);
            p.put("country",       backendBaseUrl + "/countries/" + countryId);
            locationService.updateLocation(id, p);
            ra.addFlashAttribute("successMsg", "Location updated successfully!");
        } catch (Exception e) {
            ra.addFlashAttribute("errorMsg", "Failed to update: " + extractError(e));
        }
        return "redirect:/locations";
    }

    @PostMapping("/locations/{id}/delete")
    public String deleteLocation(@PathVariable Integer id, RedirectAttributes ra) {
        try {
            locationService.deleteLocation(id);
            ra.addFlashAttribute("successMsg", "Location deleted.");
        } catch (Exception e) {
            ra.addFlashAttribute("errorMsg", "Cannot delete — location may be linked to departments.");
        }
        return "redirect:/locations";
    }

    private String extractError(Exception e) {
        String msg = e.getMessage();
        if (msg != null && msg.length() > 120) msg = msg.substring(0, 120) + "…";
        return msg != null ? msg : "Unknown error";
    }
}
