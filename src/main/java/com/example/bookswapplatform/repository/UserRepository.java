package com.example.bookswapplatform.repository;

import com.example.bookswapplatform.entity.User.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;
@Repository
public interface UserRepository extends JpaRepository<User, UUID> {
    Optional<User> findByEmail(String userName);

    Optional<User> findByFireBaseUid(String uid);
    @Query("SELECT u.id from User u where u.fireBaseUid = :uid")
    UUID getUUIDByFireBaseUid(String uid);

}
