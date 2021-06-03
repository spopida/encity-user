package uk.co.encity.user.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.method.configuration.EnableGlobalMethodSecurity;
import org.springframework.security.config.annotation.method.configuration.EnableReactiveMethodSecurity;
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.oauth2.jwt.ReactiveJwtDecoder;
import org.springframework.security.oauth2.jwt.ReactiveJwtDecoders;
import org.springframework.security.web.server.SecurityWebFilterChain;
import org.springframework.security.web.server.util.matcher.PathPatternParserServerWebExchangeMatcher;
import org.springframework.security.web.util.matcher.RegexRequestMatcher;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.reactive.CorsConfigurationSource;
import org.springframework.web.cors.reactive.UrlBasedCorsConfigurationSource;
import reactor.util.Logger;
import reactor.util.Loggers;
import java.util.Arrays;

@EnableWebFluxSecurity
@EnableReactiveMethodSecurity
public class SecurityConfig {

    /**
     * The URI of the issuer of the public key used to sign access tokens
     */
    @Value("${spring.security.oauth2.resourceserver.jwk.issuer-uri}")
    private String issuerUri;

    /**
     * The list of origins that are allowed to access this service's API
     */
    @Value("${encity.origin-list}")
    private String[] allowedOrigins;

    private Logger logger = Loggers.getLogger(getClass());

    /**
     * Does nothing except logging
     */
    public SecurityConfig() {
        logger.debug("Constructing SecurityConfig");
    }

    /**
     * Configure Spring's SecurityWebFilterChain so that this service becomes an OAuth2 Resource Server
     * with the necessary API endpoint protection in place
     * @param http the @{link ServerHttpSecurity} security configuration to be added to the filter chain
     * @return the {@link SecurityWebFilterChain} that will be added to the chain
     */
    @Bean
    public SecurityWebFilterChain configureFilterChain(ServerHttpSecurity http){

        http
                .authorizeExchange()
                .pathMatchers(HttpMethod.PATCH, "/**").permitAll()
                .pathMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                .pathMatchers(HttpMethod.GET, "/users/**").permitAll()
                //.pathMatchers(HttpMethod.GET, "/users/**").hasAuthority("SCOPE_read:user_profile")
                //.anyExchange().authenticated()
                .and()
                .csrf().disable()
                .oauth2ResourceServer()
                .jwt();

        return http.build();
    }

    /**
     * Creates a {@link ReactiveJwtDecoder} object using the values contained in a response from
     * a token issuer.  This decoder can then be used to validate access tokens in incoming requests
     * @return a {@link ReactiveJwtDecoder}
     */
    @Bean
    public ReactiveJwtDecoder jwtDecoder() {
        return ReactiveJwtDecoders.fromOidcIssuerLocation(issuerUri);
    }


    /**
     * Provides CORS-related configuration to ensure supported cross-origin requests work, and unsupported
     * ones fail
     * @return a {@link CorsConfigurationSource} object for use by Spring
     */
    @Bean
    CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration corsConfig = new CorsConfiguration();
        corsConfig.applyPermitDefaultValues();
        corsConfig.addAllowedMethod(HttpMethod.GET);
        corsConfig.addAllowedMethod(HttpMethod.OPTIONS);
        corsConfig.addAllowedMethod(HttpMethod.PATCH);
        corsConfig.setAllowedOrigins(Arrays.asList(this.allowedOrigins));

        UrlBasedCorsConfigurationSource source =
                new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", corsConfig);
        return source;
    }
}
