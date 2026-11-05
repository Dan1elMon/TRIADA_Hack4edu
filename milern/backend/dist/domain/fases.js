export const FASES = {
    fase_1: { min: 2, max: 6, umbral: 36, siguiente: 'fase_2' },
    fase_2: { min: 9, max: 15, umbral: 84, siguiente: 'fase_3' },
    fase_3: { min: 21, max: 35, umbral: 108, siguiente: 'fase_4' },
    fase_4: { min: 54, max: 66, umbral: null, siguiente: null },
};
/** Rango de agendacion propio de un Grafo de Conocimiento (54 - 66 UE). */
export const RANGO_GRAFO = { min: 54, max: 66 };
export function configDeFase(fase) {
    if (fase === 'archivado')
        throw new Error('Un nodo archivado no genera esfuerzos');
    return FASES[fase];
}
/**
 * [LOG-GEN-NODO] Evalua la transicion de fase tras confirmar un envio.
 *   - incrementa `conteo_esfuerzo`
 *   - evalua el umbral de la etapa actual
 *   - un nodo NO temporal que alcanza la cuarta fase se transfiere al grafo
 */
export function evaluarTransicion(nodo) {
    const conteo = nodo.conteo_esfuerzo + 1;
    const conteoFase = nodo.conteo_esfuerzo_fase + 1;
    const cfg = configDeFase(nodo.fase);
    if (cfg.umbral !== null && cfg.siguiente !== null && conteoFase >= cfg.umbral) {
        return {
            fase: cfg.siguiente,
            conteo_esfuerzo: conteo,
            conteo_esfuerzo_fase: 0,
            transiciono: true,
            ingresa_a_grafo: cfg.siguiente === 'fase_4' && !nodo.es_temporal,
        };
    }
    return {
        fase: nodo.fase,
        conteo_esfuerzo: conteo,
        conteo_esfuerzo_fase: conteoFase,
        transiciono: false,
        ingresa_a_grafo: false,
    };
}
/**
 * Determina si un nodo, dada su fase, sigue generando esfuerzos por si mismo.
 * En fase_4 solo los nodos temporales conservan generacion propia; los
 * estructurados delegan en el Grafo de Conocimiento.
 */
export function generaEsfuerzosPropios(fase, esTemporal) {
    if (fase === 'archivado')
        return false;
    if (fase === 'fase_4')
        return esTemporal;
    return true;
}
//# sourceMappingURL=fases.js.map