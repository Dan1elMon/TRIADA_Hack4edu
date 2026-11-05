import { consulta, uno } from '../db/pool.js';
/**
 * Log de transacciones que habilita el comando `undo` de la CLI
 * (procedimiento "manejando el conocimiento usando la terminal").
 */
export async function registrar(ej, r) {
    const row = await uno(ej, `INSERT INTO transacciones_log
       (usuario_id, origen, operacion, entidad, entidad_id, payload_anterior, payload_nuevo)
     VALUES ($1,$2,$3,$4,$5,$6,$7) RETURNING id::text`, [r.usuario_id, r.origen, r.operacion, r.entidad, r.entidad_id,
        r.payload_anterior === undefined ? null : JSON.stringify(r.payload_anterior),
        r.payload_nuevo === undefined ? null : JSON.stringify(r.payload_nuevo)]);
    return row.id;
}
export async function ultimaReversible(ej, usuarioId) {
    return uno(ej, `SELECT id::text, usuario_id, origen, operacion, entidad, entidad_id,
            payload_anterior, payload_nuevo, deshecha, creado_en
       FROM transacciones_log
      WHERE usuario_id = $1 AND NOT deshecha AND operacion <> 'undo'
      -- Se califica la columna: sin el prefijo, ORDER BY resolveria al alias de
      -- salida \`id::text\` y ordenaria alfabeticamente ('99' > '117').
      ORDER BY transacciones_log.id DESC LIMIT 1
      FOR UPDATE`, [usuarioId]);
}
export async function marcarDeshecha(ej, id) {
    await ej.query('UPDATE transacciones_log SET deshecha = TRUE, deshecha_en = now() WHERE id = $1', [id]);
}
export async function historial(ej, usuarioId, limite = 50) {
    return consulta(ej, `SELECT id::text, usuario_id, origen, operacion, entidad, entidad_id,
            payload_anterior, payload_nuevo, deshecha, creado_en
       FROM transacciones_log WHERE usuario_id = $1 ORDER BY transacciones_log.id DESC LIMIT $2`, [usuarioId, limite]);
}
//# sourceMappingURL=auditoria.repo.js.map