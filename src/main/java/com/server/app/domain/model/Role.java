package com.server.app.domain.model;

import com.server.app.domain.model.base.BaseEntity;
import jakarta.persistence.*;
import lombok.*;
import java.util.Set;

import org.hibernate.annotations.Fetch;
import org.hibernate.annotations.FetchMode;

@Entity
@Table(name = "roles")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Role extends BaseEntity {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(nullable = false, unique = true)
  private String name;

  @Builder.Default
  private Boolean active = true;

  @ManyToMany(fetch = FetchType.LAZY)
  @JoinTable(name = "role_permissions", joinColumns = @JoinColumn(name = "role_id"), inverseJoinColumns = @JoinColumn(name = "permission_id"))
  @Fetch(FetchMode.SUBSELECT)
  private Set<Permission> permissions;
}