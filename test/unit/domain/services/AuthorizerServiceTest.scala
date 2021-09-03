package unit.domain.services

import domain.models.BenefitCategory.{CULTURE, FOOD, MEAL}
import domain.models.{Account, BenefitCategory, Transaction}
import domain.ports.out.{IManageAccountsPersistence, IObtainMccFromMerchants, IObtainSupportedCategoriesFromMCCs}
import domain.services.exceptions.{CouldNotProcessTransaction, NotEnoughBalanceException}
import domain.services.{AccountManagementService, AuthorizerService, MapCategoryService}
import org.mockito.ArgumentMatchers.any
import org.mockito.MockitoSugar
import org.mockito.scalatest.ResetMocksAfterEachTest
import org.scalatest.TryValues.convertTryToSuccessOrFailure
import org.scalatestplus.play.PlaySpec

import scala.util.{Failure, Success}


class AuthorizerServiceTest extends PlaySpec with MockitoSugar with ResetMocksAfterEachTest {

	private val mockIManageAccountsPersistence = mock[IManageAccountsPersistence]
	private val accountManagementService = new AccountManagementService(mockIManageAccountsPersistence)

	private val mockIObtainMccFromMerchants = mock[IObtainMccFromMerchants]
	private val mockIObtainSupportedCategoriesFromMCCs = mock[IObtainSupportedCategoriesFromMCCs]
	private val mapCategoryService = new MapCategoryService(mockIObtainMccFromMerchants, mockIObtainSupportedCategoriesFromMCCs)

	/*
		I could have mocked MapCategoryService and AccountManagementService instead of their dependencies, but
		due to the importance of AuthorizerService, I think it's valuable to test the entire domain logic within it
		and mock only external calls. Just to make sure.
	*/
	private val service = new AuthorizerService(accountManagementService, mapCategoryService)

	"AuthorizerServiceService#authorize" should {

		//Setup a general happy path for authorization. More detailed scenarios will be provided below.
		"Authorize transaction if all conditions are met" in {
			//Arrange
			val mealBalance = 1000
			val transactionTotalAmount = 500
			val newBalance = mealBalance - transactionTotalAmount;

			val account = StubAccount(mealBalance = mealBalance)
			val transaction = StubRestaurantTransaction(totalAmount = transactionTotalAmount)

			when(mockIManageAccountsPersistence.find(account.id)).thenReturn(Some(account))
			when(mockIObtainMccFromMerchants.findFromMerchant("PADARIA DO ZE")).thenReturn(None)
			when(mockIObtainSupportedCategoriesFromMCCs.findFromMcc("5811")).thenReturn(Some(BenefitCategory.MEAL))
			when(mockIManageAccountsPersistence.updateBenefitBalance(account.id, BenefitCategory.MEAL, newBalance, account.lastTransaction)).thenReturn(Success(()))

			//Act
			val authorizationResult = service.authorize(transaction)

			//Assert
			//Parameters verification here would be redundant since mock setups are taking specific expected values.
			authorizationResult mustBe Success(())
			verify(mockIManageAccountsPersistence, never).updateCashBalance(any[Int], any[Double], any[Some[String]])
		}

		"Authorize transaction if benefit category was overridden but there is also balance in overridden category" in {
			//Arrange
			val mealBalance = 100
			val cultureBalance = 50
			val transactionTotalAmount = 25
			val newBalance = cultureBalance - transactionTotalAmount;

			val account = StubAccount(mealBalance = mealBalance, cultureBalance = cultureBalance)
			val transaction = StubRestaurantTransaction(
				totalAmount = transactionTotalAmount,
				merchant = "CULTURA OVERR               SAO PAULO BR",
			)

			when(mockIManageAccountsPersistence.find(transaction.account)).thenReturn(Some(account))
			when(mockIObtainMccFromMerchants.findFromMerchant("CULTURA OVERR")).thenReturn(Some("5815"))
			when(mockIObtainSupportedCategoriesFromMCCs.findFromMcc("5815")).thenReturn(Some(BenefitCategory.CULTURE))
			when(mockIManageAccountsPersistence.updateBenefitBalance(transaction.account, BenefitCategory.CULTURE, newBalance, account.lastTransaction)).thenReturn(Success(()))

			//Act
			val authorizationResult = service.authorize(transaction)

			//Assert
			authorizationResult mustBe Success(())
			verify(mockIManageAccountsPersistence, never).updateCashBalance(any[Int], any[Double], any[Some[String]])
		}

		"Authorize transaction if benefit is valid, but benefit category has no balance to pay, but cash has" in {
			//Arrange
			val mealBalance = 50
			val cashBalance = 500
			val transactionTotalAmount = 250
			val newBalance = cashBalance - transactionTotalAmount;

			val account = StubAccount(mealBalance = mealBalance, cashBalance = cashBalance)
			val transaction = StubRestaurantTransaction(totalAmount = transactionTotalAmount)

			when(mockIManageAccountsPersistence.find(transaction.account)).thenReturn(Some(account))
			when(mockIObtainMccFromMerchants.findFromMerchant("PADARIA DO ZE")).thenReturn(None)
			when(mockIObtainSupportedCategoriesFromMCCs.findFromMcc("5811")).thenReturn(Some(BenefitCategory.MEAL))
			when(mockIManageAccountsPersistence.updateCashBalance(transaction.account, newBalance, account.lastTransaction)).thenReturn(Success(()))

			//Act
			val authorizationResult = service.authorize(transaction)

			//Assert
			authorizationResult mustBe Success(())
			verify(mockIManageAccountsPersistence, never).updateBenefitBalance(any[Int], any[BenefitCategory.Value], any[Double], any[Some[String]])
		}

		"Authorize transaction if benefit category is not supported but there is enough cash balance" in {
			//Arrange
			val cashBalance = 100
			val transactionTotalAmount = 10
			val newBalance = cashBalance - transactionTotalAmount;

			val account = StubAccount(mealBalance = 50, cashBalance = cashBalance)
			val transaction = StubRestaurantTransaction(
				totalAmount = transactionTotalAmount,
				merchant = "NAO SUPORTADO               SAO PAULO BR",
				mcc = "0000"
			)

			when(mockIManageAccountsPersistence.find(transaction.account)).thenReturn(Some(account))
			when(mockIObtainMccFromMerchants.findFromMerchant("NAO SUPORTADO")).thenReturn(None)
			when(mockIObtainSupportedCategoriesFromMCCs.findFromMcc("0000")).thenReturn(None)
			when(mockIManageAccountsPersistence.updateCashBalance(transaction.account, newBalance, account.lastTransaction)).thenReturn(Success(()))

			//Act
			val authorizationResult = service.authorize(transaction)

			//Assert
			authorizationResult mustBe Success(())
			verify(mockIManageAccountsPersistence, never).updateBenefitBalance(any[Int], any[BenefitCategory.Value], any[Double], any[Some[String]])
		}

		"Reject by not enough balance if benefit category and cash do not have enough balance to pay" in {
			//Arrange
			val transactionTotalAmount = 5000

			val account = StubAccount()
			val transaction = StubRestaurantTransaction(totalAmount = transactionTotalAmount)

			when(mockIManageAccountsPersistence.find(transaction.account)).thenReturn(Some(account))
			when(mockIObtainMccFromMerchants.findFromMerchant("PADARIA DO ZE")).thenReturn(None)
			when(mockIObtainSupportedCategoriesFromMCCs.findFromMcc("5811")).thenReturn(Some(BenefitCategory.MEAL))

			//Act
			val authorizationResult = service.authorize(transaction)

			//Assert
			authorizationResult.isFailure mustBe true
			authorizationResult.failure.exception mustEqual NotEnoughBalanceException()
			verify(mockIManageAccountsPersistence, never).updateCashBalance(any[Int], any[Double], any[Some[String]])
			verify(mockIManageAccountsPersistence, never).updateBenefitBalance(any[Int], any[BenefitCategory.Value], any[Double], any[Some[String]])
		}

		"Reject by not enough balance if benefit category is not supported " +
			"and cash does not have enough balance to pay " +
			"even if benefits balance have enough to pay" in {
			//Arrange
			val cashBalance = 5
			val transactionTotalAmount = 20

			val account = StubAccount(cashBalance = cashBalance)
			val transaction = StubRestaurantTransaction(
				totalAmount = transactionTotalAmount,
				merchant = "NAO SUPORTADO               SAO PAULO BR",
				mcc = "0000"
			)

			when(mockIManageAccountsPersistence.find(transaction.account)).thenReturn(Some(account))
			when(mockIObtainMccFromMerchants.findFromMerchant("NAO SUPORTADO")).thenReturn(None)
			when(mockIObtainSupportedCategoriesFromMCCs.findFromMcc("0000")).thenReturn(None)

			//Act
			val authorizationResult = service.authorize(transaction)

			//Assert
			authorizationResult.isFailure mustBe true
			authorizationResult.failure.exception mustEqual NotEnoughBalanceException()
			verify(mockIManageAccountsPersistence, never).updateCashBalance(any[Int], any[Double], any[Some[String]])
			verify(mockIManageAccountsPersistence, never).updateBenefitBalance(any[Int], any[BenefitCategory.Value], any[Double], any[Some[String]])
		}

		"NOT authorize transaction when account is not found" in {
			//Arrange
			val transaction = StubRestaurantTransaction(account = 123)

			//Act
			when(mockIManageAccountsPersistence.find(transaction.account)).thenReturn(None)

			//Act
			val authorizationResult = service.authorize(transaction)

			//Assert
			authorizationResult.isFailure mustBe true
			authorizationResult.failure.exception mustEqual CouldNotProcessTransaction("Account not found")
			verify(mockIManageAccountsPersistence, never).updateCashBalance(any[Int], any[Double], any[Some[String]])
			verify(mockIManageAccountsPersistence, never).updateBenefitBalance(any[Int], any[BenefitCategory.Value], any[Double], any[Some[String]])
		}

		"NOT authorize transaction when there was an error updating benefit balance" in {
			//Arrange
			val mealBalance = 1000
			val transactionTotalAmount = 500
			val newBalance = mealBalance - transactionTotalAmount;

			val account = StubAccount(mealBalance = mealBalance)
			val transaction = StubRestaurantTransaction(totalAmount = transactionTotalAmount)

			when(mockIManageAccountsPersistence.find(account.id)).thenReturn(Some(account))
			when(mockIObtainMccFromMerchants.findFromMerchant("PADARIA DO ZE")).thenReturn(None)
			when(mockIObtainSupportedCategoriesFromMCCs.findFromMcc("5811")).thenReturn(Some(BenefitCategory.MEAL))
			when(mockIManageAccountsPersistence.updateBenefitBalance(account.id, BenefitCategory.MEAL, newBalance, account.lastTransaction))
				.thenReturn(Failure(new Exception("Error updating balance")))

			//Act
			val authorizationResult = service.authorize(transaction)


			//Assert
			authorizationResult.isFailure mustBe true
			authorizationResult.failure.exception mustEqual CouldNotProcessTransaction("Error updating balance")
			verify(mockIManageAccountsPersistence, never).updateCashBalance(any[Int], any[Double], any[Some[String]])
		}

		"NOT authorize transaction when there was an error updating cash balance" in {
			//Arrange
			val mealBalance = 10
			val cashBalance = 100
			val transactionTotalAmount = 50
			val newBalance = cashBalance - transactionTotalAmount;

			val account = StubAccount(mealBalance = mealBalance)
			val transaction = StubRestaurantTransaction(totalAmount = transactionTotalAmount)

			when(mockIManageAccountsPersistence.find(account.id)).thenReturn(Some(account))
			when(mockIObtainMccFromMerchants.findFromMerchant("PADARIA DO ZE")).thenReturn(None)
			when(mockIObtainSupportedCategoriesFromMCCs.findFromMcc("5811")).thenReturn(Some(BenefitCategory.MEAL))
			when(mockIManageAccountsPersistence.updateCashBalance(account.id, newBalance, account.lastTransaction))
				.thenReturn(Failure(new Exception("Error updating balance")))

			//Act
			val authorizationResult = service.authorize(transaction)

			//Assert
			authorizationResult.isFailure mustBe true
			authorizationResult.failure.exception mustEqual CouldNotProcessTransaction("Error updating balance")
			verify(mockIManageAccountsPersistence, never).updateBenefitBalance(any[Int], any[BenefitCategory.Value], any[Double], any[Some[String]])
		}

		"NOT process transaction if any fields are in a invalid state" in {
			//Arrange
			val transactionEmptyMcc = StubRestaurantTransaction(mcc = "")
			val transactionEmptyMerchant = StubRestaurantTransaction(merchant = "")
			val transactionMccMoreThan4Digits = StubRestaurantTransaction(mcc = "3131231")
			val transactionMccNotANumber = StubRestaurantTransaction(mcc = "not-a-number")
			val transactionMerchantNot40Characters = StubRestaurantTransaction(merchant = "not-40-characters")

			//Act
			val authorizationEmptyMcc = service.authorize(transactionEmptyMcc)
			val authorizationEmptyMerchant = service.authorize(transactionEmptyMerchant)
			val authorizationMccMoreThan4Digits = service.authorize(transactionMccMoreThan4Digits)
			val authorizationMccNotANumber = service.authorize(transactionMccNotANumber)
			val authorizationMerchantNot40Characters = service.authorize(transactionMerchantNot40Characters)

			//Assert
			authorizationEmptyMcc.isFailure mustBe true
			authorizationEmptyMerchant.isFailure mustBe true
			authorizationMccMoreThan4Digits.isFailure mustBe true
			authorizationMccNotANumber.isFailure mustBe true
			authorizationMerchantNot40Characters.isFailure mustBe true

			verify(mockIManageAccountsPersistence, never).updateBenefitBalance(any[Int], any[BenefitCategory.Value], any[Double], any[Some[String]])
			verify(mockIManageAccountsPersistence, never).find(any[Int])
			verify(mockIObtainMccFromMerchants, never).findFromMerchant(any[String])
			verify(mockIObtainSupportedCategoriesFromMCCs, never).findFromMcc(any[String])
			verify(mockIManageAccountsPersistence, never).updateCashBalance(any[Int], any[Double], any[Some[String]])
		}
	}

	private def StubAccount(
		id:Int = 1,
		mealBalance:Double = 100,
		foodBalance:Double = 100,
		cultureBalance:Double = 100,
		cashBalance:Double = 100,
		lastTransaction: Option[String] = None
	): Account = Account(
			id = id,
			benefitBalances = Map(MEAL -> mealBalance, FOOD -> foodBalance, CULTURE -> cultureBalance),
			cashBalance = cashBalance,
			lastTransaction = lastTransaction
		)

	private def StubRestaurantTransaction(
		account: Int = 1,
		totalAmount: Double = 10,
		mcc: String = "5811", // Restaurant MCC
		merchant: String = "PADARIA DO ZE               SAO PAULO BR"
	): Transaction = Transaction (
		account = account,
		totalAmount = totalAmount,
		mcc = mcc,
		merchant = merchant
	)
}






















