package com.example.bookswapplatform.security.firebase.service;

import com.google.firebase.auth.FirebaseAuthException;

import java.util.List;

public interface UserManagementService {
    void setUserClaims(String uid) throws FirebaseAuthException;

}
