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

import it.cnr.anac.transparency.scheduler.clients.RestPage;
import it.cnr.anac.transparency.scheduler.clients.ResultServiceClient;
import it.cnr.anac.transparency.scheduler.conductor.ConductorService;
import it.cnr.anac.transparency.scheduler.conductor.WorkflowDto;
import it.cnr.anac.transparency.scheduler.result.ResultWorkflowDto;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DeleteServiceTest {

    @Mock
    private ConductorService conductorService;

    @Mock
    private ResultServiceClient resultServiceClient;

    @InjectMocks
    private DeleteService deleteService;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(deleteService, "numberToPreserve", 2);
        ReflectionTestUtils.setField(deleteService, "idsToPreserveFromConfig", null);
    }

    // --- helpers ---

    private ResultWorkflowDto resultWorkflow(String workflowId, LocalDateTime endTime) {
        ResultWorkflowDto dto = new ResultWorkflowDto();
        dto.setWorkflowId(workflowId);
        dto.setStatus(ResultWorkflowDto.WorkflowStatus.COMPLETED);
        dto.setEndTime(endTime);
        return dto;
    }

    private RestPage<ResultWorkflowDto> pageOf(List<ResultWorkflowDto> workflows) {
        RestPage<ResultWorkflowDto> page = new RestPage<>();
        page.setContent(workflows);
        return page;
    }

    private WorkflowDto conductorWorkflow(String workflowId) {
        WorkflowDto dto = new WorkflowDto();
        dto.setWorkflowId(workflowId);
        dto.setStatus("COMPLETED");
        return dto;
    }

    // --- workflowIdsToPreserve ---

    @Test
    void workflowIdsToPreserve_usesResultServiceAsSource() {
        var w1 = resultWorkflow("wf-1", LocalDateTime.now().minusDays(2));
        var w2 = resultWorkflow("wf-2", LocalDateTime.now().minusDays(1));
        when(resultServiceClient.list(Optional.of(ResultWorkflowDto.WorkflowStatus.COMPLETED)))
                .thenReturn(pageOf(List.of(w1, w2)));

        deleteService.workflowIdsToPreserve();

        verify(resultServiceClient).list(Optional.of(ResultWorkflowDto.WorkflowStatus.COMPLETED));
        verifyNoInteractions(conductorService);
    }

    @Test
    void workflowIdsToPreserve_keepsNMostRecentByEndTime() {
        var oldest  = resultWorkflow("wf-oldest",  LocalDateTime.now().minusDays(10));
        var middle  = resultWorkflow("wf-middle",  LocalDateTime.now().minusDays(5));
        var newest  = resultWorkflow("wf-newest",  LocalDateTime.now().minusDays(1));
        when(resultServiceClient.list(Optional.of(ResultWorkflowDto.WorkflowStatus.COMPLETED)))
                .thenReturn(pageOf(List.of(oldest, middle, newest)));

        Set<String> toPreserve = deleteService.workflowIdsToPreserve();

        // numberToPreserve = 2: only the 2 most recent must be kept
        assertThat(toPreserve).containsExactlyInAnyOrder("wf-newest", "wf-middle");
        assertThat(toPreserve).doesNotContain("wf-oldest");
    }

    @Test
    void workflowIdsToPreserve_alwaysPreservesConfigIds() {
        ReflectionTestUtils.setField(deleteService, "idsToPreserveFromConfig", "wf-config-1,wf-config-2");
        var w1 = resultWorkflow("wf-1", LocalDateTime.now().minusDays(2));
        var w2 = resultWorkflow("wf-2", LocalDateTime.now().minusDays(1));
        when(resultServiceClient.list(Optional.of(ResultWorkflowDto.WorkflowStatus.COMPLETED)))
                .thenReturn(pageOf(List.of(w1, w2)));

        Set<String> toPreserve = deleteService.workflowIdsToPreserve();

        assertThat(toPreserve).contains("wf-config-1", "wf-config-2");
    }

    @Test
    void workflowIdsToPreserve_configIdPreservedEvenIfOldestInResultService() {
        ReflectionTestUtils.setField(deleteService, "idsToPreserveFromConfig", "wf-oldest");
        var oldest  = resultWorkflow("wf-oldest",  LocalDateTime.now().minusDays(10));
        var middle  = resultWorkflow("wf-middle",  LocalDateTime.now().minusDays(5));
        var newest  = resultWorkflow("wf-newest",  LocalDateTime.now().minusDays(1));
        when(resultServiceClient.list(Optional.of(ResultWorkflowDto.WorkflowStatus.COMPLETED)))
                .thenReturn(pageOf(List.of(oldest, middle, newest)));

        Set<String> toPreserve = deleteService.workflowIdsToPreserve();

        // wf-oldest is in config → must be preserved despite being the oldest
        assertThat(toPreserve).contains("wf-oldest", "wf-newest", "wf-middle");
    }

    // --- expiredWorkflows ---

    @Test
    void expiredWorkflows_returnsWorkflowsNotInPreserveSet() {
        var oldest  = resultWorkflow("wf-oldest",  LocalDateTime.now().minusDays(10));
        var middle  = resultWorkflow("wf-middle",  LocalDateTime.now().minusDays(5));
        var newest  = resultWorkflow("wf-newest",  LocalDateTime.now().minusDays(1));
        when(resultServiceClient.list(Optional.of(ResultWorkflowDto.WorkflowStatus.COMPLETED)))
                .thenReturn(pageOf(List.of(oldest, middle, newest)));

        List<String> expired = deleteService.expiredWorkflows();

        // numberToPreserve = 2 → only wf-oldest is expired
        assertThat(expired).containsExactly("wf-oldest");
    }

    @Test
    void expiredWorkflows_noneExpiredWhenCountWithinPreserveLimit() {
        var w1 = resultWorkflow("wf-1", LocalDateTime.now().minusDays(2));
        var w2 = resultWorkflow("wf-2", LocalDateTime.now().minusDays(1));
        when(resultServiceClient.list(Optional.of(ResultWorkflowDto.WorkflowStatus.COMPLETED)))
                .thenReturn(pageOf(List.of(w1, w2)));

        List<String> expired = deleteService.expiredWorkflows();

        assertThat(expired).isEmpty();
    }

    @Test
    void expiredWorkflows_configIdNotExpiredEvenIfOldest() {
        ReflectionTestUtils.setField(deleteService, "idsToPreserveFromConfig", "wf-oldest");
        var oldest  = resultWorkflow("wf-oldest",  LocalDateTime.now().minusDays(10));
        var middle  = resultWorkflow("wf-middle",  LocalDateTime.now().minusDays(5));
        var newest  = resultWorkflow("wf-newest",  LocalDateTime.now().minusDays(1));
        when(resultServiceClient.list(Optional.of(ResultWorkflowDto.WorkflowStatus.COMPLETED)))
                .thenReturn(pageOf(List.of(oldest, middle, newest)));

        List<String> expired = deleteService.expiredWorkflows();

        assertThat(expired).doesNotContain("wf-oldest");
    }

    // --- deleteExpiredWorkflowsOnConductor ---

    @Test
    void deleteExpiredWorkflowsOnConductor_callsDeleteOnlyForExpiredWorkflows() {
        deleteService.deleteExpiredWorkflowsOnConductor(List.of("wf-oldest"), 1);

        verify(conductorService).deleteWorkflow("wf-oldest");
        verifyNoMoreInteractions(conductorService);
    }

    // --- conductorOnlyWorkflows ---

    @Test
    void conductorOnlyWorkflows_returnsWorkflowsPresentOnConductorButNotInResultService() {
        when(conductorService.completedWorkflowsOnConductor())
                .thenReturn(List.of(
                        conductorWorkflow("wf-1"),
                        conductorWorkflow("wf-2"),
                        conductorWorkflow("wf-3")));
        // result-service ha wf-1 e wf-2, manca wf-3
        when(resultServiceClient.list(Optional.of(ResultWorkflowDto.WorkflowStatus.COMPLETED)))
                .thenReturn(pageOf(List.of(resultWorkflow("wf-1", LocalDateTime.now()),
                        resultWorkflow("wf-2", LocalDateTime.now()))));

        List<String> conductorOnly = deleteService.conductorOnlyWorkflows();

        assertThat(conductorOnly).containsExactly("wf-3");
    }

    @Test
    void conductorOnlyWorkflows_emptyWhenAllConductorWorkflowsAreInResultService() {
        when(conductorService.completedWorkflowsOnConductor())
                .thenReturn(List.of(conductorWorkflow("wf-1")));
        when(resultServiceClient.list(Optional.of(ResultWorkflowDto.WorkflowStatus.COMPLETED)))
                .thenReturn(pageOf(List.of(resultWorkflow("wf-1", LocalDateTime.now()))));

        List<String> conductorOnly = deleteService.conductorOnlyWorkflows();

        assertThat(conductorOnly).isEmpty();
    }

    @Test
    void conductorOnlyWorkflows_allOrphansWhenResultServiceIsEmpty() {
        when(conductorService.completedWorkflowsOnConductor())
                .thenReturn(List.of(conductorWorkflow("wf-1"), conductorWorkflow("wf-2")));
        when(resultServiceClient.list(Optional.of(ResultWorkflowDto.WorkflowStatus.COMPLETED)))
                .thenReturn(pageOf(List.of()));

        List<String> conductorOnly = deleteService.conductorOnlyWorkflows();

        assertThat(conductorOnly).containsExactlyInAnyOrder("wf-1", "wf-2");
    }

    // --- deleteConductorOnlyWorkflows ---

    @Test
    void deleteConductorOnlyWorkflows_callsDeleteOnlyForOrphanWorkflows() {
        deleteService.deleteConductorOnlyWorkflows(List.of("wf-orphan"));

        verify(conductorService).deleteWorkflow("wf-orphan");
        verifyNoMoreInteractions(conductorService);
    }
}
