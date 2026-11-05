import { consulta, uno } from '../db/pool.js';
export async function crear(ej, t) {
    const row = await uno(ej, `INSERT INTO api_tokens (usuario_id, nombre, token_hash) VALUES ($1,$2,$3)
     RETURNING id, usuario_id, nombre, revocado, creado_en, ultimo_uso`, [t.usuario_id, t.nombre, t.token_hash]);
    return row;
}
export async function porHash(ej, hash) {
    const row = await uno(ej, 'SELECT usuario_id FROM api_tokens WHERE token_hash = $1 AND NOT revocado', [hash]);
    if (row) {
        await ej.query('UPDATE api_tokens SET ultimo_uso = now() WHERE token_hash = $1', [hash]);
    }
    return row;
}
export async function listar(ej, usuarioId) {
    return consulta(ej, `SELECT id, usuario_id, nombre, revocado, creado_en, ultimo_uso
       FROM api_tokens WHERE usuario_id = $1 ORDER BY creado_en DESC`, [usuarioId]);
}
export async function revocar(ej, id, usuarioId) {
    const r = await ej.query('UPDATE api_tokens SET revocado = TRUE WHERE id = $1 AND usuario_id = $2', [id, usuarioId]);
    return (r.rowCount ?? 0) > 0;
}
//# sourceMappingURL=tokens.repo.js.map