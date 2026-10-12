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
package it.cnr.anac.transparency.scheduler.conductor;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import it.cnr.anac.transparency.scheduler.tasks.WorkflowCronConfig;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;

@ExtendWith(MockitoExtension.class)
class ConductorServiceTest {

  private static final String CRON_BODY = """
      {"name":"crawler_amministrazione_trasparente","correlationId":"crawler_amministrazione_trasparente",
       "version":1,"input":{"page_size":2000,"codice_categoria":"L6","codice_ipa":"","id_ipa_from":0,
       "parent_workflow_id":"","execute_child":true,"crawler_save_object":false,
       "crawler_save_screenshot":false,"rule_name":"amministrazione-trasparente",
       "root_rule":"AT_TO-BE_23-12-2024","crawler_child_type":"START_WORKFLOW",
       "result_base_url":"https://example.org/result-service/v1/results",
       "crawler_uri":"http://crawler.example.org:8080/crawl"}}
      """;

  @Mock
  private ConductorClient conductorClient;

  private final ObjectMapper objectMapper = new ObjectMapper();

  private ConductorService conductorService;

  @BeforeEach
  void setUp() {
    WorkflowCronConfig workflowCronConfig = new WorkflowCronConfig();
    workflowCronConfig.setBody(CRON_BODY);
    conductorService = new ConductorService(workflowCronConfig, conductorClient, objectMapper);
  }

  @Test
  void startWorkflowForSingleCompany() throws Exception {
    when(conductorClient.startWorkflow(anyString())).thenReturn(ResponseEntity.ok("workflow-id"));

    assertThat(conductorService.startWorkflow("c_h501")).isEqualTo("workflow-id");

    ArgumentCaptor<String> body = ArgumentCaptor.forClass(String.class);
    verify(conductorClient).startWorkflow(body.capture());
    JsonNode workflow = objectMapper.readTree(body.getValue());
    JsonNode input = workflow.get("input");
    assertThat(workflow.get("name").asText()).isEqualTo("crawler_amministrazione_trasparente");
    assertThat(workflow.get("correlationId").asText()).isEqualTo("c_h501");
    assertThat(input.get("codice_ipa").asText()).isEqualTo("c_h501");
    assertThat(input.get("codice_categoria").asText()).isEmpty();
    assertThat(input.get("page_size").asInt()).isEqualTo(1);
    assertThat(input.get("crawler_child_type").asText()).isEqualTo("SUB_WORKFLOW");
    assertThat(input.get("crawler_save_object").asBoolean()).isTrue();
    assertThat(input.get("crawler_save_screenshot").asBoolean()).isTrue();
    // I parametri non relativi alla singola PA sono quelli della configurazione
    assertThat(input.get("root_rule").asText()).isEqualTo("AT_TO-BE_23-12-2024");
    assertThat(input.get("crawler_uri").asText()).isEqualTo("http://crawler.example.org:8080/crawl");
    assertThat(input.get("result_base_url").asText())
        .isEqualTo("https://example.org/result-service/v1/results");
  }

  @Test
  void isWorkflowRunning() {
    when(conductorClient.runningWorkflows("c_h501")).thenReturn(java.util.List.of(new WorkflowDto()));
    when(conductorClient.runningWorkflows("m_it")).thenReturn(java.util.List.of());

    assertThat(conductorService.isWorkflowRunning("c_h501")).isTrue();
    assertThat(conductorService.isWorkflowRunning("m_it")).isFalse();
  }
}
