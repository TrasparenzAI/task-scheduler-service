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
package it.cnr.anac.transparency.scheduler.tasks;

import it.cnr.anac.transparency.scheduler.clients.ResultAggregatorServiceClient;
import it.cnr.anac.transparency.scheduler.clients.ResultServiceClient;
import it.cnr.anac.transparency.scheduler.conductor.ConductorService;
import lombok.RequiredArgsConstructor;
import lombok.val;
import lombok.extern.slf4j.Slf4j;
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

  private final ConductorService conductorService;
  private final ResultServiceClient resultServiceClient;
  private final ResultAggregatorServiceClient resultAggregatorServiceClient;

  @Scheduled(cron = "0 ${workflow.cron.expression}")
  void workflowStartTask() {
    conductorService.startWorkflow();
  }

  @Scheduled(cron = "0 ${workflow.cron.deleteExpression}")
  void deleteExpiredWorflows() {
    val deleted = conductorService.expiredWorkflows();
    conductorService.deleteExpiredWorkflows();
    log.info("Deleted {} expired workflows from conductor", deleted.size());
    deleted.forEach(w -> {
      resultServiceClient.deleteByWorkflow(w.getWorkflowId());
      log.info("Deleted results with workflowId = {} from result-service", w.getWorkflowId());
    });
    deleted.forEach(w -> {
      resultAggregatorServiceClient.deleteByWorkflow(w.getWorkflowId());
      log.info("Deleted aggregated results with workflowId = {} from result-aggregator-service", w.getWorkflowId());
    });
  }

  
  /**
   * Questo metodo è necessario per obbligare lo spring a ricreare il bean con 
   * l'annotazione @scheduled.
   */
  @Override
  public void onApplicationEvent(RefreshScopeRefreshedEvent refreshScopeRefreshedEvent) {
    log.debug("TaskScheduler::onApplicationEvent -> new schedules created");
  }
}
