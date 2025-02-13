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
package it.cnr.anac.transparency.scheduler.clients;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.RequestParam;

import it.cnr.anac.transparency.scheduler.security.OidcAuthZConfiguration;

/**
 * Client feign per effettuare le operazioni con il result-aggregator-service.
 *
 * @author Cristian Lucchesi
 */
@FeignClient(name = "result-aggregator-service-client", url = "${transparency.clients.result-aggregator-service.url}", 
            configuration = OidcAuthZConfiguration.class)
public interface ResultAggregatorServiceClient {

  @DeleteMapping("/v1/aggregator/geojson")
  Long deleteByWorkflow(@RequestParam("workflowId") String id);
}
