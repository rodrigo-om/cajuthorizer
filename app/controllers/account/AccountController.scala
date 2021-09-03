package controllers.account

import com.google.inject.{Inject, Singleton}
import play.api.libs.json._
import play.api.mvc.{Action, AnyContent, BaseController, ControllerComponents}


@Singleton
class AccountController @Inject()(
	val controllerComponents: ControllerComponents,
	accountManagementAdapter: AccountManagementHTTPAdapter
) extends BaseController {

	implicit val transactionRequestReader: Reads[AccountCreateRequest] = Json.reads[AccountCreateRequest]

	def find(id: Int): Action[AnyContent] = Action {
		accountManagementAdapter.find(id)
	}

	def findAll(): Action[AnyContent] = Action {
		accountManagementAdapter.findAll()
	}

	def create(): Action[JsValue] = Action(parse.json) { implicit request =>
		request.body.validate[AccountCreateRequest] match {
			case JsSuccess(accountCreateRequest, _) => accountManagementAdapter.create(accountCreateRequest)
			case JsError(_) => BadRequest
		}
	}
}
