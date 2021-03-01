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
<xsl:stylesheet version="3.0"
	xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
	xmlns:xs="http://www.w3.org/2001/XMLSchema"
	xmlns:cms="http://www.simonsoft.se/namespace/cms"
	xmlns:cmsfn="http://www.simonsoft.se/namespace/cms-functions"
	xmlns:cmsreposxml="http://www.simonsoft.se/namespace/cms-reposxml"
	>

	<!-- document's status -->
	<xsl:param name="document-status"/>
	<!-- document's patharea (release, translation) -->
	<xsl:param name="patharea"/>
	<!-- depth of reposxml extraction as determined by repositem XSL. -->
	<!-- TODO: Depth limit implemented in XmlSourceHandlerFieldExtractors can likely be removed, done here since Pipeline introduced. -->
	<xsl:param name="reposxml-depth" as="xs:integer?"/>
	<!-- ancestor attributes on an element named 'attributes' -->
	<!-- 
	<xsl:param name="ancestor-attributes"/>
	 -->
		
	
	<!-- key definition for cms:rid lookup -->
	<xsl:key name="rid" use="@cms:rid" match="*[ not(ancestor-or-self::*[@cms:tsuppress])  or ancestor-or-self::*[@cms:tsuppress = 'no'] ]"/>


	<!-- <xsl:variable name="whitespace" select="'&#x20;&#xD;&#xA;&#x9;'"/>-->
	<xsl:variable name="whitespace" select="' '"/>


	<xsl:template match="/">
		
		<xsl:variable name="cms-namespace-source" select="namespace-uri-for-prefix('cms', /element())"/>
		<xsl:if test="$cms-namespace-source!='http://www.simonsoft.se/namespace/cms'">
			<xsl:message terminate="yes">The namespace with prefix 'cms' must be 'http://www.simonsoft.se/namespace/cms'.</xsl:message>
		</xsl:if>
		
		<xsl:copy>
			<xsl:apply-templates select="node()" mode="#current"/>
		</xsl:copy>
	</xsl:template>

	<xsl:template match="@*|node()">
		<!-- Tokenize the text nodes before concat:ing them to avoid issue with missing space (btw e.g. a title and a p) -->
		<!-- Inspired by: http://stackoverflow.com/questions/12784190/xslt-tokenize-nodeset -->
		<xsl:variable name="text" select="for $elemtext in descendant-or-self::text()[not(ancestor::*[@keyref])] return tokenize(normalize-space($elemtext), $whitespace)"/>
		<!-- Text that should be / has been translated.  -->
		<xsl:variable name="text_translate" select="for $elemtext in descendant-or-self::text()[not(ancestor::*[@keyref])][not(ancestor::*[@translate='no' or @markfortrans='no'])][not(ancestor::*[@cms:tsuppress[not(. = 'no')]])] return tokenize(normalize-space($elemtext), $whitespace)"/>
		<!-- Text immediate child nodes. -->
		<xsl:variable name="text_child" select="for $elemtext in child::text() return tokenize(normalize-space($elemtext), $whitespace)"/>
		
		
		<xsl:copy>
			<!-- Number of elements -->	
			<xsl:attribute name="cmsreposxml:count_elements" select="count(//element())"/>
			
			<!-- Just concat of the tokens/words. -->
			<!-- Max 500 words or 3000 chars. -->
			<xsl:variable name="text_string" as="xs:string">
				<xsl:value-of select="$text[500 >= position()]"/>
			</xsl:variable>
			<xsl:if test="3000 >= string-length($text_string)">
				<xsl:attribute name="cmsreposxml:text" select="$text_string"/>
			</xsl:if>
			
			<!-- Word count is simple when each word is a text node. -->
			<xsl:attribute name="cmsreposxml:count_words_text" select="count($text)"/>
			<xsl:attribute name="cmsreposxml:count_words_translate" select="count($text_translate)"/>
			<xsl:attribute name="cmsreposxml:count_words_child" select="count($text_child)"/>
			
			
			<xsl:attribute name="cmsreposxml:reusevalue">
				<xsl:apply-templates select="." mode="rule-reusevalue-context"/>
			</xsl:attribute>
			
			<xsl:attribute name="cmsreposxml:reuseready">
				<xsl:apply-templates select="." mode="rule-reuseready"/>
			</xsl:attribute>
			
			<!-- RID duplicates and shallow Translation RIDs. -->
			<xsl:call-template name="reuserid"/>
			
			
			<!-- Experimental support for determining which declared namespaces are not actually used. -->
			<!-- TODO: Discuss whether the feature is worth the performance hit.  -->
			<!-- Tests with 860k indicates 5-10% higher execution time, close to 5% when test file has no unused ns. -->
			<!-- Likely increasing processing time with a larger number of declared namespaces. -->
			<xsl:attribute name="cmsreposxml:ns_unused">
				<xsl:apply-templates select="." mode="i-ns-unused"/>
			</xsl:attribute>
			
			
			<!-- reuse-normalized.xsl before in pipeline -->
			<xsl:apply-templates select="@cms:source_reuse" mode="#current"/>
			<xsl:apply-templates select="@cms:c_sha1_source_reuse" mode="#current"/>
			
			<xsl:apply-templates select="@* except @cms:source_reuse|@cms:source_reuse" mode="#current"/>
			
			<!-- Limit depth of reposxml extraction if parameter set from repositem XSL. -->
			<xsl:if test="empty($reposxml-depth) or ($reposxml-depth > count(ancestor-or-self::element()))">
				<xsl:apply-templates select="node()" mode="#current"/>
			</xsl:if>
		</xsl:copy>
	</xsl:template>
	
	<xsl:template match="@cms:source_reuse">
		<xsl:attribute name="cmsreposxml:source_reuse" select="."/>
	</xsl:template>
	
	<xsl:template match="@cms:c_sha1_source_reuse">
		<xsl:attribute name="cmsreposxml:c_sha1_source_reuse" select="."/>
	</xsl:template>

	<!-- Suppress content not used by reposxml handler. -->
	<xsl:template match="text()" mode="source source-passthrough">
		<!-- Suppress text. -->
	</xsl:template>
	
	<xsl:template match="comment()" mode="source source-passthrough">
		<!-- Suppress comments. -->
	</xsl:template>
	
	<xsl:template match="processing-instruction()" mode="source source-passthrough">
		<!-- Suppress PI. -->
	</xsl:template>


	<xsl:template name="reuserid">
		<!-- RID duplicates and shallow Translation RIDs. -->

		<xsl:if test="count(ancestor-or-self::element()) = 1">
	
			<!-- See comments below. -->
			<xsl:variable name="ridduplicates" select="descendant-or-self::*[count(key('rid', @cms:rid)) > 1]/@cms:rid"/>

			<!-- reuseridreusevalue - RIDs with reusevalue > 0 -->
			<!-- Checksums added in Java extractor-->
			<!-- Disable the whole document if there are RID duplicates. -->
			<xsl:if test="$patharea = 'translation' and empty($ridduplicates)">
				<xsl:variable name="ridelements" as="element()*" select="descendant-or-self::*[@cms:rid]"/>
				<xsl:attribute name="cmsreposxml:reuseridreusevalue">
					<xsl:for-each select="$ridelements">
						<xsl:apply-templates select="." mode="reuserid-value"/>
					</xsl:for-each>
				</xsl:attribute>
			</xsl:if>
			
			
			<!-- Lists all duplicated RID (duplicates included twice) -->
			<!-- Rids should not be duplicated in the document, but that can only be identified from the root node.-->
			<!-- This field can only identify duplicates among its children, not whether the element itself is a duplicate in the document context. -->
			<!-- Disabling reusevalue from the root node of the XML onto all children (done by an XmlIndexFieldExtraction class). -->
			<!-- TODO: Consider if we should only perform this extraction for root node (depth = 1), if performance is heavily degraded. -->
			<xsl:attribute name="cmsreposxml:reuseridduplicate">
				<xsl:value-of select="$ridduplicates"/>
			</xsl:attribute>
			
		</xsl:if>
	</xsl:template>


	
	<!-- Determine which elements / RIDs have reusevalue > 0 -->
	<xsl:template match="element()[@cms:rid]" mode="reuserid-value">
		<xsl:variable name="reusevalue" as="xs:integer">
			<xsl:apply-templates select="." mode="rule-reusevalue-context"/>
		</xsl:variable>
		<!--
		<xsl:message select="concat(@cms:rid, ' ', $reusevalue)"></xsl:message>
		-->
		<xsl:if test="$reusevalue > 0">
			<xsl:value-of select="@cms:rid"/>
			<xsl:value-of select="' '"/>
		</xsl:if>
	</xsl:template>


	<!-- Ranks elements according to how useful they would be as replacement, >0 to be at all useful -->
	<!-- TODO: Can be removed? Will not work if we extract in multiple levels. No, need to merge $ancestor-attributes tests.-->
	<xsl:template match="*" mode="rule-reusevalue-context">
		<xsl:choose>
			<!-- #716 Mechanism for suppressing parts of a document without regard to rlogicalid. -->
			<!-- Ancestors and element itself where tsuppress is set. -->
			<xsl:when test="descendant-or-self::*[@cms:tsuppress and not(@cms:tsuppress = 'no')]">-4</xsl:when>
			<!-- #716 Children where tsuppress is set above (attribute exists and its value is not-'no'). -->
			<xsl:when test="ancestor::*[@cms:tsuppress and not(@cms:tsuppress = 'no')]">-5</xsl:when>
			
			<!-- Children where tvalidate='no' is set above, disqualify. (value -6 is reserved but not used) -->
			<xsl:when test="ancestor::*[@cms:tvalidate='no']">-7</xsl:when>
			
			<!-- Rid should not have been removed from the current node -->
			<xsl:when test="not(@cms:rid)">-2</xsl:when>	
			<!-- Rid should not have been removed from a child, but note that removal must always be done on complete includes -->
			<!-- There is now the option to selectively remove RIDs below tsuppress (#716). -->
			<!-- Some installations don't set rids below certain stop tags -->
			<xsl:when test="descendant-or-self::*[@cms:rlogicalid and not(@cms:rid)]">-3</xsl:when>
			<!-- Marking a document Obsolete means we don't want to reuse from it -->
			<xsl:when test="$document-status = 'Obsolete'">-1</xsl:when>
			
			<!-- Anything else is a candidate for reuse, with tstatus set on the best match and replacements done if reuseready>0 -->
			<xsl:otherwise>1</xsl:otherwise>
		</xsl:choose>
	</xsl:template>


	<!-- Decides if a match is valid for use as replacement, >0 means true -->
	<xsl:template match="*" mode="rule-reuseready">
		<xsl:choose>
			<xsl:when test="$document-status = 'Released'">1</xsl:when>
			<xsl:otherwise>0</xsl:otherwise>
		</xsl:choose>
	</xsl:template>
	
	
	<!-- Experimental support for determining which declared namespaces are not actually used. -->
	<!-- Investigates all namespaces declared on parent, which means they are inherited by this element. -->
	<xsl:template match="*" mode="i-ns-unused">
		<xsl:variable name="localname" select="name()"/>
		<xsl:variable name="namespace-parent" select="parent::node()/namespace::*[string() != 'http://www.w3.org/XML/1998/namespace']"/>
		
		<xsl:variable name="namespace-elem" select="descendant-or-self::*[namespace-uri() != '']"/>
		<xsl:variable name="namespace-attr" select="descendant-or-self::*/@*[namespace-uri() != ''][name() != 'cms:c_sha1_source_reuse'][name() != 'cms:source_reuse']"/>
				
		<xsl:variable name="ns-uris" as="xs:anyURI*" select="for $e in ($namespace-elem union $namespace-attr) return namespace-uri($e)" />
		<!-- 
		<xsl:message select="concat('Namespaces on ', $localname, ' defined: ', count($namespace-parent), ' Namespaces used: ', count($namespace-elem), '+', count($namespace-attr), '=', count($ns-uris))"/>
		-->
		
		<xsl:for-each select="$namespace-parent">
            <xsl:variable name="ns-uri" select="."></xsl:variable>
            
            <xsl:choose>
			<xsl:when test="$ns-uri = $ns-uris">
				<!-- 
				<xsl:message select="concat('Namespace on ', $localname, ' used: ', $ns-uri)"/>
				-->
            </xsl:when>
            <xsl:otherwise>
                <xsl:value-of select="$ns-uri"></xsl:value-of>
				<xsl:value-of select="'&#xA;'"></xsl:value-of>
            </xsl:otherwise>
            </xsl:choose>
        </xsl:for-each>
	</xsl:template>
	
	
</xsl:stylesheet>