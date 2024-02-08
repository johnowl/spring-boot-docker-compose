package com.johnowl.demodockercompose

import org.slf4j.LoggerFactory
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.http.HttpHeaders
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse

@SpringBootApplication
class DemoDockerComposeApplication

fun main(args: Array<String>) {
    runApplication<DemoDockerComposeApplication>(*args)
}

@RestController
class HelloController {

    private val httpClient = HttpClient.newHttpClient()
    private val log by lazy { LoggerFactory.getLogger(this::class.java) }

    @RequestMapping("/hello")
    fun callUpstream(): ResponseEntity<String> =
        try {
            val request = HttpRequest.newBuilder()
                .uri(URI.create("http://proxy-host:8080/hello"))
                .header("X-Custom-Header", "Custom Value")
                .build()

            val response = httpClient.send(request, HttpResponse.BodyHandlers.ofString())
            val headers = HttpHeaders()
            response.headers().map().forEach { (key, value) -> headers[key] = value }

            ResponseEntity.status(response.statusCode())
                .headers(headers)
                .body(response.body())
        } catch (e: Exception) {
			log.error("Error calling upstream", e)
            ResponseEntity.status(500).body(e.stackTraceToString())
        }


    @RequestMapping("/health")
    fun health(): ResponseEntity<String> {
        log.info("Health check is OK")
        return ResponseEntity.ok("OK")
    }
}





