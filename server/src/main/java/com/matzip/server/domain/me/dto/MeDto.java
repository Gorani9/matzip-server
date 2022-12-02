package com.matzip.server.domain.me.dto;

import com.matzip.server.domain.me.validation.FollowType;
import com.matzip.server.domain.review.validation.CommentProperty;
import com.matzip.server.domain.review.validation.ReviewProperty;
import com.matzip.server.domain.user.dto.UserDto;
import com.matzip.server.domain.user.model.User;
import com.matzip.server.domain.user.validation.Password;
import com.matzip.server.domain.user.validation.Username;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.multipart.MultipartFile;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Positive;
import javax.validation.constraints.PositiveOrZero;
import java.time.LocalDateTime;

@Validated
public class MeDto {
    @RequiredArgsConstructor
    @Getter
    public static class PasswordChangeRequest {
        @Password
        private final String password;
    }

    @RequiredArgsConstructor
    @Getter
    public static class UsernameChangeRequest {
        @Username
        private final String username;
    }


    @RequiredArgsConstructor
    @Getter
    public static class PatchProfileRequest {
        private final MultipartFile profileImage;
        private final String profileString;
    }

    @RequiredArgsConstructor
    @Getter
    public static class FollowRequest {
        @NotBlank
        private final String username;
    }

    @RequiredArgsConstructor
    @Getter
    public static class FindFollowRequest {
        @PositiveOrZero
        private final Integer pageNumber;
        @Positive
        private final Integer pageSize;
        private final String sortedBy;
        @NotNull
        private final Boolean ascending;
        @FollowType
        private final String type;
    }

    @RequiredArgsConstructor
    @Getter
    public static class FindReviewRequest {
        @PositiveOrZero
        private final Integer pageNumber;
        @Positive
        private final Integer pageSize;
        @ReviewProperty
        private final String sortedBy;
        @NotNull
        private final Boolean ascending;
    }

    @RequiredArgsConstructor
    @Getter
    public static class FindCommentRequest {
        @PositiveOrZero
        private final Integer pageNumber;
        @Positive
        private final Integer pageSize;
        @CommentProperty
        private final String sortedBy;
        @NotNull
        private final Boolean ascending;
    }

    @Getter
    public static class Response extends UserDto.Response {
        private final LocalDateTime createdAt;
        private final LocalDateTime modifiedAt;

        public Response(User user) {
            super(user, user);
            this.createdAt = user.getCreatedAt();
            this.modifiedAt = user.getModifiedAt();
        }
    }
}
