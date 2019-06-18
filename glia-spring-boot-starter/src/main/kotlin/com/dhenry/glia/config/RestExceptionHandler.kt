package com.dhenry.glia.config

import org.springframework.dao.EmptyResultDataAccessException
import org.springframework.http.HttpStatus
import org.springframework.http.converter.HttpMessageNotReadableException
import org.springframework.web.bind.annotation.ControllerAdvice
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.ResponseStatus

@ControllerAdvice
class RestExceptionHandler {

  @ExceptionHandler(EmptyResultDataAccessException::class)
  @ResponseStatus(HttpStatus.NOT_FOUND)
  fun handleEmptyResultDataAccessException() {
  }

  @ExceptionHandler(HttpMessageNotReadableException::class)
  @ResponseStatus(HttpStatus.BAD_REQUEST)
  fun handleHttpMessageNotReadableExcptions() {
  }

  @ExceptionHandler(Exception::class)
  @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
  fun handleOtherExceptions() {
  }

}