package com.xstream.clouddesktop.client.proxmox;

import com.xstream.clouddesktop.config.ProxmoxProperties;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.client5.http.impl.io.PoolingHttpClientConnectionManagerBuilder;
import org.apache.hc.client5.http.ssl.SSLConnectionSocketFactoryBuilder;
import org.apache.hc.core5.ssl.SSLContextBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;

import javax.net.ssl.SSLContext;
import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;

@Configuration
@Profile("!mock") // Only active when NOT using mock profile
public class ProxmoxClientConfig {

        private final ProxmoxProperties proxmoxProperties;

        public ProxmoxClientConfig(ProxmoxProperties proxmoxProperties) {
                this.proxmoxProperties = proxmoxProperties;
        }

        @Bean(name = "proxmoxRestTemplate")
        public RestTemplate proxmoxRestTemplate()
                        throws NoSuchAlgorithmException, KeyStoreException, KeyManagementException {
                // Trust self-signed certificates
                SSLContext sslContext = SSLContextBuilder.create()
                                .loadTrustMaterial((chain, authType) -> true)
                                .build();

                CloseableHttpClient httpClient = HttpClients.custom()
                                .setConnectionManager(PoolingHttpClientConnectionManagerBuilder.create()
                                                .setSSLSocketFactory(SSLConnectionSocketFactoryBuilder.create()
                                                                .setSslContext(sslContext)
                                                                .setHostnameVerifier((hostname, session) -> true)
                                                                .build())
                                                .build())
                                .build();

                HttpComponentsClientHttpRequestFactory requestFactory = new HttpComponentsClientHttpRequestFactory(
                                httpClient);
                requestFactory.setConnectTimeout((int) Duration.ofSeconds(10).toMillis());
                requestFactory.setConnectionRequestTimeout((int) Duration.ofSeconds(10).toMillis());

                RestTemplate restTemplate = new RestTemplate(requestFactory);

                // Add Authorization header interceptor
                ClientHttpRequestInterceptor interceptor = (request, body, execution) -> {
                        String authHeader = String.format("PVEAPIToken=%s=%s",
                                        proxmoxProperties.getTokenId(),
                                        proxmoxProperties.getTokenSecret());
                        request.getHeaders().add("Authorization", authHeader);
                        return execution.execute(request, body);
                };

                restTemplate.getInterceptors().add(interceptor);

                return restTemplate;
        }
}
