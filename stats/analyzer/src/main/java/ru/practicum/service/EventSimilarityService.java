package ru.practicum.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.practicum.model.EventSimilarity;
import ru.practicum.repository.EventSimilarityRepository;
import ru.practicum.stats.avro.EventSimilarityAvro;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Set;

@Slf4j
@RequiredArgsConstructor
@Service
public class EventSimilarityService {

    private final EventSimilarityRepository repository;

    public void addSimilarity(EventSimilarityAvro value) {

        EventSimilarity eventSimilarity = EventSimilarity.builder()
                .event1(value.getEventA())
                .event2(value.getEventB())
                .similarity(value.getScore())
                .timestamp(LocalDateTime.ofInstant(value.getTimestamp(), ZoneId.systemDefault()))
                .build();

        repository.findByEvent1AndEvent2(eventSimilarity.getEvent1(), eventSimilarity.getEvent2())
                .ifPresent(oldEventSimilarity -> eventSimilarity.setId(oldEventSimilarity.getId()));
        repository.save(eventSimilarity);
    }

    @Transactional(readOnly = true)
    public List<EventSimilarity> findNPairContainsEventIdsSortedDescScore(Set<Long> eventIds, int maxResults) {
        Pageable pageable = PageRequest.of(0, maxResults);
        return repository.findTopByEvent1InOrEvent2InOrderBySimilarityDesc(eventIds, eventIds, pageable);
    }

    @Transactional(readOnly = true)
    public List<EventSimilarity> findAllContainsEventId(long eventId) {
        return repository.findByEvent1OrEvent2(eventId);
    }
}