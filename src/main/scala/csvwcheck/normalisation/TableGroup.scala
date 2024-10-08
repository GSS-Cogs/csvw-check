/*
 * Copyright 2020 Crown Copyright (Office for National Statistics)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package csvwcheck.normalisation

import com.fasterxml.jackson.databind.node.{ArrayNode, JsonNodeFactory, ObjectNode}
import csvwcheck.enums.PropertyType
import csvwcheck.errors.WarningWithCsvContext
import csvwcheck.models.ParseResult.ParseResult
import csvwcheck.normalisation.Constants.undefinedLanguage
import csvwcheck.normalisation.Context.getBaseUrlAndLanguageFromContext
import csvwcheck.normalisation.Utils.{MetadataErrorsOrParsedArrayElements, NormalisationContext, Normaliser}
import csvwcheck.traits.ObjectNodeExtentions.ObjectNodeGetMaybeNode
import shapeless.syntax.std.tuple.productTupleOps
import sttp.client3.{Identity, SttpBackend}

import scala.jdk.CollectionConverters.IteratorHasAsScala

object TableGroup {

  private val normalisers: Map[String, Normaliser] = Map(
    // https://www.w3.org/TR/2015/REC-tabular-metadata-20151217/#h-table-groups
    "@type" -> Utils.normaliseRequiredType(PropertyType.Common, "TableGroup"),
    "@context" -> Context.normaliseContext(PropertyType.Common),
    "tables" -> normaliseTables(PropertyType.TableGroup),

    "dialect" -> Dialect.normaliseDialectProperty(PropertyType.TableGroup),
    "notes" -> Table.normaliseNotesProperty(PropertyType.TableGroup),
    "transformations" -> Transformation.normaliseTransformationsProperty(PropertyType.TableGroup),
    "tableDirection" -> Table.normaliseTableDirection(PropertyType.TableGroup)
  ) ++ InheritedProperties.normalisers ++ IdProperty.normaliser

  /**
    * https://www.w3.org/TR/2015/REC-tabular-metadata-20151217/#dfn-normalization
    *
    * @param tableGroupNode The table group node which will be normalised
    * @param baseUrl        The URL of the initially loaded JSON document
    * @param lang           The default language for the loaded JSON document
    * @return
    */
  def normaliseTableGroup(tableGroupNode: ObjectNode, baseUrl: String, httpClient: SttpBackend[Identity, Any], lang: String = undefinedLanguage): ParseResult[(ObjectNode, Array[WarningWithCsvContext])] =
    tableGroupNode match {
      case tableGroupNode: ObjectNode =>
        val normalisedTableGroupStructure = normaliseSingleTableToTableGroupStructure(tableGroupNode)

        val rootNodeContext = NormalisationContext(
          node = normalisedTableGroupStructure,
          baseUrl = baseUrl,
          language = lang,
          propertyPath = Array[String](),
          httpClient = httpClient
        )

        getBaseUrlAndLanguageFromContext(rootNodeContext)
          .flatMap({ case (baseUrl, language) =>
            Utils.normaliseObjectNode(normalisers, rootNodeContext.copy(baseUrl = baseUrl, language = language))
          })
          .map({
            case (normalisedTableGroupNode, warnings) =>
              (
                normalisedTableGroupNode,
                warnings.map(w => WarningWithCsvContext(
                  "metadata_warning",
                  "",
                  "",
                  "",
                  s"${w.path.mkString(".")}: ${w.message}",
                  ""
                )
                )
              )
          })
      case tableGroupNode =>
        val rootNodeContext = NormalisationContext(
          node = tableGroupNode,
          baseUrl = baseUrl,
          language = lang,
          propertyPath = Array[String](),
          httpClient = httpClient
        )
        Left(rootNodeContext.makeError(s"Unexpected table group value ${tableGroupNode.toPrettyString}"))
    }

  private def normaliseSingleTableToTableGroupStructure(tableGroupNode: ObjectNode): ObjectNode = {
    tableGroupNode.getMaybeNode("tables")
      .map(_ => tableGroupNode)
      .orElse(
        tableGroupNode.getMaybeNode("url").map(_ => {
          val newTableGroupNode = JsonNodeFactory.instance.objectNode()
          val tables = JsonNodeFactory.instance.arrayNode()
          val newTableNode = tableGroupNode.deepCopy()

          tables.add(newTableNode)
          newTableGroupNode.set("tables", tables)

          tableGroupNode
            .getMaybeNode("@context")
            .foreach(contextNode => {
              newTableGroupNode.set("@context", contextNode)
              newTableNode.remove("@context")
            })
          newTableGroupNode
        })
      )
      .getOrElse(tableGroupNode)
  }


  private def normaliseTables(propertyType: PropertyType.Value): Normaliser = context => context.node match {
    case tablesArrayNode: ArrayNode if tablesArrayNode.isEmpty() =>
      Left(context.makeError("Empty tables property"))
    case tablesArrayNode: ArrayNode =>
      tablesArrayNode
        .elements()
        .asScala
        .zipWithIndex
        .map({ case (tableNode, index) =>
          val tableContext = context.toChild(tableNode, index.toString)
          Table.normaliseTable(PropertyType.TableGroup)(tableContext)
            .map({
              case (tableNode, warnings, _) => (Some(tableNode), warnings)
            })
        })
        .toArrayNodeAndWarnings
        .map(_ :+ propertyType)
    case tablesNode => Left(context.makeError(s"Unexpected tables value: ${tablesNode.toPrettyString}"))
  }
}
