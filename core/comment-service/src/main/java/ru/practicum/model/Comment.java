package ru.practicum.model;

import jakarta.persistence.*;
import lombok.*;
import ru.practicum.comment.enums.CommentState;

import java.time.Instant;

@Entity
@Table(name = "comments")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@ToString
public class Comment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(length = 300,
            nullable = false)
    private String annotation;

    @Column(length = 3000,
            nullable = false)
    private String text;

    @Column(name = "published_on",
            nullable = false)
    @Builder.Default
    private Instant publishedOn = Instant.now();

    @Column(name = "author_id", nullable = false)
    private Long authorId;

    @Column(name = "event_id", nullable = false)
    private Long eventId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private CommentState state = CommentState.PUBLIC;
}