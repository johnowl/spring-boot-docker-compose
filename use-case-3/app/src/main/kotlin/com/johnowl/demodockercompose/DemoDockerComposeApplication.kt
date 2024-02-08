package com.johnowl.demodockercompose

import org.slf4j.LoggerFactory
import org.springframework.beans.factory.InitializingBean
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.boot.web.client.RestTemplateBuilder
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.data.annotation.Id
import org.springframework.data.redis.core.RedisTemplate
import org.springframework.data.relational.core.mapping.Table
import org.springframework.data.repository.CrudRepository
import org.springframework.stereotype.Component
import org.springframework.stereotype.Repository
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestHeader
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.client.RestTemplate
import java.time.LocalDateTime
import kotlin.jvm.optionals.getOrNull

@SpringBootApplication
class DemoDockerComposeApplication

fun main(args: Array<String>) {
    runApplication<DemoDockerComposeApplication>(*args)
}

@Table("messages")
data class Message(@Id val id: Long, val content: String, val createdAt: LocalDateTime)

@Repository
interface MessageRepository : CrudRepository<Message, Long>

@Configuration
class ThirdPartyConfiguration {
    @Bean
    fun thirdPartyRestTemplate(): RestTemplate =
        RestTemplateBuilder().rootUri("http://localhost:9091").build()
}

@RestController
class HelloController(
    private val messageRepository: MessageRepository,
    private val redisTemplate: RedisTemplate<String, String>,
    private val restTemplate: RestTemplate
) {
    private val log by lazy { LoggerFactory.getLogger(this::class.java) }

    @GetMapping("/hello")
    fun hello(@RequestHeader("X-Custom-Header") customHeader: String = ""): String {
        log.info("Custom Header is [$customHeader]")

        val message = getRandomMessage()
        redisTemplate.opsForValue().increment("counter")
        return message?.content ?: "Hello, world!"
    }

    @GetMapping("/goodbye")
    fun goodbye(): String =
        restTemplate.getForObject("/goodbye", String::class.java)
            ?: error("Could not get response from third party service")

    @GetMapping("/statistics")
    fun statistics(): String {
        val counter = redisTemplate.opsForValue().get("counter") ?: "0"
        return "Counter: $counter"
    }

    private fun getRandomMessage(): Message? {
        val total = messageRepository.count()
        val randomId = (1..total).random()
        val message = messageRepository.findById(randomId).getOrNull()
        return message
    }
}


@Component
class PostgresSmokeTest(
    private val messageRepository: MessageRepository
) : InitializingBean {

    private val log by lazy { LoggerFactory.getLogger(this::class.java) }
    override fun afterPropertiesSet() {
        val total = messageRepository.count()
        log.info("Total messages: $total")
    }
}

@Component
class RedisSmokeTest(
    private val redisTemplate: RedisTemplate<String, String>
) : InitializingBean {

    private val log by lazy { LoggerFactory.getLogger(this::class.java) }
    override fun afterPropertiesSet() {
        redisTemplate.opsForValue().set("counter", "0")
        log.info("Counter set to: 0")
    }
}