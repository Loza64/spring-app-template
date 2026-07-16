package com.server.app.domain.mapper;

import org.mapstruct.BeanMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.NullValuePropertyMappingStrategy;

import com.server.app.domain.dto.role.RoleDto;
import com.server.app.domain.model.Role;

@Mapper(componentModel = "spring")
public interface RoleMapper {

  @Mapping(target = "permissions", ignore = true)
  Role toEntity(RoleDto dto);

  @Mapping(target = "permissions", ignore = true)
  @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
  void updateEntity(RoleDto dto, @MappingTarget Role entity);
}
