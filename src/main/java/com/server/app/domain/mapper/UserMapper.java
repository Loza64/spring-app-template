package com.server.app.domain.mapper;

import org.mapstruct.BeanMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.NullValuePropertyMappingStrategy;

import com.server.app.domain.dto.response.UserResponseDto;
import com.server.app.domain.dto.user.UpdateProfileDto;
import com.server.app.domain.dto.user.UserCreateDto;
import com.server.app.domain.dto.user.UserUpdateDto;
import com.server.app.domain.model.User;

/**
 * La contraseña se codifica siempre en la capa de servicio (requiere el
 * {@code PasswordEncoder}), y la asignacion de {@code role} requiere resolver
 * el id contra el {@code RoleRepository}; por eso ambos campos se ignoran
 * aqui y se completan explicitamente en {@code UserServiceImpl}.
 */
@Mapper(componentModel = "spring")
public interface UserMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "password", ignore = true)
    @Mapping(target = "role", ignore = true)
    @Mapping(target = "blocked", ignore = true)
    User toEntity(UserCreateDto dto);

    @Mapping(target = "role", ignore = true)
    @Mapping(target = "password", ignore = true)
    @Mapping(target = "blocked", ignore = true)
    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    void updateEntity(UpdateProfileDto dto, @MappingTarget User entity);

    @Mapping(target = "role", ignore = true)
    @Mapping(target = "password", ignore = true)
    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    void updateEntity(UserUpdateDto dto, @MappingTarget User entity);

    @Mapping(target = "roleId", source = "role.id")
    @Mapping(target = "roleName", source = "role.name")
    UserResponseDto toResponseDto(User user);
}
