package integration.controllers.repositories.account.mongodb

import org.mockito.Mockito.when
import org.mockito.scalatest.MockitoSugar
import org.mongodb.scala.bson.{BsonDouble, BsonString}
import org.mongodb.scala.model.Filters
import org.mongodb.scala.result.UpdateResult
import org.mongodb.scala.{Document, MongoClient, MongoCollection}
import org.scalatest.BeforeAndAfterEach
import org.scalatest.concurrent.{Eventually, IntegrationPatience}
import org.scalatest.time.{Seconds, Span}
import org.scalatestplus.easymock.EasyMockSugar.mock
import org.scalatestplus.play.PlaySpec
import repositories.account.mongodb.{AccountRepository, MongoDBConfig}

import java.util.concurrent.TimeUnit
import _root_.scala.concurrent.{Await, ExecutionContext}
import scala.concurrent.duration.{Duration, SECONDS}

/*
	Most of these tests are here for sanity check that there's nothing crazy going on with official drivers,
	but there are some below that test more specific scenarios like unique indexes and conditional/concurrent updates
*/
class AccountRepositoryTest extends PlaySpec with BeforeAndAfterEach with Eventually with IntegrationPatience with MockitoSugar {

	private val mockConfig: MongoDBConfig = mock[MongoDBConfig]
	private val testDb = "cajuthorizer-tests"
	private val testCollection = "accounts-tests"
	private val testConnectionString = "mongodb://localhost:27017/admin?ssl=false"

	when(mockConfig.databaseName).thenReturn(testDb)
	when(mockConfig.collectionName).thenReturn(testCollection)
	when(mockConfig.connectionString).thenReturn(testConnectionString)

	private val mongoClient = MongoClient()
	private val collection: MongoCollection[Document] = mongoClient.getDatabase(testDb).getCollection(testCollection)
	private val accountRepository = new AccountRepository(mockConfig)

	implicit val ec: ExecutionContext = ExecutionContext.global

	override def beforeEach(): Unit = {
		super.beforeEach()
		Await.result(collection.deleteMany(Filters.empty).toFuture(), Duration(5, TimeUnit.SECONDS))
	}

	"AccountRepositoryTest#Create" should {
		"Create correctly" in {
			//Arrange
			val accountMongoDBModel = stubAccountMongoDBModel()

			//Act
			val pushFuture = for {
				push <- accountRepository.insert(accountMongoDBModel)
			} yield push

			//Assert
			eventually(timeout(Span(5, Seconds))) {
				pushFuture.isCompleted mustEqual true
				val account: Option[Document] = Await.result(accountRepository.findOne(1), Duration(10, TimeUnit.SECONDS))
				account.get.getOrElse("accountId", None) mustEqual accountMongoDBModel.getOrElse("accountId", "not-found")
			}
		}

		"NOT create a second time, ensuring unique index on accountId" in {
			//Arrange
			val accountMongoDBModel = stubAccountMongoDBModel()
			val accountMongoDBModelSameAccountId = stubAccountMongoDBModel()

			accountRepository.insert(accountMongoDBModel).map(_.toString)

			//Act
			val pushFuture = for {
				_ <- accountRepository.insert(accountMongoDBModel)
				push <- accountRepository.insert(accountMongoDBModelSameAccountId)
			} yield push

			//Assert
			eventually(timeout(Span(5, Seconds))) {
				pushFuture.isCompleted mustEqual true
				val account: Seq[Document] = Await.result(accountRepository.findAll(), Duration(10, TimeUnit.SECONDS))
				account.length mustBe 1
				account.head.getOrElse("accountId", None) mustEqual accountMongoDBModel.getOrElse("accountId", "not-found")
			}
		}

		"AccountRepositoryTest#Create" should {
			"Create correctly" in {
				//Arrange
				val accountMongoDBModel = stubAccountMongoDBModel()

				//Act
				val pushFuture = for {
					push <- accountRepository.insert(accountMongoDBModel)
				} yield push

				//Assert
				eventually(timeout(Span(5, Seconds))) {
					pushFuture.isCompleted mustEqual true
					val account: Option[Document] = Await.result(accountRepository.findOne(1), Duration(10, TimeUnit.SECONDS))
					account.get.getOrElse("accountId", None) mustEqual accountMongoDBModel.getOrElse("accountId", "not-found")
				}
			}
		}
	}

	"AccountRepositoryTest#FindAll" should {
		"Find all correctly" in {
			//Arrange
			val accountMongoDBModel = stubAccountMongoDBModel()
			val accountMongoDBModel2 = stubAccountMongoDBModel(accountId = 2)

			//Act
			val findFuture = for {
				_ <- accountRepository.insert(accountMongoDBModel)
				_ <- accountRepository.insert(accountMongoDBModel2)
				find <- accountRepository.findAll()
			} yield find

			//Assert
			eventually(timeout(Span(5, Seconds))) {
				findFuture.isCompleted mustEqual true
				findFuture.value.get.get.length mustBe 2
			}
		}
	}

	"AccountRepositoryTest#FindOne" should {
		"Find one correctly" in {
			//Arrange
			val accountMongoDBModel = stubAccountMongoDBModel()
			val accountMongoDBModel2 = stubAccountMongoDBModel(accountId = 2)

			//Act
			val findFuture = for {
				_ <- accountRepository.insert(accountMongoDBModel)
				_ <- accountRepository.insert(accountMongoDBModel2)
				find <- accountRepository.findOne(2)
			} yield find

			//Assert
			eventually(timeout(Span(5, Seconds))) {
				findFuture.isCompleted mustEqual true
				findFuture.value.get.get.get.getOrElse("accountId", None) mustEqual accountMongoDBModel2.getOrElse("accountId", "not-found")
			}
		}

		"NOT Find one if id provided doesn't exist" in {
			//Arrange
			val accountMongoDBModel = stubAccountMongoDBModel()

			//Act
			val findFuture = for {
				_ <- accountRepository.insert(accountMongoDBModel)
				find <- accountRepository.findOne(2)
			} yield find

			//Assert
			eventually(timeout(Span(5, Seconds))) {
				findFuture.isCompleted mustEqual true
				findFuture.value.get.get mustBe None
			}
		}
	}

	"AccountRepositoryTest#Update" should {
		"Update correctly given all conditions are met" in {
			//Arrange
			val accountMongoDBModel = stubAccountMongoDBModel(lastTransaction = Some("a-first-transaction"))

			//Act
			val updateFuture = for {
				_ <- accountRepository.insert(accountMongoDBModel)
				update <- accountRepository.update(1, "a-second-transaction", "mealBalance", 50, Some("a-first-transaction"))
			} yield update

			//Assert
			eventually(timeout(Span(5, Seconds))) {
				updateFuture.isCompleted mustEqual true

				val account: Option[Document] = Await.result(accountRepository.findOne(1), Duration(5, TimeUnit.SECONDS))
				account.get.getOrElse("accountId", None) mustEqual accountMongoDBModel.getOrElse("accountId", "not-found")
				account.get.getOrElse("lastTransaction", None) mustEqual BsonString("a-second-transaction")
				account.get.getOrElse("mealBalance", None) mustEqual BsonDouble(50)
			}
		}

		"Update correctly even if this is the first transaction of said account" in {
			//Arrange
			val accountMongoDBModel = stubAccountMongoDBModel()

			//Act
			val updateFuture = for {
				_ <- accountRepository.insert(accountMongoDBModel)
				update <- accountRepository.update(1, "a-first-transaction", "mealBalance", 50, None)
			} yield update

			//Assert
			eventually(timeout(Span(5, Seconds))) {
				updateFuture.isCompleted mustEqual true

				val account: Option[Document] = Await.result(accountRepository.findOne(1), Duration(5, TimeUnit.SECONDS))
				account.get.getOrElse("accountId", None) mustEqual accountMongoDBModel.getOrElse("accountId", "not-found")
				account.get.getOrElse("lastTransaction", None) mustEqual BsonString("a-first-transaction")
				account.get.getOrElse("mealBalance", None) mustEqual BsonDouble(50)
			}
		}

		/*
			These one are specially important because it acts as one of the guards against concurrent updates. Since the db will lock the document
			for a write, if 2 transactions try to update the balance at the same time, one of them will be correctly processed, and the other one
			will be rejected because the "lastTransaction" they have does not match the one persisted anymore.
		*/
		"NOT Update in case the last transaction doesn't match" in {
			//Arrange
			val accountMongoDBModel = stubAccountMongoDBModel(lastTransaction = Some("a-second-transaction"))

			//Act
			val updateFuture = for {
				_ <- accountRepository.insert(accountMongoDBModel)
				update <- accountRepository.update(1, "a-third-transaction", "mealBalance", 50, Some("a-first-transaction"))
			} yield update

			//Assert
			eventually(timeout(Span(5, Seconds))) {
				updateFuture.isCompleted mustEqual true

				val account: Option[Document] = Await.result(accountRepository.findOne(1), Duration(5, TimeUnit.SECONDS))
				account.get.getOrElse("accountId", None) mustEqual accountMongoDBModel.getOrElse("accountId", "not-found")
				account.get.getOrElse("lastTransaction", None) mustEqual BsonString("a-second-transaction")
				account.get.getOrElse("mealBalance", None) mustEqual BsonDouble(100)
			}
		}

		"NOT Update one of two concurrent transactions" in {
			//Arrange
			val accountMongoDBModel = stubAccountMongoDBModel(lastTransaction = Some("a-first-transaction"))

			val createFuture = for {
				create <- accountRepository.insert(accountMongoDBModel)
			} yield create
			Await.result(createFuture, Duration(5, SECONDS))

			//Act
			val update1 = accountRepository.update(1, "a-second-1-transaction", "mealBalance", 20, Some("a-first-transaction"))
			val update2 = accountRepository.update(1, "a-second-2-transaction", "mealBalance", 40, Some("a-first-transaction"))

			//Assert
			eventually(timeout(Span(5, Seconds))) {
				update1.isCompleted mustBe true
				update2.isCompleted mustBe true

				val account: Option[Document] = Await.result(accountRepository.findOne(1), Duration(5, TimeUnit.SECONDS))

				val update1Result: UpdateResult = update1.value.get.get

				update1Result.getModifiedCount match {
					case 1 =>
						update2.value.get.get.getModifiedCount mustBe 0
						account.get.getOrElse("lastTransaction", None) mustEqual BsonString("a-second-1-transaction")
						account.get.getOrElse("mealBalance", None) mustEqual BsonDouble(20)

					case 0 =>
						update2.value.get.get.getModifiedCount mustBe 1
						account.get.getOrElse("lastTransaction", None) mustEqual BsonString("a-second-2-transaction")
						account.get.getOrElse("mealBalance", None) mustEqual BsonDouble(40)
				}
			}
		}
	}

	"AccountRepositoryTest#Delete" should {
		"Delete correctly" in {
			//Arrange
			val accountMongoDBModel = stubAccountMongoDBModel()

			//Act
			val deleteFuture = for {
				_ <- accountRepository.insert(accountMongoDBModel)
				delete <- accountRepository.delete(1)
			} yield delete

			//Assert
			eventually(timeout(Span(5, Seconds))) {
				deleteFuture.isCompleted mustEqual true
				deleteFuture.value.get.get.getDeletedCount mustBe 1
				val account: Option[Document] = Await.result(accountRepository.findOne(1), Duration(5, TimeUnit.SECONDS))
				account mustBe None
			}
		}
	}

	private def stubAccountMongoDBModel(accountId: Int = 1, lastTransaction: Option[String] = None) = {
		Document(
			"accountId" -> accountId,
			"mealBalance" -> BsonDouble(100),
			"foodBalance" -> BsonDouble(100),
			"cultureBalance" -> BsonDouble(100),
			"cashBalance" -> BsonDouble(100),
			"lastTransaction" -> lastTransaction
		)
	}
}




