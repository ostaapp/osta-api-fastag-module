package com.dipcoin.api.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;

@Configuration
@EnableWebSecurity
public class ApplicationSecurity extends WebSecurityConfigurerAdapter {
    @Override
    protected void configure(HttpSecurity http) throws Exception {
        http
            .csrf().disable() // Disable CSRF for APIs
            .authorizeRequests()
            .antMatchers("/api/**").permitAll() // Modify as per your needs
            .antMatchers("/actuator/**").permitAll() // Allow all actuator endpoints
            .antMatchers("/actuator/health/**").permitAll() // Allow health endpoints
            .antMatchers("/actuator/info").permitAll() // Allow info endpoint
            .antMatchers("/actuator/metrics/**").permitAll() // Allow metrics endpoints
            .antMatchers("/actuator/prometheus").permitAll() // Allow prometheus endpoint
            .anyRequest().authenticated();
    }
}