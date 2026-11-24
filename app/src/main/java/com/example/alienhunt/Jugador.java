package com.example.alienhunt;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.List;

/** Un jugador de la partida (el humano o un NPC) con todo lo que ha visto y hecho. */
public class Jugador {

    /** Última vez que este jugador vio a otro: dónde y cuándo (segundos de partida). */
    public static class Vista {
        public int hab;
        public float t;
    }

    /** Intervalo en el que este jugador vio a otro en una misma habitación. */
    public static class Avistamiento {
        public final int quien;
        public final int hab;
        public final float t0;
        public float t1;

        public Avistamiento(int quien, int hab, float t) {
            this.quien = quien;
            this.hab = hab;
            this.t0 = t;
            this.t1 = t;
        }
    }

    /** Entrada en una habitación. */
    public static class Visita {
        public final int hab;
        public final float t;

        public Visita(int hab, float t) {
            this.hab = hab;
            this.t = t;
        }
    }

    public final int id;
    public final String nombre;
    public final int color;
    public final boolean humano;

    public boolean alien;
    public boolean vivo = true;

    public float x, y;
    /** Habitación actual (-1 si está en un pasillo). */
    public int habitacion = -1;
    /** Último nodo del mapa alcanzado. */
    public int nodoActual;
    /** Nodos que le faltan por recorrer. */
    public final ArrayDeque<Integer> ruta = new ArrayDeque<>();

    /** Segundos que espera antes de elegir un nuevo destino (solo NPC). */
    public float espera;
    /** Segundos hasta poder volver a atacar (solo alien). */
    public float enfriamiento;

    /** Lo último que vio de cada jugador, indexado por id. */
    public final Vista[] ultimaVista = new Vista[JuegoEstacion.NUM_JUGADORES];
    public final List<Visita> visitas = new ArrayList<>();
    /** Historial completo de avistamientos (para razonar sobre varios ataques). */
    public final List<Avistamiento> historial = new ArrayList<>();
    final Avistamiento[] abierto = new Avistamiento[JuegoEstacion.NUM_JUGADORES];

    public Jugador(int id, String nombre, int color, boolean humano) {
        this.id = id;
        this.nombre = nombre;
        this.color = color;
        this.humano = humano;
    }
}
