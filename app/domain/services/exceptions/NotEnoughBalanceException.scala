package domain.services.exceptions

case class NotEnoughBalanceException() extends RuntimeException()

object NotEnoughBalanceException {
	def apply(): NotEnoughBalanceException = new NotEnoughBalanceException()
}