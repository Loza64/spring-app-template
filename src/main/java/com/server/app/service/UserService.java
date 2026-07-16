package com.server.app.service;

import com.server.app.domain.dto.auth.UpdatePasswordDto;
import com.server.app.domain.dto.response.AuthResponse;
import com.server.app.domain.dto.user.UpdateProfileDto;
import com.server.app.domain.dto.user.UserCreateDto;
import com.server.app.domain.dto.user.UserUpdateDto;
import com.server.app.domain.model.User;

public interface UserService extends ICrudService<User, Integer, UserCreateDto, UserUpdateDto> {

  AuthResponse login(String username, String password);

  AuthResponse signUp(UserCreateDto dto);

  AuthResponse updateProfile(String token, UpdateProfileDto dto);

  User updatePassword(String token, UpdatePasswordDto dto);

  org.springframework.data.domain.Page<User> search(int page, int size, String query);
}
