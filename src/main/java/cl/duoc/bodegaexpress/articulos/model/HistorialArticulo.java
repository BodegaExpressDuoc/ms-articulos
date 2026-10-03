package cl.duoc.bodegaexpress.articulos.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "historial_articulos")
@Getter
@NoArgsConstructor
public class HistorialArticulo {
    public enum Operacion { CREACION, ACTUALIZACION, ELIMINACION }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Sin clave foranea: el historial debe sobrevivir al borrado del articulo.
    @Column(nullable = false)
    private Long articuloId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Operacion operacion;

    @Column(nullable = false)
    private Instant fecha;

    @Column(nullable = false, length = 150)
    private String nombre;

    @Column(nullable = false, length = 1000)
    private String descripcion;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal precio;

    @Column(nullable = false)
    private Integer stock;

    public HistorialArticulo(Articulo articulo, Operacion operacion) {
        this.articuloId = articulo.getId();
        this.operacion = operacion;
        this.fecha = Instant.now();
        this.nombre = articulo.getNombre();
        this.descripcion = articulo.getDescripcion();
        this.precio = articulo.getPrecio();
        this.stock = articulo.getStock();
    }
}
