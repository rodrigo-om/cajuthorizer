package unit.domain.services

import domain.models.BenefitCategory
import domain.ports.out.IObtainSupportedCategoriesFromMCCs
import domain.ports.out.{IObtainMccFromMerchants, IObtainSupportedCategoriesFromMCCs}
import domain.services.MapCategoryService
import org.mockito.MockitoSugar
import org.mockito.scalatest.ResetMocksAfterEachTest
import org.scalatestplus.play.PlaySpec


class MapCategoryServiceTest extends PlaySpec with MockitoSugar with ResetMocksAfterEachTest {

  private val mockIObtainMccFromMerchants = mock[IObtainMccFromMerchants]
  private val mockIObtainSupportedCategoriesFromMCCs = mock[IObtainSupportedCategoriesFromMCCs]
  private val service = new MapCategoryService(mockIObtainMccFromMerchants, mockIObtainSupportedCategoriesFromMCCs)

  "MapCategoryService#mapToASupportedBenefitCategory" should {

    "Map category correctly when no override is done and mcc is supported" in {
      //Arrange
      val merchant = "PICPAY*NTEMOVERRIDE           GOIANIA BR"
      val merchantName = merchant.substring(0, 25).trim
      val mcc = "5811" //Restaurant MCC
      when(mockIObtainMccFromMerchants.findFromMerchant(merchantName)).thenReturn(None)
      when(mockIObtainSupportedCategoriesFromMCCs.findFromMcc(mcc)).thenReturn(Some(BenefitCategory.MEAL))

      //Act
      val maybeCategory = service.mapToASupportedBenefitCategory(merchant, mcc)

      //Assert
      maybeCategory.value mustEqual BenefitCategory.MEAL
    }

    "Map category correctly when override is done and new mcc is supported" in {
      //Arrange
      val merchant = "UBER EATS                   SAO PAULO BR"
      val merchantName = merchant.substring(0, 25).trim
      val mcc = "5815" //Audiovisual MCC
      when(mockIObtainMccFromMerchants.findFromMerchant(merchantName)).thenReturn(Some("5811"))
      when(mockIObtainSupportedCategoriesFromMCCs.findFromMcc("5811")).thenReturn(Some(BenefitCategory.MEAL))

      //Act
      val maybeCategory = service.mapToASupportedBenefitCategory(merchant, mcc)

      //Assert
      maybeCategory.value mustEqual BenefitCategory.MEAL
    }

    "Map category correctly when override is done to a supported mcc even if original mcc was not supported" in {
      //Arrange
      val merchant = "UBER EATS                   SAO PAULO BR"
      val merchantName = merchant.substring(0, 25).trim
      val mcc = "0000"
      when(mockIObtainMccFromMerchants.findFromMerchant(merchantName)).thenReturn(Some("5811"))
      when(mockIObtainSupportedCategoriesFromMCCs.findFromMcc("5811")).thenReturn(Some(BenefitCategory.MEAL))

      //Act
      val maybeCategory = service.mapToASupportedBenefitCategory(merchant, mcc)

      //Assert
      maybeCategory.value mustEqual BenefitCategory.MEAL
    }

    "NOT map to a category when no override is done and mcc is not supported" in {
      //Arrange
      val merchant = "PICPAY*NTEMOVERRIDE           GOIANIA BR"
      val merchantName = merchant.substring(0, 25).trim
      val mcc = "0000"
      when(mockIObtainMccFromMerchants.findFromMerchant(merchantName)).thenReturn(None)
      when(mockIObtainSupportedCategoriesFromMCCs.findFromMcc("0000")).thenReturn(None)

      //Act
      val maybeCategory = service.mapToASupportedBenefitCategory(merchant, mcc)

      //Assert
      maybeCategory mustBe None
    }

    "NOT map to a category when override is done but overriden mcc is not supported even if original mcc was valid" in {
      //Arrange
      val merchant = "UBER TRIP                   SAO PAULO BR"
      val merchantName = merchant.substring(0, 25).trim
      val mcc = "5811" //Restaurant MCC
      when(mockIObtainMccFromMerchants.findFromMerchant(merchantName)).thenReturn(Some("0000"))
      when(mockIObtainSupportedCategoriesFromMCCs.findFromMcc("0000")).thenReturn(None)

      //Act
      val maybeCategory = service.mapToASupportedBenefitCategory(merchant, mcc)

      //Assert
      maybeCategory mustBe None
    }
  }
}








