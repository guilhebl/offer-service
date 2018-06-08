package common.util

import product.marketplace.common.MarketplaceConstants._

object EntityValidationUtil {
  def isValidCountry(c : String) : Boolean = {
    c match {
      case (UnitedStates | Canada) => true
      case _ => false
    }
  }

  def isValidMarketplaceIdType(id : String) : Boolean = {
    id match {
      case (Id | Upc | Isbn | Ean) => true
      case _ => false
    }
  }

}
