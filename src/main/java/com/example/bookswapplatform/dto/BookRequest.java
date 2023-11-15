package com.example.bookswapplatform.dto;

import com.example.bookswapplatform.utils.DateTimeUtils;
import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.annotation.Nullable;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import javax.validation.constraints.Pattern;
import javax.validation.constraints.Size;
import java.util.Set;

@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
public class BookRequest {
    private String title;
    private String description;
    private String publisher;
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = DateTimeUtils.DATETIME_FORMAT)
    private String publishDate;
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
}
