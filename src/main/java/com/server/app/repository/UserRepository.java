package com.server.app.repository;

import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.server.app.domain.model.User;
import com.server.app.repository.base.SoftDeletableRepository;

public interface UserRepository extends SoftDeletableRepository<User, Integer> {

  Optional<User> findUserByUsername(String username);

  Optional<User> findUserByEmail(String email);

  @Query("SELECT u FROM User u WHERE u.deletedAt IS NULL AND (" +
      "LOWER(u.username) LIKE LOWER(CONCAT('%', :query, '%')) OR " +
      "LOWER(u.name) LIKE LOWER(CONCAT('%', :query, '%')) OR " +
      "LOWER(u.email) LIKE LOWER(CONCAT('%', :query, '%')))")
  Page<User> search(Pageable pageable, @Param("query") String query);
}
