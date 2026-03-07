package ru.practicum.model;

import jakarta.persistence.*;
import lombok.*;
import ru.practicum.request.enums.RequestStatus;

import java.time.Instant;

@Entity
@Table(name = "requests")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@ToString
public class Request {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @JoinColumn(name = "event_id", foreignKey = @ForeignKey(name = "fk_requests_events"))
    @ToString.Exclude
    private Long eventId;

    @JoinColumn(name = "user_id", foreignKey = @ForeignKey(name = "fk_requests_users"))
    @ToString.Exclude
    private Long requesterId;

    @Column
    @Builder.Default
    private Instant created = Instant.now();

    @Enumerated(EnumType.STRING)
    private RequestStatus status;
}