package csvwcheck.models

case class KeyWithContext(
                           rowNumber: Long,
                           keyValues: List[Any],
                           var isDuplicate: Boolean = false
                         ) {

  /**
    * KeyWithContext object holds the rowNumber information for setting better errors.
    * Thus `rowNumber` attribute doesn't need to be considered when checking the equality of 2 KeyWithContext objects
    * or for the hashCode of the KeyWithContext object.
    */
  override def equals(obj: Any): Boolean =
    obj != null &&
      obj.isInstanceOf[KeyWithContext] &&
      this.keyValues.equals(obj.asInstanceOf[KeyWithContext].keyValues)

  override def hashCode(): Int = this.keyValues.hashCode()

  def keyValuesToString(): String = {
    val stringList = keyValues.map {
      case listOfAny: List[Any] =>
        listOfAny.map(s => s.toString).mkString(",")
      case i => i.toString
    }
    stringList.mkString(",")
  }

}
