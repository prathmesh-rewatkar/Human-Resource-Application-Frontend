package com.example.HumanResourceApplicationFrontend.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class LocationNDTO {
    private Integer    locationId;
    private String     streetAddress;
    private String     postalCode;
    private String     city;
    private String     stateProvince;
    private CountryDTO country;
}

