package com.example.alienhunt;

import android.content.Intent;
import android.graphics.Color;
import android.view.View;

import androidx.activity.EdgeToEdge;
import androidx.activity.SystemBarStyle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

/** Ajustes de pantalla comunes a todas las Activities. */
public final class UiUtils {

    private UiUtils() { }

    /**
     * Activa el modo pantalla completa (edge-to-edge) con barras de sistema transparentes
     * e iconos claros (el juego siempre usa fondo oscuro), y añade al padding de "root"
     * el espacio de las barras del sistema, el notch y el teclado.
     *
     * Debe llamarse ANTES de setContentView.
     */
    public static void configurarPantalla(AppCompatActivity activity) {
        EdgeToEdge.enable(
                activity,
                SystemBarStyle.dark(Color.TRANSPARENT),
                SystemBarStyle.dark(Color.TRANSPARENT)
        );
    }

    /** Debe llamarse DESPUÉS de setContentView. */
    public static void aplicarInsets(View root) {
        final int left = root.getPaddingLeft();
        final int top = root.getPaddingTop();
        final int right = root.getPaddingRight();
        final int bottom = root.getPaddingBottom();

        ViewCompat.setOnApplyWindowInsetsListener(root, (v, insets) -> {
            Insets barras = insets.getInsets(
                    WindowInsetsCompat.Type.systemBars()
                            | WindowInsetsCompat.Type.displayCutout()
                            | WindowInsetsCompat.Type.ime());
            v.setPadding(left + barras.left, top + barras.top,
                    right + barras.right, bottom + barras.bottom);
            return insets;
        });
    }

    /** Cierra la sesión: vuelve al Login y vacía la pila de pantallas (no se puede volver atrás). */
    public static void cerrarSesion(AppCompatActivity activity) {
        Intent intent = new Intent(activity, Login.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        activity.startActivity(intent);
    }

    /** Vuelve al menú principal sin crear una copia nueva si ya estaba en la pila. */
    public static void irAlMenu(AppCompatActivity activity) {
        Intent intent = new Intent(activity, MenuActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        activity.startActivity(intent);
        activity.finish();
    }

    public static int dp(View view, int dp) {
        return Math.round(dp * view.getResources().getDisplayMetrics().density);
    }
}
