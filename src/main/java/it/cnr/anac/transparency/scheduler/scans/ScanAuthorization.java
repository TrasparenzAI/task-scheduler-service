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

import it.cnr.anac.transparency.scheduler.security.SecurityConfig;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Component;

/**
 * Verifica se un utente può avviare la scansione di una amministrazione.
 *
 * @author Cristian Lucchesi
 */
@Slf4j
@RequiredArgsConstructor
@Component
public class ScanAuthorization {

  private final RpctProperties rpctProperties;

  /**
   * Gli amministratori possono avviare la scansione di qualsiasi amministrazione, gli RPCT
   * solo di quelle presenti nel claim del JWT configurato in security.rpct.ipa-claim.
   */
  public boolean canScan(Authentication authentication, String codiceIpa) {
    if (hasAnyRole(authentication, SecurityConfig.ADMIN_ROLES)) {
      return true;
    }
    if (!hasAnyRole(authentication, List.of(rpctProperties.getRoleName()))
        || !(authentication.getPrincipal() instanceof Jwt jwt)) {
      return false;
    }
    List<String> codiciIpa = jwt.getClaimAsStringList(rpctProperties.getIpaClaim());
    boolean allowed = codiciIpa != null && codiciIpa.contains(codiceIpa);
    if (!allowed) {
      log.warn("Utente {} non autorizzato alla scansione del codice IPA {}, codici IPA autorizzati = {}",
          jwt.getSubject(), codiceIpa, codiciIpa);
    }
    return allowed;
  }

  private boolean hasAnyRole(Authentication authentication, List<String> roles) {
    return authentication.getAuthorities().stream()
        .map(GrantedAuthority::getAuthority)
        .anyMatch(authority -> roles.stream().anyMatch(role -> authority.equals("ROLE_" + role)));
  }
}
