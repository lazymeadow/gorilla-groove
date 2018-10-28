package com.example.groove.security

import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.config.annotation.web.builders.WebSecurity
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter

import javax.sql.DataSource
import org.springframework.security.web.authentication.AnonymousAuthenticationFilter

import org.springframework.http.HttpStatus.FORBIDDEN
import org.springframework.security.config.http.SessionCreationPolicy.STATELESS
import org.springframework.security.web.authentication.HttpStatusEntryPoint
import org.springframework.security.web.AuthenticationEntryPoint
import org.springframework.security.web.util.matcher.NegatedRequestMatcher
import org.springframework.security.web.util.matcher.AntPathRequestMatcher
import org.springframework.security.web.util.matcher.OrRequestMatcher
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler

// This class (and other security-oriented ones) were heavily borrowed from the guide:
// https://octoperf.com/blog/2018/03/08/securing-rest-api-spring-security/

@Suppress("unused")
@Configuration
@EnableWebSecurity
class SecurityConfiguration @Autowired constructor(
		private val tokenAuthenticationProvider: TokenAuthenticationProvider,
		private val dataSource: DataSource
) : WebSecurityConfigurerAdapter() {

	private val publicUrls = OrRequestMatcher(
			AntPathRequestMatcher("/authentication/login**")
	)
	private val protectedUrls = NegatedRequestMatcher(publicUrls)

	@Throws(Exception::class)
	override fun configure(auth: AuthenticationManagerBuilder) {
		auth.authenticationProvider(tokenAuthenticationProvider)
	}

	@Throws(Exception::class)
	override fun configure(http: HttpSecurity) {
		http
				.sessionManagement()
				.sessionCreationPolicy(STATELESS)
				.and()
				.exceptionHandling()
				// this entry point handles when you request a protected page and you are not yet authenticated
				.defaultAuthenticationEntryPointFor(forbiddenEntryPoint(), protectedUrls)
				.and()
				.authenticationProvider(tokenAuthenticationProvider)
				.addFilterBefore(restAuthenticationFilter(), AnonymousAuthenticationFilter::class.java)
				.authorizeRequests()
				.requestMatchers(protectedUrls)
				.authenticated()
				.and()
				.csrf().disable()
				.formLogin().disable()
				.httpBasic().disable()
				.logout().disable()
	}

	@Throws(Exception::class)
	override fun configure(web: WebSecurity) {
		web.ignoring().antMatchers("/resources/**", "/static/**", "/css/**", "/js/**", "/images/**")
	}

	@Bean
	fun successHandler(): SimpleUrlAuthenticationSuccessHandler {
		val successHandler = SimpleUrlAuthenticationSuccessHandler()
		successHandler.setRedirectStrategy(NoRedirectStrategy())
		return successHandler
	}

	@Bean
	@Throws(Exception::class)
	fun restAuthenticationFilter(): TokenAuthenticationFilter {
		val filter = TokenAuthenticationFilter(protectedUrls)
		filter.setAuthenticationManager(authenticationManager())
		filter.setAuthenticationSuccessHandler(successHandler())
		return filter
	}

	@Bean
	fun forbiddenEntryPoint(): AuthenticationEntryPoint {
		logger.info("TRIGGERED FORBIDDEN ENTRY POINT")
		return HttpStatusEntryPoint(FORBIDDEN)
	}

	companion object {
		val logger = LoggerFactory.getLogger(SecurityConfiguration::class.java)!!
	}
}