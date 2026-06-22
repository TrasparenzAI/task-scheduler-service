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
package it.cnr.anac.transparency.scheduler.result;

import it.cnr.anac.transparency.scheduler.clients.ResultAggregatorServiceClient;
import it.cnr.anac.transparency.scheduler.clients.ResultServiceClient;
import it.cnr.anac.transparency.scheduler.conductor.ConductorService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
@RequiredArgsConstructor
@Service
public class ResultService {

    private final ResultServiceClient resultServiceClient;
    private final ResultAggregatorServiceClient resultAggregatorServiceClient;
    private final ResultAggregatorService resultServiceAggregatorService;
    private final ConductorService conductorService;

    @Value("${workflow.cron.deleteOrphans.maxDeleted:10}")
    Integer maxDeleted;

    public Set<String> workflowsIdsToDelete() {
        Set<String> idsToPreserve = conductorService.workflowIdsToPreserve();
        Set<String> resultServiceWorkflows = resultServiceClient.list(Optional.of(ResultWorkflowDto.WorkflowStatus.COMPLETED)).getContent()
                .stream().map(ResultWorkflowDto::getWorkflowId).collect(Collectors.toSet());
        return resultServiceWorkflows.stream().filter(w -> !idsToPreserve.contains(w)).collect(Collectors.toSet());
    };

    public List<ResultWorkflowDto> deleteExpiredWorkflows() {
        List<ResultWorkflowDto> deleted =
                resultServiceClient.list(ResultWorkflowDto.WorkflowStatus.COMPLETED, 0, maxDeleted, "id").getContent();
        deleted.forEach(w -> {
            resultServiceClient.deleteByWorkflow(w.getWorkflowId());
            log.info("Deleted results with workflowId = {} from result-service", w.getWorkflowId());
            //Async
            resultServiceAggregatorService.deleteByWorkflow(w.getWorkflowId());
            log.info("Deleted aggregated results with workflowId = {} from result-aggregator-service", w.getWorkflowId());
        });
        log.info("Deleted {} workflows in result service", deleted.size());
        return deleted;
    }
}
