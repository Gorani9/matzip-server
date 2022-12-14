package com.matzip.server.domain.user.repository;

import com.matzip.server.domain.admin.dto.AdminDto;
import com.matzip.server.domain.user.dto.UserDto;
import com.matzip.server.domain.user.model.User;
import org.springframework.data.domain.Slice;

public interface UserRepositoryCustom {
    Slice<User> searchUsersByUsername(UserDto.SearchRequest searchRequest);
    Slice<User> adminSearchUsersByUsername(AdminDto.UserSearchRequest searchRequest);
}
