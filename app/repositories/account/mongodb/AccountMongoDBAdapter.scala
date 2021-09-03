package repositories.account.mongodb

import com.google.inject.{Inject, Singleton}
import domain.models.Account
import domain.models.BenefitCategory.{BenefitCategory, CULTURE, FOOD, MEAL}
import domain.ports.out.IManageAccountsPersistence
import org.mongodb.scala.Document
import org.mongodb.scala.bson.BsonDouble
import org.mongodb.scala.result.{InsertOneResult, UpdateResult}
import play.api.Logger

import java.util.UUID
import scala.concurrent.Await
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration.{Duration, MILLISECONDS}
import scala.util.{Failure, Success, Try}

@Singleton
class AccountMongoDBAdapter @Inject()(accountRepository: AccountRepository) extends IManageAccountsPersistence {

	val logger: Logger = Logger(this.getClass)

	override def find(id: Int): Option[Account] = {
		val futureFind = accountRepository.findOne(id)
			.map(fromMongoDbModel)
			.recover {
				case e: Exception =>
					logger.error(s"Account $id was not found due to error.", e)
					None
			}

		/*
			I didn't like this await either. My Scala was a bit rusty so I took a bad turn not modelling the domain logic to chain down futures,
			thinking that maybe the Future aspect could be technology-dependent. But I think for this small application this can work without
			any hassles even under unusual loads. Of course, in a production-ready code, I would have avoided this and in fact take more time
			to change the domain if the team agreed it should be done.
		*/
		Await.result(futureFind, Duration(100, MILLISECONDS))
	}

	override def findAll(): Seq[Account] = {
		val futureFind = accountRepository.findAll()
			.map((accounts: Seq[Document]) => accounts.map(accountDoc => fromMongoDbModel(Some(accountDoc)).get))
			.recover {
				case e: Exception =>
					logger.error(s"No account was found due to error.", e)
					Seq.empty
			}

		Await.result(futureFind, Duration(100, MILLISECONDS))
	}

	override def create(account: Account): Try[Unit] = {
		val document = toMongoDbModel(account)
		val futureCreate = accountRepository.insert(document)
			.map((result: InsertOneResult) => if (result.wasAcknowledged()) {
				Success(())
			} else {
				logger.error(s"Account $account not acknowledged by the database. It was not created")
				Failure(new Exception("Account creation was unacknowledged"))
			})
			.recover {
				case e: Exception =>
					logger.error(s"Account was not created due to error", e)
					Failure(e)
			}

		Await.result(futureCreate, Duration(100, MILLISECONDS))
	}

	override def updateBenefitBalance(
		id: Int,
		benefitCategory: BenefitCategory,
		newBalance: Double,
		lastTransaction: Option[String]
	): Try[Unit] = {
		val newTransactionId = UUID.randomUUID().toString
		val balanceField = fromBenefitCategory(benefitCategory)

		val futureUpdate = accountRepository.update(id, newTransactionId, balanceField, newBalance, lastTransaction)
			.map((result: UpdateResult) =>
				result.getModifiedCount match {
					case 1 => Success(())
					case 0 =>
						logger.error(s"Transaction {$id - $benefitCategory - $newBalance - $lastTransaction} was not persisted")
						Failure(new Exception("Transaction was not persisted"))
				})
			.recover {
				case e: Exception =>
					logger.error(s"Transaction {$id - $benefitCategory - $newBalance - $lastTransaction} was not persisted due to error", e)
					Failure(e)
			}

		Await.result(futureUpdate, Duration(100, MILLISECONDS))
	}

	override def updateCashBalance(
		id: Int,
		newBalance: Double,
		lastTransaction: Option[String]
	): Try[Unit] = {
		val newTransactionId = UUID.randomUUID().toString

		val futureUpdate = accountRepository.update(id, newTransactionId, "cashBalance", newBalance, lastTransaction)
			.map((result: UpdateResult) =>
				result.getModifiedCount match {
					case 1 => Success(())
					case 0 =>
						logger.error(s"Transaction {$id - $newBalance - $lastTransaction} was not persisted")
						Failure(new Exception("Transaction was not persisted"))
				})
			.recover {
				case e: Exception =>
					logger.error(s"Transaction {$id - $newBalance - $lastTransaction} was not persisted due to error", e)
					Failure(e)
			}

		Await.result(futureUpdate, Duration(100, MILLISECONDS))
	}

	private def toMongoDbModel(account: Account): Document = {
		Document(
			"accountId" -> account.id,
			"mealBalance" -> BsonDouble(account.benefitBalances.getOrElse(MEAL, 0)),
			"foodBalance" -> BsonDouble(account.benefitBalances.getOrElse(FOOD, 0)),
			"cultureBalance" -> BsonDouble(account.benefitBalances.getOrElse(CULTURE, 0)),
			"cashBalance" -> BsonDouble(account.cashBalance),
			"lastTransaction" -> account.lastTransaction
		)
	}

	private def fromMongoDbModel(document: Option[Document]): Option[Account] = {
		document match {
			case Some(doc) =>
				val benefitBalances: Map[BenefitCategory, Double] = Map(
					MEAL -> doc.getDouble("mealBalance"),
					FOOD -> doc.getDouble("foodBalance"),
					CULTURE -> doc.getDouble("cultureBalance"),
				)

				Some(Account(
					id = doc.getInteger("accountId"),
					benefitBalances = benefitBalances,
					cashBalance = doc.getDouble("cashBalance"),
					lastTransaction = Option(doc.getString("lastTransaction"))
				))

			case None => None
		}
	}

	private def fromBenefitCategory(benefitCategory: BenefitCategory): String = benefitCategory match {
		case MEAL => "mealBalance"
		case FOOD => "foodBalance"
		case CULTURE => "cultureBalance"
	}
}
