package com.blablatwo.dto;

import com.blablatwo.domain.AbstractExternalMeta;
import com.blablatwo.domain.AbstractTrip;
import com.blablatwo.ride.RideSource;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
public class ContactMethodFactory {

    public List<ContactMethodDto> buildContactMethods(AbstractTrip trip, AbstractExternalMeta meta,
                                                       String internalPhone) {
        List<ContactMethodDto> contacts = new ArrayList<>();
        if (trip.getSource() == RideSource.FACEBOOK) {
            if (meta != null && meta.getSourceUrl() != null) {
                contacts.add(new ContactMethodDto(ContactType.FACEBOOK_LINK, meta.getSourceUrl()));
            }
            if (meta != null && meta.getPhoneNumber() != null && !meta.getPhoneNumber().isBlank()) {
                contacts.add(new ContactMethodDto(ContactType.PHONE, meta.getPhoneNumber()));
            }
        } else {
            if (internalPhone != null && !internalPhone.isBlank()) {
                contacts.add(new ContactMethodDto(ContactType.PHONE, internalPhone));
            }
        }
        return contacts;
    }
}
