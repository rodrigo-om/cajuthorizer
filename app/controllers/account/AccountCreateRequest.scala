package controllers.account

case class AccountCreateRequest (
	id: Int,
	mealBalance: Double,
	foodBalance: Double,
	cultureBalance: Double,
	cashBalance: Double,
)
