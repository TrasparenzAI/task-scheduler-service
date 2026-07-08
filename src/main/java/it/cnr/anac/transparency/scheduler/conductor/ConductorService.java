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
