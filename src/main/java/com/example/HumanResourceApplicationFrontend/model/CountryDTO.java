package com.example.HumanResourceApplicationFrontend.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class CountryDTO {
    private String    countryId;
    private String    countryName;
    private RegionDTO region;
}

