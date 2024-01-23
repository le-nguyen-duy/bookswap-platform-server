package com.example.bookswapplatform.controller;

import com.example.bookswapplatform.common.FilterRequest;
import com.example.bookswapplatform.dto.BaseResponseDTO;
import com.example.bookswapplatform.dto.CategoryRequest;
import com.example.bookswapplatform.service.AdminService;
import io.swagger.v3.oas.annotations.Parameter;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.text.ParseException;
import java.time.LocalDateTime;
import java.util.Set;
import java.util.UUID;

@RestController
@RequestMapping("api/v1/admin")
@RequiredArgsConstructor
@PreAuthorize("hasAuthority('ROLE_ADMIN')")
public class AdminController {
    private final AdminService adminService;
    @PostMapping("/order/filter")
    public ResponseEntity<BaseResponseDTO> orderFilter(@Min(value = 0, message = "pageNumber must be greater than or equal to 0")
                                                @RequestParam(defaultValue = "0") int page,

                                                @Min(value = 1, message = "pageSize must be greater than or equal to 1")
                                                @Max(value = 100, message = "pageSize must be less than or equal to 100")
                                                @RequestParam(defaultValue = "6") int size,

                                                @Parameter(description = "Sort by (EX: receiverPrice, senderPrice, bookPrice...)")
                                                @RequestParam(defaultValue = "senderPrice") String sortBy,

                                                @Parameter(description = "Sort order (EX: asc, desc)")
                                                @RequestParam(defaultValue = "desc") String sortOrder,

                                                @RequestParam(required = false) String keyWord,

                                                @Parameter(description = "Status (EX: NOT_PAY, WAITING_CONFIRM, WAITING_SHIPPER,PREPARING, ON_GOING, FINISH, CANCEL)")
                                                @RequestParam(required = false) String status) {
        return adminService.orderFilter(page, size, sortBy, sortOrder, keyWord, status);

    }

    @PostMapping("/user/filter")
    public ResponseEntity<BaseResponseDTO> userFilter(@Min(value = 0, message = "pageNumber must be greater than or equal to 0")
                                                       @RequestParam(defaultValue = "0") int page,

                                                       @Min(value = 1, message = "pageSize must be greater than or equal to 1")
                                                       @Max(value = 100, message = "pageSize must be less than or equal to 100")
                                                       @RequestParam(defaultValue = "6") int size,

                                                       @Parameter(description = "Sort by (EX: lastName,email,...)")
                                                       @RequestParam(defaultValue = "firstName") String sortBy,

                                                       @Parameter(description = "Sort order (EX: asc, desc)")
                                                       @RequestParam(defaultValue = "desc") String sortOrder,

                                                       @RequestParam(required = false) String keyWord,

                                                       @Parameter(description = "Role (EX: USER, ADMIN, SHIPPER )")
                                                       @RequestParam(required = false) String role) {
        return adminService.userFilter(page, size, sortBy, sortOrder, keyWord, role);

    }

    @GetMapping("/post/statistical")
    public ResponseEntity<BaseResponseDTO> getPostWithinDateRange(@RequestParam int days, @RequestParam String object) {
        return adminService.getDataWithinDateRange(days, object);
    }
    @GetMapping("/object/count")
    public ResponseEntity<BaseResponseDTO> getObjectCount(@RequestParam String object) {
        return adminService.getObjectCount(object);
    }
    @GetMapping("/statistic/revenue")
    public ResponseEntity<BaseResponseDTO> revenueStatistic(@RequestParam int days) {
        return adminService.revenueStatistic(days);
    }

    @PostMapping("/category/add")
    public ResponseEntity<BaseResponseDTO> addCategory(@RequestBody CategoryRequest categoryRequest) {
        return adminService.addCategory(categoryRequest);
    }
    @PostMapping("/sub-sub-category/add")
    public ResponseEntity<BaseResponseDTO> addSubSubCategory( @RequestParam UUID id, @RequestParam Set<String> subSubCategoryNames) {
        return adminService.addSubSubCategory(id, subSubCategoryNames);
    }

    @PutMapping("/post/modify")
    public ResponseEntity<BaseResponseDTO> modifyPostCreateDate(@RequestParam UUID id,
                                                                @RequestParam(required = false) String createDate,
                                                                @RequestParam(required = false) Set<String> categories) throws ParseException {
        return adminService.modifyPostCreateDate(id, createDate, categories);

    }

    @PostMapping("post/filter")
    public ResponseEntity<BaseResponseDTO> filter (
            @Min(value = 0, message = "pageNumber must be greater than or equal to 0")
            @RequestParam(defaultValue = "0") int page,

            @Min(value = 1, message = "pageSize must be greater than or equal to 1")
            @Max(value = 100, message = "pageSize must be less than or equal to 100")
            @RequestParam(defaultValue = "20") int size,

            @Parameter(description = "Sort by (EX: caption, description, views, createDate)")
            @RequestParam(defaultValue = "views") String sortBy,

            @Parameter(description = "Sort order (EX: asc, desc)")
            @RequestParam(defaultValue = "desc") String sortOrder,

            @RequestParam(required = false) String keyWord,
            @RequestParam(required = false, defaultValue = "ACTIVE") String status,
            @RequestBody(required = false) FilterRequest filterRequest) {

        return adminService.postFilter(page, size, sortBy, sortOrder, keyWord, status, filterRequest);
    }

}
