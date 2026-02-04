package com.dipcoin;

import java.util.Arrays;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.web.servlet.error.ErrorMvcAutoConfiguration;
import org.springframework.boot.web.servlet.support.SpringBootServletInitializer;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.EnableAspectJAutoProxy;
import org.springframework.context.annotation.FilterType;
import org.springframework.context.annotation.ImportResource;
import org.springframework.context.annotation.PropertySource;
import org.springframework.context.annotation.PropertySources;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.transaction.annotation.EnableTransactionManagement;
import org.springframework.validation.annotation.Validated;
import org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration;

@SpringBootApplication(exclude = { ErrorMvcAutoConfiguration.class, HibernateJpaAutoConfiguration.class })
@ComponentScan(basePackages = { "com.dipcoin.api", "com.dipcoin.db", "com.dipcoin.partner" })
@ImportResource({
        "classpath*:dipcoin-core-services-application-context.xml",
        "classpath*:dipcoin-db-services-fastag.xml",
        "classpath*:dipcoin-partner-services-application-context.xml"
})
@Order(Ordered.HIGHEST_PRECEDENCE)
@PropertySources({
        @PropertySource(value = "classpath:application.properties", ignoreResourceNotFound = true),
        @PropertySource(value = "classpath:dipcoin-db-services-${spring.profiles.active}.properties", ignoreResourceNotFound = true),
        @PropertySource(value = "classpath:dipcoin-api-system-application-${spring.profiles.active}.properties", ignoreResourceNotFound = true)
})
@Validated
@EnableTransactionManagement
@EnableAspectJAutoProxy
@EnableCaching(proxyTargetClass = true)
public class CoreApiApplication extends SpringBootServletInitializer {

    private static final Logger LOG = LogManager.getLogger(CoreApiApplication.class);

    @Bean
    public CommandLineRunner run(ApplicationContext appContext) {
        return args -> {
            String[] beans = appContext.getBeanDefinitionNames();
            Arrays.stream(beans).sorted().forEach(LOG::trace);
            LOG.info("Fastag API Application (CoreApiApplication) Started successfully!");
        };
    }

    public static void main(String[] args) {
        SpringApplication app = new SpringApplication(CoreApiApplication.class);
        app.run(args);
    }
}
