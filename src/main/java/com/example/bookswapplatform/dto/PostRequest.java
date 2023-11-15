package com.example.bookswapplatform.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.Set;
import java.util.UUID;
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class PostRequest {
    private String caption;
    private String description;
    private String exchangeMethod;
    private String city;
    private String district;
    private Set<UUID> bookIds;
    private Set<BigDecimal> price;

}
