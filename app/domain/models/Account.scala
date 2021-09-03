package domain.models

import domain.models.BenefitCategory.BenefitCategory

case class Account (
	id: Int,
	benefitBalances: Map[BenefitCategory, Double],
	cashBalance: Double,
	lastTransaction: Option[String] //UUID
)
