package csvwcheck.bugs

import csvwcheck.TestPaths.resourcesPath
import csvwcheck.TestUtils.RunValidationInAkka
import csvwcheck.Validator
import org.scalatest.funsuite.AnyFunSuite

class ForeignKeys extends AnyFunSuite {

  test("A foreign key definition referencing a list column should return an error") {
    // Code reference 7da26b34-efa5-11ef-8180-57883ec4256f
    val csvwInput = resourcesPath./("csvwExamples")./("foreign-key-with-separator.csv-metadata.json")
    val validator = new Validator(Some(csvwInput.path.toString))
    val warningsAndErrors = RunValidationInAkka(validator)

    assert(warningsAndErrors.warnings.isEmpty)
    assert(warningsAndErrors.errors.length == 1)
    val error = warningsAndErrors.errors.head

    assert(error.`type` == "metadata")
    assert(error.content.contains("foreign key references list column"))
  }
}
