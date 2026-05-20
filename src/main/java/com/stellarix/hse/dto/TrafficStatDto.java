package com.stellarix.hse.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class TrafficStatDto {
    private String period;
    private long workCount;
    private long visitCount;
}
