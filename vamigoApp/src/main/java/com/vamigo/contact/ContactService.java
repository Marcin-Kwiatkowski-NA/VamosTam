package com.vamigo.contact;

import com.vamigo.contact.dto.ContactRequest;

public interface ContactService {

    void submitContactForm(Long userId, ContactRequest request);
}
