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

import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Controller REST con metodi di utilità per la gestione dei task schedulati.
 *
 * @author Cristian Lucchesi
 */
@RefreshScope
@Slf4j
@RequiredArgsConstructor
@RequestMapping("/tasks")
@RestController
public class TaskInfoController {

  private final WorkflowCronConfig workflowCronConfig;

  @GetMapping("/workflowCronConfig")
  public ResponseEntity<WorkflowCronConfig> workflowCronConfig() {
    log.info("workflowCronConfig = {}", workflowCronConfig);
    return ResponseEntity.ok(workflowCronConfig);
  }

}