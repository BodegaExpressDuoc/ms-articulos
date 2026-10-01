package cl.duoc.bodegaexpress.articulos.service;

import cl.duoc.bodegaexpress.articulos.model.Articulo;
import cl.duoc.bodegaexpress.articulos.repository.ArticuloRepository;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional(readOnly = true)
public class ArticuloService {

    private final ArticuloRepository articuloRepository;

    public ArticuloService(ArticuloRepository articuloRepository) {
        this.articuloRepository = articuloRepository;
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
        return articuloRepository.save(nuevoArticulo);
    }

    @Transactional
    public Articulo actualizarArticulo(Long id, Articulo articulo) {
        Articulo articuloExistente = buscarPorId(id);
        articuloExistente.setNombre(articulo.getNombre());
        articuloExistente.setDescripcion(articulo.getDescripcion());
        articuloExistente.setPrecio(articulo.getPrecio());
        articuloExistente.setStock(articulo.getStock());
        return articuloRepository.save(articuloExistente);
    }

    @Transactional
    public void eliminarArticulo(Long id) {
        Articulo articulo = buscarPorId(id);
        articuloRepository.delete(articulo);
    }
}
