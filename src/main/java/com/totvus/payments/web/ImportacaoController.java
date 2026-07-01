package com.totvus.payments.web;

import com.totvus.payments.application.importacao.ImportacaoResponse;
import com.totvus.payments.application.importacao.ImportacaoService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/importacao")
@Tag(name = "Importação")
public class ImportacaoController {

    private final ImportacaoService service;

    public ImportacaoController(ImportacaoService service) {
        this.service = service;
    }

    @PostMapping(value = "/contas", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @ResponseStatus(HttpStatus.ACCEPTED)
    @Operation(summary = "Importar contas via CSV (processamento assíncrono)")
    public ImportacaoResponse importar(@RequestParam("arquivo") MultipartFile arquivo) {
        return service.importar(arquivo);
    }
}