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

import com.google.common.base.Splitter;
import com.google.common.base.Strings;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Sets;
import it.cnr.anac.transparency.scheduler.conductor.ConductorService;
import it.cnr.anac.transparency.scheduler.conductor.WorkflowDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.List;
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

    public Set<String> workflowIdsToPreserveFromConfig() {
        return Strings.isNullOrEmpty(idsToPreserveFromConfig) ?
                Sets.newHashSet() : ImmutableSet.copyOf(Splitter.on(",").split(idsToPreserveFromConfig));
    }

    /**
     * L'insieme dei workflow id da non cancellare perché sono gli N (numberToPreserve) più recenti,
     * a cui si aggiungono quelli esplicatati come da non cancellare (idToPreserve).
     */
    public Set<String> workflowIdsToPreserve(List<WorkflowDto> workflows) {
        log.info("Numero di workflow da preservare = {}", numberToPreserve);
        val notExpired =
                workflows.stream()
                        .sorted((w1, w2) -> w2.getEndTime().compareTo(w1.getEndTime()))
                        .limit(numberToPreserve)
                        .map(WorkflowDto::getWorkflowId)
                        .collect(Collectors.toSet());
        val toPreserveFromConfig = workflowIdsToPreserveFromConfig();
        notExpired.addAll(toPreserveFromConfig);
        return notExpired;
    };

    /**
     * L'insieme dei workflow id da non cancellare.
     */
    public Set<String> workflowIdsToPreserve() {
        val completedWorkflows = conductorService.completedWorkflowsOnConductor();
        return workflowIdsToPreserve(completedWorkflows);
    }


    /**
     * Lista dei workflow completati più vecchi.
     */
    public List<String> expiredWorkflows() {
        val completedWorkflows = conductorService.completedWorkflowsOnConductor();
        log.info("Presenti {} workflow completati", completedWorkflows.size());
        val workflowIdsToPreserve = workflowIdsToPreserve(completedWorkflows);
        log.info("Presenti {} workflow da preservare", workflowIdsToPreserve.size());
        val expired = completedWorkflows.stream()
                .map(WorkflowDto::getWorkflowId)
                .filter(workflowId -> ! workflowIdsToPreserve.contains(workflowId))
                .collect(Collectors.toList());
        log.info("Expired workflow = {}", expired);
        return expired;
    }

    /**
     * Cancella sul conductor i workflow completati più vecchi.
     */
    @Async
    public void deleteExpiredWorkflowsOnConductor() {
        expiredWorkflows().forEach(conductorService::deleteWorkflow);
    }
}
