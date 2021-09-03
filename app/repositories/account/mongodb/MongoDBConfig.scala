package repositories.account.mongodb

import com.google.inject.{Inject, Singleton}
import play.api.Configuration

@Singleton
case class MongoDBConfig @Inject()(config: Configuration) {
	def connectionString: String = config.get[String]("mongodb")
	def databaseName: String = "cajuthorizer"
	def collectionName: String = "accounts"
}

