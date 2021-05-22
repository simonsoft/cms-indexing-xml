<?xml version="1.0" encoding="UTF-8"?>
<!--

    Copyright (C) 2009-2017 Simonsoft Nordic AB

    Licensed under the Apache License, Version 2.0 (the "License");
    you may not use this file except in compliance with the License.
    You may obtain a copy of the License at

            http://www.apache.org/licenses/LICENSE-2.0

    Unless required by applicable law or agreed to in writing, software
    distributed under the License is distributed on an "AS IS" BASIS,
    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
    See the License for the specific language governing permissions and
    limitations under the License.

-->
<xsl:stylesheet version="2.0"
	xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
	xmlns:xs="http://www.w3.org/2001/XMLSchema"
	xmlns:cms="http://www.simonsoft.se/namespace/cms"
	xmlns:cmsfn="http://www.simonsoft.se/namespace/cms-functions"
	>

	
	<!-- document's status -->
	<xsl:param name="document-status"/>
	
	<!-- document's patharea (release, translation) -->
	<xsl:param name="patharea"/>
	
	<!-- filename extension -->
	<xsl:param name="pathext" required="yes"/>
	
	<!-- Parameter containing the Ditamap (typically the property on a Release / Translation) as a document tree. -->
	<xsl:param name="ditamap" as="document-node()?" required="no"/>
			
	
	<!-- Definition of newline for convenience. -->
	<xsl:param name="newline" select="'&#xA;'"/>
	
	<!-- Names of attributes that can be references. -->
	<xsl:param name="ref-attrs" as="xs:string" select="'href fileref source'"/>
	<xsl:variable name="ref-attrs-seq" as="xs:string+" select="tokenize($ref-attrs, ' ')"/>
	<xsl:variable name="ref-attrs-conref-seq" as="xs:string+" select="$ref-attrs-seq, 'conref'">
		
	</xsl:variable>
	
	<!-- key definition for cms:rid lookup -->
	<xsl:key name="rid" use="@cms:rid" match="*[ not(ancestor-or-self::*[@cms:tsuppress])  or ancestor-or-self::*[@cms:tsuppress = 'no'] ]"/>

	<!-- Key to enable idref resolution without DTD. -->
	<xsl:key name="idref" match="//element()[@xml:id]" use="@xml:id"/>

	<xsl:variable name="is-dita-map" select="$pathext = 'ditamap'"/>
	<xsl:variable name="is-dita-topic" select="$pathext = 'dita'"/>

	<!-- Will only match the initial context element since all further processing is done with specific modes. -->
	<xsl:template match="*">
		<xsl:variable name="root" select="."/>
		<xsl:variable name="titles" as="element()*">
			<xsl:choose>
				<xsl:when test="$ditamap//booktitle/mainbooktitle">
					<!-- DITA bookmap in Release/Translation Area. -->
					<xsl:sequence select="$ditamap//booktitle/mainbooktitle"/>
				</xsl:when>
				<xsl:when test="$ditamap//title">
					<!-- Ditamap in Release/Translation Area. -->
					<xsl:sequence select="$ditamap//title"/>
				</xsl:when>
				<xsl:when test="/*/booktitle/mainbooktitle">
					<!-- DITA bookmap in Author Area. -->
					<xsl:sequence select="/*/booktitle/mainbooktitle"/>
				</xsl:when>
				<xsl:when test="//searchtitle">
					<!-- Used in DITA topics. -->
					<xsl:sequence select="//searchtitle"/>
				</xsl:when>
				<xsl:otherwise>
					<xsl:sequence select="//title"/>
				</xsl:otherwise>
			</xsl:choose> 
		</xsl:variable>
		<xsl:variable name="paras" select="//p"/>
		
		<xsl:variable name="cms-namespace-source" select="namespace-uri-for-prefix('cms', .)"/>
        <xsl:if test="$cms-namespace-source!='http://www.simonsoft.se/namespace/cms'">
            <xsl:message terminate="yes">The namespace with prefix 'cms' must be 'http://www.simonsoft.se/namespace/cms'.</xsl:message>
        </xsl:if>
	
		<!-- <xsl:variable name="whitespace" select="'&#x20;&#xD;&#xA;&#x9;'"/>-->
		<xsl:variable name="whitespace" select="' '"/>
	
		<!-- Tokenize the text nodes before concat:ing them to avoid issue with missing space (btw e.g. a title and a p) -->
		<!-- Inspired by: http://stackoverflow.com/questions/12784190/xslt-tokenize-nodeset -->
		<xsl:variable name="text" select="for $elemtext in descendant-or-self::text()[not(ancestor::*[@keyref])] return tokenize(normalize-space($elemtext), $whitespace)"/>
	
		<!-- Detect text in non-RID places. -->
		<!-- Elements containing both text and RID-children. -->
		<xsl:variable name="rid_mixed_unsafe" as="attribute()*"
			select="//element()[@cms:rid][element()[@cms:rid]][text()][count(for $elemtext in text() return tokenize(normalize-space($elemtext), $whitespace)) > 0]/@cms:rid"/>

		<doc>
			<!-- name and attributes -->
			<!-- using the embd_ field for now (with addition of xml_) -->
			<!-- TODO: Decision on field names, some test coverage in xml-tracking. -->
			<!-- NOTE: Indexing will fail if field name overlaps with Tika extraction. -->
			<field name="embd_xml_name"><xsl:value-of select="name($root)"/></field>
			<!-- attributes on the root element -->
			<xsl:for-each select="$root/@*">
				<xsl:variable name="fieldname" select="concat('embd_xml_a_', replace(name(.), ':', '.'))"/>
				<field name="{$fieldname}"><xsl:value-of select="."/></field>
			</xsl:for-each>
			
			<!-- Title, there is a specific field in repositem schema but there will be a separate handler making a selection. -->
			<!-- Attempt to resolve termref and display keyref key. -->
			<xsl:if test="$titles">
				<xsl:variable name="title">
					<xsl:apply-templates select="($titles)[1]" mode="title"/>
				</xsl:variable>
				<field name="embd_xml_title"><xsl:value-of select="normalize-space($title)"/></field>
			</xsl:if>
	
			<!-- Introduction to to text - first couple of paragraphs. -->
			<xsl:if test="count($text) > 0">
				<xsl:variable name="shortdesc">
					<xsl:apply-templates select="/*/shortdesc" mode="intro"/>
				</xsl:variable>
				<xsl:variable name="para1">
					<xsl:apply-templates select="($paras)[1]" mode="intro"/>
				</xsl:variable>
				<xsl:variable name="para2">
					<xsl:apply-templates select="($paras)[2]" mode="intro"/>
				</xsl:variable>
				<xsl:choose>
					<xsl:when test="string-length($shortdesc) > 10">
						<!-- Use the DITA shortdesc if any reasonable text in there. -->
						<field name="embd_xml_intro"><xsl:value-of select="normalize-space($shortdesc)"/></field>
					</xsl:when>
					<xsl:when test="string-length($para1) > 200">
						<field name="embd_xml_intro"><xsl:value-of select="normalize-space($para1)"/></field>
					</xsl:when>
					<xsl:when test="string-length($para1) + string-length($para2) > 200">
						<field name="embd_xml_intro"><xsl:value-of select="concat(normalize-space($para1), $newline, normalize-space($para2))"/></field>
					</xsl:when>
					<!-- Opportunity to support more paragraphs. -->
					
					<!-- Check if they actually contain any text. -->
					<xsl:when test="string-length($para1) > 0 and string-length($para2) > 0">
						<field name="embd_xml_intro"><xsl:value-of select="concat(normalize-space($para1), $newline, normalize-space($para2))"/></field>
					</xsl:when>
					<xsl:when test="string-length($para1) > 0 or string-length($para2) > 0">
						<!-- Concat without newline since at least one is empty. -->
						<field name="embd_xml_intro"><xsl:value-of select="concat(normalize-space($para1), normalize-space($para2))"/></field>
					</xsl:when>
					<xsl:otherwise>
						<!-- No text -> no field. -->
					</xsl:otherwise>
				</xsl:choose>
			</xsl:if>
			
			
	
			<xsl:if test="cmsfn:get-docno(.)">
				<field name="embd_xml_docno"><xsl:value-of select="cmsfn:get-docno(.)"/></field>
			</xsl:if>
			
			<!-- ID attributes should be searchable, will concat the tokens separated by a space. -->
			<field name="embd_xml_ids"><xsl:value-of select="//@*[name() = 'xml:id' or name() = 'id']"/></field>
			
			<!-- Linkend attribute should be searchable, will concat the tokens separated by a space. -->
			<field name="embd_xml_linkends"><xsl:value-of select="//@*[name() = 'linkend']"/></field>
			
			<!-- Reference attributes with fragment should be searchable, will concat the tokens separated by a space. -->
			<field name="embd_xml_fragments"><xsl:value-of select="for $ref in //@*[name() = 'href' or name() = 'conref'][contains(., '#')] return (substring-after($ref, '#'), tokenize(substring-after($ref, '#'), '/'))"/></field>
			
			<!-- Experimental: Extract product name metadata from both topic and techdocmap. -->
			<!-- Likely need multiValued field without tokenization to achieve good faceting. -->
			<!-- Let the DITA hierachy within prolog define most of the depth below embd_xml_meta_* -->
			<field name="embd_xml_meta_product">
				<xsl:apply-templates select="/*/techdocinfo/product | /*/prolog/metadata/prodinfo/prodname" mode="meta"/>
			</field>
			
			<!-- What about number of elements? -->	
			<field name="count_elements"><xsl:value-of select="count(//element())"/></field>
			
			<!-- Limit reposxml indexing depth for Translations. -->
			<xsl:if test="$patharea = 'translation'">
				<field name="count_reposxml_depth">
					<xsl:choose>
						<!-- TODO: Dynamically adjust depth to get reasonably sized elements in reposxml. -->
						<xsl:when test="false()">
							<!-- ditabase is perfect for depth=2. -->
						</xsl:when>
						<xsl:otherwise>
							<xsl:value-of select="1"/>
						</xsl:otherwise>
					</xsl:choose>
				</field>
			</xsl:if>
			
			<xsl:if test="@cms:twords">
				<!-- Only Pretranslated Translations -->
				<field name="count_twords"><xsl:value-of select="@cms:twords"/></field>
				
				<xsl:for-each select="distinct-values(//@cms:tstatus)">
					<xsl:variable name="status" select="."/> 
					<xsl:variable name="fieldname" select="concat('count_twords_', $status)"></xsl:variable>
					<field name="{$fieldname}"><xsl:value-of select="sum($root/descendant-or-self::*[@cms:tstatus=$status]/@cms:twords) - sum($root/descendant-or-self::*[@cms:tstatus=$status]/descendant::*[@cms:tstatus]/@cms:twords)"/></field>
				</xsl:for-each>
				
				<!-- Each source of pretranslate, key on trid-prefix for each tlogicalid. -->
				<xsl:for-each-group select="//*[@cms:trid]" group-by="cmsfn:get-rid-prefix(@cms:trid)">
					<!-- Each source of pretranslate, key on trid-prefix for each tlogicalid. -->
					<xsl:variable name="trid-prefix" select="current-grouping-key()"/>
					<!-- The tlogicalid, should be a single value. Getting first one to avoid failure due to broken data. -->
					<xsl:variable name="tlogicalid" select="current-group()[1]/@cms:tlogicalid"/>
					
					
					<!-- The tlogicalid, non-processed by indexing. -->
					<field name="embd_xml_trid_tlogicalid_{$trid-prefix}"><xsl:value-of select="$tlogicalid"/></field>
					
					<!-- A tlogicalid can only have a single tstatus value. -->
					<field name="embd_xml_trid_tstatus_{$trid-prefix}"><xsl:value-of select="current-group()[1]/@cms:tstatus"/></field>
					
					<!-- All rids for this tlogicalid. -->
					<field name="embd_xml_trid_rids_{$trid-prefix}"><xsl:value-of select="current-group()/@cms:rid"/></field>
						
					<!-- Make trids searchable, "where re-used". -->
					<field name="embd_xml_trid_trids_{$trid-prefix}"><xsl:value-of select="current-group()/@cms:trid"/></field>
					
					<!-- Word count twords for each tlogicalid source. -->
					<field name="embd_xml_trid_twords_{$trid-prefix}"><xsl:value-of select="current-group()/@cms:twords"/></field>
					
					<!-- Sum word count twords for each tlogicalid source. -->
					<!-- 
					<field name="count_trid_twords_{$trid-prefix}"><xsl:value-of select="sum(current-group()/@cms:twords)"/></field>
					-->
					<!-- Sum word count twords for each tlogicalid source and subtract underlying Pretranstions, applies to tstatus!=Released (In_Progress). -->
					<field name="count_trid_twords_{$trid-prefix}"><xsl:value-of select="sum(//*[@cms:tlogicalid=$tlogicalid]/@cms:twords) - sum(//*[@cms:tlogicalid=$tlogicalid]/descendant::*[@cms:tstatus]/@cms:twords)"/></field>
				</xsl:for-each-group>
				
			</xsl:if>
			
			<xsl:if test="@cms:rid">
				<!-- All Finalized Release / Translations -->
				
				<!-- The fixed part of the rids. -->
				<field name="embd_xml_rid_prefix"><xsl:value-of select="cmsfn:get-rid-prefix(@cms:rid)"/></field>
				
				<!-- Elements representing paragraph-like elements, i.e. RID-leaves (see PretranslateValidationStopTags). -->
				<xsl:variable name="elements_para" as="element()*"
					select="$root/descendant-or-self::*[@cms:rid][not(element()[@cms:rid])]"/>
				<!-- Select: elements [with RID], [RID-leaf] -->
				
				
				<!-- #1283 Attempt to detect complete pretranslate -->
				<!-- Requires safe condition, not flags: ridduplicate, hastsuppress, hasridmixedunsafe, hasridmissing -->
				<!-- Not sure if this can be compatible with hastsuppress. Typical tsuppress meaning before Pretranslate: "require re-translation by TSP". Must be manual. -->
				
				<!-- Elements that require translation. -->
				<!-- Excludes elements that are Translate=no if set at or above RID, does not analyze inlines. -->
				<!-- Pretranslate will not traverse into tsuppress but we don't know why tsuppress is used. -->
				<!-- Presenting tsuppress count separately, not included here. -->
				<xsl:variable name="tstatus_open_elements_all" as="element()*"
					select="$root/descendant-or-self::*[@cms:rid][not(element()[@cms:rid])][not(ancestor-or-self::*[@cms:tstatus='Released'])][not(ancestor-or-self::*[@translate='no'])][not(ancestor-or-self::*[@markfortrans='no'])][not(ancestor-or-self::*[@cms:tsuppress[not(. = 'no')]])]"/>
					<!-- Select: elements [with RID], [RID-leaf], [not Pretranslated], [not excluded from translation (2 variants)]  -->
				
				<!-- Elements that require translation, filtering those that only contain keyrefs or inlines[translate=no]. -->
				<xsl:variable name="tstatus_open_elements" as="element()*"
					select="$root/descendant-or-self::*[@cms:rid][not(element()[@cms:rid])][not(ancestor-or-self::*[@cms:tstatus='Released'])][not(ancestor-or-self::*[@translate='no'])][not(ancestor-or-self::*[@markfortrans='no'])][not(ancestor-or-self::*[@cms:tsuppress[not(. = 'no')]])][count(for $elemtext in descendant-or-self::text()[not(ancestor::*[@keyref])][not(ancestor::*[@translate='no' or @markfortrans='no'])] return tokenize(normalize-space($elemtext), $whitespace)) > 0]"/>
				
				
				<!-- Calculate In-Progress elements and word count. tstatus_progress_elements[_all] -->
				<!-- Word count will be calculated here instead of sum(@cms:twords) -->
				<xsl:variable name="tstatus_progress_elements_all" as="element()*"
					select="$root/descendant-or-self::*[@cms:rid][not(element()[@cms:rid])][not(ancestor-or-self::*[@cms:tstatus='Released'])][ancestor-or-self::*[@cms:tstatus]][not(ancestor-or-self::*[@translate='no'])][not(ancestor-or-self::*[@markfortrans='no'])][not(ancestor-or-self::*[@cms:tsuppress[not(. = 'no')]])]"/>
				<!-- Select: elements [with RID], [RID-leaf], [not Pretranslated], [In Progress], [not excluded from translation (2 variants)]  -->
				
				<!-- In-Progress elements, filtering those that only contain keyrefs or inlines[translate=no]. -->
				<xsl:variable name="tstatus_progress_elements" as="element()*"
					select="$root/descendant-or-self::*[@cms:rid][not(element()[@cms:rid])][not(ancestor-or-self::*[@cms:tstatus='Released'])][ancestor-or-self::*[@cms:tstatus]][not(ancestor-or-self::*[@translate='no'])][not(ancestor-or-self::*[@markfortrans='no'])][not(ancestor-or-self::*[@cms:tsuppress[not(. = 'no')]])][count(for $elemtext in descendant-or-self::text()[not(ancestor::*[@keyref])][not(ancestor::*[@translate='no' or @markfortrans='no'])] return tokenize(normalize-space($elemtext), $whitespace)) > 0]"/>
				
				
				<!-- Count number of elements: open / in_progress -->
				<!-- TODO: Consider adding _translate_no (not impacted by inlines), _tsuppress  -->
				<field name="count_elements_para"><xsl:value-of select="count($elements_para)"/></field>
				<field name="count_elements_translate_all"><xsl:value-of select="count($tstatus_open_elements_all)"/></field>
				<field name="count_elements_translate"><xsl:value-of select="count($tstatus_open_elements)"/></field>
				<field name="count_elements_progress_all"><xsl:value-of select="count($tstatus_progress_elements_all)"/></field>
				<field name="count_elements_progress"><xsl:value-of select="count($tstatus_progress_elements)"/></field>
				
				
				<field name="embd_xml_ridtranslate_all">
					<xsl:value-of select="distinct-values($tstatus_open_elements_all/@cms:rid)"/>
				</field>
				
				<field name="embd_xml_ridtranslate">
					<xsl:value-of select="distinct-values($tstatus_open_elements/@cms:rid)"/>
				</field>
				
				<!-- Extract text (excluding keyref): open / in_progress -->
				<xsl:variable name="tstatus_open_text" select="for $elemtext in $tstatus_open_elements/descendant-or-self::text()[not(ancestor::*[@keyref])][not(ancestor::*[@translate='no' or @markfortrans='no'])] return tokenize(normalize-space($elemtext), $whitespace)"/>
				<xsl:variable name="tstatus_progress_text" select="for $elemtext in $tstatus_progress_elements/descendant-or-self::text()[not(ancestor::*[@keyref])][not(ancestor::*[@translate='no' or @markfortrans='no'])] return tokenize(normalize-space($elemtext), $whitespace)"/>
				
				<field name="count_words_translate"><xsl:value-of select="count($tstatus_open_text)"/></field>
				<field name="count_words_progress"><xsl:value-of select="count($tstatus_progress_text)"/></field>
			</xsl:if>
			
			<!-- Elements marked translate="no" or markfortrans="no" (excluding Pretranslated elements). -->
			<!-- 
			<xsl:variable name="translate_no_text" select="for $elemtext in $root/descendant-or-self::*[not(ancestor-or-self::*[@cms:tstatus='Released'])][not(ancestor-or-self::*[@cms:tsuppress[not(. = 'no')]])][not(@keyref)]/text()[ancestor-or-self::*[@translate='no' or @markfortrans='no']] return tokenize(normalize-space($elemtext), $whitespace)"/>
			-->
			<!-- Elements marked translate="no" or markfortrans="no" (including Pretranslated elements). -->
			<xsl:variable name="translate_no_text" select="for $elemtext in $root/descendant-or-self::*[not(ancestor-or-self::*[@cms:tsuppress[not(. = 'no')]])][not(@keyref)]/text()[ancestor::*[@translate='no' or @markfortrans='no']] return tokenize(normalize-space($elemtext), $whitespace)"/>
			
			<field name="count_words_translate_no"><xsl:value-of select="count($translate_no_text)"/></field>
			
			
			<!-- Suppressed Translations. -->
			<!-- Not attempting to get each leaf element since RIDs might not be consistent. -->
			<!-- Excluding keyref words (excluded from "count_words_text"). -->
			<!-- Including translate_no words because they should not be included in count_words_translate_no (tsuppress takes precedence, completely ignore those sections). -->
			<xsl:variable name="tsuppress_text" select="for $elemtext in $root/descendant-or-self::*[ancestor-or-self::*[@cms:tsuppress[not(. = 'no')]]][not(@keyref)]/text() return tokenize(normalize-space($elemtext), $whitespace)"/>
			
			<field name="count_words_tsuppress"><xsl:value-of select="count($tsuppress_text)"/></field>
			

			<!-- Just concat of the tokens/words. Somehow becomes space-separated. -->
			<!-- Using field 'text' seems unable to override Tika extraction. -->
			<!-- TODO: Consider the impact of storing the text field. -->
			<!--
			<field name="embd_xml_text"><xsl:value-of select="$text"/></field>
			-->
			
			<!-- Word count is simple when each word is a text node. -->
			<field name="count_words_text"><xsl:value-of select="count($text)"/></field>

			
			<!-- Lists all duplicated RID (duplicates included twice) -->
			<!-- Rids should not be duplicated in the document, but that can only be identified from the root node.-->
			<!-- This field can only identify duplicates among its children, not whether the element itself is a duplicate in the document context. -->
			<!-- Disabling reusevalue from the root node of the XML onto all children (done by an XmlIndexFieldExtraction class). -->
			<!-- TODO: Consider if we should only perform this extraction for root node (depth = 1), if performance is heavily degraded. -->
			<!-- The key does not contain RIDs within tsuppress area. -->
			<xsl:variable name="ridduplicate" as="attribute()*"
				select="descendant-or-self::*[count(key('rid', @cms:rid)) > 1]/@cms:rid"/>
			
			<!--  
			The admin report of RID duplicates is currently querying reposxml.
			Can now be changed to use repositem.
			-->
			<xsl:if test="count($ridduplicate) > 0">
				<field name="embd_xml_ridduplicate">
					<xsl:value-of select="distinct-values($ridduplicate)"/>
				</field>
			</xsl:if>
			
			<!-- Boolean flag for rid duplicate. -->
			<xsl:if test="count($ridduplicate) > 0">
				<field name="flag">
					<xsl:value-of select="'hasridduplicate'"/>
				</field>
			</xsl:if>
			
			<!-- Boolean flag for tsuppress. -->
			<xsl:if test="count(//@cms:tsuppress[not(. = 'no')]) > 0">
				<field name="flag">
					<xsl:value-of select="'hastsuppress'"/>
				</field>
			</xsl:if>
			
			<!-- Mixed content, unsafe wrt RID -->
			<xsl:if test="count($rid_mixed_unsafe) > 0">
				<field name="embd_xml_ridmixedunsafe">
					<xsl:value-of select="distinct-values($rid_mixed_unsafe)"/>
				</field>
			</xsl:if>
			
			<xsl:if test="count($rid_mixed_unsafe) > 0">
				<field name="flag">
					<xsl:value-of select="'hasridmixedunsafe'"/>
				</field>
			</xsl:if>
			
			<!-- Missing RIDs -->
			<xsl:variable name="ridmissing_empty" as="attribute()*"
				select="descendant-or-self::*[@cms:rid][element()/@cms:rid = '']/@cms:rid"/>
			
			<xsl:variable name="ridmissing_parent" as="attribute()*"
				select="descendant-or-self::*[@cms:rid][parent::element()][parent::element()[not(@cms:rid)]]/@cms:rid"/>
			
			<xsl:variable name="ridmissing_sibling" as="attribute()*"
				select="descendant-or-self::*[@cms:rid][element()[@cms:rid]][element()[not(@cms:rid)]]/@cms:rid"/>
			
			<xsl:variable name="ridmissing" as="attribute()*"
				select="$ridmissing_empty | $ridmissing_parent | $ridmissing_sibling"/>
			
			<xsl:if test="count($ridmissing) > 0">
				<field name="embd_xml_ridmissing">
					<xsl:value-of select="distinct-values($ridmissing)"/>
				</field>
			</xsl:if>
			
			<xsl:if test="count($ridmissing) > 0">
				<field name="flag">
					<xsl:value-of select="'hasridmissing'"/>
				</field>
			</xsl:if>
			
			
			<!-- Detect non-CMS references in XML files.  -->
			<field name="ref_xml_noncms"><xsl:apply-templates select="//@*[name() = $ref-attrs-seq][not(starts-with(., 'x-svn:'))][not(starts-with(., '#'))][not(starts-with(., 'http:'))][not(starts-with(., 'https:'))][not(starts-with(., 'mailto:'))]" mode="refnoncms"/></field>
			
			<!-- Extract all dependencies in document order, including duplicates. -->
			<!-- ref-attrs-conref-seq contains the union of ref-attrs-seq and 'conref'. -->
			<field name="ref_itemid_dependency">
				<xsl:apply-templates select="//@*[name() = 'keyrefhref'][starts-with(., 'x-svn:')]" mode="refdep"/>
				<xsl:apply-templates select="//@*[name() = $ref-attrs-conref-seq][starts-with(., 'x-svn:')]" mode="refdep"/>
			</field>
			<!-- Extract keydef maps. -->
			<field name="ref_itemid_keydefmap">
				<xsl:apply-templates select="//@*[name() = 'keyrefhref'][starts-with(., 'x-svn:')]" mode="refkeydefmap"/>
				<xsl:apply-templates select="//@*[name() = $ref-attrs-seq][starts-with(., 'x-svn:')]" mode="refkeydefmap"/>
			</field>
			<!-- Extract xml dependencies. -->
			<field name="ref_itemid_include"><xsl:apply-templates select="//@*[name() = $ref-attrs-seq][starts-with(., 'x-svn:')]" mode="refinclude"/></field>
			<!-- Extract graphics dependencies. -->
			<field name="ref_itemid_graphic"><xsl:apply-templates select="//@*[name() = $ref-attrs-seq][starts-with(., 'x-svn:')][not(cmsfn:is-format-dita(..))]" mode="refgraphic"/></field>
			<field name="ref_itemid_graphictranslated"><xsl:apply-templates select="//@*[name() = $ref-attrs-seq][starts-with(., 'x-svn:')][../@xml:lang][not(cmsfn:is-format-dita(..))]" mode="refgraphic"/></field>
			
			<!-- Extract DITA conref dependencies. -->
			<field name="ref_itemid_conref"><xsl:apply-templates select="//@conref[starts-with(., 'x-svn:')]" mode="refconref"/></field>
			
			<xsl:if test="$is-dita-map">
				<!-- Extract DITA topicref dependencies. -->
				<field name="ref_itemid_topicref"><xsl:apply-templates select="//@href[starts-with(., 'x-svn:')][cmsfn:is-format-dita(..)]" mode="reftopicref"/></field>
			</xsl:if>
			
			<xsl:if test="$is-dita-topic">
				<!-- Extract DITA xref dependencies. -->
				<field name="ref_itemid_xref"><xsl:apply-templates select="//@href[starts-with(., 'x-svn:')][cmsfn:is-format-dita(..)]" mode="refxref"/></field>
			</xsl:if>
			
			<!-- Extract rlogicalid slaves. -->
			<field name="rel_itemid_rlogicalid"><xsl:apply-templates select="//@cms:rlogicalid" mode="relrlogicalid"/></field>

			<!-- Extract tlogicalid slaves. -->
			<field name="rel_itemid_tlogicalid"><xsl:apply-templates select="//@cms:tlogicalid" mode="reltlogicalid"/></field>

		</doc>
	</xsl:template>

	<xsl:template match="@*" mode="refnoncms">
		<xsl:message select="concat('Encountered non-CMS reference: ', .)"/>
		<xsl:value-of select="."/>
		<xsl:value-of select="' '"/>
	</xsl:template>

	<xsl:template match="@*[name() = $ref-attrs-seq]" mode="refdep">
		<xsl:value-of select="."/>
		<xsl:value-of select="' '"/>
	</xsl:template>
	
	<xsl:template match="@*[name() = 'keyrefhref']" mode="refdep refkeydefmap">
		<xsl:value-of select="."/>
		<xsl:value-of select="' '"/>
	</xsl:template>
	
	<xsl:template match="mapref/@href[../@processing-role = 'resource-only']" mode="refkeydefmap">
		<xsl:value-of select="."/>
		<xsl:value-of select="' '"/>
	</xsl:template>
	
	<xsl:template match="@*[name() = 'href'][parent::element()[local-name() = 'include'][namespace-uri() = 'http://www.w3.org/2001/XInclude']]" mode="refinclude">
			<xsl:value-of select="."/>
			<xsl:value-of select="' '"/>
	</xsl:template>
	
	<xsl:template match="@*[name() = 'href'][parent::element()[local-name() = 'include'][namespace-uri() = 'http://www.w3.org/2001/XInclude']]" mode="refgraphic" priority="100">
		<!-- Suppress XInclude when processing graphics references. -->
	</xsl:template>
	
	<xsl:template match="@*[name() = $ref-attrs-seq]" mode="refgraphic">
			<xsl:value-of select="."/>
			<xsl:value-of select="' '"/>
	</xsl:template>
	
	<xsl:template match="@*[name() = 'href']" mode="refdep reftopicref" priority="10">
		<xsl:value-of select="."/>
		<xsl:value-of select="' '"/>
	</xsl:template>
	
	<xsl:template match="@*[name() = 'conref']" mode="refdep refconref" priority="10">
		<xsl:value-of select="."/>
		<xsl:value-of select="' '"/>
	</xsl:template>
	
	<xsl:template match="@*[name() = 'href']" mode="refxref" priority="10">
		<xsl:value-of select="."/>
		<xsl:value-of select="' '"/>
	</xsl:template>
	
	<xsl:template match="@*" mode="refdep refkeydefmap refinclude refgraphic reftopicref refxref refconref" priority="-1">
		<!-- Suppress non-reference attributes. -->
	</xsl:template>
	
	<xsl:template match="@cms:rlogicalid" mode="relrlogicalid">
		<xsl:value-of select="."/>
		<xsl:value-of select="' '"/>
	</xsl:template>
	
	<xsl:template match="@cms:tlogicalid" mode="reltlogicalid">
		<xsl:value-of select="."/>
		<xsl:value-of select="' '"/>
	</xsl:template>
	
	<xsl:template match="text()" mode="title intro">
		<xsl:copy/>
	</xsl:template>
	
	<xsl:template match="*[@linkend]" mode="title intro">
		<xsl:variable name="linkend" select="key('idref', @linkend)[1]"/>
		
		<xsl:choose>
			<xsl:when test="$linkend/text()">
				<!-- Direct text content, e.g. term/termref -->
				<xsl:apply-templates select="$linkend/text()" mode="#current"/>
			</xsl:when>
			<xsl:otherwise>
				<xsl:value-of select="concat('[', @linkend ,']')"/>
			</xsl:otherwise>
		</xsl:choose>
	</xsl:template>
	
	<!-- Tentative approach for managing metadata. Likely need a multiValued field instead. -->
	<xsl:template match="element()" mode="meta">
		<!-- keyword elements are not inlines in this context, individual terms -->
		<xsl:apply-templates select="keyword" mode="meta"/>
		<!-- other elements considered inline at this time, needs more analysis -->
		<xsl:apply-templates select="(element() | text()) except keyword" mode="meta-child"/>
		<!-- Space as separator for now, likely need way of delivering multiple values to a field. -->
		<xsl:value-of select="' '"/>
	</xsl:template>
	
	<xsl:template match="element()" mode="meta-child">
		<!-- Likely inline elements which should not produce a space. -->
		<xsl:apply-templates select="element()|text()" mode="#current"/>
	</xsl:template>
	
	<xsl:template match="text()" mode="meta-child">
		<!-- TODO: Introduce field that does not tokenize on space. Workaround for now. -->
		<!-- 
		<xsl:copy/>
		-->
		<xsl:value-of select="translate(normalize-space(.),' ','_')"></xsl:value-of>
	</xsl:template>
	
	
	<xsl:template match="*[@keyref][//keydef[@keys = current()/@keyref]//keywords/keyword]" mode="title intro" priority="10">
		<!-- Matching locally defined keydefs.  -->
		<!-- Keydefs in referenced maps are much more complex and could change after indexing. -->
		<!-- TODO: Support multi-value keys attribute. -->
		<xsl:apply-templates select="//keydef[@keys = current()/@keyref]//keywords/keyword[1]" mode="#current"/>
	</xsl:template>
	
	<xsl:template match="*[@keyref][not(text())]" mode="title intro">
		<xsl:value-of select="concat('[', @keyref ,']')"/>
	</xsl:template>
	
	<xsl:template match="*[@keyref][text()]" mode="title intro">
		<xsl:apply-templates select="*"/>
	</xsl:template>
	
	
	<xsl:function name="cmsfn:get-docno" as="xs:string?">
		<xsl:param name="root" as="element()"/>
		
		<xsl:choose>
			<xsl:when test="$root//bookmeta/bookid/bookpartno[@xml:lang = /*/@xml:lang]">
				<xsl:value-of select="$root//bookmeta/bookid/bookpartno[@xml:lang = /*/@xml:lang]"/>
			</xsl:when>
			
			<xsl:when test="$root//docinfogroup/docinfo[@market = /*/@xml:lang]">
				<xsl:value-of select="$root//docinfogroup/docinfo[@market = /*/@xml:lang]/docno"/>
			</xsl:when>
			
			<xsl:when test="$root//docinfogroup/docinfo[@xml:lang = /*/@xml:lang]">
				<xsl:value-of select="$root//docinfogroup/docinfo[@xml:lang = /*/@xml:lang]/docno"/>
			</xsl:when>
			
			<xsl:when test="$root//docinfogroup/docinfo[not(@market)] and $root//docinfogroup/docinfo[not(@xml:lang)] and count($root//docinfogroup/docinfo) = 1">
				<!-- Exactly one docinfo element, no @market / @xml:lang attribute set. -->
				<xsl:value-of select="$root//docinfogroup/docinfo[1]/docno"/>
			</xsl:when>
			
			<xsl:when test="$root/@docno">
				<xsl:value-of select="$root/@docno"/>
			</xsl:when>
		</xsl:choose>
		<!-- Empty result if no match. -->
	</xsl:function>
	
	<xsl:function name="cmsfn:get-rid-prefix">
		<xsl:param name="rid" as="xs:string"/>
		<xsl:choose>
			<xsl:when test="string-length($rid) = 15">
				<xsl:value-of select="substring($rid, 1, 11)"/>
			</xsl:when>
			<xsl:otherwise>
				<!-- Fallback to whole string if the RID format is unknown. -->
				<xsl:value-of select="$rid"/>
			</xsl:otherwise>
		</xsl:choose>
	</xsl:function>
	
	
	<!-- Versioned in cms-xmlsource. -->
	<xsl:function name="cmsfn:is-format-dita" as="xs:boolean">
		<xsl:param name="refelem" as="element()"/>
		<xsl:variable name="ditaext" as="xs:string+" select="tokenize('dita ditamap', ' ')"/>
		
		<xsl:choose>
			<xsl:when test="$refelem/@format">
				<!-- The format attribute is used if set. -->
				<xsl:sequence select="$refelem/@format = $ditaext"/>
			</xsl:when>
			<xsl:otherwise>
				<!-- The CMS requires DITA files to have extensions dita or ditamap, lowercase. -->
				<!-- Must also allow extension xml according to DITA spec (only when no @format) and for migrated topics. -->
				<xsl:sequence select="matches($refelem/@href, '^[^#]*\.(xml|dita|ditamap)(#.*)?$')"/>
			</xsl:otherwise>
		</xsl:choose>
	</xsl:function>
	
</xsl:stylesheet>