package app.product.model

/**
  * Captures keyword matches in this offer
  * @param offer - the offer of this rank
  * @param uniqueMatches - Num of unique keyword matches found in offer
  * @param totalMatches - total matches of keywords found
  * @param minDistanceWords - total minimal distance between giver words in offer title
  * @param lowestIndexFirstWord - lowest keyword index found for first word (closest to start of target string)
  */
case class OfferKeywordRank (offer: Offer, uniqueMatches: Int, totalMatches: Int, minDistanceWords : Int, lowestIndexFirstWord: Int)

