package domain.ports.out

import com.google.inject.ImplementedBy
import controllers.account.AccountManagementHTTPAdapter
import domain.models.Account
import domain.models.BenefitCategory.BenefitCategory
import repositories.account.mongodb.AccountMongoDBAdapter

import scala.util.Try

@ImplementedBy(classOf[AccountMongoDBAdapter])
trait IManageAccountsPersistence {
	def findAll(): Seq[Account]

	def find(id: Int): Option[Account]

	def create(account: Account): Try[Unit]

	def updateCashBalance(id: Int, newBalance: Double, lastTransaction: Option[String]): Try[Unit]

	def updateBenefitBalance(id: Int, benefitCategory: BenefitCategory, newBalance: Double, lastTransaction: Option[String]): Try[Unit]
}
