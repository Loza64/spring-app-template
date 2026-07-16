package com.server.app.repository.specification;

import java.util.ArrayList;
import java.util.List;

import org.springframework.data.jpa.domain.Specification;

import com.server.app.domain.model.Role;

import jakarta.persistence.criteria.Predicate;

public class RoleSpecifications {

  public static Specification<Role> search(String query, Boolean showDeleted) {
    return (root, cq, cb) -> {
      List<Predicate> predicates = new ArrayList<>();

      if (showDeleted != null && showDeleted) {
        predicates.add(cb.isNotNull(root.get("deletedAt")));
      } else {
        predicates.add(cb.isNull(root.get("deletedAt")));
      }

      if (query != null && !query.isEmpty()) {
        predicates.add(cb.like(cb.lower(root.get("name")), "%" + query.toLowerCase() + "%"));
      }

      return cb.and(predicates.toArray(new Predicate[0]));
    };
  }
}