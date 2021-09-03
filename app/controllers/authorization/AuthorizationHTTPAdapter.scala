package controllers.authorization

import com.google.inject.{Inject, Singleton}
import domain.models.Transaction
import domain.ports.in.IAuthorize
import domain.services.exceptions.NotEnoughBalanceException
import play.api.Logger

import scala.util.{Failure, Success, Try}

@Singleton
class AuthorizationHTTPAdapter @Inject()(authorizerService: IAuthorize) {

	val logger: Logger = Logger(this.getClass)

	def authorize(transactionRequest: TransactionRequest): TransactionResponse = {
		toTransaction(transactionRequest)
			.flatMap { transaction: Transaction =>
				authorizerService.authorize(transaction).map(_ => TransactionResponse(code = "00"))
			}.recover {
				case e: NotEnoughBalanceException => TransactionResponse(code = "51")
				case e: Exception =>
					logger.error("Could not process transaction account due to error:", e)
					TransactionResponse(code = "07")
			}.get
	}

	def toTransaction(transactionRequest: TransactionRequest): Try[Transaction] = {
		def isEmpty(field: String) = Option(field).forall(_.trim.isEmpty)

		(transactionRequest.account, transactionRequest.mcc, transactionRequest.merchant) match {
			case (acc, _, _) if isEmpty(acc) => Failure(new IllegalArgumentException("Account can not be empty"))
			case (_, mcc, _) if isEmpty(mcc) => Failure(new IllegalArgumentException("MCC can not be empty"))
			case (_, _, mer) if isEmpty(mer) => Failure(new IllegalArgumentException("Merchant can not be empty"))
			case (acc, _, _) if acc.toIntOption.isEmpty => Failure(new IllegalArgumentException("Invalid account provided: must be a number."))
			case _ =>
				Success(Transaction(
					account = transactionRequest.account.toInt,
					totalAmount = transactionRequest.totalAmount,
					mcc = transactionRequest.mcc,
					merchant = transactionRequest.merchant
				))
		}
	}
}
