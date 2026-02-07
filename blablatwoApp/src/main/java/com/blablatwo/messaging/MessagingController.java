package com.blablatwo.messaging;

import com.blablatwo.auth.AppPrincipal;
import com.blablatwo.messaging.dto.ConversationOpenRequest;
import com.blablatwo.messaging.dto.ConversationResponseDto;
import com.blablatwo.messaging.dto.MessageDto;
import com.blablatwo.messaging.dto.SendMessageRequest;
import jakarta.validation.Valid;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/conversations")
public class MessagingController {

    private final ConversationService conversationService;

    public MessagingController(ConversationService conversationService) {
        this.conversationService = conversationService;
    }

    @PostMapping("/open")
    public ResponseEntity<ConversationResponseDto> openConversation(
            @Valid @RequestBody ConversationOpenRequest request,
            @AuthenticationPrincipal AppPrincipal principal) {

        ConversationService.OpenResult result = conversationService.openConversation(request, principal.userId());

        if (result.created()) {
            return ResponseEntity
                    .created(URI.create("/conversations/" + result.conversation().id()))
                    .body(result.conversation());
        }
        return ResponseEntity.ok(result.conversation());
    }

    @GetMapping
    public ResponseEntity<List<ConversationResponseDto>> listConversations(
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant since,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @AuthenticationPrincipal AppPrincipal principal) {

        Pageable pageable = PageRequest.of(page, size);
        return ResponseEntity.ok(conversationService.listConversations(principal.userId(), since, pageable));
    }

    @GetMapping("/{conversationId}/messages")
    public ResponseEntity<List<MessageDto>> getMessages(
            @PathVariable UUID conversationId,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant before,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant since,
            @RequestParam(defaultValue = "50") int limit,
            @AuthenticationPrincipal AppPrincipal principal) {

        int effectiveLimit = Math.min(limit, 100);
        return ResponseEntity.ok(conversationService.getMessages(
                conversationId, principal.userId(), before, since, effectiveLimit));
    }

    @PostMapping("/{conversationId}/messages")
    public ResponseEntity<MessageDto> sendMessage(
            @PathVariable UUID conversationId,
            @Valid @RequestBody SendMessageRequest request,
            @AuthenticationPrincipal AppPrincipal principal) {

        MessageDto message = conversationService.sendMessage(conversationId, request, principal.userId());
        return ResponseEntity
                .created(URI.create("/conversations/" + conversationId + "/messages/" + message.id()))
                .body(message);
    }
}
