package domain.services

import com.google.inject.Inject
import domain.models.Account
import domain.models.BenefitCategory.BenefitCategory
import domain.ports.in.IManageAccounts
import domain.ports.out.IManageAccountsPersistence
import domain.services.exceptions.CouldNotCreateAccountException

import javax.inject.Singleton
import scala.util.{Failure, Try}

@Singleton
class AccountManagementService @Inject()(accountsRepository: IManageAccountsPersistence) extends IManageAccounts {

	def findAccount(id: Int): Option[Account] = {
		accountsRepository.find(id)
	}

	override def findAll(): Seq[Account] = {
		accountsRepository.findAll()
	}

	def create(account: Account): Try[Unit] = {
		validate(account) match {
			case Some(error) => Failure(CouldNotCreateAccountException(error))
			case None =>
				findAccount(account.id) match {
					case Some(_) => Failure(CouldNotCreateAccountException("Account already exists"))
					case None => accountsRepository.create(account)
				}
		}
	}

	def updateBenefitBalance(id: Int, benefitCategory: BenefitCategory, newBalance: Double, lastTransaction: Option[String]): Try[Unit] = {
		accountsRepository.updateBenefitBalance(id, benefitCategory, newBalance, lastTransaction)
	}


	def updateCashBalance(id: Int, newBalance: Double, lastTransaction: Option[String]): Try[Unit] = {
		accountsRepository.updateCashBalance(id,newBalance,lastTransaction)
	}

	private def validate(account: Account): Option[String] = {
		val isThereABenefitBalanceLowerThanZero = account.benefitBalances.values.exists(_ < 0)

		account match {
			case _ if account.cashBalance < 0 => Some("Cash balance can't be lower than 0")
			case _ if account.lastTransaction.isDefined => Some("Last transaction must not have a value")
			case _ if isThereABenefitBalanceLowerThanZero => Some("Benefit balances can't be lower than 0")
			case _ => None
		}
	}
}
