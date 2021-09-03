package unit.repositories.authorization.hardcoded

import domain.models.BenefitCategory
import org.mockito.MockitoSugar
import org.mockito.scalatest.ResetMocksAfterEachTest
import org.scalatestplus.play.PlaySpec
import repositories.authorization.hardcoded.{MccToCategoryRepository, MerchantToMccRepository}


class MerchantToMccRepositoryTest extends PlaySpec with MockitoSugar with ResetMocksAfterEachTest {

  private val repository = new MerchantToMccRepository

  "MapCategoryService#findFromMcc" should {
    "Map MCCs correctly according to coded rules" in {
      //Arrange
      val UberEatsMerchant            = "UBER EATS"
      val UberTripMerchant            = "UBER TRIP"
      val PagJoseDaSilvaMerchant      = "PAG*JoseDaSilva"
      val PicPayBilheteUnicoMerchant  = "PICPAY*BILHETEUNICO"

      //Act
      val maybeMccUberEatsMerchant = repository.findFromMerchant(UberEatsMerchant)
      val maybeMccUberTripMerchant = repository.findFromMerchant(UberTripMerchant)
      val maybeMccPagJoseDaSilvaMerchant = repository.findFromMerchant(PagJoseDaSilvaMerchant)
      val maybeMccPicPayBilheteUnicoMerchant = repository.findFromMerchant(PicPayBilheteUnicoMerchant)
      val noMcc = repository.findFromMerchant("NO OVERRIDE")


      //Assert
      maybeMccUberEatsMerchant.value mustEqual "5811"
      maybeMccUberTripMerchant.value mustEqual "7777" //Simulating an hypothetical transport mcc
      maybeMccPagJoseDaSilvaMerchant.value mustEqual "8888" //Simulating an hypothetical random sale mcc
      maybeMccPicPayBilheteUnicoMerchant.value mustEqual "7777" //Simulating an hypothetical transport mcc
      noMcc mustBe None
    }
  }
}








