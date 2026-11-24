package com.example.alienhunt;

/**
 * Datos del perfil del jugador.
 * "avatar" guarda la clave del avatar (ver {@link Avatares#CLAVES}), por ejemplo "predator_1".
 */
public class PerfilUsuario {

    private String nombre;
    private String avatar;

    public PerfilUsuario(String nombre, String avatar) {
        this.nombre = nombre;
        this.avatar = avatar;
    }

    public String getNombre() {
        return nombre;
    }

    public String getAvatar() {
        return avatar;
    }

    public void setNombre(String nombre) {
        this.nombre = nombre;
    }

    public void setAvatar(String avatar) {
        this.avatar = avatar;
    }
}
