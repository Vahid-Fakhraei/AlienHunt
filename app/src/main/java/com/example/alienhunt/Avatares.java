package com.example.alienhunt;

/**
 * Catálogo de avatares disponibles. CLAVES y RECURSOS van en el mismo orden.
 * En el perfil se guarda la clave (texto), no el id del recurso, porque los ids
 * de R.drawable pueden cambiar entre compilaciones.
 */
public final class Avatares {

    public static final String[] CLAVES = {
            "predator_1",
            "predator_2",
            "predator_3",
            "predator_4"
    };

    public static final int[] RECURSOS = {
            R.drawable.predator_1,
            R.drawable.predator_2,
            R.drawable.predator_3,
            R.drawable.predator_4
    };

    private Avatares() { }

    /** Posición de una clave en el catálogo; si no existe devuelve 0. */
    public static int indiceDe(String clave) {
        for (int i = 0; i < CLAVES.length; i++) {
            if (CLAVES[i].equals(clave)) return i;
        }
        return 0;
    }

    /** Recurso drawable de una clave; si no existe devuelve el primer avatar. */
    public static int recursoDe(String clave) {
        return RECURSOS[indiceDe(clave)];
    }
}
