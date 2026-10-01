package cl.duoc.bodegaexpress.articulos;

import cl.duoc.bodegaexpress.articulos.controller.ArticuloController;
import cl.duoc.bodegaexpress.articulos.model.Articulo;
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
    private MockMvc mvc;
    private Articulo articulo;
    private static final String BODY = """
            {"nombre":"Caja","descripcion":"Caja mediana","precio":2500.50,"stock":10}
            """;

    @BeforeEach
    void configurar() {
        repository = mock(ArticuloRepository.class);
        mvc = MockMvcBuilders.standaloneSetup(
                new ArticuloController(new ArticuloService(repository))).build();
        articulo = new Articulo(1L, "Caja", "Caja mediana", new BigDecimal("2500.50"), 10);
    }

    @Test
    void listarYBuscar() throws Exception {
        when(repository.findAll()).thenReturn(List.of(articulo));
        when(repository.findById(1L)).thenReturn(Optional.of(articulo));
        mvc.perform(get("/api/articulos")).andExpect(status().isOk())
                .andExpect(jsonPath("$[0].nombre").value("Caja"));
        mvc.perform(get("/api/articulos/1")).andExpect(status().isOk())
                .andExpect(jsonPath("$.stock").value(10));
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
        mvc.perform(post("/api/articulos").contentType(MediaType.APPLICATION_JSON)
                        .content(BODY.replace("{", "{\"id\":99,")))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", "http://localhost/api/articulos/1"))
                .andExpect(jsonPath("$.id").value(1));
    }

    @Test
    void actualizarMantieneIdYActualizaCampos() throws Exception {
        when(repository.findById(1L)).thenReturn(Optional.of(articulo));
        when(repository.save(any(Articulo.class))).thenAnswer(i -> i.getArgument(0));
        mvc.perform(put("/api/articulos/1").contentType(MediaType.APPLICATION_JSON)
                        .content(BODY.replace("Caja", "Bolsa").replace("2500.50", "100.25").replace("10}", "3}")))
                .andExpect(status().isOk()).andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.nombre").value("Bolsa"))
                .andExpect(jsonPath("$.descripcion").value("Bolsa mediana"))
                .andExpect(jsonPath("$.precio").value(100.25))
                .andExpect(jsonPath("$.stock").value(3));
    }

    @Test
    void eliminar() throws Exception {
        when(repository.findById(1L)).thenReturn(Optional.of(articulo));
        mvc.perform(delete("/api/articulos/1")).andExpect(status().isNoContent());
        verify(repository).delete(articulo);
    }

    @Test
    void inexistenteDevuelve404SinEscribir() throws Exception {
        mvc.perform(get("/api/articulos/99")).andExpect(status().isNotFound());
        mvc.perform(put("/api/articulos/99").contentType(MediaType.APPLICATION_JSON).content(BODY))
                .andExpect(status().isNotFound());
        mvc.perform(delete("/api/articulos/99")).andExpect(status().isNotFound());
        verify(repository, never()).save(any());
        verify(repository, never()).delete(any(Articulo.class));
    }

    @Test
    void rechazarDatosInvalidosSinEscribir() throws Exception {
        for (String body : List.of("{}", BODY.replace("Caja", ""),
                BODY.replace("2500.50", "-1"), BODY.replace("2500.50", "1.001"),
                BODY.replace("10}", "-1}"))) {
            mvc.perform(post("/api/articulos").contentType(MediaType.APPLICATION_JSON).content(body))
                    .andExpect(status().isBadRequest());
            mvc.perform(put("/api/articulos/1").contentType(MediaType.APPLICATION_JSON).content(body))
                    .andExpect(status().isBadRequest());
        }
        verifyNoInteractions(repository);
    }
}
