package com.example.bookswapplatform.controller;

import com.example.bookswapplatform.common.FilterRequest;
import com.example.bookswapplatform.dto.BaseResponseDTO;
import com.example.bookswapplatform.dto.PostRequest;
import com.example.bookswapplatform.dto.PostUpdateBookRequest;
import com.example.bookswapplatform.dto.PostUpdateRequest;
import com.example.bookswapplatform.service.PostService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.UUID;

@RestController
@RequestMapping("api/v1/post")
@RequiredArgsConstructor
public class PostController {
    private final PostService postService;

    @PostMapping("/create")
    @PreAuthorize("hasAuthority('BOOK:CREATE')")
    public ResponseEntity<BaseResponseDTO> createPost (Principal principal, @RequestBody PostRequest postRequest) {
        return postService.createPost(principal, postRequest);
    }

    @Operation(description = "This api is for user get their active post info")
    @GetMapping("/active")
    @PreAuthorize("hasAuthority('BOOK:CREATE')")
    public ResponseEntity<BaseResponseDTO> getUserPost (Principal principal) {
        return postService.getUserPost(principal);
    }

    @GetMapping("/filter")
    @PreAuthorize("hasAuthority('BOOK:CREATE')")
    public ResponseEntity<BaseResponseDTO> filter (
            @Min(value = 0, message = "pageNumber must be greater than or equal to 0")
            @RequestParam(defaultValue = "0") int page,

            @Min(value = 1, message = "pageSize must be greater than or equal to 1")
            @Max(value = 100, message = "pageSize must be less than or equal to 100")
            @RequestParam(defaultValue = "6") int size,

            @Parameter(description = "Sort by (EX: caption, description, views, createDate)")
            @RequestParam(defaultValue = "views") String sortBy,

            @Parameter(description = "Sort order (EX: asc, desc)")
            @RequestParam(defaultValue = "desc") String sortOrder,

            @RequestParam(required = false) String keyWord,
            @RequestBody(required = false) FilterRequest filterRequest) {

        return postService.filterPost(page, size, sortBy, sortOrder, keyWord, filterRequest);
    }
    @GetMapping("/id")
    @PreAuthorize("hasAuthority('BOOK:READ')")
    public ResponseEntity<BaseResponseDTO> getPostDetail (@RequestParam UUID id) {
        return postService.getPostDetail(id);
    }
    @PutMapping("/modify")
    @PreAuthorize("hasAuthority('BOOK:MODIFY')")
    public ResponseEntity<BaseResponseDTO> modifyPost (Principal principal,
                                                      @RequestParam UUID postId,
                                                      @Valid @RequestBody PostUpdateRequest postUpdateRequest
    ) {
        return postService.modifyPost(principal, postId, postUpdateRequest);
    }

    @PutMapping("/modify-book")
    @PreAuthorize("hasAuthority('BOOK:MODIFY')")
    public ResponseEntity<BaseResponseDTO> modifyBook (Principal principal,
                                                       @RequestParam UUID postId,
                                                       @Valid @RequestBody PostUpdateBookRequest postUpdateBookRequest) {
        return postService.modifyBookInPost(principal, postId, postUpdateBookRequest);
    }

    @DeleteMapping("delete")
    @PreAuthorize("hasAuthority('BOOK:DELETE')")
    public ResponseEntity<BaseResponseDTO> deletePost (@RequestParam UUID postId) {
        return postService.deletePost(postId);
    }
}
