package controllers.authorization

import com.google.inject.{Inject, Singleton}
import domain.models.Transaction
import domain.services.AuthorizerService
import domain.services.exceptions.{CouldNotProcessTransaction, NotEnoughBalanceException}
import play.api.libs.json.{JsError, JsSuccess, JsValue, Json, Reads, Writes}
import play.api.mvc.{Action, BaseController, ControllerComponents}


@Singleton
class AuthorizationController @Inject()(
	val controllerComponents: ControllerComponents,
	authorizationAdapter: AuthorizationHTTPAdapter
) extends BaseController {

	implicit val transactionRequestReader: Reads[TransactionRequest] = Json.reads[TransactionRequest]
	implicit val transactionRequestWriter: Writes[TransactionResponse] = Json.writes[TransactionResponse]

	/*
		I took the "The HTTP Status Code is always 200" instruction very literally, wrapping every processing error or validation inside it,
		only exception was BadRequests of malformed bodies, because otherwise letting them would make things weirder.
	*/
	def authorize(): Action[JsValue] = Action(parse.json) { implicit request =>
		request.body.validate[TransactionRequest] match {
			case JsSuccess(transactionRequest, _) => Ok(Json.toJson(authorizationAdapter.authorize(transactionRequest)))
			case JsError(_) => BadRequest
		}
	}
}
