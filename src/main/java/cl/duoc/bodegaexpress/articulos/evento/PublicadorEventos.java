package cl.duoc.bodegaexpress.articulos.evento;

/**
 * Publica los eventos de artículos hacia el exterior (cola AWS SQS).
 */
public interface PublicadorEventos {

    void publicar(EventoArticulo evento);
}
