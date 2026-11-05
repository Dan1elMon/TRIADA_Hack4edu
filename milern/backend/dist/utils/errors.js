/** Error de dominio con codigo estable y status HTTP asociado. */
export class ErrorDominio extends Error {
    codigo;
    status;
    detalle;
    constructor(codigo, mensaje, status = 400, detalle) {
        super(mensaje);
        this.codigo = codigo;
        this.status = status;
        this.detalle = detalle;
        this.name = 'ErrorDominio';
    }
}
export const noEncontrado = (que) => new ErrorDominio('NO_ENCONTRADO', `${que} no encontrado`, 404);
export const conflicto = (codigo, mensaje) => new ErrorDominio(codigo, mensaje, 409);
export const invalido = (codigo, mensaje, detalle) => new ErrorDominio(codigo, mensaje, 422, detalle);
export const noAutorizado = (mensaje = 'Credenciales invalidas') => new ErrorDominio('NO_AUTORIZADO', mensaje, 401);
//# sourceMappingURL=errors.js.map