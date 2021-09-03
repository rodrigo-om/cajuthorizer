package unit.repositories.account.mongodb

import domain.models.Account
import domain.models.BenefitCategory.{CULTURE, FOOD, MEAL}
import org.mockito.ArgumentMatchersSugar.any
import org.mockito.scalatest.ResetMocksAfterEachTest
import org.mockito.{ArgumentMatchers, MockitoSugar}
import org.mongodb.scala.Document
import org.mongodb.scala.bson.BsonDouble
import org.mongodb.scala.result.{InsertOneResult, UpdateResult}
import org.scalatestplus.play._
import repositories.account.mongodb.{AccountMongoDBAdapter, AccountRepository}

import scala.concurrent.Future

class AccountMongoDBAdapterTest extends PlaySpec with MockitoSugar with ResetMocksAfterEachTest {

	private val mockAccountRepository = mock[AccountRepository]
	private val adapter = new AccountMongoDBAdapter(mockAccountRepository)

	"AccountMongoDBAdapter#Find" should {
		"Return Account if found by repository and lastTransaction is None" in {
			//Arrange
			val expectedAccount = stubAccount()
			val accountDocument = stubAccountDocument()

			when(mockAccountRepository.findOne(1)).thenReturn(Future.successful(Some(accountDocument)))

			//Act
			val account = adapter.find(1)

			//Assert
			account.get mustEqual expectedAccount
		}

		"Return Account if found by repository and lastTransaction has value" in {
			//Arrange
			val expectedAccount = stubAccount(lastTransaction = Some("a-first-transaction"))
			val accountDocument = stubAccountDocument(lastTransaction = Some("a-first-transaction"))

			when(mockAccountRepository.findOne(1)).thenReturn(Future.successful(Some(accountDocument)))

			//Act
			val account = adapter.find(1)

			//Assert
			account.get mustEqual expectedAccount
		}
	}

	"AccountMongoDBAdapter#FindAll" should {
		"Return all accounts find by repository" in {
			//Arrange
			val expectedAccount = stubAccount()
			val account1 = stubAccountDocument()
			val account2 = stubAccountDocument()

			when(mockAccountRepository.findAll()).thenReturn(Future.successful(Seq(account1,account2)))

			//Act
			val accounts = adapter.findAll()

			//Assert
			accounts.length mustEqual 2
			accounts.head mustEqual expectedAccount
		}
	}

	"AccountMongoDBAdapter#Create" should {
		"Be successful when repository inserts correctly" in {
			//Arrange
			val account = stubAccount()
			val expectedAccountDoc = stubAccountDocument()

			val mockResult = mock[InsertOneResult]
			when(mockAccountRepository.insert(expectedAccountDoc)).thenReturn(Future.successful(mockResult))
			when(mockResult.wasAcknowledged()).thenReturn(true)

			//Act
			val creationResult = adapter.create(account)

			//Assert
			creationResult.isSuccess mustBe true
		}

		"Be a failure when insert was unacknowledged" in {
			//Arrange
			val account = stubAccount()
			val expectedAccountDoc = stubAccountDocument()

			val mockResult = mock[InsertOneResult]
			when(mockAccountRepository.insert(expectedAccountDoc)).thenReturn(Future.successful(mockResult))
			when(mockResult.wasAcknowledged()).thenReturn(false)

			//Act
			val creationResult = adapter.create(account)

			//Assert
			creationResult.isFailure mustBe true
		}

		"Be a failure when insert had any error" in {
			//Arrange
			val account = stubAccount()
			val expectedAccountDoc = stubAccountDocument()

			when(mockAccountRepository.insert(expectedAccountDoc)).thenReturn(Future.failed(new Exception))

			//Act
			val creationResult = adapter.create(account)

			//Assert
			creationResult.isFailure mustBe true
		}
	}

	"AccountMongoDBAdapter#UpdateBenefitBalance" should {
		"Be successful when repository updates correctly and map MEAL benefits category correctly" in {
			//Arrange
			val account = stubAccount()

			val mockResult = mock[UpdateResult]
			when(mockAccountRepository.update(
				ArgumentMatchers.eq(account.id),
				any[String],
				ArgumentMatchers.eq("mealBalance"),
				ArgumentMatchers.eq(50.toDouble),
				ArgumentMatchers.eq(account.lastTransaction)
			)).thenReturn(Future.successful(mockResult))

			when(mockResult.getModifiedCount).thenReturn(1)

			//Act
			val creationResult = adapter.updateBenefitBalance(account.id, MEAL, 50, account.lastTransaction)

			//Assert
			creationResult.isSuccess mustBe true
		}

		"Be successful when repository updates correctly and map FOOD benefits category correctly" in {
			//Arrange
			val account = stubAccount()

			val mockResult = mock[UpdateResult]
			when(mockAccountRepository.update(
				ArgumentMatchers.eq(account.id),
				any[String],
				ArgumentMatchers.eq("foodBalance"),
				ArgumentMatchers.eq(50.toDouble),
				ArgumentMatchers.eq(account.lastTransaction)
			)).thenReturn(Future.successful(mockResult))

			when(mockResult.getModifiedCount).thenReturn(1)

			//Act
			val creationResult = adapter.updateBenefitBalance(account.id, FOOD, 50, account.lastTransaction)

			//Assert
			creationResult.isSuccess mustBe true
		}

		"Be successful when repository updates correctly and map CULTURE benefits category correctly" in {
			//Arrange
			val account = stubAccount()

			val mockResult = mock[UpdateResult]
			when(mockAccountRepository.update(
				ArgumentMatchers.eq(account.id),
				any[String],
				ArgumentMatchers.eq("cultureBalance"),
				ArgumentMatchers.eq(50.toDouble),
				ArgumentMatchers.eq(account.lastTransaction)
			)).thenReturn(Future.successful(mockResult))

			when(mockResult.getModifiedCount).thenReturn(1)

			//Act
			val creationResult = adapter.updateBenefitBalance(account.id, CULTURE, 50, account.lastTransaction)

			//Assert
			creationResult.isSuccess mustBe true
		}

		"Be a failure when insert was unacknowledged" in {
			//Arrange
			val account = stubAccount()

			val mockResult = mock[UpdateResult]
			when(mockAccountRepository.update(
				ArgumentMatchers.eq(account.id),
				any[String],
				ArgumentMatchers.eq("mealBalance"),
				ArgumentMatchers.eq(50.toDouble),
				ArgumentMatchers.eq(account.lastTransaction)
			)).thenReturn(Future.successful(mockResult))
			when(mockResult.getModifiedCount).thenReturn(0)

			//Act
			val creationResult = adapter.updateBenefitBalance(account.id, MEAL, 50, account.lastTransaction)

			//Assert
			creationResult.isFailure mustBe true
		}

		"Be a failure when insert had any error" in {
			//Arrange
			val account = stubAccount()

			val mockResult = mock[UpdateResult]
			when(mockAccountRepository.update(
				ArgumentMatchers.eq(account.id),
				any[String],
				ArgumentMatchers.eq("mealBalance"),
				ArgumentMatchers.eq(50.toDouble),
				ArgumentMatchers.eq(account.lastTransaction)
			)).thenReturn(Future.failed(new Exception))

			//Act
			val creationResult = adapter.updateBenefitBalance(account.id, MEAL, 50, account.lastTransaction)

			//Assert
			creationResult.isFailure mustBe true
		}
	}

	"AccountMongoDBAdapter#UpdateCashBalance" should {
		"Be successful when repository updates correctly" in {
			//Arrange
			val account = stubAccount()

			val mockResult = mock[UpdateResult]
			when(mockAccountRepository.update(
				ArgumentMatchers.eq(account.id),
				any[String],
				ArgumentMatchers.eq("cashBalance"),
				ArgumentMatchers.eq(50.toDouble),
				ArgumentMatchers.eq(account.lastTransaction)
			)).thenReturn(Future.successful(mockResult))

			when(mockResult.getModifiedCount).thenReturn(1)

			//Act
			val creationResult = adapter.updateCashBalance(account.id, 50, account.lastTransaction)

			//Assert
			creationResult.isSuccess mustBe true
		}

		"Be a failure when insert was unacknowledged" in {
			//Arrange
			val account = stubAccount()

			val mockResult = mock[UpdateResult]
			when(mockAccountRepository.update(
				ArgumentMatchers.eq(account.id),
				any[String],
				ArgumentMatchers.eq("cashBalance"),
				ArgumentMatchers.eq(50.toDouble),
				ArgumentMatchers.eq(account.lastTransaction)
			)).thenReturn(Future.successful(mockResult))
			when(mockResult.getModifiedCount).thenReturn(0)

			//Act
			val creationResult = adapter.updateCashBalance(account.id, 50, account.lastTransaction)

			//Assert
			creationResult.isFailure mustBe true
		}

		"Be a failure when insert had any error" in {
			//Arrange
			val account = stubAccount()

			val mockResult = mock[UpdateResult]
			when(mockAccountRepository.update(
				ArgumentMatchers.eq(account.id),
				any[String],
				ArgumentMatchers.eq("cashBalance"),
				ArgumentMatchers.eq(50.toDouble),
				ArgumentMatchers.eq(account.lastTransaction)
			)).thenReturn(Future.failed(new Exception))

			//Act
			val creationResult = adapter.updateCashBalance(account.id, 50, account.lastTransaction)

			//Assert
			creationResult.isFailure mustBe true
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

	private def stubAccountDocument(
		accountId: Int = 1,
		mealBalance: Double = 100,
		foodBalance: Double = 100,
		cultureBalance: Double = 100,
		cashBalance: Double = 100,
		lastTransaction: Option[String] = None
	) = {
		Document(
			"accountId" -> accountId,
			"mealBalance" -> BsonDouble(mealBalance),
			"foodBalance" -> BsonDouble(foodBalance),
			"cultureBalance" -> BsonDouble(cultureBalance),
			"cashBalance" -> BsonDouble(cashBalance),
			"lastTransaction" -> lastTransaction
		)
	}
}








