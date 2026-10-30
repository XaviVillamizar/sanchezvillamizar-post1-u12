package com.empresa.reembolsos.procesamiento.validacion;

import com.empresa.reembolsos.dominio.SolicitudReembolso;
// VIOLACIÓN INTENCIONAL: Importamos infraestructura dentro de procesamiento
import com.empresa.reembolsos.infraestructura.RepositorioSolicitudesJpa;
import org.springframework.stereotype.Component;
import java.util.List;

@Component
public class ValidadorDuplicados extends ValidadorSolicitud {
    
    // Campo dummy para forzar la violación
    private RepositorioSolicitudesJpa dependenciaProhibida;

    @Override
    protected ResultadoValidacion validarPropio(SolicitudReembolso solicitud, List<SolicitudReembolso> pendientes) {
        boolean existeDuplicado = pendientes.stream()
                .anyMatch(p -> p.getCategoria() == solicitud.getCategoria()
                        && p.getFechaGasto().getMonth() == solicitud.getFechaGasto().getMonth());
        return existeDuplicado
                ? ResultadoValidacion.rechazar("Ya existe una solicitud pendiente de la misma categoria en el periodo")
                : ResultadoValidacion.ok();
    }
}