package com.github.jamedge.moonlight.core.api.handlers

import akka.http.scaladsl.model._
import akka.http.scaladsl.model.headers.BasicHttpCredentials
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server._
import akka.http.scaladsl.server.directives.Credentials
import akka.http.scaladsl.testkit.ScalatestRouteTest
import org.json4s.DefaultFormats
import org.json4s.jackson.Serialization.read
import org.scalatest.matchers.should.Matchers
import org.scalatest.funspec.AnyFunSpec
import org.slf4j.LoggerFactory

class RejectionHandlerBuilderSpec extends AnyFunSpec with Matchers with ScalatestRouteTest {

  implicit val testLogger = LoggerFactory.getLogger(getClass)
  implicit val subject = RejectionHandlerBuilder.build()
  implicit val formats = DefaultFormats

  case class User(name: String)

  def testUserPassAuthenticator(credentials: Credentials): Option[User] =
    credentials match {
      case Credentials.Provided(id) => Some(User(id))
      case _                        => None
    }

  val testAuthorizedSiteAdmins = Set("TestAllowedUser")
  def hasAdminPermissions(user: User): Boolean =
    testAuthorizedSiteAdmins.contains(user.name)

  val testRoute =
    Route.seal(
      get {
        path("test") {
          complete("Test response..")
        } ~
        path("test-parameter") {
          parameters("test_param") { testParamString =>
            complete(s"Test response with $testParamString")
          }
        }
      } ~
      post {
        authenticateBasic(realm = "secure site", testUserPassAuthenticator) { user =>
          path("test-authorization") {
            authorize(hasAdminPermissions(user)) {
              complete(s"'${user.name}' visited test-authorization site!")
            }
          }
        }
      }
    )

  describe("#build") {
    it("should return correct 200 response when request is correct") {
      Get("/test") ~> testRoute ~> check {
        status shouldBe StatusCodes.OK
        contentType shouldBe ContentTypes.`text/plain(UTF-8)`
        responseAs[String] shouldBe "Test response.."
      }
      Post("/test-authorization") ~> addCredentials(BasicHttpCredentials("TestAllowedUser", "TestAllowedUserPassword")) ~>
        testRoute ~> check {
          status shouldBe StatusCodes.OK
          contentType shouldBe ContentTypes.`text/plain(UTF-8)`
          responseAs[String] shouldBe s"'TestAllowedUser' visited test-authorization site!"
        }
    }
    it("should return correct 403 error response when request is unauthorized") {
      Post("/test-authorization") ~> addCredentials(BasicHttpCredentials("TestNotAllowedUser", "TestNotAllowedUserPassword")) ~>
        testRoute ~> check {
          status shouldBe StatusCodes.Forbidden
          contentType shouldBe ContentTypes.`application/json`

          val errorResponseString = responseAs[String]
          val errorResponse = read[ErrorResponse](errorResponseString)

          errorResponse.status shouldBe "403"
          errorResponse.path shouldBe "/test-authorization"
          errorResponse.traceId shouldBe "0"
          errorResponse.error shouldBe ErrorResponseDetails(
            "FORBIDDEN",
            "The request was a legal request, but the server is refusing to respond to it.")
      }
    }
    it("should return correct 422 error response when param is missing") {
      Get("/test-parameter") ~> testRoute ~> check {
        status shouldBe StatusCodes.UnprocessableEntity
        contentType shouldBe ContentTypes.`application/json`

        val errorResponseString = responseAs[String]
        val errorResponse = read[ErrorResponse](errorResponseString)

        errorResponse.status shouldBe "422"
        errorResponse.path shouldBe "/test-parameter"
        errorResponse.traceId shouldBe "0"
        errorResponse.error shouldBe ErrorResponseDetails(
          "UNPROCESSABLE_ENTITY",
          "Parameter test_param is missing!")
      }
    }
    it("should return correct 405 error response when method is not allowed") {
      Put("/test") ~> testRoute ~> check {
        status shouldBe StatusCodes.MethodNotAllowed
        contentType shouldBe ContentTypes.`application/json`

        val errorResponseString = responseAs[String]
        val errorResponse = read[ErrorResponse](errorResponseString)

        errorResponse.status shouldBe "405"
        errorResponse.path shouldBe "/test"
        errorResponse.traceId shouldBe "0"
        errorResponse.error shouldBe ErrorResponseDetails(
          "METHOD_NOT_ALLOWED",
          "Method not allowed! Supported: GET or POST!")
      }
    }
    it("should return correct 404 error response when non-existent route is queried") {
      Get("/non-existent") ~> testRoute ~> check {
        status shouldBe StatusCodes.NotFound
        contentType shouldBe ContentTypes.`application/json`

        val errorResponseString = responseAs[String]
        val errorResponse = read[ErrorResponse](errorResponseString)

        errorResponse.status shouldBe "404"
        errorResponse.path shouldBe "/non-existent"
        errorResponse.traceId shouldBe "0"
        errorResponse.error shouldBe ErrorResponseDetails(
          "NOT_FOUND",
          "The requested resource could not be found but may be available again in the future.")
      }
    }
  }
}
