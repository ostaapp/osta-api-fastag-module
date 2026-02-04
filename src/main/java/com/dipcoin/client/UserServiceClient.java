package com.dipcoin.client;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import org.springframework.http.MediaType;

import com.dipcoin.api.model.UserInfoResponse;
import com.dipcoin.db.services.model.User;

import java.util.Collections;

import javax.net.ssl.SSLContext;

import org.apache.http.conn.ssl.NoopHostnameVerifier;
import org.apache.http.conn.ssl.TrustSelfSignedStrategy;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.ssl.SSLContextBuilder;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;

@Component
public class UserServiceClient {

	private static final Logger logger = LoggerFactory.getLogger(UserServiceClient.class);

	@Autowired
	@Qualifier("sslRestTemplate")
	private RestTemplate restTemplate;

	private static final String USER_SERVICE_URL = "http://localhost:7443/api/v1/customer/user/{userId}";
	
	private static final String GET_USER_URL = "http://localhost:7443/v1/customer/user";

	public UserInfoResponse getUserById(String userId, String authorizationToken, String dcCookie) {
		try {
			HttpHeaders headers = new HttpHeaders();
			headers.set(HttpHeaders.AUTHORIZATION, authorizationToken);
			headers.add(HttpHeaders.COOKIE, "dcl=" + dcCookie);
			headers.setAccept(Collections.singletonList(MediaType.APPLICATION_JSON));
			headers.setContentType(MediaType.APPLICATION_JSON);
			
			
			System.out.println("Pre " + dcCookie +" "+authorizationToken);

			HttpEntity<Void> entity = new HttpEntity<>(headers);

			System.out.println("Fetching user with ID: " + userId + " from User Service");

			ResponseEntity<UserInfoResponse> response = restTemplate.exchange(USER_SERVICE_URL, HttpMethod.GET, entity,
					UserInfoResponse.class, userId);

			System.out.println("Received response from User Service for user ID: " + userId);

			return response.getBody();

		} catch (RestClientException e) {
			throw new RuntimeException("Failed to fetch user from user service", e);
		}
	}
	
	public UserInfoResponse getUser(String authorizationToken, String dcCookie) {
		try {
			HttpHeaders headers = new HttpHeaders();
			headers.set(HttpHeaders.AUTHORIZATION, authorizationToken);
			headers.add(HttpHeaders.COOKIE, "dcl=" + dcCookie);
			headers.setAccept(Collections.singletonList(MediaType.APPLICATION_JSON));
			headers.setContentType(MediaType.APPLICATION_JSON);
			
			System.out.println("Pre " + dcCookie +" "+authorizationToken);

			HttpEntity<Void> entity = new HttpEntity<>(headers);

			logger.info("Fetching user from User Service");

			ResponseEntity<UserInfoResponse> response = restTemplate.exchange(GET_USER_URL, HttpMethod.GET, entity,
					UserInfoResponse.class);

			System.out.println("Received response from User Service for user ID: ");

			return response.getBody();

		} catch (RestClientException e) {
			throw new RuntimeException("Failed to fetch user from user service", e);
		}
	}
	
	

//	@Bean("sslRestTemplate")
//	public RestTemplate sslRestTemplate() throws Exception {
//
//		SSLContext sslContext = SSLContextBuilder.create().loadTrustMaterial(new TrustSelfSignedStrategy()).build();
//
//		SSLConnectionSocketFactory csf = new SSLConnectionSocketFactory(sslContext, new NoopHostnameVerifier());
//
//		CloseableHttpClient httpClient = HttpClients.custom().setSSLSocketFactory(csf).build();
//
//		HttpComponentsClientHttpRequestFactory requestFactory = new HttpComponentsClientHttpRequestFactory(httpClient);
//
//		return new RestTemplate(requestFactory);
//	}
	@Bean("sslRestTemplate")
	public RestTemplate sslRestTemplate() throws Exception {
	    return new RestTemplate();
	}

}
