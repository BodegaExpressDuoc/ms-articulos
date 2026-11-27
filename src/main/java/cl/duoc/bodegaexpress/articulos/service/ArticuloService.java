package cl.duoc.bodegaexpress.articulos.service;

import cl.duoc.bodegaexpress.articulos.evento.EventoArticulo;
import cl.duoc.bodegaexpress.articulos.evento.PublicadorEventos;
import cl.duoc.bodegaexpress.articulos.model.Articulo;
import cl.duoc.bodegaexpress.articulos.model.HistorialArticulo;
import cl.duoc.bodegaexpress.articulos.model.HistorialArticulo.Operacion;
import cl.duoc.bodegaexpress.articulos.repository.HistorialArticuloRepository;
import cl.duoc.bodegaexpress.articulos.repository.ArticuloRepository;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional(readOnly = true)
public class ArticuloService {

    private final ArticuloRepository articuloRepository;
    private final HistorialArticuloRepository historialRepository;
    private final PublicadorEventos publicador;

    public ArticuloService(ArticuloRepository articuloRepository,
                           HistorialArticuloRepository historialRepository,
                           PublicadorEventos publicador) {
        this.articuloRepository = articuloRepository;
        this.historialRepository = historialRepository;
        this.publicador = publicador;
    }

    public List<Articulo> listarArticulos() {
        return articuloRepository.findAll();
    }

    public Articulo buscarPorId(Long id) {
        return articuloRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Articulo no encontrado con id: " + id));
    }

    @Transactional
    public Articulo crearArticulo(Articulo articulo) {
        Articulo nuevoArticulo = new Articulo();
        nuevoArticulo.setNombre(articulo.getNombre());
        nuevoArticulo.setDescripcion(articulo.getDescripcion());
        nuevoArticulo.setPrecio(articulo.getPrecio());
        nuevoArticulo.setStock(articulo.getStock());
        Articulo creado = articuloRepository.save(nuevoArticulo);
        registrar(creado, Operacion.CREACION);
        return creado;
    }

    @Transactional
    public Articulo actualizarArticulo(Long id, Articulo articulo) {
        Articulo articuloExistente = buscarPorId(id);
        articuloExistente.setNombre(articulo.getNombre());
        articuloExistente.setDescripcion(articulo.getDescripcion());
        articuloExistente.setPrecio(articulo.getPrecio());
        articuloExistente.setStock(articulo.getStock());
        Articulo actualizado = articuloRepository.save(articuloExistente);
        registrar(actualizado, Operacion.ACTUALIZACION);
        return actualizado;
    }

    @Transactional
    public void eliminarArticulo(Long id) {
        Articulo articulo = buscarPorId(id);
        articuloRepository.delete(articulo);
        registrar(articulo, Operacion.ELIMINACION);
    }

    private void registrar(Articulo articulo, Operacion operacion) {
        historialRepository.save(new HistorialArticulo(articulo, operacion));
        publicador.publicar(EventoArticulo.de(articulo, operacion));
    }
}
