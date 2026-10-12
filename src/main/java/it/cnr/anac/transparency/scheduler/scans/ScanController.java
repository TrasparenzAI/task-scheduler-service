/*
 * Copyright (C) 2026 Consiglio Nazionale delle Ricerche
 *
 *     This program is free software: you can redistribute it and/or modify
 *     it under the terms of the GNU Affero General Public License as
 *     published by the Free Software Foundation, either version 3 of the
 *     License, or (at your option) any later version.
 *
 *     This program is distributed in the hope that it will be useful,
 *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *     GNU Affero General Public License for more details.
 *
 *     You should have received a copy of the GNU Affero General Public License
 *     along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */
package it.cnr.anac.transparency.scheduler.scans;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import it.cnr.anac.transparency.scheduler.conductor.ConductorService;
import java.util.regex.Pattern;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

/**
 * Avvio della scansione di una singola amministrazione, utilizzabile dagli RPCT per le
 * amministrazioni di cui sono responsabili e dagli amministratori per tutte.
 *
 * @author Cristian Lucchesi
 */
@SecurityRequirement(name = "bearer_authentication")
@Slf4j
@RequiredArgsConstructor
@RequestMapping("/v1/scans")
@RestController
public class ScanController {

  private static final Pattern CODICE_IPA = Pattern.compile("[A-Za-z0-9_.-]{1,64}");

  private final ConductorService conductorService;
  private final ScanAuthorization scanAuthorization;

  @Operation(summary = "Avvia la scansione dell'amministrazione con il codice IPA indicato, "
      + "restituisce l'id del workflow avviato")
  @PostMapping(value = "/{codiceIpa}", produces = MediaType.TEXT_PLAIN_VALUE)
  public ResponseEntity<String> startScan(
      @PathVariable("codiceIpa") String codiceIpa, Authentication authentication) {
    if (!CODICE_IPA.matcher(codiceIpa).matches()) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Codice IPA non valido");
    }
    if (!scanAuthorization.canScan(authentication, codiceIpa)) {
      throw new ResponseStatusException(HttpStatus.FORBIDDEN,
          "Utente non autorizzato alla scansione del codice IPA " + codiceIpa);
    }
    if (conductorService.isWorkflowRunning(codiceIpa)) {
      throw new ResponseStatusException(HttpStatus.CONFLICT,
          "Scansione già in corso per il codice IPA " + codiceIpa);
    }
    log.info("Avvio scansione del codice IPA {} richiesta da {}", codiceIpa, authentication.getName());
    return ResponseEntity.ok(conductorService.startWorkflow(codiceIpa));
  }
}
