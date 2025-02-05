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

import feign.RequestInterceptor;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.http.HttpHeaders;

/*
 * Vedi https://amithkumarg.medium.com/spring-cloud-openfeign-oauth-2-0-68f75f06836f
 */
@RequiredArgsConstructor(onConstructor_ = @Autowired)
public class OidcAuthZConfiguration {

  private final OAuth2Provider oauth2Provider;
  private final String AUTHZ_SERVER_NAME = "oidc";

  @Bean
  public RequestInterceptor barAuthZInterceptor() {
    return (requestTemplate) ->
        requestTemplate.header(
            HttpHeaders.AUTHORIZATION, oauth2Provider.getAuthenticationToken(AUTHZ_SERVER_NAME));
  }
}