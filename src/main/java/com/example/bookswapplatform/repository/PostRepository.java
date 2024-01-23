package com.example.bookswapplatform.repository;

import com.example.bookswapplatform.entity.Order.Orders;
import com.example.bookswapplatform.entity.Post.Post;
import com.example.bookswapplatform.entity.Post.PostStatus;
import com.example.bookswapplatform.entity.User.User;
import org.hibernate.annotations.Filter;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import javax.swing.text.html.Option;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface PostRepository extends JpaRepository<Post, UUID> {
    List<Post> findByCreateByAndPostStatus(User user, PostStatus postStatus);
    @Query("SELECT p FROM Post p JOIN p.createBy u WHERE p.postStatus.name = 'ACTIVE' OR p.postStatus.name = 'LOCKED' AND u.role.name = 'USER'")
    Page<Post> findAllNotDeactive(Pageable pageable);
    @Query("SELECT p FROM Post p JOIN p.createBy u WHERE p.postStatus.name = :status AND u.role.name = 'USER'")
    Page<Post> findAllWithStatus(String status, Pageable pageable);

    @Query("SELECT p FROM Post p " +
            "WHERE p.postStatus.name = 'ACTIVE' AND " +
            "(LOWER(p.caption) LIKE %:keyWord% OR LOWER(p.description) LIKE %:keyWord%)")
    Page<Post> searchPostsByKeyword(@Param("keyWord") String keyWord, Pageable pageable);

    @Query("SELECT DISTINCT p FROM Post p JOIN p.ordersSet o WHERE o = :orders")
    Post getPostByOrder(@Param("orders") Orders orders);
    @Query(value = "SELECT * FROM post WHERE id = :uuid", nativeQuery = true)
    Optional<Post> findIncludeDeletedPost(@Param("uuid") UUID uuid);

    List<Post> findByCreateDateBetween(LocalDateTime startDate, LocalDateTime endDate);


}
