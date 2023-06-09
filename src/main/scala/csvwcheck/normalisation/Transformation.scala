package csvwcheck.normalisation

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.node.{ArrayNode, JsonNodeFactory, ObjectNode}
import csvwcheck.enums.PropertyType
import csvwcheck.errors.MetadataWarning
import csvwcheck.models.ParseResult.ParseResult
import csvwcheck.normalisation.Utils.{MetadataErrorsOrParsedArrayElements, MetadataErrorsOrParsedObjectProperties, MetadataWarnings, NormContext, Normaliser, PropertyPath, invalidValueWarning, noWarnings, normaliseJsonProperty}
import csvwcheck.traits.ObjectNodeExtentions.IteratorHasGetKeysAndValues
import shapeless.syntax.std.tuple.productTupleOps

import scala.jdk.CollectionConverters.IteratorHasAsScala

object Transformation {
  val normalisers: Map[String, Normaliser] = Map(
    "@type" -> Utils.normaliseRequiredType(PropertyType.Common, "Template"),
    "scriptFormat" -> Utils.normaliseDoNothing(PropertyType.Transformation),
    "source" -> Utils.normaliseDoNothing(PropertyType.Transformation),
    "targetFormat" -> Utils.normaliseDoNothing(PropertyType.Transformation),
  ) ++ IdProperty.normaliser

  def normaliseTransformationsProperty(
                                    csvwPropertyType: PropertyType.Value
                                  ): Normaliser = { context => {
    context.node match {
      case arrayNode: ArrayNode =>
        arrayNode
          .elements()
          .asScala
          .zipWithIndex
          .map({
                case (transformationNode: ObjectNode, index) =>
                  normaliseTransformationElement(context.toChild(transformationNode, index.toString))
                case (transformationNode, index) =>
                  val elementContext = context.toChild(transformationNode, index.toString)
                  Right(
                    (None, Array(elementContext.makeWarning(s"invalid_transformation: ${transformationNode.toPrettyString}")))
                  )
          })
          .toArrayNodeAndWarnings
          .map(_ :+ csvwPropertyType)
      case _ =>
        Right(
          (
            JsonNodeFactory.instance.arrayNode(0),
            Array(context.makeWarning(invalidValueWarning)),
            csvwPropertyType
          )
        )
    }
  }
  }

  def normaliseTransformationElement(context: NormContext[ObjectNode]): ParseResult[(Option[JsonNode], MetadataWarnings)] = {
    context.node
      .getKeysAndValues
      .map({case (propertyName, valueNode) =>
        val propertyContext = context.toChild(valueNode, propertyName)
        propertyName match {
          // todo: Hmm, really not sure about this random exclusion here.
          case "url" | "titles" =>
            Right((propertyName, Some(valueNode), noWarnings))
          case _ =>
            normaliseJsonProperty(normalisers, propertyName, propertyContext)
              .map({
                case (
                  parsedTransformation,
                  Array(),
                  PropertyType.Transformation
                  ) =>
                  (propertyName, Some(parsedTransformation), noWarnings)
                case (_, warnings, PropertyType.Transformation) =>
                  (propertyName, None, warnings)
                case (_, warnings, propertyType) =>
                  (
                    propertyName,
                    None,
                    warnings :+ propertyContext.makeWarning(s"invalid_property '$propertyName' with type $propertyType")
                  )
              })
        }
      })
      .iterator
      .toObjectNodeAndWarnings
      .map({
        case (objectNode, warnings) => (Some(objectNode), warnings)
      })
  }
}
