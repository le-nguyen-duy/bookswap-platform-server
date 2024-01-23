package com.example.bookswapplatform.service;

import com.example.bookswapplatform.common.FilterRequest;
import com.example.bookswapplatform.dto.BaseResponseDTO;
import com.example.bookswapplatform.dto.CategoryRequest;
import com.example.bookswapplatform.dto.OrderFilterRequest;
import com.example.bookswapplatform.dto.UserFilterRequest;
import org.springframework.http.ResponseEntity;

import java.text.ParseException;
import java.time.LocalDateTime;
import java.util.Set;
import java.util.UUID;

public interface AdminService {
    ResponseEntity<BaseResponseDTO> orderFilter(int pageNumber,
                                                int pageSize,
                                                String sortBy,
                                                String sortOrder,
                                                String keyWord,
                                                String status);

    ResponseEntity<BaseResponseDTO> userFilter(int pageNumber,
                                               int pageSize,
                                               String sortBy,
                                               String sortOrder, String keyWord,
                                               String role);
    ResponseEntity<BaseResponseDTO> getDataWithinDateRange(int days, String object);

    ResponseEntity<BaseResponseDTO> getObjectCount(String object);
    ResponseEntity<BaseResponseDTO> revenueStatistic(int days);

    ResponseEntity<BaseResponseDTO> addCategory(CategoryRequest categoryRequest);
    ResponseEntity<BaseResponseDTO> modifyPostCreateDate(UUID id,String createDate, Set<String> categories) throws ParseException;
    ResponseEntity<BaseResponseDTO> addSubSubCategory(UUID id, Set<String> subSubCategoryNames);

    ResponseEntity<BaseResponseDTO> postFilter(int pageNumber,
                                               int pageSize,
                                               String sortBy,
                                               String sortOrder,
                                               String keyWord,
                                               String status,
                                               FilterRequest filterRequest);
    ResponseEntity<BaseResponseDTO> modifyCategory(CategoryRequest categoryRequest);
}
