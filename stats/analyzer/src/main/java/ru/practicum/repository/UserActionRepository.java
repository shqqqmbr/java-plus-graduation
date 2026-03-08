package ru.practicum.repository;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import ru.practicum.model.UserAction;

import java.util.List;
import java.util.Optional;
import java.util.Set;

@Repository
public interface UserActionRepository extends JpaRepository<UserAction, Long> {

    Optional<UserAction> findByUserIdAndEventId(Long userId, Long eventId);

    List<UserAction> findAllByEventIdIn(Set<Long> eventIds);

    List<Long> findDistinctEventIdByUserIdOrderByTimestampDesc(
            Long userId,
            Pageable pageable);

    List<Long> findDistinctEventIdByUserIdAndEventIdIn(Long userId, Set<Long> eventIds);
}