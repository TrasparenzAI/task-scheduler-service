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

import it.cnr.anac.transparency.scheduler.security.OidcAuthZConfiguration;
import java.util.List;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

/**
 * Client feign per effettuare le operazioni con il conductor.
 *
 * @author Cristian Lucchesi
 */
@FeignClient(name = "conductor-client", url = "${workflow.cron.url}", 
            configuration = OidcAuthZConfiguration.class)
public interface ConductorClient {

  @GetMapping("/crawler_amministrazione_trasparente/correlated/crawler_amministrazione_trasparente?includeClosed=true&includeTasks=false")
  List<WorkflowDto> allWorkflows();

  @DeleteMapping("/{id}/remove?archiveWorkflow=false")
  void deleteWorkflow(@PathVariable("id") String id);

  @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE)
  ResponseEntity<String> startWorkflow(@RequestBody String body);

}