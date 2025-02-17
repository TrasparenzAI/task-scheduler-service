package it.cnr.anac.transparency.scheduler.clients;

import com.google.common.base.Strings;
import feign.RequestInterceptor;
import feign.RequestTemplate;
import it.cnr.anac.transparency.scheduler.security.OAuth2Provider;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class FeignOAuthInterceptor implements RequestInterceptor {

  private final OAuth2Provider oAuth2Provider;
  private final String AUTHZ_SERVER_NAME = "oidc";

  @Override
  public void apply(RequestTemplate requestTemplate) {

    final String token = oAuth2Provider.getAuthenticationToken(AUTHZ_SERVER_NAME);
    if (! Strings.isNullOrEmpty(token)) {
      log.info("Ottenuto token client_credentials grant: {}", token);
      requestTemplate.header(HttpHeaders.AUTHORIZATION, String.format("Bearer %s", token));
    } else {
      log.error("Impossibile determinare il token client-credentials-grant");
    }
  }
}