package com.github.jamedge.moonlight.core.model.neo4j

import com.github.jamedge.moonlight.core.model.neo4j.GraphElements.{ElementClass, GraphElement}
import neotypes.DeferredQueryBuilder
import neotypes.implicits.all._

object LineQueries {
  def constructCreateOrUpdateQuery(
      node1: GraphElement,
      relationship: Option[GraphElement] = None,
      node2: Option[GraphElement] = None,
      createDuplicateNode2IfPathNotFound: Boolean = false,
      matchArgument: Option[DeferredQueryBuilder] = None,
      relationshipSetArgument: Option[DeferredQueryBuilder] = None
  ): DeferredQueryBuilder = {
    relationship.flatMap { r =>
      node2.map { n2 =>
        val result = if (createDuplicateNode2IfPathNotFound) {
          c"MATCH" + matchArgument.getOrElse(node1.toSearchObject()) +
            c"MERGE" + node1.toVariableEnclosed() + r.toObject() + n2.toSearchObject() +
            c"ON MATCH SET" + n2.toVariable() + c"+=" + n2.fields() +
            c"ON CREATE SET" + n2.toVariable() + c"=" + n2.fields()
        } else {
          c"MATCH" + matchArgument.getOrElse(node1.toSearchObject()) +
            c"MERGE" + n2.toSearchObject() +
            c"ON MATCH SET" + n2.toVariable() + c"+=" + n2.fields() +
            c"ON CREATE SET" + n2.toVariable() + c"=" + n2.fields() +
            c"MERGE" + node1.toVariableEnclosed() + r.toObject() + n2.toVariableEnclosed()
        }
        relationshipSetArgument.map { rsa =>
          result + rsa
        }.getOrElse(result)
      }
    }.getOrElse(
      c"MERGE" + node1.toSearchObject() +
        c"ON MATCH SET" + node1.toVariable() + c"+=" + node1.fields() +
        c"ON CREATE SET" + node1.toVariable() + c"=" + node1.fields()
    )
  }

  def constructDeleteQuery(
      matchNode: GraphElement,
      relationshipToDelete: GraphElement,
      nodeToDelete: GraphElement): DeferredQueryBuilder = {
    c"MATCH" + matchNode.toObject() + relationshipToDelete.toSearchObject() + nodeToDelete.toSearchObject() +
      c"DELETE" + relationshipToDelete.toVariable() + "," + nodeToDelete.toVariable()
  }

  def constructDeleteDetachedNodesQuery(elementClass: ElementClass): DeferredQueryBuilder = {
    c"MATCH" + s"(i:${elementClass.name})" + c"WHERE NOT (i) <-- () DELETE i"
  }

  def constructDeleteCleanedRelationshipsQuery(): DeferredQueryBuilder = {
    c"MATCH (i) <-[s {fromLines: []}]- () DELETE s"
  }

  def constructRelationshipTaggingSnippet(
      tagContainerFieldName: String,
      tagValue: String,
      relationship: GraphElement): DeferredQueryBuilder = {
    c"ON MATCH SET (CASE WHEN NOT ${tagValue} IN" + relationship.toVariableWithNewField(tagContainerFieldName) +
      c"THEN" + relationship.toVariable() + c"END).fromLines =" + relationship.toVariableWithNewField(tagContainerFieldName) + c" + [$tagValue]" +
      c"ON CREATE SET" + relationship.toVariableWithNewField(tagContainerFieldName) + c"= [$tagValue]"
  }

  def constructRelationshipDeleteMarkingQuery(
      tagContainerFieldName: String,
      tagValue: String,
      startElement: GraphElement): DeferredQueryBuilder = {
    c"MATCH p =" + startElement.toSearchObject() + "-[*0..]-> (x)" +
      s"FOREACH ( x IN relationships(p) | SET x.$tagContainerFieldName = FILTER ( y IN x.$tagContainerFieldName" +
      c"WHERE y <> $tagValue OR x.relationshipType = ${"permanent"}))"
  }

  def constructGetLineDataQuery(lineName: String): DeferredQueryBuilder = {
    c"""MATCH (l:Line {name: $lineName})
        OPTIONAL MATCH (l) -[lhd:HAS_DETAILS]-> (d) WHERE $lineName IN lhd.fromLines
        OPTIONAL MATCH (l) -[lhi:HAS_INPUT]-> (i:IO) -[iho:HAS_OUTPUT]-> (o:IO) WHERE $lineName IN lhi.fromLines AND $lineName IN iho.fromLines
        OPTIONAL MATCH (i) -[ihd:HAS_DETAILS]-> (id) WHERE $lineName IN ihd.fromLines
        OPTIONAL MATCH (i) -[ihs:HAS_STORAGE]-> (ist) WHERE $lineName IN ihs.fromLines
        OPTIONAL MATCH (ist) -[isthd:HAS_DETAILS]-> (istd) WHERE $lineName IN isthd.fromLines
        OPTIONAL MATCH (o) -[ohd:HAS_DETAILS]-> (od) WHERE $lineName IN ohd.fromLines
        OPTIONAL MATCH (o) -[ohs:HAS_STORAGE]-> (ost) WHERE $lineName IN ohs.fromLines
        OPTIONAL MATCH (ost) -[osthd:HAS_DETAILS]-> (ostd) WHERE $lineName IN osthd.fromLines
        OPTIONAL MATCH (l) -[lipb:IS_PROCESSED_BY]-> (pb) WHERE $lineName IN lipb.fromLines
        OPTIONAL MATCH (pb) -[pbhd:HAS_DETAILS]-> (pbd) WHERE $lineName IN pbhd.fromLines
        OPTIONAL MATCH (pb) -[pbhpf:HAS_PROCESSING_FRAMEWORK]-> (pf) WHERE $lineName IN pbhpf.fromLines
        OPTIONAL MATCH (pf) -[pfhd:HAS_DETAILS]-> (pfd) WHERE $lineName IN pfhd.fromLines
        OPTIONAL MATCH (pb) -[pbhph:HAS_PROCESSING_HISTORY]-> (ph) WHERE $lineName IN pbhph.fromLines
        OPTIONAL MATCH (ph) -[phhphr:HAS_PROCESSING_HISTORY_RECORD]-> (phr) WHERE $lineName IN phhphr.fromLines
        OPTIONAL MATCH (l) -[lhm:HAS_METRIC]-> (m) WHERE $lineName IN lhm.fromLines
        OPTIONAL MATCH (m) -[mhd:HAS_DETAILS]-> (md) WHERE $lineName IN mhd.fromLines
        OPTIONAL MATCH (m) -[mhmf:HAS_METRICS_FRAMEWORK]-> (mf) WHERE $lineName IN mhmf.fromLines
        OPTIONAL MATCH (mf) -[mfhd:HAS_DETAILS] -> (mfd) WHERE $lineName IN mfhd.fromLines
        OPTIONAL MATCH (l) -[lha:HAS_ALERT]-> (a) WHERE $lineName IN lha.fromLines
        OPTIONAL MATCH (a) -[ahd:HAS_DETAILS]-> (ad) WHERE $lineName IN ahd.fromLines
        OPTIONAL MATCH (a) -[ahaf:HAS_ALERTS_FRAMEWORK]-> (af) WHERE $lineName IN ahaf.fromLines
        OPTIONAL MATCH (af) -[afhd:HAS_DETAILS]-> (afd) WHERE $lineName IN afhd.fromLines
        OPTIONAL MATCH (l) -[lhc:HAS_CODE]-> (c) WHERE $lineName IN lhc.fromLines
        OPTIONAL MATCH (c) -[chd:HAS_DETAILS]-> (cd) WHERE $lineName IN chd.fromLines
        RETURN l AS line,
          d {.*} AS lineDetails,
          i AS input,
          id {.*} AS inputDetails,
          ist AS inputStorage,
          istd {.*} AS inputStorageDetails,
          o AS output,
          od {.*} AS outputDetails,
          ost AS outputStorage,
          ostd {.*} AS outputStorageDetails,
          pb AS processedBy,
          pbd {.*} AS processedByDetails,
          pf AS processingFramework,
          pfd {.*} AS processingFrameworkDetails,
          phhphr AS processingHistoryRecord,
          m AS metric,
          md {.*} AS metricDetails,
          mf AS metricFramework,
          mfd {.*} AS metricFrameworkDetails,
          a AS alert,
          ad {.*} AS alertDetails,
          af AS alertFramework,
          afd {.*} AS alertFrameworkDetails,
          c AS code,
          cd {.*} AS codeDetails"""
    //TODO: fix a bug which happens when user_data storage tries to be deleted
  }
}
