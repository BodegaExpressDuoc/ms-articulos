package cl.duoc.bodegaexpress.articulos.service;

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

    public ArticuloService(ArticuloRepository articuloRepository,
                           HistorialArticuloRepository historialRepository) {
        this.articuloRepository = articuloRepository;
        this.historialRepository = historialRepository;
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
        historialRepository.save(new HistorialArticulo(creado, Operacion.CREACION));
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
        historialRepository.save(new HistorialArticulo(actualizado, Operacion.ACTUALIZACION));
        return actualizado;
    }

    @Transactional
    public void eliminarArticulo(Long id) {
        Articulo articulo = buscarPorId(id);
        articuloRepository.delete(articulo);
        historialRepository.save(new HistorialArticulo(articulo, Operacion.ELIMINACION));
    }
}
