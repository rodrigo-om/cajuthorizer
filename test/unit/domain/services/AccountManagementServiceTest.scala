package unit.domain.services

import domain.models.{Account, BenefitCategory}
import domain.models.BenefitCategory.{CULTURE, FOOD, MEAL}
import domain.ports.out.IManageAccountsPersistence
import domain.services.AccountManagementService
import domain.services.exceptions.CouldNotCreateAccountException
import org.mockito.MockitoSugar
import org.mockito.scalatest.ResetMocksAfterEachTest
import org.scalatest.TryValues.convertTryToSuccessOrFailure
import org.scalatestplus.play.PlaySpec

import scala.util.{Failure, Success}


class AccountManagementServiceTest extends PlaySpec with MockitoSugar with ResetMocksAfterEachTest {

	private val mockIManageAccountsPersistence = mock[IManageAccountsPersistence]
	private val service = new AccountManagementService(mockIManageAccountsPersistence)

	"AccountManagementService#find" should {

		"Return correctly when repository returns account" in {
			//Arrange
			val foundAccount = Account(1, Map(MEAL -> 0), 100, Some("a-transaction-uuid"))
			val id = 1
			when(mockIManageAccountsPersistence.find(1)).thenReturn(Some(foundAccount))

			//Act
			val maybeAccount = service.findAccount(id)

			//Assert
			maybeAccount.value mustEqual foundAccount
		}

		"Return None when repository does not return account" in {
			//Arrange
			val id = 1
			when(mockIManageAccountsPersistence.find(1)).thenReturn(None)

			//Act
			val maybeAccount = service.findAccount(id)

			//Assert
			maybeAccount mustBe None
		}
	}

	"AccountManagementService#create" should {

		"Be successful if all validations are met and repository returns successfully" in {
			//Arrange
			val accountToCreate = Account(
				id = 1,
				benefitBalances = Map(MEAL -> 0, FOOD -> 100, CULTURE -> 100),
				cashBalance = 100,
				lastTransaction = None
			)

			when(mockIManageAccountsPersistence.find(1)).thenReturn(None)
			when(mockIManageAccountsPersistence.create(accountToCreate)).thenReturn(Success(()))

			//Act
			val tryAccountCreated = service.create(accountToCreate)

			//Assert
			tryAccountCreated mustBe Success(())
		}

		"Return Failure when cash balance is lower than zero" in {
			//Arrange
			val accountToCreate = Account(
				id = 1,
				benefitBalances = Map(MEAL -> 0, FOOD -> 100, CULTURE -> 100),
				cashBalance = -10,
				lastTransaction = None
			)

			//Act
			val tryAccountCreated = service.create(accountToCreate)

			//Assert
			tryAccountCreated.isFailure mustBe true
			tryAccountCreated.failure.exception mustEqual CouldNotCreateAccountException("Cash balance can't be lower than 0")
		}

		"Return Failure when last transaction is for some reason defined" in {
			//Arrange
			val accountToCreate = Account(
				id = 1,
				benefitBalances = Map(MEAL -> 0, FOOD -> 100, CULTURE -> -10),
				cashBalance = 100,
				lastTransaction = Some("a-last-transaction-uuid-that-should-not-be-here")
			)

			//Act
			val tryAccountCreated = service.create(accountToCreate)

			//Assert
			tryAccountCreated.isFailure mustBe true
			tryAccountCreated.failure.exception mustEqual CouldNotCreateAccountException("Last transaction must not have a value")
		}

		"Return Failure when some benefit category balance is lower than zero" in {
			//Arrange
			val accountToCreate = Account(
				id = 1,
				benefitBalances = Map(MEAL -> 0, FOOD -> 100, CULTURE -> -10),
				cashBalance = 100,
				lastTransaction = None
			)

			//Act
			val tryAccountCreated = service.create(accountToCreate)

			//Assert
			tryAccountCreated.isFailure mustBe true
			tryAccountCreated.failure.exception mustEqual CouldNotCreateAccountException("Benefit balances can't be lower than 0")
		}

		"Return failure if repository find an already existing account with same id" in {
			//Arrange
			val accountToCreate = Account(
				id = 1,
				benefitBalances = Map(MEAL -> 0, FOOD -> 100, CULTURE -> 100),
				cashBalance = 100,
				lastTransaction = None
			)

			val alreadyExistingAccount = Account(1, Map(MEAL -> 0), 100, Some("a-transaction-uuid"))

			when(mockIManageAccountsPersistence.find(1)).thenReturn(Some(alreadyExistingAccount))

			//Act
			val tryAccountCreated = service.create(accountToCreate)

			//Assert
			tryAccountCreated.isFailure mustBe true
			tryAccountCreated.failure.exception mustEqual CouldNotCreateAccountException("Account already exists")
		}

		"Return failure if repository returns a failure when trying to create account" in {
			//Arrange
			val accountToCreate = Account(
				id = 1,
				benefitBalances = Map(MEAL -> 0, FOOD -> 100, CULTURE -> 100),
				cashBalance = 100,
				lastTransaction = None
			)

			when(mockIManageAccountsPersistence.find(1)).thenReturn(None)
			when(mockIManageAccountsPersistence.create(accountToCreate)).thenReturn(Failure(new NoSuchMethodException()))

			//Act
			val tryAccountCreated = service.create(accountToCreate)

			//Assert
			tryAccountCreated.isFailure mustBe true
		}
	}

	"AccountManagementService#updateBenefitBalance" should {

		"Return success when repository returns success" in {
			//Arrange
			val id = 1
			val benefitCategory = BenefitCategory.MEAL
			val newBalance = 90
			val lastTransaction = Some("a-valid-lastTransaction-id")

			when(mockIManageAccountsPersistence.updateBenefitBalance(id, benefitCategory, newBalance, lastTransaction)).thenReturn(Success(()))

			//Act
			val result = service.updateBenefitBalance(id,benefitCategory,newBalance,lastTransaction)

			//Assert
			result mustBe Success(())
		}

		"Return Failure when repository returns a failure" in {
			//Arrange
			val id = 1
			val benefitCategory = BenefitCategory.MEAL
			val newBalance = 90
			val lastTransaction = Some("a-valid-lastTransaction-id")

			when(mockIManageAccountsPersistence.updateBenefitBalance(id, benefitCategory, newBalance, lastTransaction)).thenReturn(Failure(new RuntimeException()))

			//Act
			val result = service.updateBenefitBalance(id,benefitCategory,newBalance,lastTransaction)

			//Assert
			result.isFailure mustBe true
		}
	}

	"AccountManagementService#updateCashBalance" should {

		"Return success when repository returns success" in {
			//Arrange
			val id = 1
			val newBalance = 90
			val lastTransaction = Some("a-valid-lastTransaction-id")

			when(mockIManageAccountsPersistence.updateCashBalance(
				id,newBalance,lastTransaction
			)).thenReturn(Success(()))

			//Act
			val result = service.updateCashBalance(id,newBalance,lastTransaction)

			//Assert
			result mustBe Success(())
		}

		"Return Failure when repository returns a failure" in {
			//Arrange
			val id = 1
			val newBalance = 90
			val lastTransaction = Some("a-valid-lastTransaction-id")

			when(mockIManageAccountsPersistence.updateCashBalance(
				id,newBalance,lastTransaction
			)).thenReturn(Failure(new RuntimeException()))

			//Act
			val result = service.updateCashBalance(id,newBalance,lastTransaction)

			//Assert
			result.isFailure mustBe true
		}
	}
}











