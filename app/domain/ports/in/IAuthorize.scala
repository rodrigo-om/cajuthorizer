package domain.ports.in

import com.google.inject.ImplementedBy
import domain.models.Transaction
import domain.services.AuthorizerService

import scala.util.Try

@ImplementedBy(classOf[AuthorizerService])
trait IAuthorize {
	/*
    Possible improvement: Change this return to something more akin to a Either[Fail(AuthorizationException), Success()] to better
    expose possible specific outcomes of authorization errors.
  */
	def authorize(transaction: Transaction): Try[Unit]
}
