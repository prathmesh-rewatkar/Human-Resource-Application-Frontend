package com.example.HumanResourceApplicationFrontend.controller;

import com.example.HumanResourceApplicationFrontend.model.*;
import com.example.HumanResourceApplicationFrontend.service.CountryService;
import com.example.HumanResourceApplicationFrontend.service.RegionService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.*;

@Controller
@RequiredArgsConstructor
public class CountryController {

    private final CountryService countryService;
    private final RegionService  regionService;

    @Value("${backend.api.url}")
    private String backendBaseUrl;

    @GetMapping({"/countries", "/countries/"})
    public String listCountries(Model model) {
        model.addAttribute("countries",  countryService.getAllCountries());
        model.addAttribute("regions",    regionService.getAllRegions());
        model.addAttribute("activePage", "countries");
        return "countries/list";
    }

    @PostMapping("/countries/add")
    public String addCountry(@RequestParam String countryId, @RequestParam String countryName,
                             @RequestParam Integer regionId, RedirectAttributes ra) {
        try {
            Map<String, Object> p = new LinkedHashMap<>();
            p.put("countryId",   countryId.toUpperCase());
            p.put("countryName", countryName);
            p.put("region",      backendBaseUrl + "/regions/" + regionId);
            countryService.createCountry(p);
            ra.addFlashAttribute("successMsg", "Country '" + countryName + "' added!");
        } catch (Exception e) { ra.addFlashAttribute("errorMsg", extractError(e)); }
        return "redirect:/countries";
    }

    @PostMapping("/countries/{id}/edit")
    public String editCountry(@PathVariable String id, @RequestParam String countryName,
                              @RequestParam Integer regionId, RedirectAttributes ra) {
        try {
            Map<String, Object> p = new LinkedHashMap<>();
            p.put("countryId",   id);
            p.put("countryName", countryName);
            p.put("region",      backendBaseUrl + "/regions/" + regionId);
            countryService.updateCountry(id, p);
            ra.addFlashAttribute("successMsg", "Country updated!");
        } catch (Exception e) { ra.addFlashAttribute("errorMsg", extractError(e)); }
        return "redirect:/countries";
    }

    @PostMapping("/countries/{id}/delete")
    public String deleteCountry(@PathVariable String id, RedirectAttributes ra) {
        try {
            countryService.deleteCountry(id);
            ra.addFlashAttribute("successMsg", "Country deleted.");
        } catch (Exception e) {
            ra.addFlashAttribute("errorMsg", "Cannot delete — country may have locations linked.");
        }
        return "redirect:/countries";
    }

    private String extractError(Exception e) {
        String msg = e.getMessage();
        if (msg != null && msg.length() > 120) msg = msg.substring(0, 120) + "…";
        return msg != null ? msg : "Unknown error";
    }
}
