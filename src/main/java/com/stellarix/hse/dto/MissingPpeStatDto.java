package com.stellarix.hse.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class MissingPpeStatDto {
    private String code;
    private String label;
    private long count;
}
