import { consulta, uno } from '../db/pool.js';
export async function crearOportunidad(ej, o) {
    const row = await uno(ej, `INSERT INTO oportunidades (usuario_id, situacion, observacion, solucion_id)
     VALUES ($1,$2,$3,$4)
     RETURNING id, usuario_id, situacion, observacion, solucion_id, activo, creado_en`, [o.usuario_id, o.situacion, o.observacion, o.solucion_id ?? null]);
    return row;
}
export async function listarOportunidades(ej, usuarioId, solucionId) {
    return consulta(ej, `SELECT id, usuario_id, situacion, observacion, solucion_id, activo, creado_en
       FROM oportunidades
      WHERE usuario_id = $1 AND activo
        AND ($2::uuid IS NULL OR solucion_id = $2)
      ORDER BY creado_en DESC`, [usuarioId, solucionId ?? null]);
}
/** Vincula multiples observaciones bajo una propuesta formal de solucion. */
export async function vincularOportunidades(ej, usuarioId, solucionId, ids) {
    if (ids.length === 0)
        return 0;
    const r = await ej.query(`UPDATE oportunidades SET solucion_id = $2
      WHERE usuario_id = $1 AND id = ANY($3::uuid[])`, [usuarioId, solucionId, ids]);
    return r.rowCount ?? 0;
}
export async function desvincularOportunidad(ej, usuarioId, id) {
    await ej.query('UPDATE oportunidades SET solucion_id = NULL WHERE usuario_id = $1 AND id = $2', [usuarioId, id]);
}
export async function crearSolucion(ej, s) {
    const row = await uno(ej, `INSERT INTO soluciones (usuario_id, titulo, descripcion) VALUES ($1,$2,$3)
     RETURNING id, usuario_id, titulo, descripcion, estado, creado_en, actualizado_en`, [s.usuario_id, s.titulo, s.descripcion ?? '']);
    return row;
}
export async function listarSoluciones(ej, usuarioId) {
    return consulta(ej, `SELECT s.id, s.usuario_id, s.titulo, s.descripcion, s.estado, s.creado_en, s.actualizado_en,
            (SELECT count(*)::int FROM oportunidades o WHERE o.solucion_id = s.id AND o.activo) AS total_observaciones
       FROM soluciones s WHERE s.usuario_id = $1
      ORDER BY CASE s.estado WHEN 'en_progreso' THEN 0 WHEN 'backlog' THEN 1 ELSE 2 END, s.creado_en DESC`, [usuarioId]);
}
/** Ciclo de vida: Backlog -> En Progreso -> Completado. */
export async function cambiarEstadoSolucion(ej, usuarioId, id, estado) {
    return uno(ej, `UPDATE soluciones SET estado = $3 WHERE id = $2 AND usuario_id = $1
     RETURNING id, usuario_id, titulo, descripcion, estado, creado_en, actualizado_en`, [usuarioId, id, estado]);
}
//# sourceMappingURL=mejoras.repo.js.map