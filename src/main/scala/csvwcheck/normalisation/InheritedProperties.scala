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

import com.fasterxml.jackson.databind.node._
import csvwcheck.enums.PropertyType
import csvwcheck.normalisation.Constants.validTextDirectionValues
import csvwcheck.normalisation.Utils.{MetadataErrorsOrParsedArrayElements, Normaliser, invalidValueWarning, noWarnings}
import csvwcheck.traits.ObjectNodeExtentions.ObjectNodeGetMaybeNode
import shapeless.syntax.std.tuple.productTupleOps

import scala.jdk.CollectionConverters.IteratorHasAsScala

object InheritedProperties {
  val normalisers: Map[String, Normaliser] = Map(
    // https://www.w3.org/TR/2015/REC-tabular-metadata-20151217/#h-inherited-properties
    "aboutUrl" -> normaliseUriTemplateProperty(PropertyType.Inherited),
    "datatype" -> DataType.normaliseDataType(PropertyType.Inherited),
    "default" -> Utils.normaliseStringProperty(PropertyType.Inherited),
    "lang" -> Utils.normaliseLanguageProperty(PropertyType.Inherited),
    "null" -> normaliseNullProperty(PropertyType.Inherited),
    "ordered" -> Utils.normaliseBooleanProperty(PropertyType.Inherited),
    "propertyUrl" -> normaliseUriTemplateProperty(PropertyType.Inherited),
    "required" -> Utils.normaliseBooleanProperty(PropertyType.Inherited),
    "separator" -> normaliseSeparatorProperty(PropertyType.Inherited),
    "textDirection" -> normaliseTextDirectionProperty(PropertyType.Inherited),
    "valueUrl" -> normaliseUriTemplateProperty(PropertyType.Inherited),
  )

  /**
    * Copy inherited properties from a parent object to a child object.
    *
    * @param parentObject The parent object to inherit from
    * @param childObject  The child object to copy to
    * @return
    */
  def copyInheritedProperties(parentObject: ObjectNode, childObject: ObjectNode): ObjectNode = {
    normalisers.keys
      .foldLeft(childObject.deepCopy())({
        case (childObject, propertyName) =>
          // Copy over the inherited property from parent to child, if it exists on the parent.
          parentObject
            .getMaybeNode(propertyName)
            .foreach(valueToCopy => {
              childObject.set(propertyName, valueToCopy)
              ()
            })

          childObject
      })
  }

  private def normaliseUriTemplateProperty(
                                            csvwPropertyType: PropertyType.Value
                                          ): Normaliser = { context => {
    context.node match {
      case s: TextNode => Right((s, noWarnings, csvwPropertyType))
      case _ =>
        Right(
          (
            new TextNode(""),
            Array(context.makeWarning(invalidValueWarning)),
            csvwPropertyType
          )
        )
    }
  }
  }

  private def normaliseTextDirectionProperty(
                                              csvwPropertyType: PropertyType.Value
                                            ): Normaliser = { context => {
    context.node match {
      case s: TextNode
        if validTextDirectionValues.contains(s.asText()) =>
        Right((s, noWarnings, csvwPropertyType))
      case _ =>
        Right(
          (
            new TextNode("inherit"),
            Array(context.makeWarning(invalidValueWarning)),
            csvwPropertyType
          )
        )
    }
  }

  }

  private def normaliseSeparatorProperty(
                                          csvwPropertyType: PropertyType.Value
                                        ): Normaliser = { context => {
    context.node match {
      case s if s.isTextual || s.isNull =>
        Right((s, Array.empty, csvwPropertyType))
      case _ =>
        Right(
          (
            NullNode.getInstance(),
            Array(context.makeWarning(invalidValueWarning)),
            csvwPropertyType
          )
        )
    }
  }
  }

  private def normaliseNullProperty(
                                     csvwPropertyType: PropertyType.Value
                                   ): Normaliser = { context => {
    context.node match {
      case textNode: TextNode =>
        val standardArrayNode = JsonNodeFactory.instance.arrayNode()
        standardArrayNode.add(textNode)
        Right((standardArrayNode, noWarnings, csvwPropertyType))
      case arrayNode: ArrayNode =>
        arrayNode
          .elements()
          .asScala
          .zipWithIndex
          .map({
            case (element: TextNode, _) => Right((Some(element), noWarnings))
            case (element, index) =>
              val elementContext = context.toChild(element, index.toString)
              Right((None, Array(elementContext.makeWarning(invalidValueWarning))))
          })
          .toArrayNodeAndWarnings
          .map(_ :+ csvwPropertyType)
      case _ =>
        Right(
          (
            JsonNodeFactory.instance.arrayNode().add(""),
            if (context.node.isNull) noWarnings
            else Array(context.makeWarning(invalidValueWarning)),
            csvwPropertyType
          )
        )
    }
  }
  }
}
