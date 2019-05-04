package com.dhenry.glia.config

import com.google.common.base.Predicate
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.http.ResponseEntity
import springfox.documentation.builders.ApiInfoBuilder
import springfox.documentation.builders.PathSelectors
import springfox.documentation.service.ApiInfo
import springfox.documentation.service.Contact
import springfox.documentation.spi.DocumentationType
import springfox.documentation.spring.web.plugins.Docket
import springfox.documentation.swagger2.annotations.EnableSwagger2

@Configuration
@EnableSwagger2
abstract class SwaggerBaseConfig {

    @Bean
    fun docket(): Docket {
        return Docket(DocumentationType.SWAGGER_2)
            .select()
            .paths(paths())
            .build()
            .genericModelSubstitutes(ResponseEntity::class.java)
            .apiInfo(ApiInfoBuilder()
                .description("Initial backend service, will be split more into micro-services later")
                .title("Awesome Bugeting API")
                .version("v1")
                .contact(Contact("Darnell", "www.example.com", "lild12@gmail.com"))
                .build())

    }

    fun paths(): Predicate<String> {
        return PathSelectors.ant("/v1/**")
    }

    fun apiInfo(): ApiInfo {
        return ApiInfoBuilder()
                .description("A micro-service. Update description by overriding apiInfo in BaseConfig")
                .title("Awesome API")
                .version("v1")
                .contact(Contact("Guess Who", "www.example.com", "nobody@example.com"))
                .build()
    }


}