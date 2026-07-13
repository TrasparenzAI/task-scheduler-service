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
import com.google.common.base.Splitter;
import com.google.common.base.Strings;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Sets;
import it.cnr.anac.transparency.scheduler.clients.ResultServiceClient;
import it.cnr.anac.transparency.scheduler.conductor.ConductorService;
import it.cnr.anac.transparency.scheduler.conductor.WorkflowDto;
import it.cnr.anac.transparency.scheduler.result.ResultAggregatorService;
import it.cnr.anac.transparency.scheduler.result.ResultWorkflowDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
@RequiredArgsConstructor
@Service
public class DeleteService {

    @Value("${workflow.number.preserve:12}")
    Integer numberToPreserve;

    @Value("${workflow.id.preserve:null}")
    String idsToPreserveFromConfig;

    private final ConductorService conductorService;
    private final ResultServiceClient resultServiceClient;
    private final ResultAggregatorService resultServiceAggregatorService;

    public Set<String> workflowIdsToPreserveFromConfig() {
        return Strings.isNullOrEmpty(idsToPreserveFromConfig) ?
                Sets.newHashSet() : ImmutableSet.copyOf(Splitter.on(",").split(idsToPreserveFromConfig));
    }

    /**
     * Lista dei workflow generali (senza codiceIpa) completati presenti nel result-service.
     */
    public List<ResultWorkflowDto> completedGeneralWorkflowsOnResultService() {
        return resultServiceClient.list(Optional.of(ResultWorkflowDto.WorkflowStatus.COMPLETED)).getContent();
    }

    /**
     * L'insieme dei workflow id da non cancellare perché sono gli N (numberToPreserve) più recenti,
     * a cui si aggiungono quelli esplicatati come da non cancellare (idToPreserve).
     * Usa i workflow generali (senza codiceIpa) del result-service come sorgente.
     */
    public Set<String> workflowIdsToPreserve(List<ResultWorkflowDto> workflows) {
        log.info("Numero di workflow da preservare = {}", numberToPreserve);
        val notExpired =
                workflows.stream()
                        .sorted((w1, w2) -> w2.getEndTime().compareTo(w1.getEndTime()))
                        .limit(numberToPreserve)
                        .map(ResultWorkflowDto::getWorkflowId)
                        .collect(Collectors.toSet());
        val toPreserveFromConfig = workflowIdsToPreserveFromConfig();
        notExpired.addAll(toPreserveFromConfig);
        return notExpired;
    }

    /**
     * L'insieme dei workflow id da non cancellare.
     */
    public Set<String> workflowIdsToPreserve() {
        val completedGeneralWorkflows = completedGeneralWorkflowsOnResultService();
        return workflowIdsToPreserve(completedGeneralWorkflows);
    }

    /**
     * Lista dei workflow completati più vecchi da cancellare.
     * La sorgente sono i workflow generali (senza codiceIpa) presenti nel result-service.
     */
    public List<String> expiredWorkflows() {
        val completedGeneralWorkflows = completedGeneralWorkflowsOnResultService();
        log.info("Presenti {} workflow completati generali nel result-service", completedGeneralWorkflows.size());
        val workflowIdsToPreserve = workflowIdsToPreserve(completedGeneralWorkflows);
        log.info("Presenti {} workflow da preservare", workflowIdsToPreserve.size());
        val expired = completedGeneralWorkflows.stream()
                .map(ResultWorkflowDto::getWorkflowId)
                .filter(workflowId -> !workflowIdsToPreserve.contains(workflowId))
                .collect(Collectors.toList());
        log.info("Expired workflow = {}", expired);
        return expired;
    }

    /**
     * Cancella sul conductor i workflow completati più vecchi.
     */
    @Async
    public void deleteExpiredWorkflowsOnConductor(List<String> expiredWorkflowIds, Integer deleteMax) {
        log.info("{} expired workflows to be delete from Conductor. Trying to delete just {} workflows",
                expiredWorkflowIds.size(), deleteMax);
        val conductorWorkflowIds =
                conductorService.completedWorkflowsOnConductor().stream()
                        .map(WorkflowDto::getWorkflowId).collect(Collectors.toSet());
        expiredWorkflowIds.stream().filter(conductorWorkflowIds::contains).
                limit(deleteMax).forEach(conductorService::deleteWorkflow);
    }

    @Async
    public void deleteExpiredWorkflowsOnResultService(List<String> expiredWorkflowIds, Integer deleteMax) {
        expiredWorkflowIds.stream().limit(deleteMax).forEach(workflowId -> {
            try {
                resultServiceClient.deleteByWorkflow(workflowId);
                log.info("Deleted results with workflowId = {} from result-service", workflowId);
            } catch (Exception e) {
                log.warn("Errore nella cancellazione dei risultati dal result-service per workflowId = {}", workflowId, e);
            }
            try {
                resultServiceAggregatorService.deleteByWorkflow(workflowId);
                log.info("Deleted aggregated results with workflowId = {} from result-aggregator-service", workflowId);
            } catch (Exception e) {
                log.warn("Errore nella cancellazione dei risultati aggregati dal result-aggregator-service per workflowId = {}", workflowId, e);
            }
        });
    }

    /**
     * Lista dei workflow completati nel Conductor non presenti nel result-service.
     * Questi sono orfani lato Conductor che non hanno mai prodotto risultati.
     */
    public List<String> conductorOnlyWorkflows() {
        val conductorWorkflows = conductorService.completedWorkflowsOnConductor();
        log.info("Presenti {} workflow completati nel Conductor", conductorWorkflows.size());
        val workflowIdsToPreserve = workflowIdsToPreserve();
        val conductorOnly = conductorWorkflows.stream()
                .map(WorkflowDto::getWorkflowId)
                .filter(workflowId -> !workflowIdsToPreserve.contains(workflowId))
                .collect(Collectors.toList());
        log.info("Workflow orfani nel Conductor (non presenti nel result-service) = {}", conductorOnly);
        return conductorOnly;
    }

    /**
     * Cancella dal Conductor i workflow completati non presenti nel result-service.
     */
    @Async
    public void deleteConductorOnlyWorkflows(List<String> conductorOnlyWorkflowIds) {
        conductorOnlyWorkflowIds.forEach(conductorService::deleteWorkflow);
        log.info("Cancellato dal conductor workflow con id = {}",
                Joiner.on(",").join(conductorOnlyWorkflowIds));
    }
}
