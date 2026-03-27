package com.nexis.auth_service.service.service_implementations;

import com.nexis.auth_service.config.type.ProviderType;
import com.nexis.auth_service.dto.login.LoginRequestDto;
import com.nexis.auth_service.dto.login.LoginResponseDto;
import com.nexis.auth_service.dto.logout.LogoutRequestDto;
import com.nexis.auth_service.dto.refreshtoken.RefreshTokenRequestDto;
import com.nexis.auth_service.dto.signup.SignupRequestDto;
import com.nexis.auth_service.dto.signup.SignupResponseDto;
import com.nexis.auth_service.dto.user_profile.UserProfileResponseDto;
import com.nexis.auth_service.entity.RefreshTokenEntity;
import com.nexis.auth_service.entity.UserEntity;
import com.nexis.auth_service.exception.RefreshTokenNotFoundException;
import com.nexis.auth_service.repository.UserRepository;
import com.nexis.auth_service.security.user_principal.UserPrincipal;
import com.nexis.auth_service.service.AuthService;
import com.nexis.auth_service.util.AuthUtil;
import com.nexis.auth_service.util.RefreshTokenUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AuthServiceImplementation implements AuthService {

    private final AuthenticationManager authenticationManager;
    private final AuthUtil authUtil;
    private final RefreshTokenUtil refreshTokenUtil;
    private final PasswordEncoder passwordEncoder;
    private final UserRepository userRepository;

    //Controller
    @Override
    public SignupResponseDto signup(SignupRequestDto body) {
        UserEntity user = signupInternal(body,ProviderType.EMAIL,null,null);
        return SignupResponseDto.builder()
                .id(user.getId())
                .email(user.getEmail())
                .build();
    }


    //INTERNAL SIGNUP!!
    @Transactional
    public UserEntity signupInternal(SignupRequestDto requestDto, ProviderType providerType, String providerId,String avatarUrl) {
        //1. Check if user is already account or not
        UserEntity user = userRepository.findByEmail(requestDto.getEmail()).orElse(null);

        if(user!=null)
            throw new IllegalArgumentException("User already exists");

        String finalAvatar = (avatarUrl != null && !avatarUrl.isBlank())
                ? avatarUrl
                : "https://api.dicebear.com/9.x/identicon/svg?seed=fallback";

        //2. Create new User
        user = UserEntity.builder()
                .email(requestDto.getEmail())
                .fullname(requestDto.getFullname())
                .providerType(providerType)
                .providerId(providerId)
                .avatar(finalAvatar)
                .createdAt(LocalDateTime.now())
                .build();

        //3. Encode Password
        if(providerType == ProviderType.EMAIL && requestDto.getPassword() != null){
            user.setPassword(passwordEncoder.encode(requestDto.getPassword()));
        }

        //4. Save and return
        return userRepository.save(user);
    }

    @Override
    @Transactional
    public LoginResponseDto login(LoginRequestDto requestDto) {
        Authentication authentication = authenticationManager.
                authenticate(new UsernamePasswordAuthenticationToken(requestDto.getEmail(), requestDto.getPassword()));

        // 1. Cast to UserPrincipal instead of UserEntity
        UserPrincipal principal = (UserPrincipal) authentication.getPrincipal();

        // 2. Use the escape hatch to get the entity
        UserEntity userEntity = principal.getUserEntity();

        RefreshTokenEntity refreshToken = refreshTokenUtil.generateRefreshToken(userEntity.getId());
        String token = authUtil.generateAccessToken(userEntity);

        return LoginResponseDto.builder()
                .id(userEntity.getId())
                .email(userEntity.getEmail())
                .jwt(token)
                .refreshToken(refreshToken.getToken())
                .build();
    }

    @Override
    @Transactional
    public ResponseEntity<LoginResponseDto> handleOauth2LoginRequest(OAuth2User oAuth2User, String registrationId) {
        // Find Provider type and id
        //save the provider type and id Info with user
        //If user has an account -> directly login
        // if not -> signup -> login

        ProviderType providerType = authUtil.getProviderTypeFromRegistrationId(registrationId);
        String providerId = authUtil.determineProviderIdFromOauth2User(oAuth2User,registrationId);

        String oauthAvatarUrl = authUtil.getAvatarFromOauth2User(oAuth2User, registrationId);

        UserEntity user = userRepository.findByProviderIdAndProviderType(providerId,providerType).orElse(null);

        String email = oAuth2User.getAttribute("email");

        UserEntity emailUser = userRepository.findByEmail(email).orElse(null);

        if(user == null && emailUser == null){
            //signup flow:
            String emailSignup = authUtil.determineEmailFromOauth2User(oAuth2User,registrationId,providerId);

            user = signupInternal(new SignupRequestDto(emailSignup, null, null),providerType,providerId,oauthAvatarUrl);
        } else if (user!=null) {
            if(email!=null && !email.isBlank() && !email.equals(user.getEmail())){
                user.setEmail(email);
                userRepository.save(user);
            }
        }
        else {
            throw new BadCredentialsException("This email is already registered with provider : "+emailUser.getProviderType());
        }

        LoginResponseDto loginResponseDto = new LoginResponseDto(user.getId(), user.getEmail(), authUtil.generateAccessToken(user),refreshTokenUtil.generateRefreshToken(user.getId()).getToken());

        return ResponseEntity.ok(loginResponseDto);
    }


    @Override
    @Transactional
    public void logout(LogoutRequestDto requestDto) {
        String token = requestDto.getRefreshToken();

        refreshTokenUtil.deleteByToken(token);
    }

    @Transactional
    @Override
    public LoginResponseDto refreshToken(RefreshTokenRequestDto body) {
        RefreshTokenEntity refreshToken = refreshTokenUtil.findToken(body.getToken())
                .orElseThrow(() -> new RefreshTokenNotFoundException("Refresh token not found: " + body.getToken()));

        refreshToken = refreshTokenUtil.verifyAndRotate(refreshToken);
        UUID userId = refreshToken.getUserId();
        UserEntity user = userRepository.getById(userId);
        String accessToken = authUtil.generateAccessToken(user);

        return LoginResponseDto.builder()
                .jwt(accessToken)
                .refreshToken(refreshToken.getToken())
                .id(user.getId())
                .email(user.getEmail())
                .build();
    }

    @Transactional
    @Override
    public UserProfileResponseDto getCurrentUserProfile(){
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();

        // 1. It is ALWAYS a UserPrincipal now!
        UserPrincipal principal = (UserPrincipal) auth.getPrincipal();

        // 2. Grab the entity
        UserEntity user = principal.getUserEntity();

        return UserProfileResponseDto.builder()
                .id(user.getId())
                .fullname(user.getFullname())
                .email(user.getEmail())
                .avatar(user.getAvatar())
                .build();
    }


}
