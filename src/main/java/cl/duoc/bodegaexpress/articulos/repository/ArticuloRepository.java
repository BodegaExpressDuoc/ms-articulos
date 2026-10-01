package cl.duoc.bodegaexpress.articulos.repository;

import cl.duoc.bodegaexpress.articulos.model.Articulo;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ArticuloRepository extends JpaRepository<Articulo, Long> {
}
