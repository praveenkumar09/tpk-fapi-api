package org.tpkprav.dto;

import jakarta.validation.constraints.NotBlank;

public record TokenRequest(@NotBlank String nric, @NotBlank String uuid) {
}
