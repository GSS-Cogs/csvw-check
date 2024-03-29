package csvwcheck.errors

final case class DateFormatError(
                                  message: String = "",
                                  cause: Throwable = None.orNull
                                ) extends Exception(message, cause)
