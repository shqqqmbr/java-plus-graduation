package ru.practicum.repository;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import ru.practicum.model.EventSimilarity;

import java.util.List;
import java.util.Optional;
import java.util.Set;

public interface EventSimilarityRepository extends JpaRepository<EventSimilarity, Long> {

    Optional<EventSimilarity> findByEvent1AndEvent2(Long eventIdA, Long eventIdB);

    List<EventSimilarity> findTopByEvent1InOrEvent2InOrderBySimilarityDesc(
            Set<Long> event1Ids,
            Set<Long> event2Ids,
            Pageable pageable);

    @Query("SELECT e FROM EventSimilarity e WHERE e.event1 = :eventId OR e.event2 = :eventId")
    List<EventSimilarity> findByEvent1OrEvent2(@Param("eventId") Long eventId);
}