package com.example.bookswapplatform.repository;

import com.example.bookswapplatform.entity.Book.MainCategory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface CategoryRepository extends JpaRepository<MainCategory, UUID> {
    Optional<MainCategory> findByName (String name);
    //@Query("SELECT c from MainCategory c where c.subCategories = null")
    List<MainCategory> findByParentCategoryIsNull();
    @Query("SELECT DISTINCT mc FROM MainCategory mc " +
            "LEFT JOIN FETCH mc.subCategories sub " +
            "LEFT JOIN FETCH sub.subCategories subSub " +
            "WHERE mc.parentCategory IS NULL")
    List<MainCategory> findAllMainCategoriesWithSubCategoriesAndSubSubCategories();
}
