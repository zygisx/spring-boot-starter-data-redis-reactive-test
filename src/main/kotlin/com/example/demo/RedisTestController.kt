package com.example.demo

import io.lettuce.core.ClientOptions
import io.lettuce.core.event.DefaultEventPublisherOptions
import io.lettuce.core.event.metrics.CommandLatencyEvent
import io.lettuce.core.resource.DefaultClientResources
import kotlinx.coroutines.reactive.awaitFirstOrNull
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.boot.autoconfigure.data.redis.LettuceClientConfigurationBuilderCustomizer
import org.springframework.context.annotation.Configuration
import org.springframework.data.redis.connection.lettuce.LettuceClientConfiguration
import org.springframework.data.redis.core.ReactiveRedisTemplate
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.time.Duration

@Configuration
class LettuceCustomizer: LettuceClientConfigurationBuilderCustomizer {
    companion object {
        val LOGGER: Logger = LoggerFactory.getLogger(LettuceCustomizer::class.java)
    }

    @Override
    override fun customize(clientConfigurationBuilder: LettuceClientConfiguration.LettuceClientConfigurationBuilder) {
        val clientResources = DefaultClientResources.builder()
            .commandLatencyPublisherOptions(DefaultEventPublisherOptions.builder()
                    .eventEmitInterval(Duration.ofSeconds(5))
                    .build())
            .build()

        clientResources.eventBus().get()
                .filter { redisEvent -> redisEvent is CommandLatencyEvent }
                .cast(CommandLatencyEvent::class.java)
                .subscribe { e -> LOGGER.info("LATENCIES REPORT:\n{}", e.latencies) }

        clientConfigurationBuilder
                .clientResources(clientResources)
                .clientOptions(ClientOptions.builder()
                        .publishOnScheduler(true)
                        .build())
    }
}

@RestController
@RequestMapping("/redis-test")
class RedisTestController(
    val redisTemplate: ReactiveRedisTemplate<String, String>
) {
    companion object {
        val LOGGER: Logger = LoggerFactory.getLogger(RedisTestController::class.java)
    }

    val keys = (0..5).map { it.toString() }

    @GetMapping
    suspend fun get(): String? {
        val start = System.currentTimeMillis()
        val ret = redisTemplate.opsForValue().get("test").awaitFirstOrNull()
        val ret2 = redisTemplate.opsForValue().multiGet(keys).awaitFirstOrNull()
        val elapsed = System.currentTimeMillis() - start
        if (elapsed > 100) {
            LOGGER.info("Slow call {}", elapsed)
        }
        return ret
    }

    @PutMapping
    suspend fun put(
    ): Boolean? {
        redisTemplate.opsForValue().set("test", "test").awaitFirstOrNull()
        keys.forEach {
            redisTemplate.opsForValue().set(it, "test-value")
        }
        return true
    }
}
