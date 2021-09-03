package controllers.account

import com.google.inject.{Inject, Singleton}
import domain.models.Account
import domain.models.BenefitCategory.{BenefitCategory, CULTURE, FOOD, MEAL}
import domain.ports.in.IManageAccounts
import play.api.Logger
import play.api.libs.json.{Json, Reads, Writes}
import play.api.mvc.Result
import play.api.mvc.Results.{BadRequest, Created, InternalServerError, Ok}

import scala.util.{Failure, Success, Try}

@Singleton
class AccountManagementHTTPAdapter @Inject()(accountManagementService: IManageAccounts) {

	val logger: Logger = Logger(this.getClass)

	implicit val transactionRequestWriter: Writes[Account] = Json.writes[Account]

	def find(id: Int): Result = {
		accountManagementService.findAccount(id) match {
			case Some(account) => Ok(Json.toJson(account))
			/*
				I know, I know... the huge old discussion about IS "ITEM NOT FOUND" A 404 OR A 200???? Some say Aristotle may have started this
				discussion. Or Plato. Freud. Who knows. Well, I think it's up to discussion, but I don't want to confuse anyone with a 404 thinking
				they may have called the wrong URI, so I'm gonna side temporarily with 200 here along with a message.
			*/
			case None => Ok(Json.toJson("Account was not found"))
		}
	}

	def findAll(): Result = {
		Ok(Json.toJson(accountManagementService.findAll()))
	}

	def create(transactionRequest: AccountCreateRequest): Result = {
		toAccount(transactionRequest)
			.flatMap { account: Account =>
				accountManagementService.create(account).map(_ => Created)
			}.recover {
				case e: IllegalArgumentException => BadRequest
				case e: Exception =>
					logger.error("Could not create account due to error:", e)
					InternalServerError(e.getMessage)
			}.get
	}

	def toAccount(accountRequest: AccountCreateRequest): Try[Account] = {
		accountRequest match {
			case a if a.id < 0 => Failure(new IllegalArgumentException("Account can not be negative"))
			case a if a.mealBalance < 0 => Failure(new IllegalArgumentException("Meal balance can not be negative"))
			case a if a.foodBalance < 0 => Failure(new IllegalArgumentException("Food balance can not be negative"))
			case a if a.cultureBalance < 0 => Failure(new IllegalArgumentException("Culture balance can not be negative"))
			case a if a.cashBalance < 0 => Failure(new IllegalArgumentException("Cash balance can not be negative"))
			case a =>
				val benefitBalances: Map[BenefitCategory, Double] = Map(
					MEAL -> a.mealBalance,
					FOOD -> a.foodBalance,
					CULTURE -> a.cultureBalance,
				)

				Success(Account(id = a.id, benefitBalances = benefitBalances, cashBalance = a.cashBalance, lastTransaction = None))
		}
	}
}
