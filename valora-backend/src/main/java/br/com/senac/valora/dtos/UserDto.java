package br.com.senac.valora.dtos;

import br.com.senac.valora.entities.UserProfile;
import java.util.List;
import java.util.UUID;

public record UserDto(
        UUID id,
        String name,
        UserProfile profile,
        List<UUID> linkedCourses) {}
