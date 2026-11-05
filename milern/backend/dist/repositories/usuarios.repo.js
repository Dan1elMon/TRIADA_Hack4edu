import { consulta, uno } from '../db/pool.js';
const COLS = 'id, email, password_hash, nombre, telegram_chat_id::text, zona_horaria, activo';
export async function porEmail(ej, email) {
    return uno(ej, `SELECT ${COLS} FROM usuarios WHERE email = $1 AND activo`, [email]);
}
export async function porId(ej, id) {
    return uno(ej, `SELECT ${COLS} FROM usuarios WHERE id = $1`, [id]);
}
export async function porChatTelegram(ej, chatId) {
    return uno(ej, `SELECT ${COLS} FROM usuarios WHERE telegram_chat_id = $1::bigint AND activo`, [chatId]);
}
export async function crear(ej, u) {
    const row = await uno(ej, `INSERT INTO usuarios (email, password_hash, nombre, telegram_chat_id)
     VALUES ($1,$2,$3,$4::bigint) RETURNING ${COLS}`, [u.email, u.password_hash, u.nombre ?? '', u.telegram_chat_id ?? null]);
    return row;
}
export async function vincularTelegram(ej, id, chatId) {
    await ej.query('UPDATE usuarios SET telegram_chat_id = $2::bigint WHERE id = $1', [id, chatId]);
}
export async function activos(ej) {
    return consulta(ej, `SELECT ${COLS} FROM usuarios WHERE activo ORDER BY creado_en`);
}
//# sourceMappingURL=usuarios.repo.js.map