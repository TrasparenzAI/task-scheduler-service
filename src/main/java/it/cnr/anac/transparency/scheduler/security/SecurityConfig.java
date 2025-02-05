package it.cnr.anac.transparency.scheduler.security;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;

@Slf4j
@EnableWebSecurity
@EnableMethodSecurity
@Configuration
public class SecurityConfig {
  @Bean
  public SecurityFilterChain configure(HttpSecurity http) throws Exception {
    log.info("Enabling security config");
    return http
        .csrf(AbstractHttpConfigurer::disable)
        .sessionManagement( config -> config.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
        .authorizeHttpRequests(authz -> authz
            .requestMatchers(HttpMethod.OPTIONS).permitAll()
            .requestMatchers(HttpMethod.GET, "/actuator/*").permitAll()
            .requestMatchers(HttpMethod.GET,"/v3/api-docs/**","/swagger-ui/**").permitAll()
            //XXX: da configurare chi può accedere al servizio
            .anyRequest().permitAll()
            )
        .build();
  }
}
