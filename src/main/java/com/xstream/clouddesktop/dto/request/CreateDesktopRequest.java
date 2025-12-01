package com.xstream.clouddesktop.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.Builder;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateDesktopRequest {

    @NotBlank(message = "User ID is required")
    private String userId;

    @Size(max = 100, message = "Name cannot exceed 100 characters")
    private String name;

    @Pattern(regexp = "BASIC|STANDARD|PREMIUM", message = "Plan must be one of BASIC, STANDARD, or PREMIUM")
    private String plan;
}
