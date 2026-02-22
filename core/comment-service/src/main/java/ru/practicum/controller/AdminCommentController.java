package ru.practicum.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import ru.practicum.comment.dto.CommentFullDto;
import ru.practicum.service.CommentService;

@Slf4j
@Validated
@RestController
@RequiredArgsConstructor
@RequestMapping("/admin/events/{eventId}/comments/{commentId}")
public class AdminCommentController {

    private final CommentService commentService;

    @PatchMapping
    public ResponseEntity<CommentFullDto> patchComment(@PathVariable Long eventId,
                                                       @PathVariable Long commentId,
                                                       @RequestParam boolean published) {
        log.info("Метод patchComment(); eventId={}, commentId={}", eventId, commentId);

        CommentFullDto result = commentService.hide(eventId, commentId, published);
        return ResponseEntity.ok(result);
    }
}