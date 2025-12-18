package com.itcast.config;

import org.apache.http.HttpHost;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.apache.http.impl.nio.reactor.IOReactorConfig;
import org.elasticsearch.client.RestClient;
import org.elasticsearch.client.RestClientBuilder;
import org.elasticsearch.client.RestHighLevelClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;

/**
 * elasticsearch配置类
 */
@Configuration
public class EsConfig {

    @Value("${spring.elasticsearch.uris:}")
    private String uris;

    @Value("${spring.elasticsearch.username:}")
    private String username;

    @Value("${spring.elasticsearch.password:}")
    private String password;

    @Value("${es.host:localhost}")
    private String host;

    @Value("${es.port:9200}")
    private int port;

    @Value("${es.scheme:http}")
    private String scheme;

    @Bean
    public RestHighLevelClient client(){
        HttpHost[] hosts = buildHosts();
        RestClientBuilder builder = RestClient.builder(hosts)
                .setRequestConfigCallback(requestConfigBuilder -> requestConfigBuilder
                        .setConnectTimeout(5_000)
                        .setSocketTimeout(15_000)
                        .setConnectionRequestTimeout(5_000)
                )
                .setHttpClientConfigCallback(httpClientBuilder -> {
                    httpClientBuilder.setMaxConnTotal(100);
                    httpClientBuilder.setMaxConnPerRoute(100);
                    httpClientBuilder.setDefaultIOReactorConfig(
                            IOReactorConfig.custom()
                                    .setSoKeepAlive(true)
                                    .build()
                    );

                    if (username != null && !username.trim().isEmpty()) {
                        CredentialsProvider credentialsProvider = new BasicCredentialsProvider();
                        credentialsProvider.setCredentials(
                                AuthScope.ANY,
                                new UsernamePasswordCredentials(username.trim(), password == null ? "" : password)
                        );
                        httpClientBuilder.setDefaultCredentialsProvider(credentialsProvider);
                    }

                    return httpClientBuilder;
                });

        return new RestHighLevelClient(builder);
    }

    private HttpHost[] buildHosts() {
        List<HttpHost> hostList = new ArrayList<>();
        if (uris != null && !uris.trim().isEmpty()) {
            String[] parts = uris.split(",");
            for (String part : parts) {
                String value = part == null ? "" : part.trim();
                if (value.isEmpty()) continue;
                URI uri = URI.create(value);
                String uriScheme = uri.getScheme() != null ? uri.getScheme() : scheme;
                String uriHost = uri.getHost();
                int uriPort = uri.getPort() > 0 ? uri.getPort() : port;
                hostList.add(new HttpHost(uriHost, uriPort, uriScheme));
            }
        }

        if (hostList.isEmpty()) {
            hostList.add(new HttpHost(host, port, scheme));
        }

        return hostList.toArray(new HttpHost[0]);
    }
}
