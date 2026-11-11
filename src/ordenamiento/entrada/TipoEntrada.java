package ordenamiento.entrada;

/** Los cuatro tipos de arreglo que pide la tarea. */
public enum TipoEntrada {
    ALEATORIO("aleatorio"),
    ASCENDENTE("ascendente"),
    DESCENDENTE("descendente"),
    DUPLICADOS("duplicados");

    private final String nombre;

    TipoEntrada(String nombre) {
        this.nombre = nombre;
    }

    /** Nombre en minúscula, tal como se escribe en el CSV. */
    public String nombre() {
        return nombre;
    }
}
