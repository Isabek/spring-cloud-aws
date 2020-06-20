/*
 * Copyright 2013-2019 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.cloud.aws.messaging.endpoint;

import java.io.IOException;
import java.io.InputStreamReader;
import java.lang.reflect.Method;

import org.junit.jupiter.api.Test;
import reactor.core.publisher.Mono;

import org.springframework.core.MethodParameter;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.MediaType;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import org.springframework.util.FileCopyUtils;
import org.springframework.util.ReflectionUtils;
import org.springframework.web.reactive.BindingContext;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * @author Isabek Tashiev
 */
class NotificationMessageReactiveHandlerMethodArgumentResolverTest {

	@Test
	void supportsParameter_wrongMethodParameter_returnsFalse() {
		// Arrange
		NotificationMessageReactiveHandlerMethodArgumentResolver resolver = getResolver();

		Method method = ReflectionUtils.findMethod(NotificationMethods.class,
				"subscriptionMethod", (Class<?>[]) null);
		MethodParameter methodParameter = new MethodParameter(method, 0);

		// Act
		boolean actual = resolver.supportsParameter(methodParameter);

		// Assert
		assertThat(actual).isFalse();
	}

	@Test
	void supportsParameter_correctMethodParameter_returnsTrue() {
		// Arrange
		NotificationMessageReactiveHandlerMethodArgumentResolver resolver = getResolver();

		Method method = ReflectionUtils.findMethod(NotificationMethods.class,
				"handleMethod", String.class, String.class);
		MethodParameter methodParameter = new MethodParameter(method, 1);

		// Act
		boolean actual = resolver.supportsParameter(methodParameter);

		// Assert
		assertThat(actual).isTrue();
	}

	@Test
	void doResolveArgumentFromNotificationMessage_wrongNotificationType_reportsError()
			throws Exception {
		// Arrange
		NotificationMessageReactiveHandlerMethodArgumentResolver resolver = getResolver();

		Method method = ReflectionUtils.findMethod(NotificationMethods.class,
				"handleMethod", (Class<?>[]) null);
		MethodParameter parameter = new MethodParameter(method, 0);

		final String body = FileCopyUtils.copyToString(new InputStreamReader(
				new ClassPathResource("subscriptionConfirmation.json", getClass())
						.getInputStream()));

		MockServerWebExchange exchange = MockServerWebExchange.from(MockServerHttpRequest
				.post("/topic").contentType(MediaType.TEXT_PLAIN).body(body));

		// Act
		Mono<Object> mono = resolver.resolveArgument(parameter, new BindingContext(),
				exchange);

		// Assert
		assertThatThrownBy(mono::block).isInstanceOf(IllegalArgumentException.class)
				.hasMessageContaining(
						"@NotificationMessage annotated parameters are only allowed for method that receive a notification message.");
	}

	@Test
	void doResolveArgumentFromNotificationMessage_correctNotificationType_returnsMessage()
			throws IOException {
		// Arrange
		NotificationMessageReactiveHandlerMethodArgumentResolver resolver = getResolver();

		Method method = ReflectionUtils.findMethod(NotificationMethods.class,
				"handleMethod", (Class<?>[]) null);
		MethodParameter parameter = new MethodParameter(method, 0);

		final String body = FileCopyUtils.copyToString(new InputStreamReader(
				new ClassPathResource("notificationMessage.json", getClass())
						.getInputStream()));

		MockServerWebExchange exchange = MockServerWebExchange.from(MockServerHttpRequest
				.post("/topic").contentType(MediaType.TEXT_PLAIN).body(body));

		// Act
		Mono<Object> mono = resolver.resolveArgument(parameter, new BindingContext(),
				exchange);

		// Assert
		final Object resolvedMessage = mono.block();

		assertThat(resolvedMessage).isEqualTo("asdasd");
	}

	private NotificationMessageReactiveHandlerMethodArgumentResolver getResolver() {
		return new NotificationMessageReactiveHandlerMethodArgumentResolver();
	}

}
