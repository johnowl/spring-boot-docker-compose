package com.johnowl.demodockercompose

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertAll
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.web.client.TestRestTemplate
import org.springframework.boot.web.client.RestTemplateBuilder

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.DEFINED_PORT)
class DemoDockerComposeApplicationTests {

	private val testRestTemplate = TestRestTemplate(RestTemplateBuilder().rootUri("http://localhost:9090"))

	@Test
	fun `should return a message`() {
		val response = testRestTemplate.getForEntity("/hello", String::class.java)

		assertAll(
			{ assertThat(response.statusCode.is2xxSuccessful).isTrue() },
			{ assertThat(response.body).isNotBlank() }
		)
	}
}
