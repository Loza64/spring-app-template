package com.server.app.common.constants;

import java.util.List;

public final class RoleNames {

  public static final String SUPER_ADMIN = "SUPER_ADMIN";
  public static final String ADMIN = "ADMIN";
  public static final String CLIENT = "CLIENT";

  public static final List<String> HIERARCHY = List.of(SUPER_ADMIN, ADMIN, CLIENT);

  private RoleNames() {
  }

  public static List<String> visibleFrom(String roleName) {
    int idx = HIERARCHY.indexOf(roleName);
    if (idx < 0) {
      return List.of(roleName);
    }
    return HIERARCHY.subList(idx, HIERARCHY.size());
  }
}