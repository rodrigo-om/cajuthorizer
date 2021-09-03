package domain.ports.out

import com.google.inject.ImplementedBy
import repositories.authorization.hardcoded.MerchantToMccRepository

@ImplementedBy(classOf[MerchantToMccRepository])
trait IObtainMccFromMerchants {
	def findFromMerchant(merchant: String): Option[String]
}
