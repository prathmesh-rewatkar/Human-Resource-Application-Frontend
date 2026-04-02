package com.example.HumanResourceApplicationFrontend.controller;

import com.example.HumanResourceApplicationFrontend.model.CountryDTO;
import com.example.HumanResourceApplicationFrontend.model.RegionDTO;
import com.example.HumanResourceApplicationFrontend.service.CountryService;
import com.example.HumanResourceApplicationFrontend.service.RegionService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Controller
@RequiredArgsConstructor
public class RegionController {

    private final RegionService regionService;
    private final CountryService countryService;

    @GetMapping({"/regions", "/regions/"})
    public String listRegions(Model model) {
        List<RegionDTO> regions   = regionService.getAllRegions();
        List<CountryDTO> countries = countryService.getAllCountries();

        Map<Integer, Long> countryCountMap = new HashMap<>();
        Map<Integer, List<String>> countryCodesMap = new HashMap<>();

        for (RegionDTO r : regions) {
            List<String> codes = new ArrayList<>();
            for (CountryDTO c : countries) {
                if (c.getRegion() != null
                        && r.getRegionId() != null
                        && r.getRegionId().equals(c.getRegion().getRegionId())
                        && c.getCountryId() != null) {
                    codes.add(c.getCountryId());
                }
            }
            countryCountMap.put(r.getRegionId(), (long) codes.size());
            countryCodesMap.put(r.getRegionId(), codes);
        }

        model.addAttribute("regions",         regions);
        model.addAttribute("countryCountMap", countryCountMap);
        model.addAttribute("countryCodesMap", countryCodesMap);
        model.addAttribute("activePage",      "regions");
        return "regions/list";
    }

    @PostMapping("/regions/add")
    public String addRegion(@RequestParam String regionName, RedirectAttributes ra) {
        try {
            regionService.createRegion(Map.of("regionName", regionName));
            ra.addFlashAttribute("successMsg", "Region '" + regionName + "' added!");
        } catch (Exception e) {
            ra.addFlashAttribute("errorMsg", e.getMessage());
        }
        return "redirect:/regions";
    }

    @PostMapping("/regions/{id}/edit")
    public String editRegion(@PathVariable Integer id, @RequestParam String regionName, RedirectAttributes ra) {
        try {
            regionService.updateRegion(id, Map.of("regionName", regionName));
            ra.addFlashAttribute("successMsg", "Region updated!");
        } catch (Exception e) {
            ra.addFlashAttribute("errorMsg", e.getMessage());
        }
        return "redirect:/regions";
    }

    @PostMapping("/regions/{id}/delete")
    public String deleteRegion(@PathVariable Integer id, RedirectAttributes ra) {
        try {
            regionService.deleteRegion(id);
            ra.addFlashAttribute("successMsg", "Region deleted.");
        } catch (Exception e) {
            ra.addFlashAttribute("errorMsg", "Cannot delete — region has countries linked to it.");
        }
        return "redirect:/regions";
    }
}
