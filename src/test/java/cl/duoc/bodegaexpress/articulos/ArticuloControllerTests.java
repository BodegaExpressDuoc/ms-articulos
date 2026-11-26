package cl.duoc.bodegaexpress.articulos;

import cl.duoc.bodegaexpress.articulos.controller.ArticuloController;
import cl.duoc.bodegaexpress.articulos.evento.EventoArticulo;
import cl.duoc.bodegaexpress.articulos.evento.PublicadorEventos;
import cl.duoc.bodegaexpress.articulos.exception.ManejadorErrores;
import cl.duoc.bodegaexpress.articulos.model.Articulo;
import cl.duoc.bodegaexpress.articulos.model.HistorialArticulo;
import cl.duoc.bodegaexpress.articulos.model.HistorialArticulo.Operacion;
import cl.duoc.bodegaexpress.articulos.repository.HistorialArticuloRepository;
import org.mockito.ArgumentCaptor;
import cl.duoc.bodegaexpress.articulos.repository.ArticuloRepository;
import cl.duoc.bodegaexpress.articulos.service.ArticuloService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class ArticuloControllerTests {
    private ArticuloRepository repository;
    private HistorialArticuloRepository historialRepository;
    private PublicadorEventos publicador;
    private MockMvc mvc;
    private Articulo articulo;
    private static final String BODY = """
            {"nombre":"Caja","descripcion":"Caja mediana","precio":2500.50,"stock":10}
            """;

    @BeforeEach
    void configurar() {
        repository = mock(ArticuloRepository.class);
        historialRepository = mock(HistorialArticuloRepository.class);
        publicador = mock(PublicadorEventos.class);
        mvc = MockMvcBuilders.standaloneSetup(
                new ArticuloController(new ArticuloService(repository, historialRepository, publicador)))
                .setControllerAdvice(new ManejadorErrores())
                .build();
        articulo = new Articulo(1L, "Caja", "Caja mediana", new BigDecimal("2500.50"), 10);
    }

    @Test
    void listarYBuscar() throws Exception {
        when(repository.findAll()).thenReturn(List.of(articulo));
        when(repository.findById(1L)).thenReturn(Optional.of(articulo));
        mvc.perform(get("/api/v1/articulos")).andExpect(status().isOk())
                .andExpect(jsonPath("$[0].nombre").value("Caja"));
        mvc.perform(get("/api/v1/articulos/1")).andExpect(status().isOk())
                .andExpect(jsonPath("$.stock").value(10));
        verifyNoInteractions(historialRepository, publicador);
    }

    @Test
    void crearIgnoraIdRecibido() throws Exception {
        when(repository.save(any(Articulo.class))).thenAnswer(invocation -> {
            Articulo nuevo = invocation.getArgument(0);
            assertNull(nuevo.getId());
            assertEquals(new BigDecimal("2500.50"), nuevo.getPrecio());
            assertEquals(10, nuevo.getStock());
            nuevo.setId(1L);
            return nuevo;
        });
        mvc.perform(post("/api/v1/articulos").contentType(MediaType.APPLICATION_JSON)
                        .content(BODY.replace("{", "{\"id\":99,")))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", "http://localhost/api/v1/articulos/1"))
                .andExpect(jsonPath("$.id").value(1));
        verificarHistorial(Operacion.CREACION, "Caja", 10);
    }

    @Test
    void actualizarMantieneIdYActualizaCampos() throws Exception {
        when(repository.findById(1L)).thenReturn(Optional.of(articulo));
        when(repository.save(any(Articulo.class))).thenAnswer(i -> i.getArgument(0));
        mvc.perform(put("/api/v1/articulos/1").contentType(MediaType.APPLICATION_JSON)
                        .content(BODY.replace("Caja", "Bolsa").replace("2500.50", "100.25").replace("10}", "3}")))
                .andExpect(status().isOk()).andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.nombre").value("Bolsa"))
                .andExpect(jsonPath("$.descripcion").value("Bolsa mediana"))
                .andExpect(jsonPath("$.precio").value(100.25))
                .andExpect(jsonPath("$.stock").value(3));
        verificarHistorial(Operacion.ACTUALIZACION, "Bolsa", 3);
    }

    @Test
    void eliminar() throws Exception {
        when(repository.findById(1L)).thenReturn(Optional.of(articulo));
        mvc.perform(delete("/api/v1/articulos/1")).andExpect(status().isNoContent());
        verify(repository).delete(articulo);
        verificarHistorial(Operacion.ELIMINACION, "Caja", 10);
    }

    @Test
    void inexistenteDevuelve404SinEscribir() throws Exception {
        mvc.perform(get("/api/v1/articulos/99")).andExpect(status().isNotFound());
        mvc.perform(put("/api/v1/articulos/99").contentType(MediaType.APPLICATION_JSON).content(BODY))
                .andExpect(status().isNotFound());
        mvc.perform(delete("/api/v1/articulos/99")).andExpect(status().isNotFound());
        verify(repository, never()).save(any());
        verify(repository, never()).delete(any(Articulo.class));
        verifyNoInteractions(historialRepository, publicador);
    }

    @Test
    void rechazarDatosInvalidosSinEscribir() throws Exception {
        for (String body : List.of("{}", BODY.replace("Caja", ""),
                BODY.replace("2500.50", "-1"), BODY.replace("2500.50", "1.001"),
                BODY.replace("10}", "-1}"))) {
            mvc.perform(post("/api/v1/articulos").contentType(MediaType.APPLICATION_JSON).content(body))
                    .andExpect(status().isBadRequest());
            mvc.perform(put("/api/v1/articulos/1").contentType(MediaType.APPLICATION_JSON).content(body))
                    .andExpect(status().isBadRequest());
        }
        verifyNoInteractions(repository, historialRepository, publicador);
    }

    private void verificarHistorial(Operacion operacion, String nombre, int stock) {
        ArgumentCaptor<HistorialArticulo> captor = ArgumentCaptor.forClass(HistorialArticulo.class);
        verify(historialRepository).save(captor.capture());
        HistorialArticulo registro = captor.getValue();
        assertEquals(1L, registro.getArticuloId());
        assertEquals(operacion, registro.getOperacion());
        assertEquals(nombre, registro.getNombre());
        assertEquals(stock, registro.getStock());
        assertNotNull(registro.getFecha());

        ArgumentCaptor<EventoArticulo> eventoCaptor = ArgumentCaptor.forClass(EventoArticulo.class);
        verify(publicador).publicar(eventoCaptor.capture());
        EventoArticulo evento = eventoCaptor.getValue();
        assertEquals(operacion, evento.operacion());
        assertEquals(1L, evento.articuloId());
        assertEquals(nombre, evento.nombre());
        assertEquals(stock, evento.stock());
        assertNotNull(evento.fecha());
    }

    @Test
    void eventoSeSerializaComoJsonValido() {
        EventoArticulo evento = new EventoArticulo(Operacion.ACTUALIZACION, 7L, "Caja \"grande\"\\A",
                2, java.time.Instant.parse("2026-11-26T12:00:00Z"));
        assertEquals("{\"operacion\":\"ACTUALIZACION\",\"articuloId\":7,\"nombre\":\"Caja \\\"grande\\\"\\\\A\""
                + ",\"stock\":2,\"fecha\":\"2026-11-26T12:00:00Z\"}", evento.aJson());
    }
}
