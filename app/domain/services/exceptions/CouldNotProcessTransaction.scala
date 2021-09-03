package domain.services.exceptions

case class CouldNotProcessTransaction(message: String) extends RuntimeException(message)

object CouldNotProcessTransaction {
	def apply(message: String): CouldNotProcessTransaction = new CouldNotProcessTransaction(message)
}