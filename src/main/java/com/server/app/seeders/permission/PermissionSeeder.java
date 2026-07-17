package com.server.app.seeders.permission;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.servlet.mvc.method.RequestMappingInfo;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;

import com.server.app.config.SecurityRules;
import com.server.app.domain.model.Permission;
import com.server.app.service.PermissionService;

@Component
public class PermissionSeeder implements ApplicationListener<ApplicationReadyEvent> {

  private final RequestMappingHandlerMapping handlerMapping;
  private final PermissionService permissionService;

  public PermissionSeeder(
      @Qualifier("requestMappingHandlerMapping") RequestMappingHandlerMapping handlerMapping,
      PermissionService permissionService) {
    this.handlerMapping = handlerMapping;
    this.permissionService = permissionService;
  }

  @Override
  public void onApplicationEvent(@NonNull ApplicationReadyEvent event) {

    Map<String, Permission> candidates = new LinkedHashMap<>();

    handlerMapping.getHandlerMethods().forEach((info, method) -> {

      Set<String> paths = getPaths(info);
      Set<RequestMethod> methods = info.getMethodsCondition().getMethods();

      if (methods.isEmpty()) {
        methods = Set.of(RequestMethod.GET);
      }

      for (String path : paths) {
        for (var httpMethod : methods) {
          addCandidate(candidates, path, httpMethod.name());
        }
      }
    });

    permissionService.createAllIfNotExists(new ArrayList<>(candidates.values()));
  }

  private Set<String> getPaths(RequestMappingInfo info) {

    var pathPatterns = info.getPathPatternsCondition();
    if (pathPatterns != null) {
      return pathPatterns.getPatterns().stream().map(Object::toString).collect(Collectors.toSet());
    }

    var patterns = info.getPatternsCondition();
    if (patterns != null) {
      return patterns.getPatterns();
    }

    return Set.of();
  }

  private void addCandidate(Map<String, Permission> candidates, String path, String method) {

    if (SecurityRules.isIgnored(path)
        || SecurityRules.isPublic(method, path)
        || SecurityRules.isAuthOnly(method, path)) {
      return;
    }

    String key = method + ":" + path;
    candidates.putIfAbsent(key, Permission.builder().path(path).method(method).build());
  }
}