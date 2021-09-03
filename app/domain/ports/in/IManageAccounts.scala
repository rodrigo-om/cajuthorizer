package domain.ports.in

import com.google.inject.ImplementedBy
import domain.models.Account
import domain.services.AccountManagementService

import scala.util.Try

@ImplementedBy(classOf[AccountManagementService])
trait IManageAccounts {
	def findAll(): Seq[Account]
	def findAccount(id: Int): Option[Account]
	def create(account: Account): Try[Unit]
}
