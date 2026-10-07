package com.phivegarage;
import org.springframework.context.annotation.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.core.userdetails.*;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.config.http.SessionCreationPolicy;
@Configuration
public class Security {
 @Bean UserDetailsService users(@Value("${app.username}") String name,@Value("${app.password}") String password) {
  if(password.length()<12) throw new IllegalArgumentException("APP_PASSWORD harus minimal 12 karakter");
  return new InMemoryUserDetailsManager(User.withUsername(name).password(new BCryptPasswordEncoder().encode(password)).roles("OWNER").build());
 }
 @Bean BCryptPasswordEncoder encoder(){return new BCryptPasswordEncoder();}
 @Bean SecurityFilterChain filter(HttpSecurity http) throws Exception {
  // Requests require a custom header; browsers cannot submit it cross-origin without CORS approval.
  return http.csrf(c->c.disable()).sessionManagement(s->s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
   .authorizeHttpRequests(a->a.requestMatchers("/api/health").permitAll().anyRequest().authenticated())
   .addFilterBefore(new org.springframework.web.filter.OncePerRequestFilter(){
    @Override protected void doFilterInternal(jakarta.servlet.http.HttpServletRequest request,jakarta.servlet.http.HttpServletResponse response,jakarta.servlet.FilterChain chain) throws java.io.IOException,jakarta.servlet.ServletException {
     if(!request.getRequestURI().equals("/api/health") && !"web".equals(request.getHeader("X-PhiveGarage"))){response.sendError(403,"X-PhiveGarage header required");return;}
     chain.doFilter(request,response);
    }
   },org.springframework.security.web.authentication.www.BasicAuthenticationFilter.class)
   .httpBasic(Customizer.withDefaults()).build();
 }
}
