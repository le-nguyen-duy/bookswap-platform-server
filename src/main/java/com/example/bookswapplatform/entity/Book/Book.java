package com.example.bookswapplatform.entity.Book;

import com.example.bookswapplatform.entity.Order.Orders;
import com.example.bookswapplatform.entity.Post.Post;
import com.example.bookswapplatform.entity.User.User;
import com.example.bookswapplatform.utils.DateTimeUtils;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.ColumnDefault;
import org.hibernate.annotations.UuidGenerator;
import org.springframework.data.annotation.CreatedBy;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedBy;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;

@Entity
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@EntityListeners(AuditingEntityListener.class)
@Builder
public class Book {
    @Id
    @UuidGenerator(style = UuidGenerator.Style.RANDOM)
    private UUID id;

    private String title;

    private String description;

    private String publisher;

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = DateTimeUtils.DATE_FORMAT)
    @DateTimeFormat(pattern = DateTimeUtils.DATE_FORMAT)
    private LocalDate publishedDate;

    private String isbn;

    @Enumerated(EnumType.STRING)
    private BookLanguage language;

    private int pageCount;

    @ColumnDefault("false")
    private boolean isDone;

    @ManyToMany(mappedBy = "books")
    Set<Author> authors;

    @OneToMany(mappedBy = "book")
    private Set<BookImage> bookImages;

    @ManyToOne
    @JoinColumn(name = "category_id")
    private MainCategory category;

    @ManyToOne
    @JoinColumn(name = "post_id")
    private Post post;

    @ManyToMany(mappedBy = "books")
    Set<Orders> ordersSet;

    @CreatedDate
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = DateTimeUtils.DATETIME_FORMAT)
    @DateTimeFormat(pattern = DateTimeUtils.DATETIME_FORMAT)
    private LocalDateTime createDate;

    @LastModifiedDate
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = DateTimeUtils.DATETIME_FORMAT)
    @DateTimeFormat(pattern = DateTimeUtils.DATETIME_FORMAT)
    private LocalDateTime updateDate;

    private String createdBy;

    private String updateBy;



}
