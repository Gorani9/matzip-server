package com.matzip.server.domain.me;

import com.matzip.server.domain.me.dto.MeDto;
import com.matzip.server.domain.me.dto.MeDto.PasswordChangeRequest;
import com.matzip.server.domain.me.dto.MeDto.PatchProfileRequest;
import com.matzip.server.domain.me.dto.MeDto.UsernameChangeRequest;
import com.matzip.server.domain.me.dto.ScrapDto;
import com.matzip.server.domain.me.exception.DuplicateHeartException;
import com.matzip.server.domain.me.exception.FollowMeException;
import com.matzip.server.domain.me.exception.HeartMyReviewException;
import com.matzip.server.domain.me.exception.ScrapMyReviewException;
import com.matzip.server.domain.me.model.Follow;
import com.matzip.server.domain.me.model.Heart;
import com.matzip.server.domain.me.model.Scrap;
import com.matzip.server.domain.me.repository.FollowRepository;
import com.matzip.server.domain.me.repository.HeartRepository;
import com.matzip.server.domain.me.repository.ScrapRepository;
import com.matzip.server.domain.me.service.MeService;
import com.matzip.server.domain.review.dto.ReviewDto;
import com.matzip.server.domain.review.exception.AccessBlockedOrDeletedReviewException;
import com.matzip.server.domain.review.exception.ReviewNotFoundException;
import com.matzip.server.domain.review.model.Review;
import com.matzip.server.domain.review.repository.ReviewRepository;
import com.matzip.server.domain.user.exception.AccessBlockedOrDeletedUserException;
import com.matzip.server.domain.user.exception.UsernameAlreadyExistsException;
import com.matzip.server.domain.user.exception.UsernameNotFoundException;
import com.matzip.server.domain.user.model.User;
import com.matzip.server.domain.user.repository.UserRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@ActiveProfiles("test")
@Tag("ServiceTest")
public class MeServiceTest {
    @InjectMocks
    private MeService meService;
    @Mock
    private UserRepository userRepository;
    @Mock
    private FollowRepository followRepository;
    @Mock
    private ReviewRepository reviewRepository;
    @Mock
    private HeartRepository heartRepository;
    @Mock
    private ScrapRepository scrapRepository;
    @Mock
    private PasswordEncoder passwordEncoder;

    @Test
    @DisplayName("???????????? ?????? ?????????")
    public void changePasswordTest() {
        // given
        User me = new User("me", "password");
        given(userRepository.findMeById(1L)).willReturn(me);
        given(passwordEncoder.encode(any())).willReturn("new password");

        // when
        PasswordChangeRequest request = new PasswordChangeRequest("new password");

        // then
        assertDoesNotThrow(() -> meService.changePassword(1L, request));
    }

    @Test
    @DisplayName("???????????? ?????? ????????? ??????")
    public void changeUsernameTest_Success() {
        // given
        User me = new User("me", "password");
        given(userRepository.findMeById(1L)).willReturn(me);
        given(userRepository.existsByUsername("new_me")).willReturn(false);

        // when
        UsernameChangeRequest request = new UsernameChangeRequest("new_me");
        MeDto.Response response = meService.changeUsername(1L, request);

        // then
        assertThat(response.getUsername()).isEqualTo("new_me");
    }

    @Test
    @DisplayName("???????????? ?????? ????????? ??????: ???????????? ??????")
    public void changeUsernameTest_DuplicateUsername() {
        // given
        User me = new User("me", "password");
        given(userRepository.findMeById(1L)).willReturn(me);
        given(userRepository.existsByUsername("new_me")).willReturn(true);

        // when
        UsernameChangeRequest request = new UsernameChangeRequest("new_me");

        // then
        assertThrows(UsernameAlreadyExistsException.class, () -> meService.changeUsername(1L, request));
    }

    @Test
    @DisplayName("??? ?????? ???????????? ?????????")
    public void getMeTest() {
        // given
        User me = new User("me", "password");

        // when
        when(userRepository.findMeById(1L)).thenReturn(me);
        MeDto.Response response = meService.getMe(1L);

        // then
        assertThat(response.getUsername()).isEqualTo("me");
    }

    @Test
    @DisplayName("??? ?????? ???????????? ?????????")
    public void patchMeTest() {
        // given
        User me = new User("me", "password");
        given(userRepository.findMeById(1L)).willReturn(me);

        // when
        PatchProfileRequest request = new PatchProfileRequest(null, "my profile");
        MeDto.Response response = meService.patchMe(1L, request);

        //
        assertThat(response.getUsername()).isEqualTo("me");
        assertThat(response.getProfileString()).isEqualTo("my profile");
    }

    @Test
    @DisplayName("?????? ?????? ??????????????? ????????? ??????")
    public void followUserTest() {
        // given
        User me = new User("me", "password");
        User user = new User("user", "password");
        given(userRepository.findMeById(1L)).willReturn(me);

        //when
        when(userRepository.findByUsername("user")).thenReturn(Optional.of(user));

        // then
        assertDoesNotThrow(() -> meService.followUser(1L, "user"));
    }

    @Test
    @DisplayName("?????? ?????? ??????????????? ????????? ??????: ?????? ????????? ?????? ???????????? ????????? ???????????? ??????")
    public void followUserTest_Idempotent() {
        // given
        User me = new User("me", "password");
        User user = new User("user", "password");
        given(userRepository.findMeById(1L)).willReturn(me);
        given(userRepository.findByUsername("user")).willReturn(Optional.of(user));

        //when
        new Follow(me, user);

        // then
        assertDoesNotThrow(() -> meService.followUser(1L, "user"));
    }

    @Test
    @DisplayName("?????? ?????? ??????????????? ????????? ??????: ??????????????? ??????????????? ????????? ??????????????? ??????")
    public void followUserTest_BlockedOrDeletedUser() {
        // given
        User me = new User("me", "password");
        User user1 = new User("user1", "password");
        User user2 = new User("user2", "password");
        given(userRepository.findMeById(1L)).willReturn(me);
        given(userRepository.findByUsername("user1")).willReturn(Optional.of(user1));
        given(userRepository.findByUsername("user2")).willReturn(Optional.of(user2));

        //when
        user1.block("test");
        user2.delete();

        // then
        assertThrows(AccessBlockedOrDeletedUserException.class, () -> meService.followUser(1L, "user1"));
        assertThrows(AccessBlockedOrDeletedUserException.class, () -> meService.followUser(1L, "user2"));
    }

    @Test
    @DisplayName("?????? ?????? ??????????????? ????????? ??????: ?????? ??????????????? ?????? ????????? ?????? ??????")
    public void followUserTest_UserNotFound() {
        // given
        User me = new User("me", "password");
        given(userRepository.findMeById(1L)).willReturn(me);

        //when
        when(userRepository.findByUsername("user")).thenReturn(Optional.empty());

        // then
        assertThrows(UsernameNotFoundException.class, () -> meService.followUser(1L, "user"));
    }

    @Test
    @DisplayName("?????? ?????? ??????????????? ????????? ??????: ?????? ???????????? ??????")
    public void followUserTest_FollowingMe() {
        // given
        User me = new User("me", "password");
        given(userRepository.findMeById(1L)).willReturn(me);

        //when
        when(userRepository.findByUsername("me")).thenReturn(Optional.of(me));

        // then
        assertThrows(FollowMeException.class, () -> meService.followUser(1L, "me"));
    }

    @Test
    @DisplayName("?????? ?????? ?????????????????? ????????? ??????")
    public void unfollowUserTest() {
        // given
        User me = new User("me", "password");
        User user = new User("user", "password");
        given(userRepository.findMeById(1L)).willReturn(me);
        given(userRepository.findByUsername("user")).willReturn(Optional.of(user));

        //when
        new Follow(me, user);

        // then
        assertDoesNotThrow(() -> meService.unfollowUser(1L, "user"));
    }

    @Test
    @DisplayName("?????? ?????? ?????????????????? ????????? ??????: ????????? ?????? ????????? ????????? ???????????? ??????")
    public void unfollowUserTest_Idempotent() {
        // given
        User me = new User("me", "password");
        User user = new User("user", "password");
        given(userRepository.findMeById(1L)).willReturn(me);
        given(userRepository.findByUsername("user")).willReturn(Optional.of(user));

        //when
        when(followRepository.findByFollowerIdAndFolloweeId(eq(1L), any())).thenReturn(Optional.empty());

        // then
        assertDoesNotThrow(() -> meService.unfollowUser(1L, "user"));
    }

    @Test
    @DisplayName("?????? ?????? ?????????????????? ????????? ??????: ?????? ??????????????? ?????? ????????? ?????? ??????")
    public void unfollowUserTest_UserNotFound() {
        // given
        User me = new User("me", "password");
        given(userRepository.findMeById(1L)).willReturn(me);

        //when
        when(userRepository.findByUsername("user")).thenReturn(Optional.empty());

        // then
        assertThrows(UsernameNotFoundException.class, () -> meService.unfollowUser(1L, "user"));
    }

    @Test
    @DisplayName("?????? ?????? ?????????????????? ????????? ??????: ??????????????? ??????????????? ????????? ??????????????? ??????")
    public void unfollowUserTest_BlockedOrDeletedUser() {
        // given
        User me = new User("me", "password");
        User user1 = new User("user1", "password");
        User user2 = new User("user2", "password");
        given(userRepository.findMeById(1L)).willReturn(me);
        given(userRepository.findByUsername("user1")).willReturn(Optional.of(user1));
        given(userRepository.findByUsername("user2")).willReturn(Optional.of(user2));

        //when
        user1.block("test");
        user2.delete();

        // then
        assertThrows(AccessBlockedOrDeletedUserException.class, () -> meService.unfollowUser(1L, "user1"));
        assertThrows(AccessBlockedOrDeletedUserException.class, () -> meService.unfollowUser(1L, "user2"));
    }

    @Test
    @DisplayName("?????? ??????????????? ????????? ??????")
    public void heartReviewTest() {
        // given
        User me = new User("me", "password");
        User reviewer = new User("reviewer", "password");
        Review review = new Review(reviewer, new ReviewDto.PostRequest("content", null, 9, "location"));
        given(userRepository.findMeById(1L)).willReturn(me);
        given(reviewRepository.findById(1L)).willReturn(Optional.of(review));

        // when
        when(heartRepository.existsByUserIdAndReviewId(1L, 1L)).thenReturn(false);

        // then
        assertDoesNotThrow(() -> meService.heartReview(1L, 1L));
    }

    @Test
    @DisplayName("?????? ??????????????? ????????? ??????: ?????? ?????? ???????????? ?????? ????????? ???????????? ?????? ???")
    public void heartReviewTest_ReviewNotFound() {
        // given
        User me = new User("me", "password");
        given(userRepository.findMeById(1L)).willReturn(me);

        // when
        when(reviewRepository.findById(1L)).thenReturn(Optional.empty());

        // then
        assertThrows(ReviewNotFoundException.class, () -> meService.heartReview(1L, 1L));
    }

    @Test
    @DisplayName("?????? ??????????????? ????????? ??????: ?????? ????????? ????????? ??????????????? ??????")
    public void heartReviewTest_MyReview() {
        // given
        User me = new User("me", "password");
        Review review = new Review(me, new ReviewDto.PostRequest("content", null, 9, "location"));
        given(userRepository.findMeById(1L)).willReturn(me);

        // when
        when(reviewRepository.findById(1L)).thenReturn(Optional.of(review));

        // then
        assertThrows(HeartMyReviewException.class, () -> meService.heartReview(1L, 1L));
    }

    @Test
    @DisplayName("?????? ??????????????? ????????? ??????: ?????? ???????????? ?????? ??????")
    public void heartReviewTest_DuplicateHeart() {
        // given
        User me = new User("me", "password");
        User reviewer = new User("reviewer", "password");
        Review review = new Review(reviewer, new ReviewDto.PostRequest("content", null, 9, "location"));
        given(userRepository.findMeById(1L)).willReturn(me);
        given(reviewRepository.findById(1L)).willReturn(Optional.of(review));

        // when
        new Heart(me, review);
        when(heartRepository.existsByUserIdAndReviewId(1L, 1L)).thenReturn(true);

        // then
        assertThrows(DuplicateHeartException.class, () -> meService.heartReview(1L, 1L));
    }

    @Test
    @DisplayName("?????? ??????????????? ????????? ??????: ????????? ????????? ????????? ??????")
    public void heartReviewTest_ReviewBlockedOrDeleted() {
        // given
        User me = new User("me", "password");
        User reviewer = new User("reviewer", "password");
        Review review1 = new Review(reviewer, new ReviewDto.PostRequest("content", null, 9, "location"));
        Review review2 = new Review(reviewer, new ReviewDto.PostRequest("content", null, 9, "location"));
        given(userRepository.findMeById(1L)).willReturn(me);
        given(reviewRepository.findById(1L)).willReturn(Optional.of(review1));
        given(reviewRepository.findById(2L)).willReturn(Optional.of(review2));

        // when
        review1.block("test");
        review2.delete();

        // then
        assertThrows(AccessBlockedOrDeletedReviewException.class, () -> meService.heartReview(1L, 1L));
        assertThrows(AccessBlockedOrDeletedReviewException.class, () -> meService.heartReview(1L, 2L));
    }

    @Test
    @DisplayName("?????? ????????? ???????????? ????????? ??????")
    public void deleteHeartFromReviewTest() {
        // given
        User me = new User("me", "password");
        User reviewer = new User("reviewer", "password");
        Review review = new Review(reviewer, new ReviewDto.PostRequest("content", null, 9, "location"));
        given(userRepository.findMeById(1L)).willReturn(me);
        given(reviewRepository.findById(1L)).willReturn(Optional.of(review));

        // when
        Heart heart = new Heart(me, review);
        when(heartRepository.findByUserIdAndReviewId(1L, 1L)).thenReturn(Optional.of(heart));

        // then
        assertDoesNotThrow(() -> meService.deleteHeartFromReview(1L, 1L));
    }

    @Test
    @DisplayName("?????? ????????? ???????????? ????????? ??????: ???????????? ????????? ?????? ???????????? ??????")
    public void deleteHeartFromReviewTest_Idempotent() {
        // given
        User me = new User("me", "password");
        User reviewer = new User("reviewer", "password");
        Review review = new Review(reviewer, new ReviewDto.PostRequest("content", null, 9, "location"));
        given(userRepository.findMeById(1L)).willReturn(me);
        given(reviewRepository.findById(1L)).willReturn(Optional.of(review));

        // when
        when(heartRepository.findByUserIdAndReviewId(1L, 1L)).thenReturn(Optional.empty());

        // then
        assertDoesNotThrow(() -> meService.deleteHeartFromReview(1L, 1L));
    }

    @Test
    @DisplayName("?????? ????????? ???????????? ????????? ??????: ??????????????? ????????? ??????")
    public void deleteHeartFromReviewTest_ReviewBlockedOrDeleted() {
        // given
        User me = new User("me", "password");
        User reviewer = new User("reviewer", "password");
        Review review1 = new Review(reviewer, new ReviewDto.PostRequest("content", null, 9, "location"));
        Review review2 = new Review(reviewer, new ReviewDto.PostRequest("content", null, 9, "location"));
        given(userRepository.findMeById(1L)).willReturn(me);
        given(reviewRepository.findById(1L)).willReturn(Optional.of(review1));
        given(reviewRepository.findById(2L)).willReturn(Optional.of(review2));

        // when
        Heart heart1 = new Heart(me, review1);
        Heart heart2 = new Heart(me, review2);
        review1.block("test");
        review2.delete();

        // then
        assertThrows(AccessBlockedOrDeletedReviewException.class, () -> meService.deleteHeartFromReview(1L, 1L));
        assertThrows(AccessBlockedOrDeletedReviewException.class, () -> meService.deleteHeartFromReview(1L, 2L));
    }

    @Test
    @DisplayName("?????? ????????? ????????? ??????")
    public void scrapReviewTest() {
        // given
        User me = new User("me", "password");
        User reviewer = new User("reviewer", "password");
        Review review = new Review(reviewer, new ReviewDto.PostRequest("content", null, 9, "location"));
        given(userRepository.findMeById(1L)).willReturn(me);
        given(reviewRepository.findById(1L)).willReturn(Optional.of(review));

        // when
        ScrapDto.PostRequest request = new ScrapDto.PostRequest("description");
        when(scrapRepository.findByUserIdAndReviewId(1L, 1L)).thenReturn(Optional.empty());
        when(scrapRepository.save(any())).thenReturn(new Scrap(me, review));

        // then
        assertDoesNotThrow(() -> meService.scrapReview(1L, 1L, request));
    }

    @Test
    @DisplayName("?????? ????????? ????????? ??????: ?????? ???????????? ???????????? ?????????")
    public void scrapReviewTest_DuplicateScrap() {
        // given
        User me = new User("me", "password");
        User reviewer = new User("reviewer", "password");
        Review review = new Review(reviewer, new ReviewDto.PostRequest("content", null, 9, "location"));
        given(userRepository.findMeById(1L)).willReturn(me);
        given(reviewRepository.findById(1L)).willReturn(Optional.of(review));
        Scrap scrap = new Scrap(me, review);
        scrap.setDescription("before");

        // when
        when(scrapRepository.findByUserIdAndReviewId(1L, 1L)).thenReturn(Optional.of(scrap));
        ScrapDto.PostRequest request = new ScrapDto.PostRequest("after");
        ScrapDto.Response response = meService.scrapReview(1L, 1L, request);

        // then
        assertThat(response.getDescription()).isNotEqualTo("before");
        assertThat(response.getDescription()).isEqualTo("after");
    }

    @Test
    @DisplayName("?????? ????????? ????????? ??????: ?????? ?????? ???????????? ?????? ????????? ???????????? ?????? ???")
    public void scrapReviewTest_ReviewNotFound() {
        // given
        User me = new User("me", "password");
        given(userRepository.findMeById(1L)).willReturn(me);

        // when
        ScrapDto.PostRequest request = new ScrapDto.PostRequest("description");
        when(reviewRepository.findById(1L)).thenReturn(Optional.empty());

        // then
        assertThrows(ReviewNotFoundException.class, () -> meService.scrapReview(1L, 1L, request));
    }

    @Test
    @DisplayName("?????? ????????? ????????? ??????: ?????? ????????? ????????? ??????????????? ??????")
    public void scrapReviewTest_MyReview() {
        // given
        User me = new User("me", "password");
        Review review = new Review(me, new ReviewDto.PostRequest("content", null, 9, "location"));
        given(userRepository.findMeById(1L)).willReturn(me);

        // when
        ScrapDto.PostRequest request = new ScrapDto.PostRequest("description");
        when(reviewRepository.findById(1L)).thenReturn(Optional.of(review));

        // then
        assertThrows(ScrapMyReviewException.class, () -> meService.scrapReview(1L, 1L, request));
    }

    @Test
    @DisplayName("?????? ????????? ????????? ??????: ????????? ????????? ????????? ??????")
    public void scrapReviewTest_ReviewBlockedOrDeleted() {
        // given
        User me = new User("me", "password");
        User reviewer = new User("reviewer", "password");
        Review review1 = new Review(reviewer, new ReviewDto.PostRequest("content", null, 9, "location"));
        Review review2 = new Review(reviewer, new ReviewDto.PostRequest("content", null, 9, "location"));
        given(userRepository.findMeById(1L)).willReturn(me);
        given(reviewRepository.findById(1L)).willReturn(Optional.of(review1));
        given(reviewRepository.findById(2L)).willReturn(Optional.of(review2));

        // when
        review1.block("test");
        review2.delete();
        ScrapDto.PostRequest request = new ScrapDto.PostRequest("description");

        // then
        assertThrows(AccessBlockedOrDeletedReviewException.class, () -> meService.scrapReview(1L, 1L, request));
        assertThrows(AccessBlockedOrDeletedReviewException.class, () -> meService.scrapReview(1L, 2L, request));
    }

    @Test
    @DisplayName("?????? ????????? ???????????? ????????? ??????")
    public void deleteMyScrapTest() {
        // given
        User me = new User("me", "password");
        User reviewer = new User("reviewer", "password");
        Review review = new Review(reviewer, new ReviewDto.PostRequest("content", null, 9, "location"));
        given(userRepository.findMeById(1L)).willReturn(me);
        given(reviewRepository.findById(1L)).willReturn(Optional.of(review));

        // when
        Scrap scrap = new Scrap(me, review);
        when(scrapRepository.findByUserIdAndReviewId(1L, 1L)).thenReturn(Optional.of(scrap));

        // then
        assertDoesNotThrow(() -> meService.deleteMyScrap(1L, 1L));
    }

    @Test
    @DisplayName("?????? ????????? ???????????? ????????? ??????: ???????????? ????????? ?????? ???????????? ??????")
    public void deleteMyScrapTest_Idempotent() {
        // given
        User me = new User("me", "password");
        User reviewer = new User("reviewer", "password");
        Review review = new Review(reviewer, new ReviewDto.PostRequest("content", null, 9, "location"));
        given(userRepository.findMeById(1L)).willReturn(me);
        given(reviewRepository.findById(1L)).willReturn(Optional.of(review));

        // when
        when(scrapRepository.findByUserIdAndReviewId(1L, 1L)).thenReturn(Optional.empty());

        // then
        assertDoesNotThrow(() -> meService.deleteMyScrap(1L, 1L));
    }

    @Test
    @DisplayName("?????? ????????? ???????????? ????????? ??????: ??????????????? ????????? ??????")
    public void deleteMyScrapTest_ReviewBlockedOrDeleted() {
        // given
        User me = new User("me", "password");
        User reviewer = new User("reviewer", "password");
        Review review1 = new Review(reviewer, new ReviewDto.PostRequest("content", null, 9, "location"));
        Review review2 = new Review(reviewer, new ReviewDto.PostRequest("content", null, 9, "location"));
        given(userRepository.findMeById(1L)).willReturn(me);
        given(reviewRepository.findById(1L)).willReturn(Optional.of(review1));
        given(reviewRepository.findById(2L)).willReturn(Optional.of(review2));

        // when
        Heart scrap1 = new Heart(me, review1);
        Heart scrap2 = new Heart(me, review2);
        review1.block("test");
        review2.delete();

        // then
        assertThrows(AccessBlockedOrDeletedReviewException.class, () -> meService.deleteMyScrap(1L, 1L));
        assertThrows(AccessBlockedOrDeletedReviewException.class, () -> meService.deleteMyScrap(1L, 2L));
    }

}
