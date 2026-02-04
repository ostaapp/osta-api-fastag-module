package com.dipcoin.api.config;

import com.dipcoin.api.filter.RequestHandlerInterceptor;
import com.dipcoin.api.filter.ResponseHeadersInterceptor;
import com.dipcoin.commons.LogFormatter;
import java.util.HashSet;
import javax.servlet.MultipartConfigElement;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.web.servlet.DispatcherServletAutoConfiguration;
import org.springframework.boot.web.servlet.MultipartConfigFactory;
import org.springframework.boot.web.servlet.ServletRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.retry.annotation.EnableRetry;
import org.springframework.scheduling.annotation.AsyncConfigurer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jwt.JwtValidators;
import org.springframework.util.unit.DataSize;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.servlet.DispatcherServlet;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import java.util.concurrent.Executor;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;


@Configuration
@EnableRetry
@org.springframework.scheduling.annotation.EnableAsync
public class ApplicationConfig implements WebMvcConfigurer, AsyncConfigurer {

  private static final Logger LOG = LogManager.getLogger(ApplicationConfig.class);

  private static String DIPCOIN_COM = "dipcoin.com";
  private static String OSTAAPP_COM = "ostaapp.com";

  private static int ACCESS_CONTROL_MAX_AGE_IN_SECONDS = 12 * 60 * 60;

  private static final HashSet<String> TRUSTED_SOURCES = new HashSet<String>();

  static {
    TRUSTED_SOURCES.add("http://localhost:4200");
  }

  // method to add trusted sources via application context
  public static void setTrustedSources(final HashSet<String> sources) {
    TRUSTED_SOURCES.addAll(sources);
  }



 @Autowired
 @Qualifier("com.dipcoin.api.filter.RequestHandlerInterceptor")
 private RequestHandlerInterceptor requestHandlerInterceptor;

 @Autowired
 private ResponseHeadersInterceptor responseHeadersInterceptor;
 ;
//  @Override
//  public void addInterceptors(InterceptorRegistry registry) {
//    LOG.info(LogFormatter.instance().message("Initializing Interceptors").format());
//
//    /*
//     * @NOTE - this should be the entry point to the interceptors. DO NOT CHANGE this order.
//     */
//   
//  }
 
 @Override
 public void addInterceptors(InterceptorRegistry registry) {
   LOG.info(LogFormatter.instance().message("Initializing Interceptors").format());

   /*
    * @NOTE - this should be the entry point to the interceptors. DO NOT CHANGE this order.
    */
   registry.addInterceptor(requestHandlerInterceptor).order(1);

   //registry.addInterceptor(corsResponseInterceptor).order(1);
   registry.addInterceptor(responseHeadersInterceptor).order(1);
 }


  @Override
  public void addCorsMappings(CorsRegistry registry) {
      registry.addMapping("/**") // Allow all endpoints
              .allowedOrigins("*") // Allow requests from any origin
              .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS") // Allow these HTTP methods
              .allowedHeaders("*") // Allow all headers
              .allowCredentials(true); // Allow credentials (set `false` if using public APIs)
  }

  @Bean
  public DispatcherServlet dispatcherServlet() {
    return new DipcoinDispatcherServlet();
  }

  @SuppressWarnings({"rawtypes", "unchecked"})
  @Bean
  public ServletRegistrationBean dispatchServletRegistration() {

    ServletRegistrationBean registration = new ServletRegistrationBean(
        this.dispatcherServlet());

    registration.setLoadOnStartup(1);
    registration
        .setName(
            DispatcherServletAutoConfiguration.DEFAULT_DISPATCHER_SERVLET_REGISTRATION_BEAN_NAME);

    registration.addUrlMappings("/api/*", "/*");
    registration.setMultipartConfig(multipartConfigElement());

    return registration;

  }
  


  @Bean
  @ConditionalOnMissingBean(MultipartConfigElement.class)
  public MultipartConfigElement multipartConfigElement() {
    MultipartConfigFactory factory = new MultipartConfigFactory();
    factory.setMaxFileSize(DataSize.ofBytes(50000000));
    factory.setMaxRequestSize(DataSize.ofBytes(60000000));
    factory.setFileSizeThreshold(DataSize.ofBytes(0));
    factory.setLocation("/tmp");
    return factory.createMultipartConfig();
  }

  // ✅ This makes RestTemplate available for autowiring everywhere
  @Bean
  public RestTemplate restTemplate() {
    return new RestTemplate();
  }


  @Bean
  public JwtDecoder jwtDecoder(
      @Value("${spring.security.oauth2.resourceserver.jwt.jwk-set-uri}") String jwkSetUri,
      @Value("${spring.security.oauth2.resourceserver.jwt.issuer-uri}") String issuer) {
    NimbusJwtDecoder decoder = NimbusJwtDecoder.withJwkSetUri(jwkSetUri).build();
    decoder.setJwtValidator(JwtValidators.createDefaultWithIssuer(issuer));
    return decoder;
  }


  @Bean(name = "taskExecutor")
  @Primary
  public Executor taskExecutor() {
    ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
    executor.setCorePoolSize(5);
    executor.setMaxPoolSize(20);
    executor.setQueueCapacity(100);
    executor.setThreadNamePrefix("OstaAsync-");
    executor.initialize();
    return executor;
  }

  @Override
  public Executor getAsyncExecutor() {
    return taskExecutor();
  }
}

