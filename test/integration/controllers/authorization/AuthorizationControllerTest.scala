package integration.controllers.authorization

import controllers.authorization.{AuthorizationHTTPAdapter, TransactionRequest, TransactionResponse}
import org.mockito.ArgumentMatchers.any
import org.mockito.MockitoSugar
import org.mockito.scalatest.ResetMocksAfterEachTest
import org.scalatestplus.play._
import play.api
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.test.Helpers._
import play.api.test._

class AuthorizationControllerTest extends PlaySpec with MockitoSugar with ResetMocksAfterEachTest {

  private val mockAuthorizerAdapter = mock[AuthorizationHTTPAdapter]

  "AuthorizationController#POSTAuthorize" should {

    val application = new GuiceApplicationBuilder()
      .overrides(api.inject.bind[AuthorizationHTTPAdapter].toInstance(mockAuthorizerAdapter))
      .build()

    "Serialize request correctly and return OK with correct serialized response" in {
      val transactionBody: String =
        """
          |{
          | "account": "1",
          | "totalAmount": 100,
          | "mcc": "5811",
          | "merchant": "PADARIA DO ZE               SAO PAULO BR"
          |}
      """.stripMargin

      val expectedTransactionRequest = TransactionRequest (
        account = "1",
        totalAmount = 100,
        mcc = "5811",
        merchant = "PADARIA DO ZE               SAO PAULO BR"
      )

      when(mockAuthorizerAdapter.authorize(expectedTransactionRequest)).thenReturn(TransactionResponse(code = "00"))

      val request = FakeRequest(POST, "/authorize").withBody(transactionBody).withHeaders(("Content-Type","application/json"))
      val authorize = route(application, request).get

      status(authorize) mustBe OK
      contentType(authorize) mustBe Some("application/json")
      contentAsString(authorize) must include ("""{"code":"00"}""")
    }

    "Receive request but return BadRequest due to malformed body" in {
      val transactionBody: String =
        """
          |{
          | "totalAmount": 100,
          | "mcc": "5811",
          | "merchant": "PADARIA DO ZE               SAO PAULO BR"
          |}
      """.stripMargin


      val request = FakeRequest(POST, "/authorize").withBody(transactionBody).withHeaders(("Content-Type","application/json"))
      val authorize = route(application, request).get

      status(authorize) mustBe BAD_REQUEST
      verify(mockAuthorizerAdapter, never).authorize(any[TransactionRequest])
    }
  }
}




