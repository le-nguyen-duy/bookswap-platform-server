package com.example.bookswapplatform.security.firebase.service.impl;

import com.example.bookswapplatform.dto.BaseResponseDTO;
import com.example.bookswapplatform.entity.User.User;
import com.example.bookswapplatform.repository.RoleRepository;
import com.example.bookswapplatform.repository.UserRepository;
import com.example.bookswapplatform.security.firebase.service.UserManagementService;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthException;
import com.google.firebase.auth.UserRecord;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

@Service
@RequiredArgsConstructor
public class UserManagementServiceImpl implements UserManagementService {
    private final FirebaseAuth firebaseAuth;
    private final RoleRepository roleRepository;
    private final UserRepository userRepository;
    @Override
    public ResponseEntity<BaseResponseDTO> setUserClaims(String uid) throws FirebaseAuthException {
        UserRecord userRecord = firebaseAuth.getUser(uid);
        //kiểm tra user đã tồn tại trong hệ thống chưa
        if(userRepository.findByEmail(userRecord.getEmail()).isEmpty()) {
            //lưu user đăng nhập từ google vào database
            User user = new User();
            user.setEmail(userRecord.getEmail());
            user.setProvider(Arrays.toString(userRecord.getProviderData()));
            user.setImage(userRecord.getPhotoUrl());
            user.setFireBaseUid(uid);
            user.setRole(roleRepository.findByName("USER"));
            user.setEnable(true);
            user.setPhone(null);
            userRepository.save(user);
            //set claims cho idToken
            Map<String, Object> claims = convertAuthoritiesToClaims(user.getAuthorities());
            firebaseAuth.setCustomUserClaims(uid, claims);
        }
        return ResponseEntity.ok(new BaseResponseDTO(LocalDateTime.now(), HttpStatus.CREATED, "Successfully"));

        //revoked idToken để client dùng refesh token tạo 1 idToken mới có chứa claims

    }

    //Chuyển danh sách authorities thành claims
    public Map<String, Object> convertAuthoritiesToClaims(Collection<? extends GrantedAuthority> authorities) {
        Map<String, Object> claims = new HashMap<>();
        List<String> authorityValues = new ArrayList<>();
        String role = null;

        for (GrantedAuthority authority : authorities) {
            String authorityValue = authority.getAuthority();

            if (authorityValue.startsWith("ROLE_")) {
                role = authorityValue;
            } else {
                authorityValues.add(authorityValue);
            }
        }

        claims.put("authority", authorityValues);

        if (role != null) {
            claims.put("role", role);
        }

        return claims;
    }


}

