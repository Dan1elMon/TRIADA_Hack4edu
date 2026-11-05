import pg from 'pg';
import { env } from '../config/env.js';
// Los BIGINT/NUMERIC de Postgres llegan como string; los indices globales y
// contadores caben holgadamente en un Number seguro, asi que se convierten.
pg.types.setTypeParser(pg.types.builtins.INT8, (v) => Number(v));
pg.types.setTypeParser(pg.types.builtins.NUMERIC, (v) => Number(v));
export const pool = new pg.Pool({
    host: env.db.host,
    port: env.db.port,
    user: env.db.user,
    password: env.db.password,
    database: env.db.database,
    max: env.db.max,
    idleTimeoutMillis: 30_000,
    connectionTimeoutMillis: 10_000,
});
export async function consulta(ej, sql, params = []) {
    const r = await ej.query(sql, params);
    return r.rows;
}
export async function uno(ej, sql, params = []) {
    const rows = await consulta(ej, sql, params);
    return rows[0] ?? null;
}
/**
 * Ejecuta el callback dentro de una transaccion.
 * [PRT-INTEGRIDAD.2] La validacion de no-ciclos y toda mutacion del grafo se
 * ejecutan de forma transaccional previa a la confirmacion.
 */
export async function enTransaccion(fn) {
    const cx = await pool.connect();
    try {
        await cx.query('BEGIN');
        const r = await fn(cx);
        await cx.query('COMMIT');
        return r;
    }
    catch (e) {
        await cx.query('ROLLBACK').catch(() => undefined);
        throw e;
    }
    finally {
        cx.release();
    }
}
export async function esperarDisponible(intentos = 30, esperaMs = 2000) {
    for (let i = 1; i <= intentos; i++) {
        try {
            await pool.query('SELECT 1');
            return;
        }
        catch (e) {
            if (i === intentos)
                throw e;
            await new Promise((r) => setTimeout(r, esperaMs));
        }
    }
}
//# sourceMappingURL=pool.js.map