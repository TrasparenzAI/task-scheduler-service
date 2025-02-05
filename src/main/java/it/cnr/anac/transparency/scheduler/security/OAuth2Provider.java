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
package it.cnr.anac.transparency.scheduler.security;

import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.oauth2.client.OAuth2AuthorizeRequest;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientManager;
import org.springframework.stereotype.Service;

/*
 * Vedi https://amithkumarg.medium.com/spring-cloud-openfeign-oauth-2-0-68f75f06836f
 */
@RequiredArgsConstructor
@Service
public class OAuth2Provider {

  // Using anonymous user principal as its S2S authentication
  public static final Authentication ANONYMOUS_USER_AUTHENTICATION =
      new AnonymousAuthenticationToken(
          "key", "anonymous", AuthorityUtils.createAuthorityList("ROLE_ANONYMOUS"));

  private final OAuth2AuthorizedClientManager authorizedClientManager;

  public String getAuthenticationToken(final String authZServerName) {
    final OAuth2AuthorizeRequest request =
        OAuth2AuthorizeRequest.withClientRegistrationId(authZServerName)
            .principal(ANONYMOUS_USER_AUTHENTICATION)
            .build();
    return "Bearer " + authorizedClientManager.authorize(request).getAccessToken().getTokenValue();
  }
}
