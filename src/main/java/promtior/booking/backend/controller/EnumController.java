package promtior.booking.backend.controller;

import java.util.Arrays;
import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import promtior.booking.backend.dto.EnumResource;
import promtior.booking.backend.enums.IEnumResource;
import promtior.booking.backend.exception.ResourceNotFoundException;

/**
 * Endpoint genérico de enums: {@code GET /enums/{enumName}?lang=es} devuelve
 * {@code [{id, description}]} con traducciones (ver CLAUDE.md).
 *
 * Para agregar un enum nuevo, sumar un caso en {@link #resolveValues(String)}.
 */
@RestController
@RequestMapping("/enums")
public class EnumController {

   @GetMapping("/{enumName}")
   public List<EnumResource> getEnum(@PathVariable String enumName, @RequestParam(required = false) String lang) {
      IEnumResource[] values = resolveValues(enumName);
      return Arrays.stream(values)
            .map(v -> v.getEnumResourceAndSetLocaleMessageLanguage(lang))
            .toList();
   }

   private IEnumResource[] resolveValues(String enumName) {
      // Agregar un case por cada enum de dominio, p. ej.:
      // if ("SomeEnum".equals(enumName)) return SomeEnum.values();
      throw new ResourceNotFoundException("Unknown enum: " + enumName);
   }
}
