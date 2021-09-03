package repositories.authorization.hardcoded

import domain.models.BenefitCategory.{BenefitCategory, CULTURE, FOOD, MEAL}
import domain.ports.out.IObtainSupportedCategoriesFromMCCs

class MccToCategoryRepository extends IObtainSupportedCategoriesFromMCCs {
	override def findFromMcc(mcc: String): Option[BenefitCategory] = {
		mcc match {
			case "5811" => Some(MEAL)
			case "5812" => Some(MEAL)
			case "5813" => Some(MEAL)
			case "5814" => Some(MEAL)
			case "5411" => Some(FOOD)
			case "5815" => Some(CULTURE)
			case _ => None
		}
	}
}
