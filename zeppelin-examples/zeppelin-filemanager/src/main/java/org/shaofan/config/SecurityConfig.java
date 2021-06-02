package org.shaofan.config;

import java.io.IOException;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.ImportResource;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

@Configuration
@EnableWebSecurity
@ImportResource(locations= {"classpath:spring-security.xml"})
public class SecurityConfig extends WebSecurityConfigurerAdapter 
{
	 
	public void successfulAuthentication(HttpServletRequest request,HttpServletResponse response,
            FilterChain chain, Authentication authentication) throws IOException, ServletException {

		//your some custom code
		
		// Add the authentication to the Security context
		SecurityContextHolder.getContext().setAuthentication(authentication);
	}
}