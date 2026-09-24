package ue.edu.co.splitbill.dto;

import java.time.Instant;
import java.util.UUID;

import ue.edu.co.splitbill.entity.Group;

public record GroupResponse(UUID id, String name, String currency, UUID ownerId,
                            Instant createdAt, Instant updatedAt) {

    public static GroupResponse from(Group group) {
        return new GroupResponse(group.getId(), group.getName(), group.getCurrency(), group.getOwnerId(),
                group.getCreatedAt(), group.getUpdatedAt());
    }
}
