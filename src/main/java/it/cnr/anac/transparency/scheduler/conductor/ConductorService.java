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

import com.google.common.base.Splitter;
import com.google.common.base.Strings;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import it.cnr.anac.transparency.scheduler.tasks.WorkflowCronConfig;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.val;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.stereotype.Service;

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

  @Value("${workflow.number.preserve:12}")
  Integer numberToPreserve;

  @Value("${workflow.id.preserve:null}")
  String idsToPreserveFromConfig;

  private final WorkflowCronConfig workflowCron;
  
  private final ConductorClient conductorClient;

  public List<WorkflowDto> completedWorkflows() {
    return conductorClient.allWorkflows().stream()
        .filter(w -> w.getStatus().equals("COMPLETED")).collect(Collectors.toList());
  }

  public Set<String> workflowIdsToPreserveFromConfig() {
    return Strings.isNullOrEmpty(idsToPreserveFromConfig) ? 
        Sets.newHashSet() : ImmutableSet.copyOf(Splitter.on(",").split(idsToPreserveFromConfig));
  }

  /**
   * L'insieme dei workflow id da non cancellare perché sono gli N (numberToPreserve) più recenti,
   * a cui si aggiungono quelli esplicatati come da non cancellare (idToPreserve).
   */
  Set<String> workflowIdsToPreserve(List<WorkflowDto> workflows) {
    log.info("Numero di workflow da preservare = {}", numberToPreserve);
    val notExpired = 
        workflows.stream()
        .sorted((w1, w2) -> w2.getEndTime().compareTo(w1.getEndTime()))
        .limit(numberToPreserve)
        .map(w -> w.getWorkflowId())
        .collect(Collectors.toSet());
    val toPreserveFromConfig = workflowIdsToPreserveFromConfig();
    notExpired.addAll(toPreserveFromConfig);
    return notExpired;
  };

  /**
   * Lista dei workflow completati più vecchi.
   */
  public List<WorkflowDto> expiredWorkflows() {
    val completedWorkflows = completedWorkflows();
    log.info("Presenti {} workflow completati", completedWorkflows.size());
    val workflowIdsToPreserve = workflowIdsToPreserve(completedWorkflows);
    log.info("Presenti {} workflow da preservare", workflowIdsToPreserve.size());
    val expired = completedWorkflows.stream()
        .filter(workflow -> ! workflowIdsToPreserve.contains(workflow.getWorkflowId()))
        .collect(Collectors.toList());
    log.info("Expired workflow = {}", expired);
    return expired;
  }

  /**
   * Cancella sul conductor i workflow completati più vecchi.
   */
  public List<WorkflowDto> deleteExpiredWorkflows() {
    List<WorkflowDto> deleted = Lists.newArrayList();
    expiredWorkflows().forEach(w -> {
      try {
        conductorClient.deleteWorkflow(w.getWorkflowId());
        deleted.add(w);
        log.info("Eliminato workflow id = {}", w.getWorkflowId());
      } catch (Exception e) {
        log.error("Impossibile cancellare il workflow id = {} dal conductor", w.getWorkflowId(), e);
      }
    });
    return deleted;
  }

  public void deleteWorkflow(String workflowId) {
    log.debug("Elimino il workflow con id = {}", workflowId);
    conductorClient.deleteWorkflow(workflowId);
    log.info("Eliminato workflow con id = {}", workflowId);
  }

  public String startWorkflow() {
    log.info("Executing workflow start, url = {}, body = {}", workflowCron.getUrl(), workflowCron.getBody());
    val response = conductorClient.startWorkflow(workflowCron.getBody());
    log.info("Conductor response.statusCode = {}, response.body = {}", 
        response.getStatusCode(), response.getBody());
    return response.getBody();
  }

}
