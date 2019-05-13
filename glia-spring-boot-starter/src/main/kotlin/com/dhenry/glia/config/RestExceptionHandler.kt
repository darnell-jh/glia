package com.dhenry.glia.config

import org.springframework.dao.EmptyResultDataAccessException
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.ControllerAdvice
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.ResponseStatus

@ControllerAdvice
class RestExceptionHandler {

  @ExceptionHandler(EmptyResultDataAccessException::class)
  @ResponseStatus(HttpStatus.NOT_FOUND)
  fun handleEmptyResultDataAccessException() {
  }

}