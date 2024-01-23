package com.example.bookswapplatform.dto;

import com.example.bookswapplatform.utils.DateTimeUtils;
import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.validator.constraints.Range;

import java.util.List;
import java.util.Set;

@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
public class BookRequest {
    private String title;
    private String description;
    private String publisher;
    @Digits(integer = 4, fraction = 0, message = "Please enter a valid year")
    @Range(min = 1900, max = 2100, message = "Year must be between 1900 and 2100")
    private Integer year;
    private String isbn;
    private String language;
    private int pageCount;
    private Set<String> authors;
    private String category;
    private String subCategory;
    private String subSubCategory;

    @Size(min = 1, max = 3, message = "New percent only in 0 and 100%")
    @Pattern(regexp = "\\d+", message = "New percent must contain only digits")
    private String newPercent;
    private String coverImage;
    private Set<String> imageUrls;
}
