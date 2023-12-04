package com.example.bookswapplatform.entity.User;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.UuidGenerator;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.util.UUID;

@Entity
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@EntityListeners(AuditingEntityListener.class)
@Builder
public class RateCard {
    @Id
    @UuidGenerator(style = UuidGenerator.Style.RANDOM)
    private UUID id;

    private String content;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "rate_id")
    private Rate rate;
}
