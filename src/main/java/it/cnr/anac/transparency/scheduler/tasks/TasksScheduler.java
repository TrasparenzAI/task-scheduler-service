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

  @Scheduled(cron = "${tasks.fake.cron.expression}")
  void fakeTask() {
    log.info("Fake task executed");
  }

  /**
   * Questo metodo è necessario per obbligatore lo spring a ricreare il bean con 
   * l'annotazione @scheduled.
   */
  @Override
  public void onApplicationEvent(RefreshScopeRefreshedEvent refreshScopeRefreshedEvent) {
    log.info("TaskScheduler::onApplicationEvent -> new schedules created");
  }
}
