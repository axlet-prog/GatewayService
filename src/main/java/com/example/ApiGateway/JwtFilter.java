package com.example.ApiGateway;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.factory.AbstractGatewayFilterFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

@Component
public class JwtFilter extends AbstractGatewayFilterFactory<JwtFilter.Config> {
    private final WebClient webClient;

    @Value("${spring.cloud.gateway.routes[0].uri}")
    private String authServiceUrl;

    @Value("${values.auth_endpoint}")
    private String authEndpoint;

    public JwtFilter(WebClient.Builder webClientBuilder) {
        super(Config.class);
        this.webClient = webClientBuilder.build();
    }

    @Override
    public GatewayFilter apply(Config config) {
        return (exchange, chain) -> {

            ServerHttpRequest request = exchange.getRequest();
            String path = request.getURI().getPath();
            String methodType = request.getMethod().name().toLowerCase();

            System.out.println(config.getAuthorizedEndpoints());
            boolean requiresAuth = config.getAuthorizedEndpoints().stream()
                    .anyMatch(endpoint ->
                            Pattern.matches(endpoint.getPathPattern(), path) &&
                                    endpoint.getMethodType().toLowerCase().equals(methodType)
                    );

            System.out.println(path);
            System.out.println(methodType);

            if (!requiresAuth) {
                return chain.filter(exchange);
            }

            List<String> headers = request.getHeaders().getOrEmpty("Authorization");
            if (headers.isEmpty() || !headers.get(0).startsWith("Bearer ")) {
                return unauthorized(exchange);
            }

            String token = headers.get(0).substring("Bearer ".length());
            String rolesCsv = config.getRoles().toString().replace("[", "").replace("]", "").replace(" ", "");
            System.out.println(authServiceUrl + authEndpoint + "?role=" + rolesCsv);
            return webClient.post()
                    .uri(authServiceUrl + authEndpoint + "?role=" + rolesCsv)
                    .header("Authorization", "Bearer " + token)
                    .retrieve()
                    .onStatus((code) -> code.value() == 401, clientResponse -> Mono.error(new RuntimeException("Unautorized")))
                    .bodyToMono(Void.class)
                    .then(Mono.defer(() -> chain.filter(exchange)))
                    .onErrorResume(e -> unauthorized(exchange));
        };
    }

    private Mono<Void> unauthorized(ServerWebExchange exchange) {
        exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
        return exchange.getResponse().setComplete();
    }

    public static class Config {
        private List<Endpoint> authorizedEndpoints = new ArrayList<>();
        private List<String> roles;

        public List<Endpoint> getAuthorizedEndpoints() {
            return authorizedEndpoints;
        }

        public void setAuthorizedEndpoints(List<Endpoint> authorizedEndpoints) {
            this.authorizedEndpoints = authorizedEndpoints;
        }

        public List<String> getRoles() {
            return roles;
        }

        public void setRoles(List<String> roles) {
            this.roles = roles;
        }
    }
}
