package com.nexis.auth_service.dto.user_profile;

import lombok.*;

import java.util.UUID;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class UserProfileResponseDto {
    private UUID id;
    private String fullname;
    private String email;
    private String avatar;
}
