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
import org.springframework.scheduling.annotation.EnableScheduling
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component
import org.springframework.stereotype.Repository
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestHeader
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.client.RestTemplate
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials
import software.amazon.awssdk.regions.Region
import software.amazon.awssdk.services.sqs.SqsClient
import software.amazon.awssdk.services.sqs.model.CreateQueueRequest
import java.net.URI
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

@Configuration
@EnableScheduling
class SqsConfiguration {
    @Bean
    fun sqsClient(): SqsClient {
        val credentials = AwsBasicCredentials.create("test", "test")

        return SqsClient.builder()
            .region(Region.US_EAST_1)
            .endpointOverride(URI.create("http://localhost:4566"))
            .credentialsProvider { credentials }
            .build()
    }
}

//@Profile("test", "local")
@Component
class CreateAwsSqs(
    private val sqsClient: SqsClient
) : InitializingBean {
    private val log by lazy { LoggerFactory.getLogger(this::class.java) }
    override fun afterPropertiesSet() {
        log.info("Creating AWS SQS")
        val createQueueRequest = CreateQueueRequest
            .builder()
            .queueName("my-queue")
            .build()
        sqsClient.createQueue(createQueueRequest)
        log.info("AWS SQS created, URL: ${sqsClient.getQueueUrl { it.queueName("my-queue") }.queueUrl()}")
    }
}

@Component
class MessagePublisher(
    private val sqsClient: SqsClient
) {
    private val log by lazy { LoggerFactory.getLogger(this::class.java) }

    @Scheduled(fixedRate = 1000)
    fun publish() {
        val message = "Message published at ${LocalDateTime.now()}"
        log.info("Publishing message: $message")
        val queueUrl = sqsClient.getQueueUrl { it.queueName("my-queue") }.queueUrl()
        sqsClient.sendMessage { it.queueUrl(queueUrl).messageBody(message) }
    }
}

@Component
class MessageConsumer(
    private val sqsClient: SqsClient
) {
    private val log by lazy { LoggerFactory.getLogger(this::class.java) }

    @Scheduled(fixedRate = 5000)
    fun consume() {
        val queueUrl = sqsClient.getQueueUrl { it.queueName("my-queue") }.queueUrl()
        val messages = sqsClient.receiveMessage { it.queueUrl(queueUrl).maxNumberOfMessages(10) }.messages()
        messages.forEach { message ->
            log.info("---- Consuming message: ${message.body()}")
            sqsClient.deleteMessage { it.queueUrl(queueUrl).receiptHandle(message.receiptHandle()) }
        }
    }
}
