/*
 * Copyright 2012-2013 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.springframework.data.rest.webmvc;

import java.net.URI;

import org.springframework.core.MethodParameter;
import org.springframework.core.convert.ConversionService;
import org.springframework.data.mapping.PersistentEntity;
import org.springframework.data.repository.support.Repositories;
import org.springframework.data.rest.repository.invoke.RepositoryInvoker;
import org.springframework.data.rest.repository.invoke.RepositoryInvokerFactory;
import org.springframework.data.rest.repository.mapping.ResourceMetadata;
import org.springframework.util.Assert;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;

/**
 * @author Jon Brisbin
 * @author Oliver Gierke
 */
public class RepositoryRestRequestHandlerMethodArgumentResolver implements HandlerMethodArgumentResolver {

	private final Repositories repositories;
	private final RepositoryInvokerFactory invokerFactory;
	private final ResourceMetadataHandlerMethodArgumentResolver resourceMetadataResolver;
	private final BaseUriMethodArgumentResolver baseUriResolver;

	/**
	 * Creates a new {@link RepositoryRestRequestHandlerMethodArgumentResolver} using the given {@link Repositories} and
	 * {@link ConversionService}.
	 * 
	 * @param repositories must not be {@literal null}.
	 * @param conversionService must not be {@literal null}.
	 */
	public RepositoryRestRequestHandlerMethodArgumentResolver(Repositories repositories,
			ConversionService conversionService, ResourceMetadataHandlerMethodArgumentResolver resourceMetadataResolver,
			BaseUriMethodArgumentResolver baseUriResolver) {

		Assert.notNull(repositories, "Repositories must not be null!");
		Assert.notNull(conversionService, "ConversionService must not be null!");

		this.repositories = repositories;
		this.invokerFactory = new RepositoryInvokerFactory(repositories, conversionService);
		this.resourceMetadataResolver = resourceMetadataResolver;
		this.baseUriResolver = baseUriResolver;
	}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.web.method.support.HandlerMethodArgumentResolver#supportsParameter(org.springframework.core.MethodParameter)
	 */
	@Override
	public boolean supportsParameter(MethodParameter parameter) {
		return RepositoryRestRequest.class.isAssignableFrom(parameter.getParameterType());
	}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.web.method.support.HandlerMethodArgumentResolver#resolveArgument(org.springframework.core.MethodParameter, org.springframework.web.method.support.ModelAndViewContainer, org.springframework.web.context.request.NativeWebRequest, org.springframework.web.bind.support.WebDataBinderFactory)
	 */
	@Override
	public RepositoryRestRequest resolveArgument(MethodParameter parameter, ModelAndViewContainer mavContainer,
			NativeWebRequest webRequest, WebDataBinderFactory binderFactory) throws Exception {

		URI baseUri = baseUriResolver.resolveArgument(parameter, mavContainer, webRequest, binderFactory);
		ResourceMetadata repoInfo = resourceMetadataResolver.resolveArgument(parameter, mavContainer, webRequest,
				binderFactory);

		RepositoryInvoker repositoryInvoker = invokerFactory.getInvokerFor(repoInfo.getDomainType());
		PersistentEntity<?, ?> persistentEntity = repositories.getPersistentEntity(repoInfo.getDomainType());

		// TODO reject if ResourceMetadata cannot be resolved

		return new RepositoryRestRequest(persistentEntity, webRequest, baseUri, repoInfo, repositoryInvoker);
	}
}
