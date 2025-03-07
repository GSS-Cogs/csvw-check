package csvwcheck.models

object Values {
  /**
   * Part of a KeyValue. Whereas a KeyValue may take values from multiple columns, a KeyComponent value is from
   * a single column.
   */
  type KeyComponentValue = Any

  /**
   * The value associated with a Key in a given row.
   */
  type KeyValue = List[KeyComponentValue]

  /**
   * Represents the value from one particular column.
   *
   * May contain multiple values if the column represents a list (i.e. has a separator set).
   */
  type ColumnValue = List[Any]
}
