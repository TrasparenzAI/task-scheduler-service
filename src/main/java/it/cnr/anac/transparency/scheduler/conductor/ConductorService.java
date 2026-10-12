/*
 * Copyright (C) 2025 Consiglio Nazionale delle Ricerche
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
package it.cnr.anac.transparency.scheduler.conductor;

import java.util.List;
import java.util.stream.Collectors;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import it.cnr.anac.transparency.scheduler.tasks.WorkflowCronConfig;
import lombok.RequiredArgsConstructor;
import lombok.val;
import lombok.extern.slf4j.Slf4j;

/**
 * Servizio per effettuare le operazioni con il conductor.
 *
 * @author Cristian Lucchesi
 */
@Slf4j
@RequiredArgsConstructor
@Service
@RefreshScope
public class ConductorService {

  private final WorkflowCronConfig workflowCron;
  
  private final ConductorClient conductorClient;

  private final ObjectMapper objectMapper;

  public List<WorkflowDto> completedWorkflowsOnConductor() {
    return conductorClient.allWorkflows().stream()
        .filter(w -> w.getStatus().equals("COMPLETED")).collect(Collectors.toList());
  }

  public String startWorkflow() {
    log.info("Executing workflow start, url = {}, body = {}", workflowCron.getUrl(), workflowCron.getBody());
    val response = conductorClient.startWorkflow(workflowCron.getBody());
    log.info("Conductor response.statusCode = {}, response.body = {}", 
        response.getStatusCode(), response.getBody());
    return response.getBody();
  }

  /**
   * Avvia la scansione di una singola amministrazione.
   * Il body del workflow è quello della configurazione (workflow.cron.body), con i soli
   * parametri necessari alla scansione di una PA impostati lato server, in modo che il
   * chiamante non possa modificare gli altri parametri (per esempio gli URL dei servizi).
   *
   * @param codiceIpa codice IPA dell'amministrazione da scansionare.
   * @return l'id del workflow avviato.
   */
  public String startWorkflow(String codiceIpa) {
    val body = singleCompanyWorkflowBody(codiceIpa);
    log.info("Avvio del workflow per il codice IPA {}, url = {}, body = {}",
        codiceIpa, workflowCron.getUrl(), body);
    val response = conductorClient.startWorkflow(body);
    log.info("Conductor response.statusCode = {}, response.body = {}",
        response.getStatusCode(), response.getBody());
    return response.getBody();
  }

  /**
   * Verifica se è già in corso una scansione per il codice IPA indicato.
   */
  public boolean isWorkflowRunning(String codiceIpa) {
    return !conductorClient.runningWorkflows(codiceIpa).isEmpty();
  }

  String singleCompanyWorkflowBody(String codiceIpa) {
    try {
      val body = (ObjectNode) objectMapper.readTree(workflowCron.getBody());
      body.put("correlationId", codiceIpa);
      val input = body.withObjectProperty("input");
      input.put("codice_ipa", codiceIpa);
      input.put("codice_categoria", "");
      input.put("page_size", 1);
      input.put("id_ipa_from", 0);
      input.put("parent_workflow_id", "");
      input.put("crawler_child_type", "SUB_WORKFLOW");
      input.put("crawler_save_object", true);
      input.put("crawler_save_screenshot", true);
      return objectMapper.writeValueAsString(body);
    } catch (JsonProcessingException | ClassCastException e) {
      throw new IllegalStateException("workflow.cron.body non è un JSON valido", e);
    }
  }

  @Async
  public void deleteWorkflow(String workflowId) {
    try {
      log.info("Elimino il workflow con id = {}", workflowId);
      conductorClient.deleteWorkflow(workflowId);
      log.info("Eliminato workflow con id = {}", workflowId);
    } catch (feign.RetryableException e) {
      log.warn("Timeout raggiunto! Conductor è molto lento nel cancellare il workflow id = {}. " +
                      "Probabilmente il workflow verrà comunque cancellato senza problemi. ", workflowId);
    } catch (Exception e) {
      log.warn("Workflow id = {} non eliminato", workflowId, e);
    }
  }
}
