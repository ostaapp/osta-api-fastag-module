package com.dipcoin.api.config;

import com.dipcoin.commons.LogFormatter;
import com.mchange.v2.c3p0.ComboPooledDataSource;
import java.util.Arrays;
import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.sql.DataSource;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;


@Component("systemConfig")
public class SystemConfig {
  private static final Logger LOG = LogManager.getLogger(SystemConfig.class);

  @Autowired
  private ApplicationContext appContext;

  @Autowired
  private Environment environment;

  private String[] beanDefinitionNames;
  private DataSource dataSource;
  private String[] activeProfiles;


  public String[] getBeanDefinitionNames() {
    return beanDefinitionNames;
  }

  public String getDataSource() {
    return (dataSource instanceof ComboPooledDataSource)
        ? ((ComboPooledDataSource) dataSource).toString(true)
        : dataSource.toString();
  }

  public String[] getActiveProfiles() {
    return activeProfiles;
  }

  /*
   * 
   */
  @PostConstruct
  public void construct() {
    LOG.debug(LogFormatter.instance()
        .message("Constructing " + this.getClass().getSimpleName() + " ...").format());

    beanDefinitionNames = appContext.getBeanDefinitionNames();
    Arrays.sort(beanDefinitionNames);

    dataSource = (DataSource) appContext.getBean("dataSource");

    activeProfiles = environment.getActiveProfiles();
  }

  @PreDestroy
  public void cleanUp() {
    LOG.debug(LogFormatter.instance()
        .message("Cleaning up " + this.getClass().getSimpleName() + " ...").format());
  }
}
