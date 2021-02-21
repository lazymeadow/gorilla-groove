package com.example.groove.security

import com.example.groove.properties.S3Properties
import com.example.groove.util.logger
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.core.env.Environment
import org.springframework.http.HttpMethod
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
import org.springframework.web.servlet.config.annotation.CorsRegistry
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer


// This class (and other security-oriented ones) were heavily borrowed from the guide:
// https://octoperf.com/blog/2018/03/08/securing-rest-api-spring-security/

@Suppress("unused")
@Configuration
@EnableWebSecurity
class SecurityConfiguration(
		private val tokenAuthenticationProvider: TokenAuthenticationProvider,
		private val dataSource: DataSource,
		private val environment: Environment,
		s3Properties: S3Properties
) : WebSecurityConfigurerAdapter() {

	private val publicUrls = OrRequestMatcher(
			AntPathRequestMatcher("/api/authentication/login**"),
			AntPathRequestMatcher("/api/user-invite-link/public/**"), // Unauthenticated users need to request the full details of their invite link
			AntPathRequestMatcher("/api/user/public/**"), // Unauthenticated users need to be able to create their own accounts
			AntPathRequestMatcher("/api/file/track-link/*"),
			AntPathRequestMatcher("/api/track/preview/public/*"),
			AntPathRequestMatcher("/api/version**"),
			AntPathRequestMatcher("/"), // Allow serving the frontend through 'index.html' from our static files
			AntPathRequestMatcher("/login"), // These are routes used in the frontend. Probably a better way to handle this
			AntPathRequestMatcher("/track-link/*"),
			AntPathRequestMatcher("/create-account/*"),
			AntPathRequestMatcher("/password-reset/*"),
			AntPathRequestMatcher("/privacy-policy"),
			AntPathRequestMatcher("/dist/bundle.js"),
			AntPathRequestMatcher("/dist/index.css"),
			AntPathRequestMatcher("/node_modules/react-toastify/dist/ReactToastify.min.css"), // I hate that I did this, but I didn't want to deal with webpack to combine them
			AntPathRequestMatcher("/images/**"),
			AntPathRequestMatcher("/favicon.ico"),
			AntPathRequestMatcher("/favicon-16x16.png"),
			AntPathRequestMatcher("/favicon-32x32.png"),
			AntPathRequestMatcher("/**", HttpMethod.OPTIONS.toString(), false) // allow CORS option calls
	)

	// Used when not running stuff on S3
	private val localDiskUrls = OrRequestMatcher(
			AntPathRequestMatcher("/music/**"),
			AntPathRequestMatcher("/album-art/**")
	)

	private val protectedUrls = if (s3Properties.awsStoreInS3) {
		NegatedRequestMatcher(publicUrls)
	} else {
		NegatedRequestMatcher(OrRequestMatcher(publicUrls, localDiskUrls))
	}

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
		return HttpStatusEntryPoint(FORBIDDEN)
	}

	@Bean
	fun corsConfigurer(): WebMvcConfigurer {
		return object : WebMvcConfigurer {
			override fun addCorsMappings(registry: CorsRegistry) {
				val mapping = registry.addMapping("/**")

				// Only add CORS in prod to make life easier when developing locally
				if (environment.activeProfiles.contains("prod")) {
					mapping.allowedOrigins(*allowedOrigins)
				}

				mapping.allowCredentials(true)
						.allowedMethods("HEAD", "GET", "PUT", "POST", "DELETE", "PATCH")
			}
		}
	}

	companion object {
		val logger = logger()

		val allowedOrigins = arrayOf(
				"http://127.0.0.1:8080",
				"http://localhost:8080",
				"http://127.0.0.1:8081",
				"http://localhost:8081",
				"http://192.168.1.25:8081",
				"http://gorillagroove.net",
				"https://gorillagroove.net"
		)
	}
}
