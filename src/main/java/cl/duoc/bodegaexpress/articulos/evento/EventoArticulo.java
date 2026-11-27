package cl.duoc.bodegaexpress.articulos.evento;

import cl.duoc.bodegaexpress.articulos.model.Articulo;
import cl.duoc.bodegaexpress.articulos.model.HistorialArticulo.Operacion;

import java.time.Instant;

/**
 * Mensaje que ms-articulos publica en la cola SQS cada vez que un artículo
 * se crea, actualiza o elimina. Lo consume la Lambda de alerta de stock bajo.
 */
public record EventoArticulo(Operacion operacion, Long articuloId, String nombre,
                             Integer stock, Instant fecha) {

    public static EventoArticulo de(Articulo articulo, Operacion operacion) {
        return new EventoArticulo(operacion, articulo.getId(), articulo.getNombre(),
                articulo.getStock(), Instant.now());
    }

    public String aJson() {
        return "{\"operacion\":\"" + operacion + "\""
                + ",\"articuloId\":" + articuloId
                + ",\"nombre\":\"" + escapar(nombre) + "\""
                + ",\"stock\":" + stock
                + ",\"fecha\":\"" + fecha + "\"}";
    }

    private static String escapar(String texto) {
        StringBuilder sb = new StringBuilder();
        for (char c : texto.toCharArray()) {
            switch (c) {
                case '"' -> sb.append("\\\"");
                case '\\' -> sb.append("\\\\");
                case '\n' -> sb.append("\\n");
                case '\r' -> sb.append("\\r");
                case '\t' -> sb.append("\\t");
                default -> {
                    if (c < 0x20) {
                        sb.append(String.format("\\u%04x", (int) c));
                    } else {
                        sb.append(c);
                    }
                }
            }
        }
        return sb.toString();
    }
}
