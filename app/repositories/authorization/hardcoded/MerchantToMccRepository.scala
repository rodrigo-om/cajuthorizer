package repositories.authorization.hardcoded

import domain.ports.out.IObtainMccFromMerchants

class MerchantToMccRepository extends IObtainMccFromMerchants {
	override def findFromMerchant(merchant: String): Option[String] = {
		merchant match {
			case "UBER EATS" => Some("5811")
			case "UBER TRIP" => Some("7777")
			case "PAG*JoseDaSilva" => Some("8888")
			case "PICPAY*BILHETEUNICO" => Some("7777")
			case _ => None
		}
	}
}
