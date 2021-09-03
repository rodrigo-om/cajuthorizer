package unit.controllers.authorization

import controllers.authorization.{AuthorizationHTTPAdapter, TransactionRequest, TransactionResponse}
import domain.models.Transaction
import domain.services.AuthorizerService
import domain.services.exceptions.{CouldNotProcessTransaction, NotEnoughBalanceException}
import org.mockito.ArgumentMatchersSugar.any
import org.mockito.MockitoSugar
import org.mockito.scalatest.ResetMocksAfterEachTest
import org.scalatestplus.play._

import scala.util.{Failure, Success}

class AuthorizationHTTPAdapterTest extends PlaySpec with MockitoSugar with ResetMocksAfterEachTest {

  private val mockAuthorizerService = mock[AuthorizerService]
  private val adapter = new AuthorizationHTTPAdapter(mockAuthorizerService)

  "AuthorizationHTTPAdapter#Authorize" should {

    "Return Code 00 when transaction is approved" in {
      //Arrange
      val transactionRequest = stubTransactionRequest()
      val expectedTransaction = Transaction(
        account = 1,
        totalAmount = 10,
        mcc = "5811",
        merchant = "PADARIA DO ZE               SAO PAULO BR"
      )

      when(mockAuthorizerService.authorize(expectedTransaction)).thenReturn(Success(()))

      //Act
      val responseCode = adapter.authorize(transactionRequest)

      //Assert
      responseCode mustBe TransactionResponse(code = "00")
    }

	  "Return code 51 when transaction is rejected by not enough balance" in {
		  //Arrange
		  val transactionRequest = stubTransactionRequest()
		  val expectedTransaction = Transaction(
			  account = 1,
			  totalAmount = 10,
			  mcc = "5811",
			  merchant = "PADARIA DO ZE               SAO PAULO BR"
		  )

		  when(mockAuthorizerService.authorize(expectedTransaction)).thenReturn(Failure(NotEnoughBalanceException()))

		  //Act
		  val responseCode = adapter.authorize(transactionRequest)

		  //Assert
		  responseCode mustBe TransactionResponse(code = "51")
	  }

	  "Return 07 when transaction is rejected by a expected reason" in {
		  //Arrange
		  val transactionRequest = stubTransactionRequest()
		  val expectedTransaction = Transaction(
			  account = 1,
			  totalAmount = 10,
			  mcc = "5811",
			  merchant = "PADARIA DO ZE               SAO PAULO BR"
		  )

		  when(mockAuthorizerService.authorize(expectedTransaction)).thenReturn(Failure(CouldNotProcessTransaction("any-message")))

		  //Act
		  val responseCode = adapter.authorize(transactionRequest)

		  //Assert
		  responseCode mustBe TransactionResponse(code = "07")
	  }

	  "Return 07 when transaction is rejected by any other reason" in {
		  //Arrange
		  val transactionRequest = stubTransactionRequest()
		  val expectedTransaction = Transaction(
			  account = 1,
			  totalAmount = 10,
			  mcc = "5811",
			  merchant = "PADARIA DO ZE               SAO PAULO BR"
		  )

		  when(mockAuthorizerService.authorize(expectedTransaction)).thenReturn(Failure(new Exception("any-message")))

		  //Act
		  val responseCode = adapter.authorize(transactionRequest)

		  //Assert
		  responseCode mustBe TransactionResponse(code = "07")
	  }

	  "Return 07 if any transaction fields are in a invalid state" in {
		  //Arrange
		  val transactionEmptyMcc = stubTransactionRequest(mcc = "")
		  val transactionEmptyAccount = stubTransactionRequest(account = "")
		  val transactionEmptyMerchant = stubTransactionRequest(merchant = "")
		  val transactionAccountNotANumber = stubTransactionRequest(account = "not-a-number")

		  //Act
		  val responseCodeEmptyMcc = adapter.authorize(transactionEmptyMcc)
		  val responseCodeEmptyAccount = adapter.authorize(transactionEmptyAccount)
		  val responseCodeEmptyMerchant = adapter.authorize(transactionEmptyMerchant)
		  val responseCodeMccNotANumber = adapter.authorize(transactionAccountNotANumber)

		  //Assert
		  responseCodeEmptyMcc mustBe TransactionResponse(code = "07")
		  responseCodeEmptyMerchant mustBe TransactionResponse(code = "07")
		  responseCodeEmptyAccount mustBe TransactionResponse(code = "07")
		  responseCodeMccNotANumber mustBe TransactionResponse(code = "07")
		  verify(mockAuthorizerService, never).authorize(any[Transaction])
	  }
  }

  private def stubTransactionRequest (
    account: String = "1",
    totalAmount: Double = 10,
    mcc: String = "5811", // Restaurant MCC
    merchant: String = "PADARIA DO ZE               SAO PAULO BR"
  ): TransactionRequest = TransactionRequest (
    account = account,
    totalAmount = totalAmount,
    mcc = mcc,
    merchant = merchant
  )
}








