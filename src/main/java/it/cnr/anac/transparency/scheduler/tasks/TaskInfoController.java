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
package it.cnr.anac.transparency.scheduler.tasks;

import it.cnr.anac.transparency.scheduler.clients.ResultAggregatorServiceClient;
import it.cnr.anac.transparency.scheduler.clients.ResultServiceClient;
import it.cnr.anac.transparency.scheduler.conductor.ConductorService;
import it.cnr.anac.transparency.scheduler.conductor.WorkflowDto;
import java.util.List;
import java.util.Set;

import lombok.RequiredArgsConstructor;
import lombok.val;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.v3.oas.annotations.security.SecurityRequirement;

/**
 * Controller REST con metodi di utilità per la gestione dei task schedulati.
 *
 * @author Cristian Lucchesi
 */
@SecurityRequirement(name = "bearer_authentication")
@RefreshScope
@Slf4j
@RequiredArgsConstructor
@RequestMapping("/tasks")
@RestController
public class TaskInfoController {

  private final WorkflowCronConfig workflowCronConfig;
  private final ConductorService conductorService;
  private final ResultServiceClient resultServiceClient;
  private final ResultAggregatorServiceClient resultAggregatorServiceClient;

  @GetMapping("/workflowCronConfig")
  public ResponseEntity<WorkflowCronConfig> workflowCronConfig() {
    log.info("workflowCronConfig = {}", workflowCronConfig);
    return ResponseEntity.ok(workflowCronConfig);
  }

  @GetMapping("/workflowIdsToPreserveFromConfig")
  public ResponseEntity<Set<String>> idsToPreserve() {
    return ResponseEntity.ok(conductorService.workflowIdsToPreserveFromConfig());
  }

  @GetMapping("/completedWorkflows")
  public ResponseEntity<List<WorkflowDto>> completedWorkflows() {
    return ResponseEntity.ok(conductorService.completedWorkflows());
  }

  @GetMapping("/expiredWorkflows")
  public ResponseEntity<List<WorkflowDto>> expiredWorkflows() {
    return ResponseEntity.ok(conductorService.expiredWorkflows());
  }

  @DeleteMapping("/deleteExpiredWorkflows")
  public ResponseEntity<List<WorkflowDto>> deleteExpiredWorkflows() {
    val workflowDtos = conductorService.expiredWorkflows();
    conductorService.deleteExpiredWorkflows();
    return ResponseEntity.ok(workflowDtos);
  }

  @PostMapping("/startWorkflow")
  public ResponseEntity<String> startWorkflow() {
    return ResponseEntity.ok(conductorService.startWorkflow());
  }

  @DeleteMapping("/deleteWorkflow")
  public ResponseEntity<Void> deleteWorkflow(@RequestParam("workflowId") String workflowId) {
    conductorService.deleteWorkflow(workflowId);
    val resultDeleted = resultServiceClient.deleteByWorkflow(workflowId);
    log.info("Eliminati {} risultati dal result service per workflowId = {}", resultDeleted, workflowId);
    val resultAggregatedDeleted = resultAggregatorServiceClient.deleteByWorkflow(workflowId);
    log.info("Eliminati {} risultati dal result service aggregator per workflowId = {}", resultAggregatedDeleted, workflowId);
    return ResponseEntity.ok().build();
  }

}