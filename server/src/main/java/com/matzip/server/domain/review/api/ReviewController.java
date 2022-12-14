package com.matzip.server.domain.review.api;

import com.matzip.server.domain.review.dto.ReviewDto;
import com.matzip.server.domain.review.model.ReviewProperty;
import com.matzip.server.domain.review.service.ReviewService;
import com.matzip.server.global.auth.CurrentUser;
import lombok.RequiredArgsConstructor;
import org.hibernate.validator.constraints.Length;
import org.springframework.data.domain.Slice;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import javax.validation.constraints.Max;
import javax.validation.constraints.Positive;
import javax.validation.constraints.PositiveOrZero;

@Validated
@RequiredArgsConstructor
@RestController
@RequestMapping("/api/v1/reviews")
public class ReviewController {
    private final ReviewService reviewService;

    @GetMapping
    public ResponseEntity<Slice<ReviewDto.Response>> searchReviews(
            @CurrentUser Long myId,
            @RequestParam(value = "keyword", required = false) @Length(max = 100) String keyword,
            @RequestParam(value = "page", required = false, defaultValue = "0") @PositiveOrZero Integer page,
            @RequestParam(value = "size", required = false, defaultValue = "20") @Positive @Max(100) Integer size,
            @RequestParam(value = "sort", required = false) String reviewProperty,
            @RequestParam(value = "asc", required = false, defaultValue = "false") Boolean asc) {
        return ResponseEntity.ok()
                .body(reviewService.searchReviews(
                        myId,
                        new ReviewDto.SearchRequest(keyword, page, size, ReviewProperty.from(reviewProperty), asc)));
    }

    @PostMapping(consumes={"multipart/form-data"})
    public ResponseEntity<ReviewDto.DetailedResponse> postReview(
            @CurrentUser Long myId,
            @ModelAttribute @Valid ReviewDto.PostRequest postRequest) {
        return ResponseEntity.ok().body(reviewService.postReview(myId, postRequest));
    }

    @GetMapping("/{review-id}")
    public ResponseEntity<ReviewDto.DetailedResponse> getReview(
            @CurrentUser Long myId,
            @PathVariable("review-id") @Positive Long reviewId) {
        return ResponseEntity.ok().body(reviewService.fetchReview(myId, reviewId));
    }

    @PatchMapping(value="/{review-id}", consumes={"multipart/form-data"})
    public ResponseEntity<ReviewDto.DetailedResponse> patchReview(
            @CurrentUser Long myId,
            @PathVariable("review-id") @Positive Long reviewId,
            @ModelAttribute @Valid ReviewDto.PatchRequest patchRequest) {
        return ResponseEntity.ok().body(reviewService.patchReview(myId, reviewId, patchRequest));
    }

    @DeleteMapping("/{review-id}")
    public ResponseEntity<Object> deleteReview(
            @CurrentUser Long myId,
            @PathVariable("review-id") @Positive Long reviewId) {
        reviewService.deleteReview(myId, reviewId);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/hot")
    public ResponseEntity<ReviewDto.HotResponse> getHotReviews(@CurrentUser Long myId) {
        return ResponseEntity.ok().body(reviewService.getHotReviews(myId));
    }

    @GetMapping("/hall-of-fame")
    public ResponseEntity<ReviewDto.HallOfFameResponse> getHallOfFameReviews(@CurrentUser Long myId) {
        return ResponseEntity.ok().body(reviewService.getHallOfFameReviews(myId));
    }
}
