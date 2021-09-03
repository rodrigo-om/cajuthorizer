package controllers.authorization

case class TransactionRequest (
	account: String,
	totalAmount: Double,
	mcc: String,
	merchant: String
)