package br.com.senac.valora.dtos;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import java.util.List;
import java.util.UUID;

public record CreateStudentRequest(
        @NotBlank @Size(max = 50) String registrationCode,
        @NotBlank @Size(max = 255) String name,
        @NotBlank @Email @Size(max = 255) String email,
        @NotEmpty(message = "Pelo menos 1 curso vinculado") List<UUID> courseIds
) {}
