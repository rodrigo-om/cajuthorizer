package repositories.account.mongodb

import com.google.inject.{Inject, Singleton}
import org.mongodb.scala.model.Filters._
import org.mongodb.scala.model.IndexOptions
import org.mongodb.scala.model.Updates.{combine, set}
import org.mongodb.scala.result.{DeleteResult, InsertOneResult, UpdateResult}
import org.mongodb.scala.{Document, MongoClient, MongoCollection}

import scala.concurrent.Future

@Singleton
class AccountRepository @Inject()(config: MongoDBConfig) {

	private val mongoClient = MongoClient(config.connectionString)

	private lazy val collection: MongoCollection[Document] = mongoClient
		.getDatabase(config.databaseName)
		.getCollection(config.collectionName)

	private lazy val createIndex = collection.createIndex(Document("accountId" -> 1), IndexOptions().unique(true))

	def findAll(limit: Int = 100): Future[Seq[Document]] = {
		collection.find().limit(limit).toFuture()
	}

	def findOne(id: Int): Future[Option[Document]] = {
		collection.find(equal("accountId", id)).headOption()
	}

	def insert(account: Document): Future[InsertOneResult] = {
		createIndex.head()
		collection.insertOne(account).toFuture()
	}

	def update(accountId: Int, newTransactionId: String, balanceField: String, newBalance: Double, lastTransaction: Option[String]): Future[UpdateResult] = {
		collection.updateOne(
			and(equal("accountId",accountId), equal("lastTransaction",lastTransaction.orNull)),
			combine(set(balanceField,newBalance), set("lastTransaction", newTransactionId))
		).toFuture()
	}

	def delete(accountId: Int): Future[DeleteResult] = {
		collection.deleteOne(equal("accountId",accountId)).toFuture()
	}
}

