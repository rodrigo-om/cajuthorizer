package domain.services.exceptions

case class CouldNotCreateAccountException(message: String) extends RuntimeException(message)

object CouldNotCreateAccountException {
	def apply(message: String): CouldNotCreateAccountException = new CouldNotCreateAccountException(message)
}