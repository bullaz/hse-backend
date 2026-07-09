package com.stellarix.hse.dto;

import java.util.List;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class HseInductionRequest {
    @NotBlank
    private String firstName;

    @NotBlank
    private String lastName;

    @NotBlank
    private String cinNumber;

    @NotBlank
    private String phone;

    @NotBlank
    @Email
    private String email;

    @NotBlank
    private String work;

    // Habilitations stay optional — not every inducted person holds a certification.
    private List<Integer> habilitationIds;

    @NotNull
    private Integer companyId;

    // Legacy — superseded by the free-text `work` field above; the webapp create/edit
    // form doesn't collect this, so it can't be made required without breaking creation.
    private Integer roleId;
}
