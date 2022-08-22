package com.matzip.server.domain.review.repository;

import com.matzip.server.domain.review.model.Review;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface ReviewRepository extends JpaRepository<Review, Long> {
    Optional<Review> findAllById(Long id);

    Page<Review> findAllByUser_Username(Pageable pageable, String username);

    Page<Review> findAllByTitleContains(Pageable pageable, String keyword);

    Page<Review> findAllByContentContains(Pageable pageable, String keyword);

    Page<Review> findAllByTitleContainsOrContentContains(Pageable pageable, String titleKeyword, String contentKeyword);

    Page<Review> findAllByTitleContainsAndContentContains(
            Pageable pageable,
            String titleKeyword,
            String contentKeyword);

    List<Review> findAllByCreatedAtAfter(Pageable pageable, LocalDateTime localDateTime);
}