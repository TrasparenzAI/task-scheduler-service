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

import it.cnr.anac.transparency.scheduler.clients.ResultServiceClient;
import it.cnr.anac.transparency.scheduler.tasks.DeleteService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@RequiredArgsConstructor
@Service
public class ResultService {

    private final DeleteService deleteService;
    private final ResultServiceClient resultServiceClient;

    public Set<String> workflowsIdsToDelete() {
        Set<String> idsToPreserve = deleteService.workflowIdsToPreserve();
        Set<String> resultServiceWorkflows = resultServiceClient.list(Optional.of(ResultWorkflowDto.WorkflowStatus.COMPLETED)).getContent()
                .stream().map(ResultWorkflowDto::getWorkflowId).collect(Collectors.toSet());
        return resultServiceWorkflows.stream().filter(w -> !idsToPreserve.contains(w)).collect(Collectors.toSet());
    }
}
