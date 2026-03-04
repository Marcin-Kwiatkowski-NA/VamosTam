package com.vamigo.contact;

import com.vamigo.auth.AppPrincipal;
import com.vamigo.contact.dto.ContactRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class ContactController {

    private final ContactService contactService;

    public ContactController(ContactService contactService) {
        this.contactService = contactService;
    }

    @PostMapping("/contact")
    public ResponseEntity<Void> submitContactForm(
            @Valid @RequestBody ContactRequest request,
            @AuthenticationPrincipal AppPrincipal principal) {
        contactService.submitContactForm(principal.userId(), request);
        return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
    }
}
