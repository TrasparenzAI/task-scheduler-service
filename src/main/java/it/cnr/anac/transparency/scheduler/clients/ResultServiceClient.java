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

import it.cnr.anac.transparency.scheduler.result.ResultWorkflowDto;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.Optional;

/**
 * Client feign per effettuare le operazioni con il result-service.
 *
 * @author Cristian Lucchesi
 */
@FeignClient(name = "result-service-client", url = "${transparency.clients.result-service.url}")
public interface ResultServiceClient {


 @GetMapping("/v1/workflows")
 RestPage<ResultWorkflowDto> list(
      @RequestParam(value = "status", required = false) ResultWorkflowDto.WorkflowStatus status,
      @RequestParam("page") int page,
      @RequestParam("size") int size,
      @RequestParam("sort") String sort);
  default RestPage<ResultWorkflowDto> list(
      Optional<ResultWorkflowDto.WorkflowStatus> status) {
    return list(status.orElse(null), 0, 1000, "id");
  }

  @DeleteMapping("/v1/results/byWorkflow/{id}")
  long deleteByWorkflow(@PathVariable("id") String id);
}
