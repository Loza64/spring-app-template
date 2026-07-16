package com.server.app.domain.mapper;

import org.mapstruct.BeanMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.NullValuePropertyMappingStrategy;

import com.server.app.domain.dto.permission.PermissionResponseDto;
import com.server.app.domain.dto.permission.PermissionUpdateDto;
import com.server.app.domain.model.Permission;

@Mapper(componentModel = "spring")
public interface PermissionMapper {

  PermissionResponseDto toResponseDto(Permission p);

  @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
  @Mapping(target = "id", ignore = true)
  @Mapping(target = "path", ignore = true)
  @Mapping(target = "method", ignore = true)
  void updateEntity(PermissionUpdateDto dto, @MappingTarget Permission entity);
}