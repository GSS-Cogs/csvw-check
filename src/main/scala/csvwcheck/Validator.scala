package csvwcheck

import akka.NotUsed
import akka.stream.scaladsl.Source
import com.fasterxml.jackson.databind.node.ObjectNode
import com.fasterxml.jackson.databind.{JsonMappingException, JsonNode}
import com.typesafe.scalalogging.Logger
import csvwcheck.ConfiguredObjectMapper.objectMapper
import csvwcheck.errors._
import csvwcheck.models._
import csvwcheck.traits.LoggerExtensions.LogDebugException
import sttp.client3.{HttpClientSyncBackend, Identity, SttpBackend, basicRequest}
import sttp.model.Uri

import java.io.{File, IOException}
import java.net.URI
import scala.language.postfixOps

class Validator(
    val schemaUri: Option[String],
    csvUri: Option[String] = None,
    httpClient: SttpBackend[Identity, Any] = HttpClientSyncBackend()
) {
  val parallelism: Int = sys.env.get("PARALLELISM") match {
    case Some(value) => value.toInt
    case None        => Runtime.getRuntime.availableProcessors()
  }
  val rowGrouping: Int = sys.env.get("ROW_GROUPING") match {
    case Some(value) => value.toInt
    case None        => 1000
  }
  private val logger = Logger(this.getClass.getName)

  def validate(): Source[WarningsAndErrors, NotUsed] = {
    val absoluteSchemaUri = schemaUri.map(getAbsoluteSchemaUri)

    val maybeCsvUri = csvUri.map(new URI(_))

    val schemaUrisToCheck = maybeCsvUri
      .map(csvUri =>
        Array(
          absoluteSchemaUri,
          Some(new URI(s"${getUriWithoutQueryString(csvUri)}-metadata.json")),
          Some(csvUri.resolve("csv-metadata.json"))
        )
      )
      .getOrElse(Array(absoluteSchemaUri))
      .flatten
      .distinct

    findAndValidateCsvwSchemaFileForCsv(maybeCsvUri, schemaUrisToCheck.toSeq)
  }

  private def getAbsoluteSchemaUri(schemaPath: String): URI = {
    val inputSchemaUri = new URI(schemaPath)
    if (inputSchemaUri.getScheme == null) {
      new URI(s"file://${new File(schemaPath).getAbsolutePath}")
    } else {
      inputSchemaUri
    }
  }

  private def attemptToFindMatchingTableGroup(
      maybeCsvUri: Option[URI],
      possibleSchemaUri: URI
  ): Either[CsvwLoadError, WithWarningsAndErrors[TableGroup]] = {
    try {
      fileUriToJson[ObjectNode](possibleSchemaUri)
        .flatMap(objectNode =>
          Schema
            .fromCsvwMetadata(possibleSchemaUri.toString, objectNode) match {
            case Right(tableGroup) => Right(tableGroup)
            case Left(metadataError) =>
              Left(GeneralCsvwLoadError(metadataError))
          }
        )
        .flatMap(parsedTableGroup =>
          maybeCsvUri
            .map(csvUri => {
              val workingWithUserSpecifiedMetadata =
                schemaUri.isDefined && possibleSchemaUri.toString == schemaUri.get
              if (
                tableGroupContainsCsv(
                  parsedTableGroup.component,
                  csvUri
                ) || parsedTableGroup.warningsAndErrors.errors.nonEmpty || workingWithUserSpecifiedMetadata
              ) {
                Right(parsedTableGroup)
              } else {
                Left(
                  SchemaDoesNotContainCsvError(
                    new IllegalArgumentException(
                      s"Schema file does not contain a definition for $maybeCsvUri"
                    )
                  )
                )
              }
            })
            .getOrElse(Right(parsedTableGroup))
        )
    } catch {
      case e: Throwable =>
        logger.debug(e)
        Left(GeneralCsvwLoadError(e))
    }
  }

  private def fileUriToJson[TJsonNode <: JsonNode](
      fileUri: URI
  ): Either[CsvwLoadError, TJsonNode] = {
    if (fileUri.getScheme == "file") {
      try {
        Right(objectMapper.readTree(new File(fileUri)).asInstanceOf[TJsonNode])
      } catch {
        case e: IOException => Left(CascadeToOtherFilesError(e))
      }
    } else {
      val response = httpClient.send(basicRequest.get(Uri(fileUri)))
      response.body match {
        case Left(error) => Left(CascadeToOtherFilesError(new Exception(error)))
        case Right(body) =>
          try {
            Right(objectMapper.readTree(body).asInstanceOf[TJsonNode])
          } catch {
            case e: JsonMappingException => Left(GeneralCsvwLoadError(e))
          }
      }
    }
  }

  private def tableGroupContainsCsv(
      tableGroup: TableGroup,
      csvUri: URI
  ): Boolean = {
    val csvUrl = csvUri.toString
    val tables = tableGroup.tables

    val csvUrlWithoutQueryString = getUriWithoutQueryString(csvUri).toString

    // todo: We need to be able to try both relative & absolute CSVURIs here.

    tables.contains(csvUrl) || tables.contains(csvUrlWithoutQueryString)
  }

  private def getUriWithoutQueryString(csvUri: URI): URI = {
    if (csvUri.getRawQuery == null)
      csvUri
    else {
      val queryStringLength = csvUri.getRawQuery.length
      val url = csvUri.toString
      new URI(url.substring(0, url.length - (queryStringLength + 1)))
    }
  }

  private def findAndValidateCsvwSchemaFileForCsv(
      maybeCsvUri: Option[URI],
      schemaUrisToCheck: Seq[URI]
  ): Source[WarningsAndErrors, NotUsed] = {
    schemaUrisToCheck match {
      case Seq() =>
        if (schemaUri.isDefined) {
          val error = ErrorWithCsvContext(
            "metadata",
            "cannot locate schema",
            "",
            "",
            s"${schemaUri.get} not found",
            ""
          )
          Source(List(
            models.WarningsAndErrors(errors = Array[ErrorWithCsvContext](error))
          ))
        } else Source(List(WarningsAndErrors()))
      case Seq(uri, uris @ _*) =>
        attemptToFindMatchingTableGroup(
          maybeCsvUri,
          uri
        ) match {
          case Right(parsedTableGroup) =>
            val tableGroup = parsedTableGroup.component
            tableGroup.validateCsvsAgainstTables(parallelism, rowGrouping).map { wAndE2 =>
              WarningsAndErrors(
                wAndE2.warnings ++ parsedTableGroup.warningsAndErrors.warnings,
                wAndE2.errors ++ parsedTableGroup.warningsAndErrors.errors
              )
            }
          case Left(GeneralCsvwLoadError(err)) =>
            val error = ErrorWithCsvContext(
              "metadata",
              err.getClass.getName,
              "",
              "",
              err.getMessage,
              ""
            )
            logger.debug(err)
            Source(List(WarningsAndErrors(errors = Array(error))))
          case Left(SchemaDoesNotContainCsvError(err)) =>
            logger.debug(err)
            findAndValidateCsvwSchemaFileForCsv(maybeCsvUri, uris)
              .map(warningsAndErrors => warningsAndErrors.copy(warnings = warningsAndErrors.warnings :+ WarningWithCsvContext(
                    "source_url_mismatch",
                    s"CSV supplied not found in metadata $uri",
                    "",
                    "",
                    "",
                    ""
                  )
                )
              )
          case Left(CascadeToOtherFilesError(err)) =>
            logger.debug(err)
            findAndValidateCsvwSchemaFileForCsv(maybeCsvUri, uris)
          case Left(err) =>
            throw new IllegalArgumentException(s"Unhandled CsvwLoadError $err")

        }
    }
  }

  implicit val ec: scala.concurrent.ExecutionContext =
    scala.concurrent.ExecutionContext.global
}
