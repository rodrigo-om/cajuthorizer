package unit.repositories.authorization.hardcoded

import domain.models.BenefitCategory
import domain.ports.out.{IObtainMccFromMerchants, IObtainSupportedCategoriesFromMCCs}
import domain.services.MapCategoryService
import org.mockito.MockitoSugar
import org.mockito.scalatest.ResetMocksAfterEachTest
import org.scalatestplus.play.PlaySpec
import repositories.authorization.hardcoded.MccToCategoryRepository


class MccToCategoryRepositoryTest extends PlaySpec with MockitoSugar with ResetMocksAfterEachTest {

  private val repository = new MccToCategoryRepository

  "MapCategoryService#findFromMcc" should {
    "Map MCCs correctly according to coded rules" in {
      //Arrange
      val restaurantMCC5811 = "5811"
      val restaurantMCC5812 = "5812"
      val restaurantMCC5813 = "5813"
      val restaurantMCC5814 = "5814"
      val supermarketMCC5411 = "5411"
      val cultureMCC5815 = "5815"

      //Act
      val maybeCategoryRestaurantMCC5811 = repository.findFromMcc(restaurantMCC5811)
      val maybeCategoryRestaurantMCC5812 = repository.findFromMcc(restaurantMCC5812)
      val maybeCategoryRestaurantMCC5813 = repository.findFromMcc(restaurantMCC5813)
      val maybeCategoryRestaurantMCC5814 = repository.findFromMcc(restaurantMCC5814)
      val maybeCategorySupermarketMCC5411 = repository.findFromMcc(supermarketMCC5411)
      val maybeCategoryCultureMCC5815 = repository.findFromMcc(cultureMCC5815)
      val noCategory = repository.findFromMcc("0000")


      //Assert
      maybeCategoryRestaurantMCC5811.value mustEqual BenefitCategory.MEAL
      maybeCategoryRestaurantMCC5812.value mustEqual BenefitCategory.MEAL
      maybeCategoryRestaurantMCC5813.value mustEqual BenefitCategory.MEAL
      maybeCategoryRestaurantMCC5814.value mustEqual BenefitCategory.MEAL
      maybeCategorySupermarketMCC5411.value mustEqual BenefitCategory.FOOD
      maybeCategoryCultureMCC5815.value mustEqual BenefitCategory.CULTURE
      noCategory mustBe None
    }
  }
}








