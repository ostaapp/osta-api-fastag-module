package com.dipcoin.mock.bank.config;

import javax.sql.DataSource;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;

@Configuration
public class MockBankConfig {

    @Value("${mock.bank.datasource.url:jdbc:mysql://stage-db-new.cf2ik20ms4wt.ap-south-1.rds.amazonaws.com:3306/mockbank?allowMultiQueries=true&useSSL=false&useUnicode=true&useJDBCCompliantTimezoneShift=true&useLegacyDatetimeCode=false&serverTimezone=UTC&createDatabaseIfNotExist=true}")
    private String url;

    @Value("${mock.bank.datasource.username:dipcoindb}")
    private String username;

    @Value("${mock.bank.datasource.password:O5taD1pC0in}")
    private String password;

    @Bean(name = "mockBankDataSource")
    public DataSource mockBankDataSource() {
        DriverManagerDataSource dataSource = new DriverManagerDataSource();
        dataSource.setDriverClassName("com.mysql.cj.jdbc.Driver");
        dataSource.setUrl(url);
        dataSource.setUsername(username);
        dataSource.setPassword(password);
        return dataSource;
    }

    @Bean(name = "mockBankJdbcTemplate")
    public NamedParameterJdbcTemplate namedParameterJdbcTemplate() {
        return new NamedParameterJdbcTemplate(mockBankDataSource());
    }
}
