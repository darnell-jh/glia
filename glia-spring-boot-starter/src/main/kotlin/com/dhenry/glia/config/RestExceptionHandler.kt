package com.dhenry.glia.config

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.dao.EmptyResultDataAccessException
import org.springframework.http.HttpStatus
import org.springframework.http.converter.HttpMessageNotReadableException
import org.springframework.web.bind.annotation.ControllerAdvice
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.ResponseStatus
import javax.validation.ConstraintViolationException

@ControllerAdvice
class RestExceptionHandler {

  companion object {
    private val LOGGER: Logger = LoggerFactory.getLogger(RestExceptionHandler::class.java)
  }

  @ExceptionHandler(EmptyResultDataAccessException::class)
  @ResponseStatus(HttpStatus.NOT_FOUND)
  fun handleEmptyResultDataAccessException(ex: EmptyResultDataAccessException) {
    LOGGER.error("No data found", ex)
  }

  @ExceptionHandler(HttpMessageNotReadableException::class)
  @ResponseStatus(HttpStatus.BAD_REQUEST)
  fun handleHttpMessageNotReadableExcptions(ex: HttpMessageNotReadableException) {
    LOGGER.error("Failed to read message", ex)
  }

  @ExceptionHandler(ConstraintViolationException::class)
  @ResponseStatus(HttpStatus.BAD_REQUEST)
  fun handleConstraintViolationException(ex: ConstraintViolationException) {
    LOGGER.error("Constraint violated", ex)
  }

  @ExceptionHandler(Exception::class)
  @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
  fun handleOtherExceptions(ex: Exception) {
    LOGGER.error("Unknown exception caught", ex)
  }

}