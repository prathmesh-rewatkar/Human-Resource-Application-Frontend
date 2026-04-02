package com.example.HumanResourceApplicationFrontend.service;


import com.example.HumanResourceApplicationFrontend.model.RegionDTO;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.*;

@Slf4j
@Service
public class RegionService extends BaseService {

    public RegionService(RestTemplate restTemplate) {
        super(restTemplate);
    }

    public RegionDTO buildRegion(JsonNode n) {
        RegionDTO r = new RegionDTO();
        r.setRegionId(integer(n, "regionId"));
        r.setRegionName(str(n, "regionName"));
        return r;
    }

    public List<RegionDTO> getAllRegions() {
        List<RegionDTO> result = new ArrayList<>();
        for (JsonNode n : embedded(rawGet("/regions?size=100"), "regions"))
            result.add(buildRegion(n));
        log.info("getAllRegions → {} regions", result.size());
        return result;
    }

    public void createRegion(Map<String, Object> payload) { post("/regions", payload); }
    public void updateRegion(Integer id, Map<String, Object> payload) { put("/regions/" + id, payload); }
    public void deleteRegion(Integer id) { delete("/regions/" + id); }
}
