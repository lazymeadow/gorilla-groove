package com.example.groove.config

import org.springframework.beans.factory.ObjectFactory
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.context.ApplicationContext
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.core.convert.ConversionService
import org.springframework.data.web.PageableHandlerMethodArgumentResolver
import org.springframework.data.web.config.SpringDataWebConfiguration

// Spring has a global setting that limits pagination to 2000. This is stupid. I don't like it.
// If I want to make big requests, let me.
// I specifically changed this to allow the mobile apps to fetch other users' libraries in one request.
@Configuration
class PaginationConfiguration(
		context: ApplicationContext,
		@Qualifier("mvcConversionService") conversionService: ObjectFactory<ConversionService>
) : SpringDataWebConfiguration(context, conversionService) {
	@Bean
	override fun pageableResolver(): PageableHandlerMethodArgumentResolver {
		val pageableHandlerMethodArgumentResolver = PageableHandlerMethodArgumentResolver(sortResolver())
		pageableHandlerMethodArgumentResolver.setMaxPageSize(Int.MAX_VALUE)
		return pageableHandlerMethodArgumentResolver
	}
}
