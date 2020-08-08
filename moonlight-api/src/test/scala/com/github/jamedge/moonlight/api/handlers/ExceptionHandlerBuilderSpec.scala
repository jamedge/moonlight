package com.github.jamedge.moonlight.api.handlers

import akka.http.scaladsl.model._
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server._
import akka.http.scaladsl.testkit.ScalatestRouteTest
import com.fasterxml.jackson.core.JsonParseException
import org.json4s.DefaultFormats
import org.scalatest.matchers.should.Matchers
import org.scalatest.funspec.AnyFunSpec
import org.slf4j.LoggerFactory

class ExceptionHandlerBuilderSpec extends AnyFunSpec with Matchers with ScalatestRouteTest {

  implicit val testLogger = LoggerFactory.getLogger(getClass)
  val subject = ExceptionHandlerBuilder.build()
  implicit val formats = DefaultFormats

  val testRoute =
    Route.seal(
      handleExceptions(subject) {
        get {
          path("test-exception") {
            throw new NullPointerException("Test exception...")
          } ~
          path("test-exception-parsing") {
            throw new ApiException(
              new JsonParseException(null, "Parsing problems exception..."),
              StatusCodes.BadRequest,
              Some("Error during unmarshalling from request json.")
            )
          } ~
            path("test-exception-parsing-default") {
              throw new ApiException(
                new JsonParseException(null, "Parsing problems default exception.."),
                StatusCodes.BadRequest
              )
            }
        }
      }
    )

  describe("#build") {
    describe("should return correct 500 error response when there's an error during processing the request") {
      it ("by testing trowing of Exception") {
        Get("/test-exception") ~> testRoute ~> check {
          status shouldBe StatusCodes.InternalServerError
          contentType shouldBe ContentTypes.`application/json`

          val errorResponseString = responseAs[String]
          val errorResponse = ErrorResponse.unmarshallErrorResponse(errorResponseString)

          errorResponse.status shouldBe "500"
          errorResponse.path shouldBe "/test-exception"
          errorResponse.traceId shouldBe "0"
          errorResponse.error shouldBe ErrorResponseDetails(
            "INTERNAL_SERVER_ERROR",
            "There was an internal server error. Check API logs for details.")
        }
      }
      it ("by testing trowing of ApiException encapsulating JsonParseException") {
        Get("/test-exception-parsing") ~> testRoute ~> check {
          status shouldBe StatusCodes.BadRequest
          contentType shouldBe ContentTypes.`application/json`

          val errorResponseString = responseAs[String]
          val errorResponse = ErrorResponse.unmarshallErrorResponse(errorResponseString)

          errorResponse.status shouldBe "400"
          errorResponse.path shouldBe "/test-exception-parsing"
          errorResponse.traceId shouldBe "0"
          errorResponse.error shouldBe ErrorResponseDetails(
            "BAD_REQUEST",
            "Error during unmarshalling from request json.")
        }
      }
      it ("by testing trowing of ApiException encapsulating JsonParseException with default message") {
        Get("/test-exception-parsing-default") ~> testRoute ~> check {
          status shouldBe StatusCodes.BadRequest
          contentType shouldBe ContentTypes.`application/json`

          val errorResponseString = responseAs[String]
          val errorResponse = ErrorResponse.unmarshallErrorResponse(errorResponseString)

          errorResponse.status shouldBe "400"
          errorResponse.path shouldBe "/test-exception-parsing-default"
          errorResponse.traceId shouldBe "0"
          errorResponse.error shouldBe ErrorResponseDetails(
            "BAD_REQUEST",
            "The request contains bad syntax or cannot be fulfilled.")
        }
      }
    }
  }
}
