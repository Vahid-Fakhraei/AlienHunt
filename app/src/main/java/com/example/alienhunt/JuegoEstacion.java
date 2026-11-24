package com.example.alienhunt;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Random;

/**
 * Lógica del juego (sin dependencias de Android).
 *
 * 10 jugadores: el humano (id 0) y 9 NPC. Uno de los NPC es el alien. El alien elimina
 * tripulantes cuando se queda a solas con uno. Los cuerpos y las emergencias provocan una
 * reunión donde se puede preguntar a los demás y votar. Los tripulantes responden con lo que
 * realmente han visto; el alien miente.
 */
public class JuegoEstacion {

    public enum Fase { JUGANDO, REUNION, FIN }

    public interface Oyente {
        void alIniciarReunion();

        void alTerminar(boolean victoria, String mensaje);
    }

    public static class Cuerpo {
        public final Jugador jugador;
        public final int habitacion;
        public final float x, y;
        public final float t;

        Cuerpo(Jugador jugador, int habitacion, float x, float y, float t) {
            this.jugador = jugador;
            this.habitacion = habitacion;
            this.x = x;
            this.y = y;
            this.t = t;
        }
    }

    public static final int NUM_JUGADORES = 10;
    public static final int MAX_EMERGENCIAS = 2;

    public static final String[] PREGUNTAS = {
            "¿Dónde estabas hace poco?",
            "¿A quién has visto?",
            "¿Quién crees que es el alien?"
    };

    private static final String[] NOMBRES_NPC = {
            "ROJO", "AZUL", "VERDE", "AMARILLO", "NARANJA", "ROSA", "MORADO", "MARRÓN", "GRIS"
    };
    private static final int[] COLORES_NPC = {
            0xFFE53935, 0xFF1E88E5, 0xFF43A047, 0xFFFDD835, 0xFFFB8C00,
            0xFFEC407A, 0xFF8E24AA, 0xFF795548, 0xFF9E9E9E
    };
    private static final int COLOR_HUMANO = 0xFF00E5FF;

    private static final float VEL_HUMANO = 170f;
    private static final float VEL_NPC = 130f;
    private static final float ESPERA_MIN = 1.5f, ESPERA_MAX = 5f;
    private static final float ENFRIAMIENTO_ATAQUE = 30f;
    private static final float ATAQUE_INICIAL = 35f;
    private static final float PAUSA_ENTRE_REUNIONES = 20f;
    private static final float MEMORIA_VISTAS = 90f;
    /** El humano no puede ser atacado mientras queden más tripulantes vivos que esto. */
    private static final int PROTEGIDO_HASTA = 5;

    private final Random rnd;
    private final Oyente oyente;
    private final List<Jugador> jugadores = new ArrayList<>();
    private final List<Cuerpo> cuerpos = new ArrayList<>();
    private final List<String> registro = new ArrayList<>();
    private final List<String> testimonios = new ArrayList<>();
    private final Jugador humano;
    private final Jugador alien;

    private Fase fase = Fase.JUGANDO;
    private boolean pausado;
    private float tiempo;
    private int ultimaMuerteHab = -1;
    private float ultimaMuerteT;
    /** Cada ataque: {habitación, instante}. */
    private final List<float[]> ataques = new ArrayList<>();
    private int emergenciasUsadas;
    private float reunionPermitidaDesde = 15f;
    private boolean victoria;
    private String mensajeFin = "";

    public JuegoEstacion(String nombreHumano, Random rnd, Oyente oyente) {
        this.rnd = rnd;
        this.oyente = oyente;

        String nombre = (nombreHumano == null || nombreHumano.trim().isEmpty())
                ? "TÚ" : nombreHumano.trim();
        for (String n : NOMBRES_NPC) {
            if (n.equalsIgnoreCase(nombre)) nombre = nombre + " (TÚ)";
        }

        humano = new Jugador(0, nombre, COLOR_HUMANO, true);
        jugadores.add(humano);
        for (int i = 0; i < NOMBRES_NPC.length; i++) {
            jugadores.add(new Jugador(i + 1, NOMBRES_NPC[i], COLORES_NPC[i], false));
        }
        alien = jugadores.get(1 + rnd.nextInt(NOMBRES_NPC.length));
        alien.alien = true;
        alien.enfriamiento = ATAQUE_INICIAL;

        colocarEnCentroControl();
        for (Jugador p : jugadores) {
            p.visitas.add(new Jugador.Visita(Estacion.CTRL, 0f));
            p.espera = rnd.nextFloat() * 3f;
        }
    }

    // ------------------------------------------------------------------ acceso

    public List<Jugador> getJugadores() { return jugadores; }
    public List<Cuerpo> getCuerpos() { return cuerpos; }
    public List<String> getRegistro() { return registro; }
    public List<String> getTestimonios() { return testimonios; }
    public Jugador getHumano() { return humano; }
    public Jugador getAlien() { return alien; }
    public Fase getFase() { return fase; }
    public boolean esVictoria() { return victoria; }
    public String getMensajeFin() { return mensajeFin; }
    public float getTiempo() { return tiempo; }
    public void setPausado(boolean pausado) { this.pausado = pausado; }

    public int tripulantesVivos() {
        int n = 0;
        for (Jugador p : jugadores) if (p.vivo && !p.alien) n++;
        return n;
    }

    public String nombreHabitacionHumano() {
        return Estacion.nombre(humano.habitacion);
    }

    /** ¿Ve el humano a este jugador ahora mismo? */
    public boolean visibleParaHumano(Jugador p) {
        return p.humano || (p.vivo && puedeVer(humano, p));
    }

    public boolean cuerpoVisible(Cuerpo c) {
        return c.habitacion == humano.habitacion;
    }

    public boolean hayCuerpoCerca() {
        if (fase != Fase.JUGANDO || !humano.vivo || humano.habitacion < 0) return false;
        for (Cuerpo c : cuerpos) if (c.habitacion == humano.habitacion) return true;
        return false;
    }

    public List<Jugador> jugadoresCercaDelHumano() {
        List<Jugador> lista = new ArrayList<>();
        if (fase != Fase.JUGANDO) return lista;
        for (Jugador p : jugadores) {
            if (!p.humano && p.vivo && puedeVer(humano, p)) lista.add(p);
        }
        return lista;
    }

    public int emergenciasRestantes() {
        return MAX_EMERGENCIAS - emergenciasUsadas;
    }

    public boolean puedeEmergencia() {
        return fase == Fase.JUGANDO && humano.vivo && humano.habitacion == Estacion.CTRL
                && emergenciasRestantes() > 0 && tiempo >= reunionPermitidaDesde;
    }

    // ------------------------------------------------------------------ bucle

    public void tick(float dt) {
        if (fase != Fase.JUGANDO || pausado) return;
        tiempo += dt;

        for (Jugador p : jugadores) {
            if (!p.vivo) continue;
            mover(p, dt, p.humano ? VEL_HUMANO : VEL_NPC);
            actualizarHabitacion(p);
            if (!p.humano) pensar(p, dt);
            if (p.enfriamiento > 0) p.enfriamiento -= dt;
        }
        actualizarVistas();
        intentarAtacar();
        if (fase == Fase.JUGANDO) buscarCuerpos();
    }

    private void mover(Jugador p, float dt, float vel) {
        float restante = vel * dt;
        while (restante > 0 && !p.ruta.isEmpty()) {
            int destino = p.ruta.peekFirst();
            float dx = Estacion.NODOS[destino][0] - p.x;
            float dy = Estacion.NODOS[destino][1] - p.y;
            float d = (float) Math.hypot(dx, dy);
            if (d <= restante) {
                p.x = Estacion.NODOS[destino][0];
                p.y = Estacion.NODOS[destino][1];
                p.nodoActual = destino;
                p.ruta.pollFirst();
                restante -= d;
            } else {
                p.x += dx / d * restante;
                p.y += dy / d * restante;
                restante = 0;
            }
        }
    }

    private void actualizarHabitacion(Jugador p) {
        int hab = Estacion.habitacionEn(p.x, p.y);
        if (hab != p.habitacion) {
            p.habitacion = hab;
            if (hab >= 0) p.visitas.add(new Jugador.Visita(hab, tiempo));
        }
    }

    /** Los NPC vagan de habitación en habitación. */
    private void pensar(Jugador p, float dt) {
        if (!p.ruta.isEmpty()) return;
        if (p.espera > 0) {
            p.espera -= dt;
            return;
        }
        int destino;
        do {
            destino = rnd.nextInt(Estacion.NUM_HAB);
        } while (destino == p.habitacion);
        irA(p, destino);
        p.espera = ESPERA_MIN + rnd.nextFloat() * (ESPERA_MAX - ESPERA_MIN);
    }

    /** Calcula la ruta hacia un nodo; si va por un pasillo puede dar la vuelta. */
    private void irA(Jugador p, int nodoDestino) {
        int inicio = p.nodoActual;
        Integer siguiente = p.ruta.peekFirst();
        if (siguiente != null) {
            float costeA = Estacion.distanciaANodo(p.x, p.y, p.nodoActual)
                    + Estacion.coste(p.nodoActual, nodoDestino);
            float costeB = Estacion.distanciaANodo(p.x, p.y, siguiente)
                    + Estacion.coste(siguiente, nodoDestino);
            inicio = (costeA <= costeB) ? p.nodoActual : siguiente;
        }
        p.ruta.clear();
        for (int n : Estacion.camino(inicio, nodoDestino)) p.ruta.addLast(n);
    }

    /** El humano toca el mapa: se mueve a esa habitación (o al nodo de pasillo más cercano). */
    public void moverHumano(float x, float y) {
        if (fase != Fase.JUGANDO || pausado || !humano.vivo) return;
        int hab = Estacion.habitacionEn(x, y);
        int nodo;
        if (hab >= 0) {
            nodo = hab;
        } else {
            nodo = Estacion.nodoMasCercano(x, y);
            if (Estacion.distanciaANodo(x, y, nodo) > 130f) return;
        }
        irA(humano, nodo);
    }

    /** Nodo al que se dirige el humano ahora, o -1. */
    public int destinoHumano() {
        Integer ultimo = humano.ruta.peekLast();
        return ultimo == null ? -1 : ultimo;
    }

    private boolean puedeVer(Jugador a, Jugador b) {
        if (a.habitacion >= 0 || b.habitacion >= 0) return a.habitacion == b.habitacion;
        return Math.hypot(a.x - b.x, a.y - b.y) < 130;
    }

    private void actualizarVistas() {
        for (Jugador a : jugadores) {
            if (!a.vivo) continue;
            for (Jugador b : jugadores) {
                if (a == b || !b.vivo || !puedeVer(a, b)) continue;
                Jugador.Vista v = a.ultimaVista[b.id];
                if (v == null) {
                    v = new Jugador.Vista();
                    a.ultimaVista[b.id] = v;
                }
                v.hab = b.habitacion;
                v.t = tiempo;

                Jugador.Avistamiento ab = a.abierto[b.id];
                if (ab != null && ab.hab == b.habitacion && tiempo - ab.t1 < 0.5f) {
                    ab.t1 = tiempo;
                } else {
                    ab = new Jugador.Avistamiento(b.id, b.habitacion, tiempo);
                    a.abierto[b.id] = ab;
                    a.historial.add(ab);
                }
            }
        }
    }

    // ------------------------------------------------------------------ ataques y cuerpos

    private void intentarAtacar() {
        if (!alien.vivo || alien.enfriamiento > 0 || alien.habitacion < 0) return;
        Jugador victima = null;
        int presentes = 0;
        for (Jugador p : jugadores) {
            if (p.vivo && p != alien && p.habitacion == alien.habitacion) {
                presentes++;
                victima = p;
            }
        }
        if (presentes != 1) return; // solo ataca si está a solas con una víctima
        // El jugador no es atacable hasta que la tripulación se ha reducido (da tiempo a investigar)
        if (victima.humano && tripulantesVivos() > PROTEGIDO_HASTA) return;

        victima.vivo = false;
        cuerpos.add(new Cuerpo(victima, victima.habitacion, victima.x, victima.y, tiempo));
        ultimaMuerteHab = victima.habitacion;
        ultimaMuerteT = tiempo;
        ataques.add(new float[]{victima.habitacion, tiempo});
        registrarTestigos(victima.habitacion);
        alien.enfriamiento = ENFRIAMIENTO_ATAQUE;
        alien.ruta.clear();
        alien.espera = 0;

        if (victima.humano) {
            terminar(false, "El alien te ha atacado cuando estabas a solas con él en "
                    + Estacion.nombre(ultimaMuerteHab) + ".");
        } else if (tripulantesVivos() <= 1) {
            terminar(false, "Quedan muy pocos tripulantes: el alien se ha hecho con la estación.");
        }
    }

    /** Quien esté cerca de la escena puede ver al alien huir. */
    private void registrarTestigos(int habAtaque) {
        for (Jugador o : jugadores) {
            if (!o.vivo || o == alien || o.habitacion == habAtaque) continue;
            if (Math.hypot(o.x - alien.x, o.y - alien.y) > 380f) continue;
            if (rnd.nextInt(100) >= 35) continue;
            o.historial.add(new Jugador.Avistamiento(alien.id, habAtaque, tiempo));
            Jugador.Vista v = o.ultimaVista[alien.id];
            if (v == null) {
                v = new Jugador.Vista();
                o.ultimaVista[alien.id] = v;
            }
            v.hab = habAtaque;
            v.t = tiempo;
        }
    }

    private void buscarCuerpos() {
        if (cuerpos.isEmpty()) return;
        for (Jugador p : jugadores) {
            if (!p.vivo || p.humano || p.alien || p.habitacion < 0) continue;
            for (Cuerpo c : cuerpos) {
                if (c.habitacion == p.habitacion) {
                    iniciarReunion(p, false);
                    return;
                }
            }
        }
    }

    public boolean reportarCuerpo() {
        if (!hayCuerpoCerca()) return false;
        iniciarReunion(humano, false);
        return true;
    }

    public boolean llamarEmergencia() {
        if (!puedeEmergencia()) return false;
        emergenciasUsadas++;
        iniciarReunion(humano, true);
        return true;
    }

    // ------------------------------------------------------------------ reunión

    private void iniciarReunion(Jugador quien, boolean emergencia) {
        fase = Fase.REUNION;
        registro.clear();
        String quienTxt = quien.humano ? "Tú" : quien.nombre;
        if (emergencia) {
            registro.add("REUNIÓN DE EMERGENCIA convocada por " + quienTxt + ".");
        } else {
            registro.add(quienTxt + (quien.humano ? " has reportado" : " ha reportado") + " un cuerpo.");
        }
        for (Cuerpo c : cuerpos) {
            registro.add("Cuerpo de " + c.jugador.nombre + " hallado en " + Estacion.nombre(c.habitacion) + ".");
        }
        if (cuerpos.isEmpty()) registro.add("No hay ningún cuerpo. ¡Interrogad a los demás!");
        cuerpos.clear();

        colocarEnCentroControl();
        charlaAutomatica();
        if (oyente != null) oyente.alIniciarReunion();
    }

    private void colocarEnCentroControl() {
        int vivos = 0;
        for (Jugador p : jugadores) if (p.vivo) vivos++;
        int i = 0;
        for (Jugador p : jugadores) {
            if (!p.vivo) continue;
            double ang = 2 * Math.PI * i / vivos;
            p.x = Estacion.NODOS[Estacion.CTRL][0] + (float) Math.cos(ang) * 70f;
            p.y = Estacion.NODOS[Estacion.CTRL][1] + (float) Math.sin(ang) * 50f;
            p.ruta.clear();
            p.nodoActual = Estacion.CTRL;
            p.habitacion = Estacion.CTRL;
            i++;
        }
    }

    /** Los NPC se preguntan entre ellos y el humano lo oye todo. */
    private void charlaAutomatica() {
        List<Jugador> npcs = new ArrayList<>();
        for (Jugador p : jugadores) if (p.vivo && !p.humano) npcs.add(p);
        if (npcs.size() < 2) return;
        int charlas = Math.min(3, npcs.size() / 2);
        for (int i = 0; i < charlas; i++) {
            Jugador pregunta = npcs.get(rnd.nextInt(npcs.size()));
            Jugador responde;
            do {
                responde = npcs.get(rnd.nextInt(npcs.size()));
            } while (responde == pregunta);
            int tipo = rnd.nextInt(PREGUNTAS.length);
            registro.add(pregunta.nombre + " a " + responde.nombre + ": «" + PREGUNTAS[tipo] + "»");
            String resp = responder(responde, tipo);
            registro.add(responde.nombre + ": " + resp);
            testimonios.add("[" + formatoTiempo(tiempo) + "] " + responde.nombre + ": " + resp);
        }
    }

    /** El humano pregunta algo a un jugador. Devuelve la respuesta. */
    public String preguntar(Jugador a, int tipo) {
        String resp = responder(a, tipo);
        testimonios.add("[" + formatoTiempo(tiempo) + "] " + a.nombre + ": " + resp);
        if (fase == Fase.REUNION) {
            registro.add("Tú a " + a.nombre + ": «" + PREGUNTAS[tipo] + "»");
            registro.add(a.nombre + ": " + resp);
        }
        return resp;
    }

    private String responder(Jugador r, int tipo) {
        boolean miente = r.alien && rnd.nextInt(100) >= 25; // el alien dice la verdad a veces para camuflarse
        switch (tipo) {
            case 0:
                return miente ? mentirDonde(r) : verdadDonde(r);
            case 1:
                return miente ? mentirVi(r) : verdadVi(r);
            default:
                return r.alien ? mentirSospecha(r) : verdadSospecha(r);
        }
    }

    private String verdadDonde(Jugador r) {
        List<Jugador.Visita> v = r.visitas;
        Jugador.Visita ultima = v.get(v.size() - 1);
        Jugador.Visita anterior = null;
        for (int i = v.size() - 2; i >= 0; i--) {
            if (v.get(i).hab != ultima.hab) {
                anterior = v.get(i);
                break;
            }
        }
        String s = "Estuve en " + Estacion.nombre(ultima.hab) + " hace " + hace(ultima.t);
        if (anterior != null) {
            s += " y antes en " + Estacion.nombre(anterior.hab) + " hace " + hace(anterior.t);
        }
        return s + ".";
    }

    private String mentirDonde(Jugador r) {
        int h1 = habitacionFalsa(r);
        int h2;
        do {
            h2 = habitacionFalsa(r);
        } while (h2 == h1);
        return "Estuve en " + Estacion.nombre(h1) + " hace " + (8 + rnd.nextInt(20))
                + " s y antes en " + Estacion.nombre(h2) + " hace " + (35 + rnd.nextInt(30)) + " s.";
    }

    private int habitacionFalsa(Jugador r) {
        List<Jugador.Visita> v = r.visitas;
        int ultima = v.get(v.size() - 1).hab;
        int h;
        do {
            h = rnd.nextInt(Estacion.NUM_HAB);
        } while (h == ultima);
        return h;
    }

    private String verdadVi(Jugador r) {
        List<Integer> ids = new ArrayList<>();
        for (Jugador p : jugadores) {
            Jugador.Vista v = r.ultimaVista[p.id];
            if (p != r && v != null && tiempo - v.t <= MEMORIA_VISTAS) ids.add(p.id);
        }
        if (ids.isEmpty()) return "No he visto a nadie.";
        final Jugador yo = r;
        Collections.sort(ids, new Comparator<Integer>() {
            @Override
            public int compare(Integer a, Integer b) {
                return Float.compare(yo.ultimaVista[b].t, yo.ultimaVista[a].t);
            }
        });
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < Math.min(2, ids.size()); i++) {
            Jugador p = jugadores.get(ids.get(i));
            Jugador.Vista v = r.ultimaVista[p.id];
            if (i > 0) sb.append(" ");
            sb.append(fraseVista(p, v.hab, hace(v.t)));
        }
        return sb.toString();
    }

    private String mentirVi(Jugador r) {
        List<Jugador> candidatos = new ArrayList<>();
        for (Jugador p : jugadores) if (p.vivo && p != r) candidatos.add(p);
        if (candidatos.isEmpty()) return "No he visto a nadie.";
        Collections.shuffle(candidatos, rnd);
        int cuantos = 1 + rnd.nextInt(2);
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < Math.min(cuantos, candidatos.size()); i++) {
            if (i > 0) sb.append(" ");
            sb.append(fraseVista(candidatos.get(i), rnd.nextInt(Estacion.NUM_HAB),
                    (8 + rnd.nextInt(35)) + " s"));
        }
        return sb.toString();
    }

    private String fraseVista(Jugador visto, int hab, String hace) {
        if (visto.humano) return "Te vi en " + Estacion.nombre(hab) + " hace " + hace + ".";
        return "Vi a " + visto.nombre + " en " + Estacion.nombre(hab) + " hace " + hace + ".";
    }

    private String verdadSospecha(Jugador r) {
        int s = sospechosoDe(r);
        if (s < 0) return "No tengo ni idea de quién puede ser.";
        Jugador p = jugadores.get(s);
        Jugador.Vista v = r.ultimaVista[s];
        String donde = (v != null) ? " Lo vi cerca de " + Estacion.nombre(v.hab) + "." : "";
        if (p.humano) return "Creo que eres tú." + (v != null ? " Te vi cerca de " + Estacion.nombre(v.hab) + "." : "");
        return "Sospecho de " + p.nombre + "." + donde;
    }

    private String mentirSospecha(Jugador r) {
        List<Jugador> candidatos = new ArrayList<>();
        for (Jugador p : jugadores) if (p.vivo && p != r) candidatos.add(p);
        if (candidatos.isEmpty()) return "No sé quién puede ser.";
        Jugador p = candidatos.get(rnd.nextInt(candidatos.size()));
        if (p.humano) return "Sospecho de ti, te comportas de forma extraña.";
        return "Yo sospecho de " + p.nombre + ", se comporta de forma extraña.";
    }

    /**
     * A quién culpa un tripulante: suma puntos a quien vio cerca de cada ataque
     * (más si fue en la misma habitación). Devuelve -1 si no tiene pruebas claras.
     */
    private int sospechosoDe(Jugador obs) {
        if (ataques.isEmpty()) return -1;
        int[] puntos = new int[NUM_JUGADORES];
        for (Jugador.Avistamiento a : obs.historial) {
            if (a.quien == obs.id) continue;
            for (float[] k : ataques) {
                int kh = (int) k[0];
                float kt = k[1];
                if (a.t1 < kt - 40f || a.t0 > kt + 15f) continue;
                if (a.hab == kh) puntos[a.quien] += 3;
                else if (Estacion.sonCercanas(a.hab, kh)) puntos[a.quien] += 1;
            }
        }
        int mejor = -1;
        int mejorPuntos = 2; // hace falta al menos una prueba fuerte
        int empates = 0;
        for (Jugador p : jugadores) {
            if (p == obs || !p.vivo) continue;
            if (puntos[p.id] > mejorPuntos) {
                mejorPuntos = puntos[p.id];
                mejor = p.id;
                empates = 1;
            } else if (puntos[p.id] == mejorPuntos && mejor >= 0) {
                empates++;
                if (rnd.nextInt(empates) == 0) mejor = p.id;
            }
        }
        return mejor;
    }

    // ------------------------------------------------------------------ votación

    /** El humano vota (-1 = saltar) y los NPC también. Devuelve el resumen del resultado. */
    public String votar(int votoHumano) {
        if (fase != Fase.REUNION) return "";
        int[] voto = new int[NUM_JUGADORES];
        Arrays.fill(voto, -2);

        if (votoHumano < 0 || votoHumano >= NUM_JUGADORES || votoHumano == humano.id
                || !jugadores.get(votoHumano).vivo) {
            votoHumano = -1;
        }
        voto[humano.id] = votoHumano;

        int[] cuenta = new int[NUM_JUGADORES];
        for (Jugador p : jugadores) {
            if (p.vivo && !p.humano && !p.alien) {
                voto[p.id] = votoTripulante(p);
            }
        }
        for (int v : voto) if (v >= 0) cuenta[v]++;
        if (alien.vivo) voto[alien.id] = votoAlien(cuenta);

        Arrays.fill(cuenta, 0);
        int saltos = 0;
        StringBuilder sb = new StringBuilder("VOTOS:\n");
        for (Jugador p : jugadores) {
            if (!p.vivo) continue;
            int v = voto[p.id];
            String quien = p.humano ? "Tú" : p.nombre;
            if (v >= 0) {
                cuenta[v]++;
                sb.append(quien).append(" → ").append(jugadores.get(v).nombre).append("\n");
            } else {
                saltos++;
                sb.append(quien).append(" → saltar\n");
            }
        }

        int max = 0, idMax = -1;
        boolean empate = false;
        for (int i = 0; i < NUM_JUGADORES; i++) {
            if (cuenta[i] > max) {
                max = cuenta[i];
                idMax = i;
                empate = false;
            } else if (cuenta[i] == max && max > 0) {
                empate = true;
            }
        }

        sb.append("\n");
        if (idMax < 0 || empate || max <= saltos) {
            sb.append("Nadie es expulsado.");
            reanudar();
            return sb.toString();
        }

        Jugador expulsado = jugadores.get(idMax);
        expulsado.vivo = false;
        sb.append(expulsado.humano ? "Te han expulsado de la estación." : expulsado.nombre + " ha sido expulsado.");
        if (expulsado.alien) {
            sb.append(" ¡ERA EL ALIEN!");
            terminar(true, "Has descubierto al alien. ¡La estación está a salvo!");
        } else if (expulsado.humano) {
            terminar(false, "Tus compañeros te han expulsado creyendo que eras el alien.");
        } else {
            sb.append(" No era el alien.");
            if (tripulantesVivos() <= 1) {
                terminar(false, "Quedan muy pocos tripulantes: el alien se ha hecho con la estación.");
            } else {
                reanudar();
            }
        }
        return sb.toString();
    }

    private int votoTripulante(Jugador p) {
        // Sin pruebas, un tripulante prudente se abstiene en lugar de votar al azar
        return sospechosoDe(p);
    }

    /** El alien se camufla votando al más votado por los tripulantes. */
    private int votoAlien(int[] cuentaTripulantes) {
        int max = 0, id = -1;
        for (int i = 0; i < NUM_JUGADORES; i++) {
            if (i != alien.id && jugadores.get(i).vivo && cuentaTripulantes[i] > max) {
                max = cuentaTripulantes[i];
                id = i;
            }
        }
        if (id >= 0) return id;
        List<Jugador> vivos = new ArrayList<>();
        for (Jugador q : jugadores) if (q.vivo && q != alien) vivos.add(q);
        return vivos.isEmpty() ? -1 : vivos.get(rnd.nextInt(vivos.size())).id;
    }

    private void reanudar() {
        fase = Fase.JUGANDO;
        reunionPermitidaDesde = tiempo + PAUSA_ENTRE_REUNIONES;
        alien.enfriamiento = Math.max(alien.enfriamiento, 15f);
        for (Jugador p : jugadores) {
            if (!p.vivo) continue;
            p.ruta.clear();
            p.espera = rnd.nextFloat() * 2f;
        }
    }

    private void terminar(boolean gana, String mensaje) {
        fase = Fase.FIN;
        victoria = gana;
        mensajeFin = mensaje + "\nEl alien era " + alien.nombre + ".";
        if (oyente != null) oyente.alTerminar(gana, mensajeFin);
    }

    // ------------------------------------------------------------------ cuaderno

    public String cuaderno() {
        StringBuilder sb = new StringBuilder("ÚLTIMA VEZ QUE LOS VISTE\n");
        for (Jugador p : jugadores) {
            if (p.humano) continue;
            Jugador.Vista v = humano.ultimaVista[p.id];
            sb.append("• ").append(p.nombre).append(p.vivo ? "" : " (ELIMINADO)").append(": ");
            if (v == null) sb.append("aún no lo has visto");
            else sb.append(Estacion.nombre(v.hab)).append(", hace ").append(hace(v.t));
            sb.append("\n");
        }
        sb.append("\nTESTIMONIOS\n");
        if (testimonios.isEmpty()) {
            sb.append("Todavía nadie te ha contado nada. Usa HABLAR con quien esté en tu habitación.");
        } else {
            for (int i = Math.max(0, testimonios.size() - 25); i < testimonios.size(); i++) {
                sb.append(testimonios.get(i)).append("\n");
            }
        }
        return sb.toString();
    }

    // ------------------------------------------------------------------ utilidades

    private String hace(float t) {
        int s = Math.max(0, Math.round(tiempo - t));
        if (s <= 1) return "un instante";
        return s < 60 ? s + " s" : (s / 60) + " min";
    }

    private static String formatoTiempo(float t) {
        int s = (int) t;
        return (s / 60) + ":" + (s % 60 < 10 ? "0" : "") + (s % 60);
    }
}
