package com.dhenry.glia.config

import com.dhenry.glia.service.ProjectRegistry
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.web.servlet.handler.HandlerInterceptorAdapter
import java.util.*
import javax.servlet.http.HttpServletRequest
import javax.servlet.http.HttpServletResponse

const val X_PROJECT = "x-project"

class LoggingInterceptor: HandlerInterceptorAdapter() {

  companion object {
    private val LOGGER: Logger = LoggerFactory.getLogger(LoggingInterceptor::class.java)
  }

  override fun preHandle(request: HttpServletRequest, response: HttpServletResponse, handler: Any): Boolean {
    // Log request
    val projectLog = Optional.ofNullable(ProjectRegistry.projectId)
        .map{ " for project $it" }
        .orElse("")
    val queryParamLog = Optional.ofNullable(request.queryString)
        .map{ "?$it" }
        .orElse("")
    LOGGER.info("Received request to {} {}{}{}", request.method, request.requestURI, queryParamLog, projectLog)
    return true
  }
}

class ProjectInterceptor: HandlerInterceptorAdapter() {

  companion object {
    private val LOGGER: Logger = LoggerFactory.getLogger(LoggingInterceptor::class.java)
  }

  override fun preHandle(request: HttpServletRequest, response: HttpServletResponse, handler: Any): Boolean {
    // Log request
    ProjectRegistry.projectId = request.getHeader(X_PROJECT)

    return true
  }
}