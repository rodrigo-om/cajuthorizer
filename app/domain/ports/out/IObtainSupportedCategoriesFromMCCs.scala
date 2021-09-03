package domain.ports.out

import com.google.inject.ImplementedBy
import domain.models.BenefitCategory.BenefitCategory
import repositories.authorization.hardcoded.MccToCategoryRepository

@ImplementedBy(classOf[MccToCategoryRepository])
trait IObtainSupportedCategoriesFromMCCs {
	def findFromMcc(mcc: String): Option[BenefitCategory]
}
