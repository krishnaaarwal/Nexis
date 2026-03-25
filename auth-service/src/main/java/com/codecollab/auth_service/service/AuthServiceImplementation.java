package com.codecollab.auth_service.service;

import com.codecollab.auth_service.dto.login.LoginRequestDto;
import com.codecollab.auth_service.dto.login.LoginResponseDto;
import com.codecollab.auth_service.dto.signup.SignupRequestDto;
import com.codecollab.auth_service.dto.signup.SignupResponseDto;
import com.codecollab.auth_service.entity.UserEntity;
import com.codecollab.auth_service.repository.UserRepository;
import com.codecollab.auth_service.util.AuthUtil;
import lombok.RequiredArgsConstructor;
import org.modelmapper.ModelMapper;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class AuthServiceImplementation implements AuthService{

    private final AuthenticationManager authenticationManager;
    private final AuthUtil authUtil;
    private final PasswordEncoder passwordEncoder;
    private final UserRepository userRepository;

    //Controller
    @Override
    public SignupResponseDto signup(SignupRequestDto body) {
        UserEntity user = signupInternal(body);
        return SignupResponseDto.builder()
                .id(user.getId())
                .email(user.getEmail())
                .build();
    }


    //INTERNAL SIGNUP!!
    @Transactional
    public UserEntity signupInternal(SignupRequestDto requestDto) {
        //1. Check if user is already account or not
        UserEntity user = userRepository.findByEmail(requestDto.getEmail()).orElse(null);

        if(user!=null)
            throw new IllegalArgumentException("User already exists");

        //2. Create new User
        user = UserEntity.builder()
                .email(requestDto.getEmail())
                .fullname(requestDto.getFullname())
                .createdAt(LocalDateTime.now())
                .build();

        //3. Encode Password
        if(requestDto.getPassword() != null){
            user.setPassword(passwordEncoder.encode(requestDto.getPassword()));
        }

        //4. Save and return
        return userRepository.save(user);
    }

    @Override
    public LoginResponseDto login(LoginRequestDto requestDto) {
        // 1. AuthenticationManager delegates to AuthenticationProvider
        Authentication authentication = authenticationManager.
                authenticate(new UsernamePasswordAuthenticationToken(requestDto.getEmail(), requestDto.getPassword()));

        //Principals() -> Username and details
        //Credentials() -> Password
        //Details() -> session id and ip address

        // Actually, AuthenticationManager → ProviderManager → DaoAuthenticationProvider
        // DaoAuthenticationProvider uses UserDetailsService + PasswordEncoder

        // 2. Principal is the authenticated user (UserDetails implementation)
        UserEntity userEntity = (UserEntity) authentication.getPrincipal();

        //3. Generate Access token for it
        String token = authUtil.generateAccessToken(userEntity);

        return LoginResponseDto.builder()
                .id(userEntity.getId())
                .email(userEntity.getEmail())
                .jwt(token)
                .build();
    }

    @Override
    public void deleteAccount(LoginRequestDto requestDto) {

    }

    @Override
    public void logout(LoginRequestDto requestDto) {

    }
}
