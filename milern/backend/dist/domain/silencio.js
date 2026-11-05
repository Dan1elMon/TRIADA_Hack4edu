import { deltaUE, horaLocal } from '../utils/tiempo.js';
import { env } from '../config/env.js';
/** Numero de horas que abarca la ventana, cruce de medianoche incluido. */
export function duracionVentanaHoras(cfg) {
    return cfg.horaInicio > cfg.horaFin
        ? 24 - cfg.horaInicio + cfg.horaFin // ventana que cruza medianoche: 22 -> 7 = 9 h
        : cfg.horaFin - cfg.horaInicio;
}
/**
 * [LOG-SILENCIO, paso 1] Comprueba si un indice cae en el rango 10pm - 7am.
 *
 * El intervalo es cerrado por la izquierda y abierto por la derecha: a las
 * 22:00 ya hay silencio, a las 07:00 ya no. Asi las dos fronteras no se solapan
 * y el desplazamiento de 54 UE siempre escapa de la ventana.
 */
export function enHorasDeSilencio(indice, cfg = env.silencio) {
    if (!cfg.activo)
        return false;
    const hora = horaLocal(indice, cfg.zonaHoraria);
    return cfg.horaInicio > cfg.horaFin
        ? hora >= cfg.horaInicio || hora < cfg.horaFin // cruza medianoche
        : hora >= cfg.horaInicio && hora < cfg.horaFin;
}
/**
 * [LOG-SILENCIO, paso 2] "cuando se va a generar un nuevo
 * indice_siguiente_esfuerzo para un nodo o grafo este se suma 54 UE (9 horas)
 * si es que el indice_siguiente_esfuerzo iba a estar en el rango de horas
 * 10pm - 7am".
 *
 * Con la ventana por defecto (9 h) y el desplazamiento por defecto (54 UE = 9 h)
 * una sola suma basta siempre, que es exactamente lo que dice la especificacion.
 * El bucle solo actua si se configura una ventana mas ancha que el
 * desplazamiento; ver docs/decisiones.md DEC-016.
 */
export function desplazarFueraDeSilencio(indice, cfg = env.silencio) {
    if (!cfg.activo)
        return indice;
    let resultado = indice;
    // Cota defensiva: 24 h de ventana como maximo teorico, nunca deberia agotarse.
    for (let i = 0; i < 32 && enHorasDeSilencio(resultado, cfg); i++) {
        resultado += cfg.desplazamientoUE;
    }
    return resultado;
}
/**
 * Punto unico de agendamiento: calcula `desde + random(min, max)` y lo aparta de
 * las horas de silencio. Todo `indice_siguiente_esfuerzo` del sistema —de nodo o
 * de grafo— se genera aqui, de modo que la regla no se puede olvidar en un
 * camino nuevo.
 */
export function agendarSiguiente(desde, min, max, cfg = env.silencio) {
    return desplazarFueraDeSilencio(desde + deltaUE(min, max), cfg);
}
/** Descripcion legible de la ventana, para registros y diagnostico. */
export function describirVentana(cfg = env.silencio) {
    if (!cfg.activo)
        return 'horas de silencio desactivadas';
    const dosDigitos = (n) => String(n).padStart(2, '0');
    return `silencio de ${dosDigitos(cfg.horaInicio)}:00 a ${dosDigitos(cfg.horaFin)}:00 ` +
        `(${cfg.zonaHoraria}, ${duracionVentanaHoras(cfg)} h)`;
}
//# sourceMappingURL=silencio.js.map