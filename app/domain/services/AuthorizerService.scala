package domain.services

import com.google.inject.{Inject, Singleton}
import domain.models.BenefitCategory.BenefitCategory
import domain.models.{Account, Transaction}
import domain.ports.in.IAuthorize
import domain.services.exceptions.{CouldNotProcessTransaction, NotEnoughBalanceException}

import scala.util.{Failure, Success, Try}

@Singleton
class AuthorizerService @Inject()(accountManagementService: AccountManagementService, mapCategoryService: MapCategoryService) extends IAuthorize {

	def authorize(transaction: Transaction): Try[Unit] = {

		validateTransaction(transaction).flatMap { _ =>
			val authorized: Try[Unit] = for {
				account <- getAccount(transaction.account)
				(category, newBalance) <- calculateBalance(account, transaction)
				updatedBalance <- updateBalance(account, category, newBalance)
			} yield updatedBalance

			authorized
		}.recoverWith {
			case e: NotEnoughBalanceException => println("Not enough balance"); Failure(e)
			case e: CouldNotProcessTransaction => println("Could not process transaction"); Failure(e)
			case e: IllegalArgumentException => println("A transaction argument was invalid"); Failure(e)
			case e: Exception => println("Not expected error"); Failure(CouldNotProcessTransaction(e.getMessage))
		}
	}

	def validateTransaction(transaction: Transaction): Try[Unit] = {
		def isEmpty(field: String) = Option(field).forall(_.trim.isEmpty)

		(transaction.mcc, transaction.merchant) match {
			case (mcc,_) if isEmpty(mcc) => Failure(new IllegalArgumentException("MCC can not be empty"))
			case (_,mer) if isEmpty(mer) => Failure(new IllegalArgumentException("Merchant can not be empty"))
			case (mcc,_) if mcc.length > 4 => Failure(new IllegalArgumentException("MCCs can not have more than 4 digits"))
			case (mcc,_) if mcc.toIntOption.isEmpty => Failure(new IllegalArgumentException("Invalid mcc provided: must be a number."))
			case (_,mer) if mer.length != 40 => Failure(new IllegalArgumentException("Merchant names must have exactly 40 characters"))
			case _ => Success(())
		}
	}

	private def getAccount(account: Int): Try[Account] = Try {
		accountManagementService.findAccount(account).getOrElse(throw CouldNotProcessTransaction("Account not found"))
	}

	private def calculateBalance(account: Account, transaction: Transaction): Try[(Option[BenefitCategory], Double)] = {

		def hasEnoughCashBalance(account: Account, transaction: Transaction): Boolean = {
			account.cashBalance > transaction.totalAmount
		}

		def hasEnoughBenefitBalance(account: Account, transaction: Transaction, benefitCategory: BenefitCategory): Boolean = {
			val benefitBalance: Double = account.benefitBalances.getOrElse(benefitCategory, 0)
			benefitBalance > transaction.totalAmount
		}

		val maybeBenefitCategory: Option[BenefitCategory] = mapCategoryService.mapToASupportedBenefitCategory(transaction.merchant, transaction.mcc)

		maybeBenefitCategory match {
			case None if hasEnoughCashBalance(account, transaction) =>
				Success(None, account.cashBalance - transaction.totalAmount)

			case Some(benefitCategory) if hasEnoughBenefitBalance(account, transaction, benefitCategory) =>
				val benefitBalance: Double = account.benefitBalances.getOrElse(benefitCategory, 0)
				Success(Some(benefitCategory), benefitBalance - transaction.totalAmount)

			case Some(_) if hasEnoughCashBalance(account, transaction) =>
				Success(None, account.cashBalance - transaction.totalAmount)

			case _ => Failure(NotEnoughBalanceException())
		}
	}

	private def updateBalance(account: Account, benefitCategory: Option[BenefitCategory], newBalance: Double): Try[Unit] = {
		benefitCategory match {
			case Some(category) => accountManagementService.updateBenefitBalance(account.id, category, newBalance, account.lastTransaction)
			case None => accountManagementService.updateCashBalance(account.id, newBalance, account.lastTransaction)
		}
	}
}
