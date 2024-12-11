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

import lombok.RequiredArgsConstructor;
import lombok.val;
import lombok.extern.slf4j.Slf4j;
import it.cnr.anac.transparency.scheduler.conductor.ConductorService;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpResponse.BodyHandlers;

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

  private final WorkflowCronConfig workflowCron;
  private final ConductorService conductorService;

  @Scheduled(cron = "0 ${workflow.cron.expression}")
  void workflowStartTask() {

    log.info("Executing workflow start, url = {}, body = {}", workflowCron.getUrl(), workflowCron.getBody());
    val client =  HttpClient.newHttpClient();
    try {
      HttpRequest request = HttpRequest.newBuilder()
          .uri(new URI(workflowCron.getUrl()))
          .setHeader("Content-Type", "application/json")
          .POST(HttpRequest.BodyPublishers.ofString(workflowCron.getBody()))
          .build();
      try {
        HttpResponse<String> response = client.send(request, BodyHandlers.ofString());
        log.info("Conductor response.statusCode = {}, response.body = {}", 
            response.statusCode(), response.body());

      } catch (IOException | InterruptedException e) {
        log.error("Problem calling conductor", e);
      }
    } catch (URISyntaxException e) {
      log.error("URI conductor errato", e);
    }
  }

  @Scheduled(cron = "0 ${workflow.cron.deleteExpression}")
  void deleteExpiredWorflows() {
    val deleted = conductorService.deleteExpiredWorkflows();
    log.info("Deleted {} expired workflows", deleted.size());
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
