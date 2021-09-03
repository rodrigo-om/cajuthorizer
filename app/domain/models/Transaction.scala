package domain.models

case class Transaction(
	account: Int,
	totalAmount: Double,
	mcc: String,
	merchant: String
)
