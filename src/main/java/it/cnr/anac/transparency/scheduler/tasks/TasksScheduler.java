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
package it.cnr.anac.transparency.scheduler.tasks;

import com.google.common.base.Joiner;
import it.cnr.anac.transparency.scheduler.clients.ResultServiceClient;
import it.cnr.anac.transparency.scheduler.conductor.ConductorService;
import it.cnr.anac.transparency.scheduler.result.ResultAggregatorService;
import it.cnr.anac.transparency.scheduler.result.ResultService;
import lombok.RequiredArgsConstructor;
import lombok.val;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.NonNull;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.cloud.context.scope.refresh.RefreshScopeRefreshedEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Task da eseguire in modo schedulato.
 *
 * @author Cristian Lucchesi
 */
@RefreshScope
@RequiredArgsConstructor
@Slf4j
@Component
public class TasksScheduler implements ApplicationListener<RefreshScopeRefreshedEvent>{

  private final DeleteService deleteService;
  private final ConductorService conductorService;
  private final ResultServiceClient resultServiceClient;
  private final ResultAggregatorService resultServiceAggregatorService;
  private final ResultService resultService;

  @Value(value = "${workflow.cron.deleteConductorOrphans.enabled:-false}")
  boolean deleteConductorOrphansEnabled = false;
  @Value(value = "${workflow.cron.deleteMax:-1}")
  Integer deleteMax = 1;

  @Scheduled(cron = "0 ${workflow.cron.expression}")
  void workflowStartTask() {
    conductorService.startWorkflow();
  }

  @Scheduled(cron = "0 ${workflow.cron.deleteExpression}")
  void deleteExpiredWorkflows() {
    val toDelete = deleteService.expiredWorkflows();
    deleteService.deleteExpiredWorkflowsOnConductor(toDelete, deleteMax);
    deleteService.deleteExpiredWorkflowsOnResultService(toDelete, deleteMax);
  }

  // Workflow completati nel Conductor che non hanno una corrispondenza nel result-service
  @Scheduled(cron = "0 ${workflow.cron.deleteConductorOrphans.expression}")
  void deleteConductorOrphanWorkflows() {
    val orphans = deleteService.conductorOnlyWorkflows();
    log.info("Trovati {} workflow orfani nel Conductor", orphans.size());
    log.info("Workflow da cancellare id = {}", Joiner.on(",").join(orphans));
    if (deleteConductorOrphansEnabled) {
      deleteService.deleteConductorOnlyWorkflows(orphans);
    } else {
      log.info("Cancellazione workflow orfani disabilitata");
    }
  }

  /**
   * Questo metodo è necessario per obbligare lo spring a ricreare il bean con 
   * l'annotazione @scheduled.
   */
  @Override
  public void onApplicationEvent(@NonNull RefreshScopeRefreshedEvent refreshScopeRefreshedEvent) {
    log.debug("TaskScheduler::onApplicationEvent -> new schedules created");
  }
}