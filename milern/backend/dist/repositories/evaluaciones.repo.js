import { consulta, uno } from '../db/pool.js';
const COLS = `id, usuario_id, semana_iso, indice_global, estado, total_items,
              aciertos, fallos, puntaje, generada_en, cerrada_en`;
export async function crear(ej, e) {
    return uno(ej, `INSERT INTO evaluaciones (usuario_id, semana_iso, indice_global)
     VALUES ($1,$2,$3)
     ON CONFLICT (usuario_id, semana_iso) DO NOTHING
     RETURNING ${COLS}`, [e.usuario_id, e.semana_iso, e.indice_global]);
}
export async function porSemana(ej, usuarioId, semana) {
    return uno(ej, `SELECT ${COLS} FROM evaluaciones WHERE usuario_id = $1 AND semana_iso = $2`, [usuarioId, semana]);
}
export async function obtener(ej, id, usuarioId) {
    return uno(ej, `SELECT ${COLS} FROM evaluaciones WHERE id = $1 AND usuario_id = $2`, [id, usuarioId]);
}
export async function listar(ej, usuarioId, limite = 30) {
    return consulta(ej, `SELECT ${COLS} FROM evaluaciones WHERE usuario_id = $1 ORDER BY generada_en DESC LIMIT $2`, [usuarioId, limite]);
}
export async function insertarItems(ej, evaluacionId, items) {
    if (items.length === 0)
        return;
    const params = [];
    const filas = items.map((it, i) => {
        const base = i * 5;
        params.push(evaluacionId, it.nodo_id, it.orden, it.premisa, it.contraste);
        return `($${base + 1},$${base + 2},$${base + 3},$${base + 4},$${base + 5})`;
    });
    await ej.query(`INSERT INTO evaluacion_items (evaluacion_id, nodo_id, orden, premisa, contraste)
     VALUES ${filas.join(',')}`, params);
    await ej.query('UPDATE evaluaciones SET total_items = $2 WHERE id = $1', [evaluacionId, items.length]);
}
export async function items(ej, evaluacionId) {
    return consulta(ej, `SELECT id, evaluacion_id, nodo_id, orden, premisa, contraste, resultado, respondido_en
       FROM evaluacion_items WHERE evaluacion_id = $1 ORDER BY orden ASC`, [evaluacionId]);
}
export async function calificarItem(ej, itemId, resultado) {
    return uno(ej, `UPDATE evaluacion_items SET resultado = $2, respondido_en = now()
      WHERE id = $1 RETURNING id, evaluacion_id, nodo_id, orden, premisa, contraste, resultado, respondido_en`, [itemId, resultado]);
}
/** Recalcula agregados y cierra la evaluacion cuando no quedan pendientes. */
export async function recalcular(ej, evaluacionId) {
    const row = await uno(ej, `WITH agg AS (
        SELECT count(*) FILTER (WHERE resultado = 'acierto')::int AS aciertos,
               count(*) FILTER (WHERE resultado = 'fallo')::int   AS fallos,
               count(*) FILTER (WHERE resultado = 'pendiente')::int AS pendientes,
               count(*)::int AS total
          FROM evaluacion_items WHERE evaluacion_id = $1
     )
     UPDATE evaluaciones e
        SET aciertos = agg.aciertos,
            fallos   = agg.fallos,
            puntaje  = CASE WHEN agg.total = 0 THEN NULL
                            ELSE ROUND(agg.aciertos::numeric * 100 / agg.total, 2) END,
            estado   = CASE WHEN agg.pendientes = 0 AND agg.total > 0 THEN 'calificada'::estado_evaluacion
                            WHEN agg.aciertos + agg.fallos > 0 THEN 'en_progreso'::estado_evaluacion
                            ELSE 'generada'::estado_evaluacion END,
            cerrada_en = CASE WHEN agg.pendientes = 0 AND agg.total > 0 THEN now() ELSE NULL END
       FROM agg
      WHERE e.id = $1
      RETURNING ${COLS.split(',').map((c) => 'e.' + c.trim()).join(', ')}`, [evaluacionId]);
    return row;
}
export async function registrarRetencion(ej, e) {
    await ej.query(`INSERT INTO retencion_historico
       (usuario_id, evaluacion_id, semana_iso, total_items, aciertos, fallos, puntaje)
     VALUES ($1,$2,$3,$4,$5,$6,$7)`, [e.usuario_id, e.id, e.semana_iso, e.total_items, e.aciertos, e.fallos, e.puntaje ?? 0]);
}
export async function historicoRetencion(ej, usuarioId, limite = 52) {
    return consulta(ej, `SELECT semana_iso, total_items, aciertos, fallos, puntaje, registrado_en
       FROM retencion_historico WHERE usuario_id = $1
      ORDER BY registrado_en DESC LIMIT $2`, [usuarioId, limite]);
}
//# sourceMappingURL=evaluaciones.repo.js.map