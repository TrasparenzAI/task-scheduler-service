/*
 * Copyright (C) 2024 Consiglio Nazionale delle Ricerche
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
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.google.common.base.Splitter;
import com.google.common.collect.Lists;
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
public class ConductorService {

  @Value("${workflow.number.preserve:12}")
  Integer numberToPreserve;

  @Value("${workflow.id.preserve:null}")
  String idToPreserve;

  private final ConductorClient conductorClient;
  
  public List<WorkflowDto> completedWorkflows() {
    return conductorClient.allWorkflows().stream()
        .filter(w -> w.getStatus().equals("COMPLETED")).collect(Collectors.toList());
  }

  /**
   * L'insieme dei workflow id da non cancellare perché sono gli N (numberToPreserve) più recenti,
   * a cui si aggiungono quelli esplicatati come da non cancellare (idToPreserve).
   */
  private Set<String> workflowIdsToPreserve(List<WorkflowDto> workflows) {
    val notExpired = 
        workflows.stream()
        .sorted((w1, w2) -> w2.getEndTime().compareTo(w1.getEndTime()))
        .limit(numberToPreserve)
        .map(w -> w.getWorkflowId())
        .collect(Collectors.toSet());
    if (idToPreserve != null) {
      Splitter.on(",").split(idToPreserve).forEach(id -> notExpired.add(id));
    }
    return notExpired;
  };

  /**
   * Lista dei workflow completati più vecchi.
   */
  public List<WorkflowDto> expiredWorkflows() {
    val completedWorkflows = completedWorkflows();
    val workflowIdsToPreserve = workflowIdsToPreserve(completedWorkflows);
    return completedWorkflows.stream()
        .filter(workflow -> ! workflowIdsToPreserve.contains(workflow.getWorkflowId()))
        .collect(Collectors.toList());
  }

  /**
   * Cancella sul conductor i workflow completati più vecchi.
   */
  public List<WorkflowDto> deleteExpiredWorkflows() {
    List<WorkflowDto> toDelete = Lists.newArrayList();
    expiredWorkflows().forEach(w -> {
      try {
        conductorClient.deleteWorkflow(w.getWorkflowId());
        toDelete.add(w);
        log.info("Eliminato workflow id = {}", w.getWorkflowId());
      } catch (Exception e) {
        log.error("Impossibile cancellare il workflow id = {}", w.getWorkflowId(), e);
      }
    });
    return toDelete;
  }
}