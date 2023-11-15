package com.example.bookswapplatform.service.impl;

import com.example.bookswapplatform.dto.BaseResponseDTO;
import com.example.bookswapplatform.dto.CategoryDTO;
import com.example.bookswapplatform.dto.SubCategoriesDTO;
import com.example.bookswapplatform.dto.SubSubCategoriesDTO;
import com.example.bookswapplatform.entity.Book.MainCategory;
import com.example.bookswapplatform.repository.CategoryRepository;
import com.example.bookswapplatform.service.CategoryService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CategoryServiceImpl implements CategoryService {
    private final CategoryRepository categoryRepository;
    @Override
    public ResponseEntity<BaseResponseDTO> getAllCategory() {
        List<MainCategory> mainCategories = categoryRepository.findByParentCategoryIsNull();
        List<CategoryDTO> categoryDTOs = new ArrayList<>();

        for (MainCategory mainCategory : mainCategories) {
            CategoryDTO mainCategoryDTO = new CategoryDTO();
            mainCategoryDTO.setName(mainCategory.getName());
            mainCategoryDTO.setSubCategoriesDTOS(getSubcategories(mainCategory.getSubCategories()));
            categoryDTOs.add(mainCategoryDTO);
        }

        return ResponseEntity.ok(new BaseResponseDTO(LocalDateTime.now(), HttpStatus.OK, "Successfully",null, categoryDTOs));
    }

    private List<SubCategoriesDTO> getSubcategories(Set<MainCategory> subcategories) {
        List<SubCategoriesDTO> subcategoryDTOs = new ArrayList<>();

        for (MainCategory subcategory : subcategories) {
            SubCategoriesDTO subcategoryDTO = new SubCategoriesDTO();

            subcategoryDTO.setName(subcategory.getName());
            subcategoryDTO.setSubSubCategoriesDTOS(getSubSubCategories(subcategory.getSubSubCategories()));
            subcategoryDTOs.add(subcategoryDTO);
        }

        return subcategoryDTOs;
    }

    private List<SubSubCategoriesDTO> getSubSubCategories(Set<MainCategory> subSubCategories) {
        List<SubSubCategoriesDTO> subSubCategoriesDTOS = new ArrayList<>();

        for (MainCategory subSubCategory : subSubCategories) {
            SubSubCategoriesDTO subSubCategoriesDTO = new SubSubCategoriesDTO();
            subSubCategoriesDTO.setName(subSubCategory.getName());
            subSubCategoriesDTOS.add(subSubCategoriesDTO);
        }

        return subSubCategoriesDTOS;
    }
}
