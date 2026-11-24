package com.example.alienhunt;

import android.content.Context;
import android.content.SharedPreferences;

/**
 * "Base de datos" local del perfil, guardada en SharedPreferences.
 */
public class PerfilStorage {

    private static final String PREFS = "perfil_usuario";
    private static final String KEY_NOMBRE = "nombre";
    private static final String KEY_AVATAR = "avatar";

    private final SharedPreferences preferencias;

    public PerfilStorage(Context context) {
        preferencias = context.getApplicationContext()
                .getSharedPreferences(PREFS, Context.MODE_PRIVATE);
    }

    public void guardarPerfil(PerfilUsuario perfil) {
        preferencias.edit()
                .putString(KEY_NOMBRE, perfil.getNombre())
                .putString(KEY_AVATAR, perfil.getAvatar())
                .apply();
    }

    public PerfilUsuario cargarPerfil() {
        String nombre = preferencias.getString(KEY_NOMBRE, "");
        String avatar = preferencias.getString(KEY_AVATAR, Avatares.CLAVES[0]);
        return new PerfilUsuario(nombre, avatar);
    }

    /** Hay perfil si se guardó un nombre no vacío. */
    public boolean existePerfil() {
        String nombre = preferencias.getString(KEY_NOMBRE, "");
        return nombre != null && !nombre.trim().isEmpty();
    }

    public void borrarPerfil() {
        preferencias.edit().clear().apply();
    }
}
