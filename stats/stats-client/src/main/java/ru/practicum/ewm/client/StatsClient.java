package ru.practicum.ewm.client;

import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;
import ru.practicum.ewm.NewHitDto;
import ru.practicum.ewm.ReqStatsParams;
import ru.practicum.ewm.StatsDto;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Component
public class StatsClient {

    private final RestTemplate restTemplate;

    private static final String STATS_SERVICE_NAME = "stats-server";

    @Autowired
    public StatsClient(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
        log.info("StatsClient инициализирован для сервиса: {}", STATS_SERVICE_NAME);
    }

    public void hit(HttpServletRequest eventRequest) {
        log.debug("Метод hit(): {}", eventRequest);

        try {
            NewHitDto hitDto = NewHitDto.builder()
                    .app("ewm-main-service")
                    .ip(eventRequest.getRemoteAddr())
                    .uri(eventRequest.getRequestURI())
                    .timestamp(LocalDateTime.now())
                    .build();

            log.debug("Создан hit(): {}", hitDto);
            String url = "http://" + STATS_SERVICE_NAME + "/hit";

            restTemplate.postForObject(url, hitDto, Void.class);
            log.debug("Hit успешно отправлен");

        } catch (Exception e) {
            log.warn("Ошибка при отправке hit: {}", e.getMessage());
        }
    }

    public List<StatsDto> getStats(ReqStatsParams params) {
        log.debug("Метод getStats(): start={}, end={}, uris={}, unique={}",
                params.getStart(), params.getEnd(), params.getUris(), params.isUnique());

        try {
            String baseUrl = "http://" + STATS_SERVICE_NAME;

            UriComponentsBuilder builder = UriComponentsBuilder.fromHttpUrl(baseUrl + "/stats")
                    .queryParam("start", params.getStart())
                    .queryParam("end", params.getEnd());

            if (params.getUris() != null && !params.getUris().isEmpty()) {
                builder.queryParam("uris", String.join(",", params.getUris()));
            }

            builder.queryParam("unique", params.isUnique());

            String url = builder.build().toUriString();
            log.debug("URL для запроса статистики: {}", url);

            HttpEntity<Void> requestEntity = new HttpEntity<>(defaultHeaders());

            ResponseEntity<List<StatsDto>> response = restTemplate.exchange(
                    url,
                    HttpMethod.GET,
                    requestEntity,
                    new ParameterizedTypeReference<List<StatsDto>>() {}
            );

            log.info("Получена статистика: {} записей",
                    response.getBody() != null ? response.getBody().size() : 0);

            return response.getBody();

        } catch (Exception e) {
            log.error("Ошибка при получении статистики: {}", e.getMessage());
            throw new RuntimeException("Не удалось получить статистику", e);
        }
    }

    private HttpHeaders defaultHeaders() {
        HttpHeaders httpHeaders = new HttpHeaders();
        httpHeaders.setContentType(MediaType.APPLICATION_JSON);
        httpHeaders.setAccept(List.of(MediaType.APPLICATION_JSON));
        return httpHeaders;
    }
}