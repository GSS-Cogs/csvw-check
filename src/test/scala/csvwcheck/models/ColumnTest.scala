package csvwcheck.models

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.node.{IntNode, JsonNodeFactory, ObjectNode, TextNode}
import csvwcheck.ConfiguredObjectMapper.objectMapper
import csvwcheck.errors.{ErrorWithoutContext, WarningWithCsvContext}
import org.scalatest.funsuite.AnyFunSuite

//noinspection HttpUrlsUsage
class ColumnTest extends AnyFunSuite {
  def getColumnWithFormat(formatJson: Option[String]): Column = {
    val json =
      s"""
         |{
         |"name":"countryCode",
         |"format": ${formatJson.getOrElse("null")}
         |}"
         |""".stripMargin

    val jsonNode = objectMapper.readTree(json)
    val Right(column) = Column.fromJson(
      1,
      jsonNode.asInstanceOf[ObjectNode]
    )
    column
  }

  test("should provide appropriate default values") {
    val json =
      """
        |{
        |"name":"countryCode"
        |}"
        |""".stripMargin

    val jsonNode = objectMapper.readTree(json)
    val Right(column) = Column.fromJson(
      1,
      jsonNode.asInstanceOf[ObjectNode]
    )

    val datatypeDefaultValue = "http://www.w3.org/2001/XMLSchema#string"

    assert(column.name.get === "countryCode")
    assert(column.baseDataType === datatypeDefaultValue)
    assert(column.lang === "und")
    assert(column.textDirection === "inherit")
    assert(!column.virtual)
    assert(column.columnOrdinal == 1)
    assert(!column.ordered)
    assert(!column.required)
    assert(!column.suppressOutput)
    assert(column.urls.aboutUrl.isEmpty)
    assert(column.default == "")
    assert(column.urls.propertyUrl.isEmpty)
    assert(column.separator.isEmpty)
    assert(column.titleValues.isEmpty)
  }

  test("should override default values") {
    //noinspection HttpUrlsUsage
    val json =
      """
        |{
        |   "name": "countryCode",
        |   "titles": {
        |     "en": ["countryCode"]
        |   },
        |   "propertyUrl": "http://www.geonames.org/ontology",
        |   "aboutUrl": "sampleUrl",
        |   "datatype": {
        |     "@id": "http://www.w3.org/2001/XMLSchema#integer"
        |   },
        |   "lang": "en",
        |   "default": "00",
        |   "null": ["-"],
        |   "ordered": true,
        |   "propertyUrl": "http://www.geonames.org/ontology",
        |   "required": true,
        |   "separator": ",",
        |   "suppressOutput": true,
        |   "textDirection": "rtl",
        |   "valueUrl": "http://www.geonames.org/ontology",
        |   "virtual": true
        |}
        |""".stripMargin

    val jsonNode = objectMapper.readTree(json)
    val Right(column) = Column.fromJson(
      1,
      jsonNode.asInstanceOf[ObjectNode]
    )
    val expectedDataType = "http://www.w3.org/2001/XMLSchema#integer"

    assert(column.name.get === "countryCode")
    assert(column.columnOrdinal === 1)
    assert(column.id === None)
    assert(column.urls.aboutUrl.get === "sampleUrl")
    assert(column.baseDataType === expectedDataType)
    assert(column.default === "00")
    assert(column.lang === "en")
    assert(column.nullParam === Array[String]("-"))
    assert(column.ordered === true)
    assert(column.urls.propertyUrl.get === "http://www.geonames.org/ontology")
    assert(column.required === true)
    assert(column.separator.get === ",")
    assert(column.suppressOutput === true)
    assert(column.textDirection === "rtl")
    assert(column.titleValues("en") === Array("countryCode"))
    assert(column.urls.valueUrl.get === "http://www.geonames.org/ontology")
    assert(column.virtual === true)
  }

  test("it should return no warnings for a null value of null") {

    val json =
      """
        |{
        |   "name": "countryCode",
        |   "null": null
        |}
        |""".stripMargin

    val jsonNode = objectMapper.readTree(json)
    val Right(column) = Column.fromJson(
      1,
      jsonNode.asInstanceOf[ObjectNode]
    )
    assert(column.nullParam === Array[String](""))
  }

  test("should set the correct datatype") {
    val json =
      """
        |{
        |   "name": "Id",
        |   "required": true,
        |   "datatype": { "base": "string", "minLength": 3 }
        |}
        |""".stripMargin

    val jsonNode = objectMapper.readTree(json)
    val Right(column) = Column.fromJson(
      1,
      jsonNode.asInstanceOf[ObjectNode]
    )
    val expectedDatatypeValue: ObjectNode = JsonNodeFactory.instance
      .objectNode()
    expectedDatatypeValue.set(
      "base",
      new TextNode("http://www.w3.org/2001/XMLSchema#string")
    )
    expectedDatatypeValue.set("minLength", new IntNode(3))

    assert(column.name.get === "Id")
    assert(column.required)
    assert(column.lengthRestrictions.minLength.get === 3)
  }

  // Tests for processFloatDatatype method
  test("should process valid float datatype value without errors") {
    val columnWithNoFormat = getColumnWithFormat(None)
    val result = columnWithNoFormat.processFloatDatatype("4268.22752E11")
    assert(result.isRight)
    (result: @unchecked) match {
      case Right(floatValue) =>
        assert(floatValue == "4268.22752E11".toFloat)
    }
  }

  test("should return error when invalid float value is processed") {
    val columnWithNoFormat = getColumnWithFormat(None)
    val result =
      columnWithNoFormat.processFloatDatatype(
        "-3E2.4"
      ) //the exponent must be an integer
    assert(result.isLeft)
    (result: @unchecked) match {
      case Left(ErrorWithoutContext(errorType, _)) =>
        assert(errorType == "invalid_float")
    }
  }

  // Tests for processDoubleDatatype method
  test("should process valid double datatype value without errors") {
    val columnWithNoFormat = getColumnWithFormat(None)
    val result = columnWithNoFormat.processDoubleDatatype("4268.22752E11")
    assert(result.isRight)
    (result: @unchecked) match {
      case Right(doubleDatatype) =>
        assert(doubleDatatype == 4.26822752e14)
    }
  }

  test(
    "should process valid double (negative infinity) datatype value without errors"
  ) {
    val columnWithNoFormat = getColumnWithFormat(None)
    val result = columnWithNoFormat.processDoubleDatatype("-INF")
    assert(result.isRight)
    (result: @unchecked) match {
      case Right(doubleValue) =>
        assert(doubleValue.isNegInfinity)
    }
  }

  test("should return error when invalid double value is processed") {
    val columnWithNoFormat = getColumnWithFormat(None)
    val result = columnWithNoFormat.processDoubleDatatype("NAN")
    assert(
      result.isLeft
    ) //values are case-sensitive, must be capitalized correctly
    (result: @unchecked) match {
      case Left(error) =>
        assert(error.`type` == "invalid_double")
    }
  }

  // Tests for processNegativeInteger
  test("should return error when invalid negative integer is processed") {
    val columnWithNoFormat = getColumnWithFormat(None)
    val result = columnWithNoFormat.processNegativeInteger("0")
    assert(
      result.isLeft
    ) // 0 is not considered negative
    (result: @unchecked) match {
      case Left(error) =>
        assert(error.`type` == "invalid_negativeInteger")
    }
  }

  test(
    "should process valid negativeInteger datatype value without errors"
  ) {
    val columnWithNoFormat = getColumnWithFormat(None)
    val result =
      columnWithNoFormat
        .processNegativeInteger("-00122") // leading zeros are permitted
    assert(result.isRight)
    (result: @unchecked) match {
      case Right(negativeInteger) =>
        assert(negativeInteger.toString == "-122")
    }
  }

  // Tests for processNonPositiveInteger

  test("should return error when invalid NonPositiveInteger is processed") {
    val columnWithNoFormat = getColumnWithFormat(None)
    val result =
      columnWithNoFormat.processNonPositiveInteger(
        "3.0"
      ) //value must not contain a decimal point
    assert(result.isLeft)
    (result: @unchecked) match {
      case Left(error) =>
        assert(error.`type` == "invalid_nonPositiveInteger")
    }
  }

  test("should process valid non positive Integer value without errors") {
    val columnWithNoFormat = getColumnWithFormat(None)
    val result = columnWithNoFormat
      .processNonPositiveInteger("0")
    assert(result.isRight)
    (result: @unchecked) match {
      case Right(nonPositiveInteger) =>
        assert(nonPositiveInteger.toString == "0")
    }
  }

  // Tests for processUnsignedByte
  test("should process valid unsigned byte value without errors") {
    val columnWithNoFormat = getColumnWithFormat(None)
    val result = columnWithNoFormat
      .processUnsignedByte("+3") //
    assert(result.isRight)
    (result: @unchecked) match {
      case Right(unsignedByte) =>
        assert(unsignedByte.toString == "3")
    }
  }

  test("should return errors when invalid unsigned byte value is processed") {
    val columnWithNoFormat = getColumnWithFormat(None)
    val result =
      columnWithNoFormat.processUnsignedByte(
        "256"
      ) // Number is too large to be a unsigned byte
    assert(result.isLeft)
    (result: @unchecked) match {
      case Left(error) =>
        assert(error.`type` == "invalid_unsignedByte")
    }
  }

  // Tests for processUnsignedShort
  test("should return error when invalid unsigned short value is processed") {
    val columnWithNoFormat = getColumnWithFormat(None)
    val result =
      columnWithNoFormat.processUnsignedShort(
        "-123"
      ) //negative values are not allowed
    assert(result.isLeft)
    (result: @unchecked) match {
      case Left(error) =>
        assert(error.`type` == "invalid_unsignedShort")
    }
  }

  test("should process valid unsigned short datatype value without errors") {
    val columnWithNoFormat = getColumnWithFormat(None)
    val result = columnWithNoFormat
      .processUnsignedShort("0")
    assert(result.isRight)
    (result: @unchecked) match {
      case Right(unsignedShort) =>
        assert(unsignedShort.toString == "0")
    }
  }

  // Tests for processUnsignedInt

  test("should process valid unsigned Int datatype value without errors") {
    val columnWithNoFormat = getColumnWithFormat(None)
    val result = columnWithNoFormat.processUnsignedInt("4545454")
    assert(result.isRight)
    (result: @unchecked) match {
      case Right(unsignedInt) =>
        assert(unsignedInt.toString == "4545454")
    }
  }

  test("should return error when invalid unsigned int value is processed") {
    val columnWithNoFormat = getColumnWithFormat(None)
    val result =
      columnWithNoFormat.processUnsignedInt("4294967299") // number is too large
    assert(result.isLeft)
    (result: @unchecked) match {
      case Left(error) =>
        assert(error.`type` == "invalid_unsignedInt")
    }
  }

  //Tests for processUnsignedLong
  test("should return error when invalid unsigned Long value is processed") {
    val columnWithNoFormat = getColumnWithFormat(None)
    val result =
      columnWithNoFormat.processUnsignedLong(
        "18446744073709551620"
      ) // number is too large
    assert(result.isLeft)
    (result: @unchecked) match {
      case Left(error) =>
        assert(error.`type` == "invalid_unsignedLong")
    }
  }

  test("should process valid unsigned Long datatype value without errors") {
    val columnWithNoFormat = getColumnWithFormat(None)
    val result = columnWithNoFormat.processUnsignedLong("+3")
    assert(result.isRight)
    (result: @unchecked) match {
      case Right(unsignedLong) =>
        assert(unsignedLong.toString == "3")
    }
  }

  // Tests for processPositiveInteger
  test("should process valid positive integer datatype value without errors") {
    val columnWithNoFormat = getColumnWithFormat(None)
    val result = columnWithNoFormat.processPositiveInteger("00122")
    assert(result.isRight)
    (result: @unchecked) match {
      case Right(positiveInteger) =>
        assert(positiveInteger.toString() == "122")
    }
  }

  test("should return error when invalid positive integer value is processed") {
    val columnWithNoFormat = getColumnWithFormat(None)
    val result =
      columnWithNoFormat.processPositiveInteger("3.0")
    assert(result.isLeft)
    (result: @unchecked) match {
      case Left(error) =>
        assert(error.`type` == "invalid_positiveInteger")
    }
  }

  // Tests for processNonNegativeInteger
  test(
    "should return error when invalid non negative integer value is processed"
  ) {
    val columnWithNoFormat = getColumnWithFormat(None)
    val result =
      columnWithNoFormat.processNonNegativeInteger("-3")
    assert(result.isLeft)
    (result: @unchecked) match {
      case Left(error) =>
        assert(error.`type` == "invalid_nonNegativeInteger")
    }
  }

  test(
    "should process valid non negative integer datatype value without errors"
  ) {
    val columnWithNoFormat = getColumnWithFormat(None)
    val result = columnWithNoFormat.processNonNegativeInteger("0")
    assert(result.isRight)
    (result: @unchecked) match {
      case Right(nonNegativeInteger) =>
        assert(nonNegativeInteger.toString() == "0")
    }
  }

  // Tests for processByteDatatype
  test(
    "should process valid byte datatype value without errors"
  ) {
    val columnWithNoFormat = getColumnWithFormat(None)
    val result = columnWithNoFormat.processByteDatatype("-123")
    assert(result.isRight)
    (result: @unchecked) match {
      case Right(byteValue) =>
        assert(byteValue == -123)
    }
  }

  test(
    "should return error when invalid byte value is processed"
  ) {
    val columnWithNoFormat = getColumnWithFormat(None)
    val result =
      columnWithNoFormat.processByteDatatype("2.23")
    assert(result.isLeft)
    (result: @unchecked) match {
      case Left(error) =>
        assert(error.`type` == "invalid_byte")
    }
  }

  // Tests for processShortDatatype
  test(
    "should return error when invalid short value is processed"
  ) {
    val columnWithNoFormat = getColumnWithFormat(None)
    val result =
      columnWithNoFormat.processShortDatatype("32770")
    assert(result.isLeft)
    (result: @unchecked) match {
      case Left(error) =>
        assert(error.`type` == "invalid_short")
    }
  }

  test("should process valid short datatype value without errors") {
    val columnWithNoFormat = getColumnWithFormat(None)
    val result = columnWithNoFormat.processShortDatatype("-1231")
    assert(result.isRight)
    (result: @unchecked) match {
      case Right(value) =>
        assert(value.toString == "-1231")
    }
  }

  // Tests for processIntDatatype
  test("should process valid int datatype value without errors") {
    val columnWithNoFormat = getColumnWithFormat(None)
    val result = columnWithNoFormat.processIntDatatype("-12312")
    assert(result.isRight)
    (result: @unchecked) match {
      case Right(value) =>
        assert(value.toString == "-12312")
    }
  }

  test(
    "should return error when invalid int value is processed"
  ) {
    val columnWithNoFormat = getColumnWithFormat(None)
    val result =
      columnWithNoFormat.processIntDatatype("2147483650") //number too large)
    assert(result.isLeft)
    (result: @unchecked) match {
      case Left(error) =>
        assert(error.`type` == "invalid_int")
    }
  }

  // Tests for processLongDatatype
  test(
    "should return error when invalid Long value is processed"
  ) {
    val columnWithNoFormat = getColumnWithFormat(None)
    val result =
      columnWithNoFormat.processLongDatatype(
        "9223372036854775810"
      ) //number too large
    assert(result.isLeft)
    (result: @unchecked) match {
      case Left(error) =>
        assert(
          error.`type` == "invalid_long"
        )
    }
  }

  test("should process valid Long datatype value without errors") {
    val columnWithNoFormat = getColumnWithFormat(None)
    val result = columnWithNoFormat.processLongDatatype("-1231235555")
    assert(result.isRight)
    (result: @unchecked) match {
      case Right(value) =>
        assert(value.toString == "-1231235555")
    }
  }

  // Tests for processIntegerDatatype
  test("should process valid Integer datatype value without errors") {
    val columnWithNoFormat = getColumnWithFormat(None)
    val result = columnWithNoFormat.processIntegerDatatype("-00122")
    assert(result.isRight)
    (result: @unchecked) match {
      case Right(value) =>
        assert(value.toString() == "-122")
    }
  }

  test(
    "should return error when invalid Integer value is processed"
  ) {
    val columnWithNoFormat = getColumnWithFormat(None)
    val result =
      columnWithNoFormat.processIntegerDatatype(
        "3.0"
      ) // an integer must not contain a decimal point
    assert(result.isLeft)
    (result: @unchecked) match {
      case Left(error) =>
        assert(
          error.`type` == "invalid_integer"
        )
    }
  }

  // Tests for processDecimalDatatype
  test(
    "should return error when invalid Decimal value is processed"
  ) {
    val columnWithNoFormat = getColumnWithFormat(None)
    val result =
      columnWithNoFormat.processDecimalDatatype(
        "3,5"
      ) // commas are not permitted unless specified in format

    assert(result.isLeft)
    (result: @unchecked) match {
      case Left(error) =>
        assert(
          error.`type` == "invalid_decimal"
        )
    }
  }

  test(
    "should process valid decimal datatype value beginning with decimal point without errors"
  ) {
    val columnWithNoFormat = getColumnWithFormat(None)
    val result = columnWithNoFormat.processDecimalDatatype(".3")
    assert(result.isRight)
    (result: @unchecked) match {
      case Right(value) =>
        assert(value == 0.3)
    }
  }

  test(
    "should process valid decimal datatype value ending with decimal point without errors"
  ) {
    val columnWithNoFormat = getColumnWithFormat(None)
    val result = columnWithNoFormat.processDecimalDatatype("3.")
    assert(result.isRight)
    (result: @unchecked) match {
      case Right(value) =>
        assert(value == 3)
    }
  }

  //Tests for processBooleanDatatype
  test(
    "should process 1 as true for boolean datatype"
  ) {
    val columnWithNoFormat = getColumnWithFormat(None)
    val result = columnWithNoFormat.processBooleanDatatype("1")
    assert(result.isRight)
    (result: @unchecked) match {
      case Right(value) =>
        assert(value)
    }
  }

  test(
    "should process 0 as false for boolean datatype"
  ) {
    val columnWithNoFormat = getColumnWithFormat(None)
    val result = columnWithNoFormat.processBooleanDatatype("0")
    assert(result.isRight)
    (result: @unchecked) match {
      case Right(value) =>
        assert(!value)
    }
  }

  test(
    "should process true as true for boolean datatype"
  ) {
    val columnWithNoFormat = getColumnWithFormat(None)
    val result = columnWithNoFormat.processBooleanDatatype("true")
    assert(result.isRight)
    (result: @unchecked) match {
      case Right(value) =>
        assert(value)
    }
  }

  test(
    "should process Y as true for boolean datatype when appropriate format is provided"
  ) {
    val Right((columnWithFormat)) = {
      val json =
        s"""
           |{
           |"name":"countryCode",
           |"datatype": {
           |  "base": "boolean",
           |   "format": "Y|N"
           | }
           |}"
           |""".stripMargin

      val jsonNode = objectMapper.readTree(json)
      Column.fromJson(
        1,
        jsonNode.asInstanceOf[ObjectNode]
      )
    }
    val result = columnWithFormat.processBooleanDatatype("Y")
    assert(result.isRight)
    (result: @unchecked) match {
      case Right(value) =>
        assert(value)
    }
  }

  test(
    "should NOT process TRUE as true for boolean datatype"
  ) {
    val columnWithNoFormat = getColumnWithFormat(None)
    val result =
      columnWithNoFormat.processBooleanDatatype(
        "TRUE"
      ) // values are case sensitive
    assert(result.isLeft)
    (result: @unchecked) match {
      case Left(error) =>
        assert(
          error.`type` == "invalid_boolean"
        )
    }
  }

  test("should set errors when length is less than min length specified") {
    val json =
      """
      |{"name":"Measure",
      |"datatype": {
      |  "base": "string",
      |  "minLength": 10,
      |  "maxLength": 1000
      |}
      |}
      |""".stripMargin
    val jsonNode = objectMapper.readTree(json)
    val Right(column) = Column.fromJson(
      1,
      jsonNode.asInstanceOf[ObjectNode]
    )

    val errors = column.validateLength("12")
    assert(errors.length == 1)
    assert(errors(0).`type` == "minLength")
  }

  test("should set errors when length is greater than max length specified") {
    val json =
      """
        |{"name":"Measure",
        |"datatype": {
        |  "base": "string",
        |  "minLength": 1,
        |  "maxLength": 4
        |}
        |}
        |""".stripMargin
    val jsonNode = objectMapper.readTree(json)
    val Right(column) = Column.fromJson(
      1,
      jsonNode.asInstanceOf[ObjectNode]
    )

    val errors = column.validateLength("ABCDEFG")
    assert(errors.length == 1)
    assert(errors(0).`type` == "maxLength")
  }

  test("should set errors when length is different from length specified") {
    val json =
      """
        |{"name":"Measure",
        |"datatype": {
        |  "base": "string",
        |  "length": 3,
        |  "minLength": 1,
        |  "maxLength": 4
        |}
        |}
        |""".stripMargin
    val jsonNode = objectMapper.readTree(json)
    val Right(column) = Column.fromJson(
      1,
      jsonNode.asInstanceOf[ObjectNode]
    )
    val errors = column.validateLength("ABC4")
    assert(errors.length == 1)
    assert(errors(0).`type` == "length")
  }

  // Tests for validateNumericValue
  test(
    "should correctly set errors when minInclusive and maxInclusive are bigDecimals"
  ) {
    val json =
      """
        |{"name":"Measure",
        |"datatype": {
        |  "base": "decimal",
        |  "minInclusive": 312938797193279127391467892467284100.45,
        |  "maxInclusive": 312938797193279127391467892467284500.45
        |}
        |}
        |""".stripMargin
    val jsonNode = objectMapper.readTree(json)
    val Right(column) = Column.fromJson(
      1,
      jsonNode.asInstanceOf[ObjectNode]
    )
    val error1 =
      column.validateValue(5)
    assert(error1.length == 1)
    assert(error1(0).`type` === "minInclusive")

    val error2 =
      column.validateValue(
        BigDecimal("312938797193279127391467892467284100")
      )
    assert(error2.length == 1)
    assert(error2(0).`type` === "minInclusive")

    val error3 =
      column.validateValue(
        BigDecimal("312938797193279127391467892467284501.45")
      )
    assert(error3.length == 1)
    assert(error3(0).`type` === "maxInclusive")

    val error4 =
      column.validateValue(
        BigInt("312938797193279127391467892467284099").bigInteger
      )
    assert(error4.length == 1)
    assert(error4(0).`type` === "minInclusive")

    val error5 =
      column.validateValue(
        BigInt("312938797193279127391467892467284999").bigInteger
      )
    assert(error5.length == 1)
    assert(error5(0).`type` === "maxInclusive")

    val error6 =
      column.validateValue(
        BigDecimal("312938797193279127391467892467284300.234234234")
      )
    assert(error6.length == 0)
  }

  test(
    "should correctly set errors when minInclusive and maxInclusive are integers"
  ) {
    val json =
      """
        |{"name":"Measure",
        |"datatype": {
        |  "base": "decimal",
        |  "minInclusive": 45,
        |  "maxInclusive": 54
        |}
        |}
        |""".stripMargin
    val jsonNode = objectMapper.readTree(json)
    val Right(column) = Column.fromJson(
      1,
      jsonNode.asInstanceOf[ObjectNode]
    )
    val error1 =
      column.validateValue(3)
    assert(error1.length == 1)
    assert(error1(0).`type` === "minInclusive")
    val error2 =
      column.validateValue(55)
    assert(error2.length == 1)
    assert(error2(0).`type` === "maxInclusive")

    val error3 =
      column.validateValue(
        BigInt("232423423423423423423423423423423423").bigInteger
      )
    assert(error3.length == 1)
    assert(error3(0).`type` === "maxInclusive")
    val error4 =
      column.validateValue(
        BigInt("4").bigInteger
      )
    assert(error4.length == 1)
    assert(error4(0).`type` === "minInclusive")

    val error5 =
      column.validateValue(
        BigDecimal("232423423423423423423423423423423423.123123")
      )
    assert(error5.length == 1)
    assert(error5(0).`type` === "maxInclusive")
    val error6 =
      column.validateValue(
        BigDecimal("4.123123123")
      )
    assert(error6.length == 1)
    assert(error6(0).`type` === "minInclusive")

    val error7 =
      column.validateValue(
        BigDecimal("45.123123123")
      )
    assert(error7.length == 0)
  }

  test(
    "should correctly set errors when minInclusive and maxInclusive are BigIntegers"
  ) {
    val json =
      """
        |{"name":"Measure",
        |"datatype": {
        |  "base": "decimal",
        |  "minInclusive": 9223372036854771000,
        |  "maxInclusive": 9223372036854775807
        |}
        |}
        |""".stripMargin
    val jsonNode = objectMapper.readTree(json)
    val Right(column) = Column.fromJson(
      1,
      jsonNode.asInstanceOf[ObjectNode]
    )
    val error1 =
      column.validateValue(3)
    assert(error1.length == 1)
    assert(error1(0).`type` === "minInclusive")
    val error2 =
      column.validateValue(BigInt("9223372036854771000").bigInteger)
    assert(error2.length == 0)

    val error3 =
      column.validateValue(
        BigInt("232423423423423423423423423423423423").bigInteger
      )
    assert(error3.length == 1)
    assert(error3(0).`type` === "maxInclusive")
    val error4 =
      column.validateValue(
        BigInt("922337203685477").bigInteger
      )
    assert(error4.length == 1)
    assert(error4(0).`type` === "minInclusive")

    val error5 =
      column.validateValue(
        BigDecimal("232423423423423423423423423423423423.123123")
      )
    assert(error5.length == 1)
    assert(error5(0).`type` === "maxInclusive")
    val error6 =
      column.validateValue(
        BigDecimal("4.123123123")
      )
    assert(error6.length == 1)
    assert(error6(0).`type` === "minInclusive")
  }

  // Test for minExclusive and maxExclusive properties along with tests for error messages
  test(
    "should correctly set errors when minExclusive and maxExclusive are BigIntegers"
  ) {
    val json =
      """
        |{"name":"Measure",
        |"datatype": {
        |  "base": "decimal",
        |  "minExclusive": 9223372036854771000,
        |  "maxExclusive": 9223372036854775807
        |}
        |}
        |""".stripMargin
    val jsonNode = objectMapper.readTree(json)
    val Right(column) = Column.fromJson(
      1,
      jsonNode.asInstanceOf[ObjectNode]
    )
    val error1 =
      column.validateValue(BigDecimal("9223372036854771000"))
    assert(error1.length == 1)
    assert(error1(0).`type` === "minExclusive")
    assert(
      error1(
        0
      ).content === "value '9223372036854771000' less than or equal to minExclusive value '9223372036854771000'"
    )

    val error2 =
      column.validateValue(BigInt("9223372036854775899").bigInteger)
    assert(error2.length == 1)
    assert(error2(0).`type` === "maxExclusive")
    assert(
      error2(
        0
      ).content === "value '9223372036854775899' greater than or equal to maxExclusive value '9223372036854775807'"
    )

  }
}
