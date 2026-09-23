package com.cocheraya.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class RegisterRequest {

    @NotBlank(message = "El nombre es obligatorio")
    private String name;

    @NotBlank(message = "El email es obligatorio")
    @Email(message = "El email debe tener un formato válido")
    private String email;

    @NotBlank(message = "La contraseña es obligatoria")
    @jakarta.validation.constraints.Size(min = 8, message = "La contraseña debe tener al menos 8 caracteres")
    @jakarta.validation.constraints.Pattern(
            regexp = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d).*$",
            message = "La contraseña debe contener al menos una letra mayúscula, una minúscula y un número"
    )
    private String password;

    @NotBlank(message = "El teléfono es obligatorio")
    private String phone;

    
    @NotBlank(message = "El rol es obligatorio")
    private String role;
}
