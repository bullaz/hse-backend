package com.stellarix.hse.dto;

import java.util.List;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class HseInductionRequest {
    @NotBlank
    private String firstName;

    @NotBlank
    private String lastName;

    private String phone;
    private String email;
    private String work;
    private List<Integer> habilitationIds;
    private Integer companyId;
    private Integer roleId;
}
