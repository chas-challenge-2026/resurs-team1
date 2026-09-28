package se.comerit.resurs.security;


import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.HttpStatusEntryPoint;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;
import org.springframework.security.web.csrf.CsrfTokenRequestAttributeHandler;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

   @Bean
    SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
       return http
               .csrf(csrf -> csrf
                       .csrfTokenRepository(CookieCsrfTokenRepository.withHttpOnlyFalse())
                       .csrfTokenRequestHandler(csrfTokenRequestHandler()))

               .authorizeHttpRequests(auth -> auth
                       .requestMatchers("/api/auth/login/**", "/api/auth/logout").permitAll()
                       .requestMatchers("/error").permitAll()
                       .requestMatchers("/v3/api-docs/**", "/swagger-ui/**", "/swagger-ui.html").permitAll()

                       .requestMatchers("/api/backoffice/**").hasRole("CASE_WORKER")
                       .requestMatchers("/api/caseworkers/**").hasRole("CASE_WORKER")
                       .requestMatchers("/api/application/company").hasRole("CASE_WORKER")
                       .requestMatchers(HttpMethod.GET, "/api/application/{id:[0-9]+}").hasAnyRole("COMPANY", "CASE_WORKER")
                       .requestMatchers("/api/application/**").hasRole("COMPANY")

                       .requestMatchers("/api/documents/**").authenticated()
                       .requestMatchers("/api/status/**").authenticated()

                       .anyRequest().authenticated())

               .exceptionHandling(ex -> ex
                       .authenticationEntryPoint(
                               new HttpStatusEntryPoint(HttpStatus.UNAUTHORIZED)))

               .logout(logout -> logout
                       .logoutUrl("/api/auth/logout")
                       .logoutSuccessHandler((req, res, auth) ->
                               res.setStatus(HttpStatus.NO_CONTENT.value()))
                       .invalidateHttpSession(true)
                       .deleteCookies("JSESSIONID"))

               .httpBasic(AbstractHttpConfigurer::disable)
               .formLogin(AbstractHttpConfigurer::disable)
               .build();
   }

    @Bean
    PasswordEncoder passwordEncoder(){
       return new BCryptPasswordEncoder();
    }

    private CsrfTokenRequestAttributeHandler csrfTokenRequestHandler() {
        CsrfTokenRequestAttributeHandler handler = new CsrfTokenRequestAttributeHandler();
        handler.setCsrfRequestAttributeName(null);
        return handler;
    }
}
