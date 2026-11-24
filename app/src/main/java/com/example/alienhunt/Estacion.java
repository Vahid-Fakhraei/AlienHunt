package com.example.alienhunt;

import java.util.ArrayList;
import java.util.List;

/**
 * Datos del mapa de la estación (coordenadas en píxeles de la imagen mapa_estacion.jpg, 1672x940).
 * Los nodos 0..9 son los centros de las habitaciones; del 10 en adelante son cruces y puertas de pasillo.
 */
public final class Estacion {

    private Estacion() {}

    public static final int ANCHO = 1672;
    public static final int ALTO = 940;

    public static final int LAB = 0, ENF = 1, PAS = 2, SEG = 3, MOT = 4,
            CTRL = 5, ALM = 6, REA = 7, ARM = 8, HAN = 9;
    public static final int NUM_HAB = 10;

    public static final String[] HABITACIONES = {
            "LABORATORIO", "ENFERMERÍA", "PASILLO", "SEGURIDAD", "SALA MOTORES",
            "CENTRO DE CONTROL", "ALMACÉN", "SALA DE REACTOR", "ARMERÍA", "HANGAR"
    };

    /** Posición (x, y) de cada nodo. */
    public static final float[][] NODOS = {
            {430, 165}, {1140, 165}, {415, 350}, {1030, 350}, {270, 520},
            {810, 540}, {1200, 520}, {360, 730}, {1130, 730}, {1460, 730},
            {820, 170}, {820, 345}, {820, 715}, {425, 275}, {425, 420},
            {380, 615}, {1155, 275}, {1150, 430}, {1220, 615}, {1290, 715},
            {610, 518}, {995, 518}
    };

    /** Conexiones entre nodos (se pueden recorrer en los dos sentidos). */
    public static final int[][] ARISTAS = {
            {0, 10}, {1, 10}, {10, 11}, {2, 11}, {3, 11}, {11, 5}, {5, 12}, {12, 7}, {12, 8},
            {0, 13}, {13, 2}, {2, 14}, {14, 4}, {4, 15}, {15, 7},
            {1, 16}, {16, 3}, {3, 17}, {17, 6}, {6, 18}, {18, 8}, {8, 19}, {19, 9},
            {4, 20}, {20, 5}, {5, 21}, {21, 6}
    };

    /** Rectángulo de cada habitación: izquierda, arriba, derecha, abajo. */
    public static final int[][] RECTS = {
            {170, 75, 690, 255}, {965, 75, 1330, 255}, {215, 295, 620, 400},
            {930, 295, 1330, 410}, {130, 435, 560, 595}, {660, 435, 955, 640},
            {1035, 450, 1375, 585}, {130, 640, 590, 825}, {1015, 650, 1275, 820},
            {1310, 625, 1610, 830}
    };

    private static final int N = NODOS.length;
    private static final float[][] D = new float[N][N];
    private static final int[][] SIG = new int[N][N];

    static {
        final float inf = 1e9f;
        for (int i = 0; i < N; i++) {
            for (int j = 0; j < N; j++) {
                D[i][j] = (i == j) ? 0 : inf;
                SIG[i][j] = (i == j) ? i : -1;
            }
        }
        for (int[] a : ARISTAS) {
            float d = (float) Math.hypot(NODOS[a[0]][0] - NODOS[a[1]][0],
                    NODOS[a[0]][1] - NODOS[a[1]][1]);
            D[a[0]][a[1]] = d;
            D[a[1]][a[0]] = d;
            SIG[a[0]][a[1]] = a[1];
            SIG[a[1]][a[0]] = a[0];
        }
        // Floyd-Warshall: distancias mínimas y siguiente nodo del camino
        for (int k = 0; k < N; k++) {
            for (int i = 0; i < N; i++) {
                for (int j = 0; j < N; j++) {
                    if (D[i][k] + D[k][j] < D[i][j]) {
                        D[i][j] = D[i][k] + D[k][j];
                        SIG[i][j] = SIG[i][k];
                    }
                }
            }
        }
    }

    /** Habitación que contiene el punto, o -1 si está en un pasillo / fuera. */
    public static int habitacionEn(float x, float y) {
        for (int i = 0; i < NUM_HAB; i++) {
            int[] r = RECTS[i];
            if (x >= r[0] && x <= r[2] && y >= r[1] && y <= r[3]) return i;
        }
        return -1;
    }

    public static int nodoMasCercano(float x, float y) {
        int mejor = 0;
        float mejorD = Float.MAX_VALUE;
        for (int i = 0; i < N; i++) {
            float d = (float) Math.hypot(NODOS[i][0] - x, NODOS[i][1] - y);
            if (d < mejorD) {
                mejorD = d;
                mejor = i;
            }
        }
        return mejor;
    }

    public static float distanciaANodo(float x, float y, int nodo) {
        return (float) Math.hypot(NODOS[nodo][0] - x, NODOS[nodo][1] - y);
    }

    /** Longitud del camino más corto entre dos nodos. */
    public static float coste(int a, int b) {
        return D[a][b];
    }

    /** Lista de nodos del camino más corto, incluyendo el de salida y el de llegada. */
    public static List<Integer> camino(int a, int b) {
        List<Integer> ruta = new ArrayList<>();
        ruta.add(a);
        int actual = a;
        while (actual != b) {
            actual = SIG[actual][b];
            ruta.add(actual);
        }
        return ruta;
    }

    /** Dos habitaciones están "cerca" si el camino entre ellas es corto. */
    public static boolean sonCercanas(int habA, int habB) {
        return habA >= 0 && habB >= 0 && D[habA][habB] <= 350f;
    }

    public static String nombre(int hab) {
        return hab >= 0 ? HABITACIONES[hab] : "un pasillo";
    }
}
