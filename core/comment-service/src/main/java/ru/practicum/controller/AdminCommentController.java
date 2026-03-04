package ru.practicum.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import ru.practicum.comment.dto.CommentFullDto;
import ru.practicum.service.CommentService;

@Validated
@RestController
@RequiredArgsConstructor
@RequestMapping("/admin/events/{eventId}/comments/{commentId}")
public class AdminCommentController {

    private final CommentService serviceService;

    @PatchMapping
    public ResponseEntity<CommentFullDto> patchComment(@PathVariable Long eventId,
                                                       @PathVariable Long commentId,
                                                       @RequestParam boolean published) {
        CommentFullDto result = serviceService.hide(eventId, commentId, published);
        return ResponseEntity.ok(result);
    }
}