package com.stellarix.hse.dto;

import java.util.List;
import java.util.Map;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class PpeMatrixResponse {
    private Integer siteId;
    private String siteName;
    /** Keys are "WORK" and "VISIT"; values are the required PPE item codes for that intent. */
    private Map<String, List<String>> requirements;
}
