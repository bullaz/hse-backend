package com.stellarix.hse.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class TopWorkerStatDto {
    private String firstName;
    private String lastName;
    private long count;
}
