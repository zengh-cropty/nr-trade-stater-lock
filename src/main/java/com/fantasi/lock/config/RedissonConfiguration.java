package com.fantasi.lock.config;

import com.fantasi.lock.core.RedissonManager;
import com.fantasi.lock.prop.RedissonProperties;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RedissonClient;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;


/**
 * Redisson自动化配置
 *
 * @author zh
 */
@Slf4j
@Configuration
@ConditionalOnClass(RedissonProperties.class)
@EnableConfigurationProperties(RedissonProperties.class)
public class RedissonConfiguration {



	@Bean
	@ConditionalOnMissingBean(RedissonClient.class)
	public RedissonClient redissonClient(RedissonProperties redissonProperties) {
		RedissonManager redissonManager = new RedissonManager(redissonProperties);
		return redissonManager.getRedisson();
	}

}
