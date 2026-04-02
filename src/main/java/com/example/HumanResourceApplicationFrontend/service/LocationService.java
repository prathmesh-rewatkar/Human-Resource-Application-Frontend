package com.example.HumanResourceApplicationFrontend.service;


import com.example.HumanResourceApplicationFrontend.model.CountryDTO;
import com.example.HumanResourceApplicationFrontend.model.LocationNDTO;
import com.example.HumanResourceApplicationFrontend.model.RegionDTO;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.*;

@Slf4j
@Service
public class LocationService extends BaseService {

    private final RegionService  regionService;
    private final CountryService countryService;

    public LocationService(RestTemplate restTemplate,
                           RegionService regionService,
                           CountryService countryService) {
        super(restTemplate);
        this.regionService  = regionService;
        this.countryService = countryService;
    }

    /**
     * Build a LocationDTO from a locationDetail projection node.
     *
     * The backend's locationDetail projection exposes:
     *   - countryName  (from CountryInfo.getCountryName())
     *   - regionName   (from RegionInfo.getRegionName())
     *
     * exposeIdsFor(Country, Region, Location) adds IDs to the JSON for top-level
     * entities, but NOT to nested projection objects. So countryId / regionId
     * may NOT be present in the nested country/region objects.
     *
     * Fix: when IDs are absent from the nested node, look them up from the
     * countryToRegion map (which is built from /regions/{id}/countries and
     * has full countryId → RegionDTO mapping).
     * We also build a regionName → RegionDTO map for region name matching.
     */
    public LocationNDTO buildLocation(JsonNode n,
                                      Map<String, RegionDTO> countryToRegion,
                                      Map<String, RegionDTO> regionNameToRegion) {
        LocationNDTO loc = new LocationNDTO();
        loc.setLocationId(integer(n, "locationId"));
        loc.setStreetAddress(str(n, "streetAddress"));
        loc.setCity(str(n, "city"));
        loc.setPostalCode(str(n, "postalCode"));
        loc.setStateProvince(str(n, "stateProvince"));

        JsonNode cn = n.path("country");
        if (!cn.isMissingNode() && cn.isObject()) {
            CountryDTO c = new CountryDTO();

            // countryId: present if exposeIdsFor(Country.class) was set AND
            // the projection returned it; may be null for nested projections
            String countryId = str(cn, "countryId");
            c.setCountryId(countryId);
            c.setCountryName(str(cn, "countryName"));

            // Region: try nested first, then look up by countryId, then by regionName
            JsonNode rn = cn.path("region");
            if (!rn.isMissingNode() && rn.isObject()) {
                RegionDTO r = new RegionDTO();
                r.setRegionId(integer(rn, "regionId"));
                r.setRegionName(str(rn, "regionName"));
                // If regionId is null, try to fill from regionNameToRegion map
                if (r.getRegionId() == null && r.getRegionName() != null) {
                    RegionDTO mapped = regionNameToRegion.get(r.getRegionName());
                    if (mapped != null) r.setRegionId(mapped.getRegionId());
                }
                c.setRegion(r);
            } else {
                // No nested region node — look up by countryId
                RegionDTO r = null;
                if (countryId != null) r = countryToRegion.get(countryId);
                c.setRegion(r);
            }

            loc.setCountry(c);
        }
        return loc;
    }

    /** Convenience overload used internally. */
    private LocationNDTO buildLocation(JsonNode n, Map<String, RegionDTO> c2r) {
        // Build regionName map from c2r values
        Map<String, RegionDTO> regionNameMap = new HashMap<>();
        for (RegionDTO r : c2r.values()) {
            if (r.getRegionName() != null) regionNameMap.put(r.getRegionName(), r);
        }
        return buildLocation(n, c2r, regionNameMap);
    }

    public List<LocationNDTO> getAllLocations() {
        List<RegionDTO> regions = regionService.getAllRegions();
        Map<String, RegionDTO> c2r = countryService.buildCountryToRegionMap(regions);
        Map<String, RegionDTO> regionNameMap = buildRegionNameMap(regions);

        List<LocationNDTO> result = new ArrayList<>();
        for (JsonNode n : embedded(rawGet("/locations?size=1000&projection=locationDetail"), "locations"))
            result.add(buildLocation(n, c2r, regionNameMap));
        log.info("getAllLocations → {} locations", result.size());
        return result;
    }

    public LocationNDTO getLocationById(Integer id) {
        try {
            List<RegionDTO> regions = regionService.getAllRegions();
            Map<String, RegionDTO> c2r = countryService.buildCountryToRegionMap(regions);
            Map<String, RegionDTO> regionNameMap = buildRegionNameMap(regions);
            JsonNode n = parseJson(rawGet("/locations/" + id + "?projection=locationDetail"));
            return buildLocation(n, c2r, regionNameMap);
        } catch (Exception e) {
            log.error("getLocationById {}: {}", id, e.getMessage());
            return null;
        }
    }

    private Map<String, RegionDTO> buildRegionNameMap(List<RegionDTO> regions) {
        Map<String, RegionDTO> map = new HashMap<>();
        for (RegionDTO r : regions) {
            if (r.getRegionName() != null) map.put(r.getRegionName(), r);
        }
        return map;
    }

    public void createLocation(Map<String, Object> payload) { post("/locations", payload); }
    public void updateLocation(Integer id, Map<String, Object> payload) { put("/locations/" + id, payload); }
    public void deleteLocation(Integer id) { delete("/locations/" + id); }
}
