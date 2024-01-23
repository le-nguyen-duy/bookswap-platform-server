package com.example.bookswapplatform.config;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import com.google.firebase.auth.FirebaseAuth;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.io.IOException;
import java.io.InputStream;

@Configuration
public class FirebaseAdminConfiguration {
    @Bean
    public FirebaseAuth firebaseAuth() throws IOException {
        InputStream serviceAccount = getClass().getClassLoader().getResourceAsStream("bookswapplatform-firebase-adminsdk.json");

        FirebaseOptions options = new FirebaseOptions.Builder()
                .setCredentials(GoogleCredentials.fromStream(serviceAccount))
                .build();

        FirebaseApp.initializeApp(options);
        return FirebaseAuth.getInstance(FirebaseApp.getInstance());
    }
}
