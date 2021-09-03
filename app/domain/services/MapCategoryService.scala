package domain.services

import com.google.inject.{Inject, Singleton}
import domain.models.BenefitCategory.BenefitCategory
import domain.ports.out.IObtainSupportedCategoriesFromMCCs
import domain.ports.out.{IObtainMccFromMerchants, IObtainSupportedCategoriesFromMCCs}

@Singleton
class MapCategoryService @Inject()(obtainMccFromMerchants: IObtainMccFromMerchants, obtainSupportedCategoriesFromMCCs: IObtainSupportedCategoriesFromMCCs) {
  def mapToASupportedBenefitCategory(merchant: String, mcc: String): Option[BenefitCategory] = {
    /*
      Merchant values are composed by 40 characters. The first 25 refer to the merchant name and the last 15
      refer to its location. We override MCCs by the merchant name, no matter the location, so we extract only
      the first 25 characters.
    */
    val merchantName = merchant.substring(0, 25).trim
    val maybeMccOverride = obtainMccFromMerchants.findFromMerchant(merchantName)
    obtainSupportedCategoriesFromMCCs.findFromMcc(maybeMccOverride.getOrElse(mcc))
  }
}
