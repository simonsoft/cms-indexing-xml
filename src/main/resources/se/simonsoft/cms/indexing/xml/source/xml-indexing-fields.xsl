<?xml version="1.0" encoding="UTF-8"?>
<!--

    Copyright (C) 2009-2016 Simonsoft Nordic AB

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
	>

	<!-- document's status -->
	<xsl:param name="document-status"/>
	<!-- depth of element relative to document -->
	<xsl:param name="document-depth"/>
	<!-- ancestor attributes on an element named 'attributes' -->
	<xsl:param name="ancestor-attributes"/>
	
	<!-- key definition for cms:rid lookup -->
	<xsl:key name="rid" use="@cms:rid" match="*[ not(ancestor-or-self::*[@cms:tsuppress])  or ancestor-or-self::*[@cms:tsuppress = 'no'] ]"/>


	<!-- Will only match the initial context element since all further processing is done with specific modes. -->
	<xsl:template match="*">
	
		<xsl:variable name="cms-namespace-source" select="namespace-uri-for-prefix('cms', .)"/>
        <xsl:if test="$cms-namespace-source!='http://www.simonsoft.se/namespace/cms'">
            <xsl:message terminate="yes">The namespace with prefix 'cms' must be 'http://www.simonsoft.se/namespace/cms'.</xsl:message>
        </xsl:if>
	
		<!-- <xsl:variable name="whitespace" select="'&#x20;&#xD;&#xA;&#x9;'"/>-->
		<xsl:variable name="whitespace" select="' '"/>
	
		<!-- Tokenize the text nodes before concat:ing them to avoid issue with missing space (btw e.g. a title and a p) -->
		<!-- Inspired by: http://stackoverflow.com/questions/12784190/xslt-tokenize-nodeset -->
		<xsl:variable name="text" select="for $elemtext in descendant-or-self::*[not(@keyref)]/text() return tokenize(normalize-space($elemtext), $whitespace)"/>
	
		<doc>

			<!-- skip name and attributes, already extracted -->

			<!-- skip plain source, already extracted -->
			
			<!-- What about number of elements? -->	
			
			
			<!-- Just concat of the tokens/words. Somehow becomes space-separated. -->
			<field name="text"><xsl:value-of select="$text"/></field>
			
			<!-- Word count is simple when each word is a text node. -->
			<field name="words_text"><xsl:value-of select="count($text)"/></field>
			
			<field name="reusevalue">
				<xsl:apply-templates select="." mode="rule-reusevalue"/>
			</field>

			<field name="reuseready">
				<xsl:apply-templates select="." mode="rule-reuseready"/>
			</field>
			
			<!-- Lists all duplicated RID (duplicates included twice) -->
			<!-- Rids should not be duplicated in the document, but that can only be identified from the root node.-->
			<!-- This field can only identify duplicates among its children, not whether the element itself is a duplicate in the document context. -->
			<!-- Disabling reusevalue from the root node of the XML onto all children (done by an XmlIndexFieldExtraction class). -->
			<!-- TODO: Consider if we should only perform this extraction for root node (depth = 1), if performance is heavily degraded. -->
			<field name="reuseridduplicate">
				<xsl:value-of select="descendant-or-self::*[count(key('rid', @cms:rid)) > 1]/@cms:rid"/>
			</field>
			
			<!-- Experimental support for determining which declared namespaces are not actually used. -->
			<!-- TODO: Discuss whether the feature is worth the performance hit.  -->
			<!-- Tests with 860k indicates 5-10% higher execution time, close to 5% when test file has no unused ns. -->
			<!-- Likely increasing processing time with a larger number of declared namespaces. -->
			<field name="ns_unused">
				<xsl:apply-templates select="." mode="i-ns-unused"/>
			</field>
			
		</doc>
		
	</xsl:template>


	
	
	
	<!-- Ranks elements according to how useful they would be as replacement, >0 to be at all useful -->
	<xsl:template match="*" mode="rule-reusevalue">
		<xsl:choose>
			<!-- #716 Mechanism for suppressing parts of a document without regard to rlogicalid. -->
			<!-- Ancestors and element itself where tsuppress is set. -->
			<xsl:when test="descendant-or-self::*[@cms:tsuppress and not(@cms:tsuppress = 'no')]">-4</xsl:when>
			<!-- #716 Children where tsuppress is set above (attribute exists and its value is not-'no'). -->
			<xsl:when test="$ancestor-attributes/*/@cms:tsuppress and not($ancestor-attributes/*/@cms:tsuppress = 'no')">-5</xsl:when>
			
			<!-- Children where tvalidate='no' is set above, disqualify. (value -6 is reserved but not used) -->
			<xsl:when test="$ancestor-attributes/*/@cms:tvalidate='no'">-7</xsl:when>
			
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
		<xsl:variable name="namespace-parent" select="parent::node()/namespace::*[string() != 'http://www.w3.org/XML/1998/namespace']"/>
		
		<xsl:variable name="namespace-elem" select="descendant-or-self::*[namespace-uri() != '']"/>
		<xsl:variable name="namespace-attr" select="descendant-or-self::*/@*[namespace-uri() != '']"/>
		
		<xsl:variable name="ns-uris" as="xs:anyURI*" select="for $e in ($namespace-elem union $namespace-attr) return namespace-uri($e)" />
		
		<xsl:for-each select="$namespace-parent">
            <xsl:variable name="ns-uri" select="."></xsl:variable>
            
            <xsl:if test="$ns-uri != $ns-uris">
                <!-- TODO: Support multi-value fields using Solr arr/str notation. -->
                    <xsl:value-of select="$ns-uri"></xsl:value-of>
					<xsl:value-of select="'&#xA;'"></xsl:value-of>
            </xsl:if>
        </xsl:for-each>
	</xsl:template>
	
	
</xsl:stylesheet>