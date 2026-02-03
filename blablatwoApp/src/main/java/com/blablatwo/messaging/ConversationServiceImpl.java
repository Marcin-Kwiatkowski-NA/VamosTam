package com.blablatwo.messaging;

import com.blablatwo.exceptions.NoSuchRideException;
import com.blablatwo.exceptions.NoSuchTravelerException;
import com.blablatwo.messaging.dto.ConversationDto;
import com.blablatwo.messaging.dto.CreateConversationRequest;
import com.blablatwo.messaging.dto.MessageDto;
import com.blablatwo.messaging.dto.SendMessageRequest;
import com.blablatwo.messaging.event.MessageCreatedEvent;
import com.blablatwo.messaging.exception.ConversationNotFoundException;
import com.blablatwo.messaging.exception.ExternalRideException;
import com.blablatwo.messaging.exception.InvalidDriverException;
import com.blablatwo.messaging.exception.NotBookedOnRideException;
import com.blablatwo.messaging.exception.NotParticipantException;
import com.blablatwo.messaging.exception.SelfConversationException;
import com.blablatwo.ride.Ride;
import com.blablatwo.ride.RideRepository;
import com.blablatwo.ride.RideSource;
import com.blablatwo.traveler.Traveler;
import com.blablatwo.traveler.TravelerRepository;
import jakarta.persistence.OptimisticLockException;
import org.hibernate.StaleObjectStateException;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.function.Supplier;

@Service
public class ConversationServiceImpl implements ConversationService {

    private final ConversationRepository conversationRepository;
    private final MessageRepository messageRepository;
    private final RideRepository rideRepository;
    private final TravelerRepository travelerRepository;
    private final ConversationMapper conversationMapper;
    private final MessageMapper messageMapper;
    private final ApplicationEventPublisher eventPublisher;

    public ConversationServiceImpl(ConversationRepository conversationRepository,
                                   MessageRepository messageRepository,
                                   RideRepository rideRepository,
                                   TravelerRepository travelerRepository,
                                   ConversationMapper conversationMapper,
                                   MessageMapper messageMapper,
                                   ApplicationEventPublisher eventPublisher) {
        this.conversationRepository = conversationRepository;
        this.messageRepository = messageRepository;
        this.rideRepository = rideRepository;
        this.travelerRepository = travelerRepository;
        this.conversationMapper = conversationMapper;
        this.messageMapper = messageMapper;
        this.eventPublisher = eventPublisher;
    }

    @Override
    @Transactional
    public InitResult initConversation(CreateConversationRequest request, Long passengerId) {
        Ride ride = rideRepository.findById(request.rideId())
                .orElseThrow(() -> new NoSuchRideException(request.rideId()));

        if (ride.getSource() != RideSource.INTERNAL) {
            throw new ExternalRideException();
        }

        if (!ride.getDriver().getId().equals(request.driverId())) {
            throw new InvalidDriverException(request.driverId(), ride.getDriver().getId());
        }

        if (passengerId.equals(request.driverId())) {
            throw new SelfConversationException();
        }

        if (!isPassengerBookedOnRide(request.rideId(), passengerId)) {
            throw new NotBookedOnRideException(request.rideId(), passengerId);
        }

        var existingConversation = conversationRepository
                .findByRideIdAndDriverIdAndPassengerId(request.rideId(), request.driverId(), passengerId);

        if (existingConversation.isPresent()) {
            return new InitResult(toConversationDto(existingConversation.get(), passengerId), false);
        }

        Traveler passenger = travelerRepository.findById(passengerId)
                .orElseThrow(() -> new NoSuchTravelerException(passengerId));

        try {
            Conversation conversation = Conversation.builder()
                    .ride(ride)
                    .driver(ride.getDriver())
                    .passenger(passenger)
                    .build();

            Conversation saved = conversationRepository.save(conversation);
            return new InitResult(toConversationDto(saved, passengerId), true);
        } catch (DataIntegrityViolationException e) {
            // Race condition: another request created the conversation
            var created = conversationRepository
                    .findByRideIdAndDriverIdAndPassengerId(request.rideId(), request.driverId(), passengerId)
                    .orElseThrow(() -> e);
            return new InitResult(toConversationDto(created, passengerId), false);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<ConversationDto> listConversations(Long travelerId, Instant since, Pageable pageable) {
        List<Conversation> conversations;

        if (since != null) {
            conversations = conversationRepository.findByParticipantIdAndUpdatedAtAfter(travelerId, since, pageable);
        } else {
            conversations = conversationRepository.findByParticipantId(travelerId, pageable);
        }

        return conversations.stream()
                .map(c -> toConversationDto(c, travelerId))
                .toList();
    }

    @Override
    @Transactional
    public List<MessageDto> getMessages(UUID conversationId, Long travelerId, Instant before, Instant since, int limit) {
        Conversation conversation = conversationRepository.findById(conversationId)
                .orElseThrow(() -> new ConversationNotFoundException(conversationId));

        validateParticipant(conversation, travelerId);

        List<Message> messages;
        Pageable pageable = PageRequest.of(0, limit);

        if (since != null) {
            // Polling for new messages - limit to prevent abuse after long offline
            messages = messageRepository.findByConversationIdAndCreatedAtAfterOrderByCreatedAtAsc(
                    conversationId, since, pageable);
        } else if (before != null) {
            // Cursor-based "load older" - DESC, then reverse
            messages = messageRepository.findByConversationIdAndCreatedAtBeforeOrderByCreatedAtDesc(
                    conversationId, before, pageable);
            messages = new ArrayList<>(messages);
            Collections.reverse(messages);
        } else {
            // Initial load - DESC, then reverse for display order
            messages = messageRepository.findByConversationIdOrderByCreatedAtDesc(conversationId, pageable);
            messages = new ArrayList<>(messages);
            Collections.reverse(messages);
        }

        // Reset viewer's unread count (opening thread marks all as read)
        resetUnreadCount(conversation, travelerId);

        return messages.stream()
                .map(m -> toMessageDto(m, travelerId))
                .toList();
    }

    @Override
    public MessageDto sendMessage(UUID conversationId, SendMessageRequest request, Long senderId) {
        // Validation (read-only) happens BEFORE transaction to avoid wasting retries
        Conversation conversation = conversationRepository.findById(conversationId)
                .orElseThrow(() -> new ConversationNotFoundException(conversationId));

        validateParticipant(conversation, senderId);

        // Execute the write operation with manual retry
        return executeWithRetry(() ->
                performSendMessage(conversationId, request, senderId)
        );
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    protected MessageDto performSendMessage(UUID conversationId, SendMessageRequest request, Long senderId) {
        // Re-fetch inside transaction to get latest version
        Conversation conversation = conversationRepository.findById(conversationId)
                .orElseThrow(() -> new ConversationNotFoundException(conversationId));

        Message message = Message.builder()
                .conversation(conversation)
                .sender(travelerRepository.getReferenceById(senderId))
                .body(request.body())
                .build();

        Message savedMessage = messageRepository.save(message);

        // Update conversation denormalized fields
        conversation.setLastMessageId(savedMessage.getId());
        conversation.setLastMessageBody(savedMessage.getBody());
        conversation.setLastMessageCreatedAt(savedMessage.getCreatedAt());
        conversation.setLastMessageSenderId(senderId);

        // Update unread counts
        boolean isDriver = conversation.getDriver().getId().equals(senderId);
        if (isDriver) {
            conversation.setPassengerUnreadCount(conversation.getPassengerUnreadCount() + 1);
            conversation.setDriverUnreadCount(0);
        } else {
            conversation.setDriverUnreadCount(conversation.getDriverUnreadCount() + 1);
            conversation.setPassengerUnreadCount(0);
        }

        conversationRepository.save(conversation);

        // Publish event (listener fires after commit)
        eventPublisher.publishEvent(new MessageCreatedEvent(
                savedMessage.getId(),
                conversationId,
                senderId,
                savedMessage.getCreatedAt()
        ));

        return new MessageDto(
                savedMessage.getId(),
                conversationId,
                senderId,
                true,
                savedMessage.getBody(),
                savedMessage.getCreatedAt()
        );
    }

    private <T> T executeWithRetry(Supplier<T> action) {
        int maxAttempts = 3;
        for (int attempt = 0; attempt < maxAttempts; attempt++) {
            try {
                return action.get();
            } catch (OptimisticLockException | StaleObjectStateException e) {
                if (attempt == maxAttempts - 1) {
                    throw e;
                }
            }
        }
        throw new IllegalStateException("Retry loop exited without result");
    }

    private void validateParticipant(Conversation conversation, Long travelerId) {
        boolean isDriver = conversation.getDriver().getId().equals(travelerId);
        boolean isPassenger = conversation.getPassenger().getId().equals(travelerId);
        if (!isDriver && !isPassenger) {
            throw new NotParticipantException(conversation.getId(), travelerId);
        }
    }

    private boolean isPassengerBookedOnRide(Long rideId, Long passengerId) {
        return rideRepository.existsByIdAndPassengers_Id(rideId, passengerId);
    }

    private void resetUnreadCount(Conversation conversation, Long travelerId) {
        boolean isDriver = conversation.getDriver().getId().equals(travelerId);
        if (isDriver) {
            conversation.setDriverUnreadCount(0);
        } else {
            conversation.setPassengerUnreadCount(0);
        }
        // JPA flushes dirty entities on commit - no explicit save needed
    }

    private ConversationDto toConversationDto(Conversation conversation, Long viewerId) {
        ConversationDto baseDto = conversationMapper.toDto(conversation);

        // Build lastMessage from denormalized fields if present
        MessageDto lastMessage = null;
        if (conversation.getLastMessageId() != null) {
            lastMessage = new MessageDto(
                    conversation.getLastMessageId(),
                    conversation.getId(),
                    conversation.getLastMessageSenderId(),
                    conversation.getLastMessageSenderId().equals(viewerId),
                    conversation.getLastMessageBody(),
                    conversation.getLastMessageCreatedAt()
            );
        }

        // Determine unread count based on viewer role
        boolean isDriver = conversation.getDriver().getId().equals(viewerId);
        int unreadCount = isDriver
                ? conversation.getDriverUnreadCount()
                : conversation.getPassengerUnreadCount();

        return new ConversationDto(
                baseDto.id(),
                baseDto.rideId(),
                baseDto.driverId(),
                baseDto.driverName(),
                baseDto.passengerId(),
                baseDto.passengerName(),
                baseDto.originName(),
                baseDto.destinationName(),
                lastMessage,
                unreadCount,
                baseDto.updatedAt()
        );
    }

    private MessageDto toMessageDto(Message message, Long viewerId) {
        MessageDto baseDto = messageMapper.toDto(message);
        return new MessageDto(
                baseDto.id(),
                baseDto.conversationId(),
                baseDto.senderId(),
                message.getSender().getId().equals(viewerId),
                baseDto.body(),
                baseDto.createdAt()
        );
    }
}
