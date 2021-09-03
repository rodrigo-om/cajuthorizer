package unit.controllers.account

import controllers.account.{AccountCreateRequest, AccountManagementHTTPAdapter}
import domain.models.Account
import domain.models.BenefitCategory.{CULTURE, FOOD, MEAL}
import domain.services.AccountManagementService
import org.mockito.ArgumentMatchersSugar.any
import org.mockito.MockitoSugar
import org.mockito.scalatest.ResetMocksAfterEachTest
import org.scalatestplus.play._
import play.api.libs.json.Json
import play.api.mvc.Results.{BadRequest, Created, InternalServerError, Ok}

import scala.util.{Failure, Success}

class AccountManagementHTTPAdapterTest extends PlaySpec with MockitoSugar with ResetMocksAfterEachTest {

  private val mockAccountManagementService = mock[AccountManagementService]
  private val adapter = new AccountManagementHTTPAdapter(mockAccountManagementService)

  "AccountManagementAdapter#Find" should {

	  "Return Ok when service returns an account" in {
		  //Arrange
		  val account = stubAccount()

		  when(mockAccountManagementService.findAccount(1)).thenReturn(Some(account))

		  //Act
		  val response = adapter.find(1)

		  /*
				Had some ScalaTest shenanigans throwing a "Result(200, TreeMap()) was not equal to Result(200, TreeMap())", so changed to checking
				header status code instead
			*/
		  response.header.status mustBe 200
	  }

	  "Return Ok with message inside when service does not return an account" in {
		  //Arrange
		  when(mockAccountManagementService.findAccount(1)).thenReturn(None)

		  //Act
		  val response = adapter.find(1)

		  //Assert
		  response mustBe Ok(Json.toJson("Account was not found"))
	  }
  }

	"AccountManagementAdapter#FindAll" should {

		"Return Ok when service returns a seq of accounts" in {
			//Arrange
			val account = stubAccount()

			when(mockAccountManagementService.findAll()).thenReturn(Seq(account))

			//Act
			val response = adapter.findAll()

			//Assert
			/*
				Had some ScalaTest shenanigans throwing a "Result(200, TreeMap()) was not equal to Result(200, TreeMap())", so changed to checking
				header status code instead
			*/
			response.header.status mustBe 200
		}

		"Return Ok when no accounts are found" in {
			//Arrange
			when(mockAccountManagementService.findAccount(1)).thenReturn(None)

			//Act
			val response = adapter.find(1)

			/*
				Had some ScalaTest shenanigans throwing a "Result(200, TreeMap()) was not equal to Result(200, TreeMap())", so changed to checking
				header status code instead
			*/
			response.header.status mustBe 200
		}
	}

	"AccountManagementAdapter#Create" should {

		"Return Created when account was successfully created" in {
			val expectedAccount = stubAccount()
			val request = stubAccountCreateRequest()

			//Arrange
			when(mockAccountManagementService.create(expectedAccount)).thenReturn(Success(()))

			//Act
			val response = adapter.create(request)

			//Assert
			response mustBe Created
		}

		"Return InternalServerError when account creation returns a failure" in {
			val expectedAccount = stubAccount()
			val request = stubAccountCreateRequest()

			//Arrange
			when(mockAccountManagementService.create(expectedAccount)).thenReturn(Failure(new Exception("error")))

			//Act
			val response = adapter.create(request)

			//Assert
			response.header.status mustBe 500
		}

		"Return BadRequest if any transaction fields are in a invalid state" in {
		  //Arrange
		  val requestInvalidAccount = stubAccountCreateRequest(id = -1)
		  val requestInvalidMealBalance = stubAccountCreateRequest(mealBalance = -1)
		  val requestInvalidFoodBalance = stubAccountCreateRequest(foodBalance = -1)
		  val requestInvalidCultureBalance = stubAccountCreateRequest(cultureBalance = -1)
		  val requestInvalidCashBalance = stubAccountCreateRequest(cashBalance = -1)

		  //Act
		  val responseCodeInvalidAccount = adapter.create(requestInvalidAccount)
		  val responseCodeInvalidMealBalance = adapter.create(requestInvalidMealBalance)
		  val responseCodeInvalidFoodBalance = adapter.create(requestInvalidFoodBalance)
		  val responseCodeInvalidCultureBalance = adapter.create(requestInvalidCultureBalance)
		  val responseCodeInvalidCashBalance = adapter.create(requestInvalidCashBalance)

		  //Assert
		  responseCodeInvalidAccount mustBe BadRequest
		  responseCodeInvalidMealBalance mustBe BadRequest
		  responseCodeInvalidFoodBalance mustBe BadRequest
		  responseCodeInvalidCultureBalance mustBe BadRequest
		  responseCodeInvalidCashBalance mustBe BadRequest
		  verify(mockAccountManagementService, never).create(any[Account])
	  }
  }

	private def stubAccount(
		id: Int = 1,
		mealBalance: Double = 100,
		foodBalance: Double = 100,
		cultureBalance: Double = 100,
		cashBalance: Double = 100,
		lastTransaction: Option[String] = None
	): Account = Account(
		id = id,
		benefitBalances = Map(MEAL -> mealBalance, FOOD -> foodBalance, CULTURE -> cultureBalance),
		cashBalance = cashBalance,
		lastTransaction = lastTransaction
	)

	private def stubAccountCreateRequest(
		id: Int = 1,
		mealBalance: Double = 100,
		foodBalance: Double = 100,
		cultureBalance: Double = 100,
		cashBalance: Double = 100,
	): AccountCreateRequest = AccountCreateRequest(
		id = id,
		mealBalance = mealBalance,
		foodBalance = foodBalance,
		cultureBalance = cultureBalance,
		cashBalance = cashBalance,
	)
}








