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
package it.cnr.anac.transparency.scheduler.scans;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import it.cnr.anac.transparency.scheduler.conductor.ConductorService;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

/**
 * Verifica delle regole di accesso per l'avvio della scansione di una amministrazione.
 *
 * @author Cristian Lucchesi
 */
@SpringBootTest
@AutoConfigureMockMvc
class ScanControllerTest {

  private static final String CODICE_IPA = "c_h501";

  @Autowired
  MockMvc mockMvc;

  @MockitoBean
  ConductorService conductorService;

  private RequestPostProcessor rpct(Object codiciIpa) {
    return jwt()
        .jwt(jwt -> {
          jwt.subject("rpct-user");
          if (codiciIpa != null) {
            jwt.claim("rpct_ipas", codiciIpa);
          }
        })
        .authorities(new SimpleGrantedAuthority("ROLE_RPCT"));
  }

  @Test
  void anonymousIsUnauthorized() throws Exception {
    mockMvc.perform(post("/v1/scans/" + CODICE_IPA)).andExpect(status().isUnauthorized());
    verify(conductorService, never()).startWorkflow(anyString());
  }

  @Test
  void userWithoutRpctRoleIsForbidden() throws Exception {
    mockMvc.perform(post("/v1/scans/" + CODICE_IPA)
            .with(jwt()
                .jwt(jwt -> jwt.claim("rpct_ipas", List.of(CODICE_IPA)))
                .authorities(new SimpleGrantedAuthority("ROLE_USER"))))
        .andExpect(status().isForbidden());
    verify(conductorService, never()).startWorkflow(anyString());
  }

  @Test
  void rpctOfTheCompanyCanStartScan() throws Exception {
    when(conductorService.startWorkflow(CODICE_IPA)).thenReturn("workflow-id");
    mockMvc.perform(post("/v1/scans/" + CODICE_IPA).with(rpct(List.of("m_it", CODICE_IPA))))
        .andExpect(status().isOk())
        .andExpect(content().string("workflow-id"));
    verify(conductorService).startWorkflow(CODICE_IPA);
  }

  @Test
  void rpctWithSingleValueClaimCanStartScan() throws Exception {
    when(conductorService.startWorkflow(CODICE_IPA)).thenReturn("workflow-id");
    mockMvc.perform(post("/v1/scans/" + CODICE_IPA).with(rpct(CODICE_IPA)))
        .andExpect(status().isOk());
  }

  @Test
  void rpctOfAnotherCompanyIsForbidden() throws Exception {
    mockMvc.perform(post("/v1/scans/" + CODICE_IPA).with(rpct(List.of("m_it"))))
        .andExpect(status().isForbidden());
    mockMvc.perform(post("/v1/scans/" + CODICE_IPA).with(rpct(null)))
        .andExpect(status().isForbidden());
    verify(conductorService, never()).startWorkflow(anyString());
  }

  @Test
  void adminCanStartScanOfAnyCompany() throws Exception {
    when(conductorService.startWorkflow(CODICE_IPA)).thenReturn("workflow-id");
    mockMvc.perform(post("/v1/scans/" + CODICE_IPA)
            .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_ADMIN"))))
        .andExpect(status().isOk());
  }

  @Test
  void invalidCodiceIpaIsBadRequest() throws Exception {
    mockMvc.perform(post("/v1/scans/{codiceIpa}", "c h501;x").with(rpct(List.of("c h501;x"))))
        .andExpect(status().isBadRequest());
    verify(conductorService, never()).startWorkflow(anyString());
  }

  @Test
  void scanAlreadyRunningIsConflict() throws Exception {
    when(conductorService.isWorkflowRunning(CODICE_IPA)).thenReturn(true);
    mockMvc.perform(post("/v1/scans/" + CODICE_IPA).with(rpct(List.of(CODICE_IPA))))
        .andExpect(status().isConflict());
    verify(conductorService, never()).startWorkflow(anyString());
  }

  @Test
  void rpctCannotUseTasksEndpoints() throws Exception {
    mockMvc.perform(get("/tasks/workflowCronConfig").with(rpct(List.of(CODICE_IPA))))
        .andExpect(status().isForbidden());
    mockMvc.perform(post("/tasks/startWorkflow").with(rpct(List.of(CODICE_IPA))))
        .andExpect(status().isForbidden());
    verify(conductorService, never()).startWorkflow();
  }
}
