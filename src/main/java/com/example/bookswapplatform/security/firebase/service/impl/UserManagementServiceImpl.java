package com.example.bookswapplatform.security.firebase.service.impl;

import com.example.bookswapplatform.entity.User;
import com.example.bookswapplatform.repository.AuthorityRepository;
import com.example.bookswapplatform.repository.RoleRepository;
import com.example.bookswapplatform.repository.UserRepository;
import com.example.bookswapplatform.security.firebase.service.UserManagementService;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthException;
import com.google.firebase.auth.UserRecord;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

@Service
@RequiredArgsConstructor
public class UserManagementServiceImpl implements UserManagementService {
    private final FirebaseAuth firebaseAuth;
    private final RoleRepository roleRepository;
    private final UserRepository userRepository;
    @Override
    public void setUserClaims(String uid) throws FirebaseAuthException {
        UserRecord userRecord = firebaseAuth.getUser(uid);
        //kiểm tra user đã tồn tại trong hệ thống chưa
        if(userRepository.findByEmail(userRecord.getEmail()).isEmpty()) {
            //lưu user đăng nhập từ google vào database
            User user = new User();
            user.setEmail(userRecord.getEmail());
            user.setProvider(userRecord.getProviderData());
            user.setImage(userRecord.getPhotoUrl());
            user.setRole(roleRepository.findByName("USER"));
            userRepository.save(user);
            //set claims cho idToken
            Map<String, Object> claims = convertAuthoritiesToClaims(user.getAuthorities());
            firebaseAuth.setCustomUserClaims(uid, claims);
        }

        //revoked idToken để client dùng refesh token tạo 1 idToken mới có chứa claims

    }

    public Map<String, Object> convertAuthoritiesToClaims(Collection<? extends GrantedAuthority> authorities) {
        Map<String, Object> claims = new HashMap<>();
        AtomicInteger i = new AtomicInteger(1);
        // Chuyển đổi danh sách authorities thành một map claims
        authorities.forEach(authority -> {
            claims.put("authority" + i.getAndIncrement(), authority.getAuthority());
        });

        return claims;
    }

}

