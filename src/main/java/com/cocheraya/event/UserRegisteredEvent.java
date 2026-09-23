package com.cocheraya.event;

import com.cocheraya.entity.User;
import lombok.Getter;
import org.springframework.context.ApplicationEvent;

/**
 * Evento de dominio publicado cuando un nuevo usuario se registra en CocheraYa.
 *
 * El {@link CocherayaEventListener} escucha este evento para disparar
 * el envío del correo de bienvenida de forma asíncrona.
 */
@Getter
public class UserRegisteredEvent extends ApplicationEvent {

    private final Long userId;
    private final String email;
    private final String name;
    private final User.Role role;

    /**
     * Crea una nueva instancia del evento de registro de usuario.
     *
     * @param source fuente del evento (generalmente el servicio que lo publica)
     * @param userId identificador del usuario registrado
     * @param email  correo electrónico del usuario
     * @param name   nombre del usuario
     * @param role   rol del usuario registrado
     */
    public UserRegisteredEvent(Object source, Long userId, String email, String name, User.Role role) {
        super(source);
        this.userId = userId;
        this.email = email;
        this.name = name;
        this.role = role;
    }
}
