package domain.models

case object BenefitCategory extends Enumeration {
  type BenefitCategory = Value
  val MEAL, FOOD, CULTURE = Value
}
