package com.example.bookswapplatform.dto;

import com.example.bookswapplatform.utils.DateTimeUtils;
import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;
import java.util.Set;

@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
public class BookRequest {
    @NotEmpty
    private String title;
    @NotEmpty
    private String description;
    private String publisher;
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = DateTimeUtils.DATETIME_FORMAT)
    private String publishDate;
    @NotEmpty
    private String isbn;
    @NotEmpty
    private String language;
    @NotNull
    private int pageCount;
    @NotEmpty
    private Set<String> authors;
    @NotEmpty
    private String category;
    private String subCategory;
    private String subSubCategory;

    @NotEmpty
    @Size(min = 1, max = 3, message = "New percent only in 0 and 100%")
    @Pattern(regexp = "\\d+", message = "New percent must contain only digits")
    private String newPercent;
    @NotEmpty
    private String coverImage;
    @NotEmpty
    private Set<String> imageUrls;
}
