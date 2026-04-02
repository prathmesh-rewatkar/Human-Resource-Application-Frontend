package com.example.HumanResourceApplicationFrontend.service;


import com.example.HumanResourceApplicationFrontend.model.CountryDTO;
import com.example.HumanResourceApplicationFrontend.model.RegionDTO;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.*;

@Slf4j
@Service
public class CountryService extends BaseService {

    private final RegionService regionService;

    public CountryService(RestTemplate restTemplate, RegionService regionService) {
        super(restTemplate);
        this.regionService = regionService;
    }

    public CountryDTO buildCountry(JsonNode n, Map<String, RegionDTO> countryToRegion) {
        CountryDTO c = new CountryDTO();
        c.setCountryId(str(n, "countryId"));
        c.setCountryName(str(n, "countryName"));
        if (c.getCountryId() != null)
            c.setRegion(countryToRegion.get(c.getCountryId()));
        return c;
    }

    /**
     * Build countryId → RegionDTO map by walking each region's /countries endpoint.
     * Makes N calls (N = number of regions, typically 5) instead of per-country.
     */
    public Map<String, RegionDTO> buildCountryToRegionMap(List<RegionDTO> regions) {
        Map<String, RegionDTO> map = new HashMap<>();
        for (RegionDTO region : regions) {
            if (region.getRegionId() == null) continue;
            try {
                for (JsonNode cn : embedded(rawGet("/regions/" + region.getRegionId() + "/countries"), "countries")) {
                    String cid = str(cn, "countryId");
                    if (cid != null) map.put(cid, region);
                }
            } catch (Exception e) {
                log.warn("Could not load countries for region {}: {}", region.getRegionId(), e.getMessage());
            }
        }
        log.info("countryToRegion map built: {} entries", map.size());
        return map;
    }

    public List<CountryDTO> getAllCountries() {
        List<RegionDTO> regions = regionService.getAllRegions();
        Map<String, RegionDTO> c2r = buildCountryToRegionMap(regions);
        List<CountryDTO> result = new ArrayList<>();
        for (JsonNode n : embedded(rawGet("/countries?size=300"), "countries"))
            result.add(buildCountry(n, c2r));
        log.info("getAllCountries → {} countries", result.size());
        return result;
    }

    public void createCountry(Map<String, Object> payload) { post("/countries", payload); }
    public void updateCountry(String id, Map<String, Object> payload) { put("/countries/" + id, payload); }
    public void deleteCountry(String id) { delete("/countries/" + id); }
}

