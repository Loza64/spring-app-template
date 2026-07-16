package com.server.app.common.exceptions.response;

import lombok.AllArgsConstructor;

@AllArgsConstructor
public class ExceptionResponse {
  private final int status;
  private final String message;
}
