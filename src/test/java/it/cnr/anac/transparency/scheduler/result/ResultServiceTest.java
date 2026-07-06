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

import it.cnr.anac.transparency.scheduler.clients.RestPage;
import it.cnr.anac.transparency.scheduler.clients.ResultServiceClient;
import it.cnr.anac.transparency.scheduler.tasks.DeleteService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ResultServiceTest {

    @Mock
    private DeleteService deleteService;

    @Mock
    private ResultServiceClient resultServiceClient;

    @InjectMocks
    private ResultService resultService;

    // --- helpers ---

    private ResultWorkflowDto workflow(String workflowId) {
        ResultWorkflowDto dto = new ResultWorkflowDto();
        dto.setWorkflowId(workflowId);
        dto.setStatus(ResultWorkflowDto.WorkflowStatus.COMPLETED);
        return dto;
    }

    private RestPage<ResultWorkflowDto> pageOf(List<ResultWorkflowDto> workflows) {
        RestPage<ResultWorkflowDto> page = new RestPage<>();
        page.setContent(workflows);
        return page;
    }

    // --- workflowsIdsToDelete ---

    @Test
    void workflowsIdsToDelete_returnsWorkflowsNotInPreserveSet() {
        when(deleteService.workflowIdsToPreserve()).thenReturn(Set.of("wf-preserve"));
        when(resultServiceClient.list(Optional.of(ResultWorkflowDto.WorkflowStatus.COMPLETED)))
                .thenReturn(pageOf(List.of(workflow("wf-preserve"), workflow("wf-to-delete"))));

        Set<String> toDelete = resultService.workflowsIdsToDelete();

        assertThat(toDelete).containsExactly("wf-to-delete");
        assertThat(toDelete).doesNotContain("wf-preserve");
    }

    @Test
    void workflowsIdsToDelete_emptyWhenAllWorkflowsArePreserved() {
        when(deleteService.workflowIdsToPreserve()).thenReturn(Set.of("wf-1", "wf-2"));
        when(resultServiceClient.list(Optional.of(ResultWorkflowDto.WorkflowStatus.COMPLETED)))
                .thenReturn(pageOf(List.of(workflow("wf-1"), workflow("wf-2"))));

        Set<String> toDelete = resultService.workflowsIdsToDelete();

        assertThat(toDelete).isEmpty();
    }

    @Test
    void workflowsIdsToDelete_queriesResultServiceForWorkflowList() {
        when(deleteService.workflowIdsToPreserve()).thenReturn(Set.of());
        when(resultServiceClient.list(Optional.of(ResultWorkflowDto.WorkflowStatus.COMPLETED)))
                .thenReturn(pageOf(List.of(workflow("wf-1"))));

        resultService.workflowsIdsToDelete();

        verify(resultServiceClient).list(Optional.of(ResultWorkflowDto.WorkflowStatus.COMPLETED));
    }
}
