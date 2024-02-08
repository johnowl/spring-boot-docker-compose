package com.johnowl.demodockercompose

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.web.client.TestRestTemplate

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class DemoDockerComposeApplicationTests {

	@Autowired
	lateinit var testRestTemplate: TestRestTemplate

	@Test
	fun `should return a message`() {
		val response = testRestTemplate.getForEntity("/hello", String::class.java)
		assertThat(response.statusCode.is2xxSuccessful).isTrue()
		assertThat(response.body).isNotBlank()
	}
}
