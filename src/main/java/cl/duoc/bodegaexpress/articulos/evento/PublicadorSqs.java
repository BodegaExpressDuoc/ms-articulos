package cl.duoc.bodegaexpress.articulos.evento;

import jakarta.annotation.PreDestroy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.sqs.SqsClient;

/**
 * Productor de la cola SQS.
 * <ul>
 *   <li>Si {@code SQS_COLA_URL} no está definida, no publica nada (desarrollo local y tests).</li>
 *   <li>Publica después del commit de la transacción, para no avisar de cambios revertidos.</li>
 *   <li>Un error al publicar se registra en el log, pero no hace fallar la operación del usuario.</li>
 * </ul>
 * Las credenciales se obtienen de la cadena por defecto del SDK (en la EC2, del perfil de instancia).
 */
@Component
public class PublicadorSqs implements PublicadorEventos {

    private static final Logger log = LoggerFactory.getLogger(PublicadorSqs.class);

    private final String colaUrl;
    private final SqsClient cliente;

    public PublicadorSqs(@Value("${sqs.cola-url:}") String colaUrl,
                         @Value("${sqs.region:us-east-1}") String region) {
        this.colaUrl = colaUrl;
        if (colaUrl == null || colaUrl.isBlank()) {
            this.cliente = null;
            log.info("SQS_COLA_URL no definida: los eventos de artículos no se publicarán");
        } else {
            this.cliente = SqsClient.builder().region(Region.of(region)).build();
            log.info("Eventos de artículos se publicarán en {}", colaUrl);
        }
    }

    @Override
    public void publicar(EventoArticulo evento) {
        if (cliente == null) {
            return;
        }
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    enviar(evento);
                }
            });
        } else {
            enviar(evento);
        }
    }

    private void enviar(EventoArticulo evento) {
        try {
            String id = cliente.sendMessage(m -> m.queueUrl(colaUrl).messageBody(evento.aJson())).messageId();
            log.info("Evento {} del artículo {} publicado en SQS (messageId={})",
                    evento.operacion(), evento.articuloId(), id);
        } catch (RuntimeException e) {
            log.error("No se pudo publicar el evento {} del artículo {} en SQS",
                    evento.operacion(), evento.articuloId(), e);
        }
    }

    @PreDestroy
    void cerrar() {
        if (cliente != null) {
            cliente.close();
        }
    }
}
