<?xml version="1.0" encoding="UTF-8"?>
<!--

    Copyright (C) 2009-2013 Simonsoft Nordic AB

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
	<xsl:key name="rid" use="@cms:rid" match="*"/>


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
		<xsl:variable name="text" select="for $elemtext in descendant-or-self::text() return tokenize(normalize-space($elemtext), $whitespace)"/>
	
		<doc>

			<!-- skip name and attributes, already extracted -->

			<!-- skip plain source, already extracted -->
			
			<!-- What about number of elements? -->	
			
			
			<!-- Just concat of the tokens/words. Somehow becomes space-separated. -->
			<field name="text"><xsl:value-of select="$text"/></field>
			
			<!-- Word count is simple when each word is a text node. -->
			<field name="words_text"><xsl:value-of select="count($text)"/></field>
			
			<field name="source_reuse">
				<xsl:apply-templates select="." mode="source-reuse-root"/>
			</field>
			
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

	<xsl:template match="*" mode="source-reuse-root">
	
		<!-- The root node of each indexed element: 
		- Basic rule is to exclude all attributes. 
		- Some attributes might have a fundamental impact on the contained text (cause a variation in translation).
		- Possible to customize how inherited attributes are potentially used and/or combined with local attributes.
		-->
		<xsl:variable name="attrs" as="attribute() *">
			<xsl:apply-templates select="$ancestor-attributes/*/@*" mode="source-reuse-root-inherited"/>
            <xsl:apply-templates select="@*" mode="source-reuse-root"/>
        </xsl:variable>
	
		<xsl:text>&lt;</xsl:text>
		<xsl:value-of select="name()" />
		<xsl:apply-templates mode="source-reuse-serialize" select="$attrs"/>
		<xsl:text>&gt;</xsl:text>
		<xsl:apply-templates mode="source-reuse-child" />
		<xsl:text>&lt;/</xsl:text>
		<xsl:value-of select="name()" />
		<xsl:text>&gt;</xsl:text>
	</xsl:template>
	
	<xsl:template match="@*" mode="source-reuse-root-inherited">
		<!-- Exclude attributes that are not explicitly matched. -->
	</xsl:template>
	
	<xsl:template match="@*" mode="source-reuse-root">
		<!-- Exclude attributes that are not explicitly matched. -->
	</xsl:template>
	
	<xsl:template match="@markfortrans|@translate" mode="source-reuse-root-inherited">
		<!-- #743 Attribute defining no-translation of element: markfortrans, translate etc. -->
		<!-- With this approach of including attr in checksum (with inheritance) we are 'neutral' to DTD default values. --> 
		<!-- Defaulting to anything for non-inline elements is not recommended. -->
		<!-- We consider the expected behaviour undefined when defaulting below an explicit 'yes'/'no'. -->
		<!-- Consider also supporting a CMS-namespace attribute: cms:translate or cms:markfortrans -->
		<xsl:copy/>
	</xsl:template>
	
	<xsl:template match="@markfortrans|@translate" mode="source-reuse-root">
		<!-- #743 Attribute defining no-translation of element: markfortrans, translate etc. -->
		<xsl:copy/>
	</xsl:template>
	
	
	

	<xsl:template match="*" mode="source-reuse-child">
		<!-- NOTE: Empty elements are serialized in long form (start and end tag). -->
		
		<!-- This variable construction is not strictly required for 'source-reuse-child' (at this time). -->
		<xsl:variable name="attrs" as="attribute() *">
			<!-- filtering of attributes is done in match statements of templates, easier to customize. -->
			<!-- Base rule is to include attributes. -->
            <xsl:apply-templates select="@*" mode="source-reuse-child"/>
        </xsl:variable>
            
		<xsl:text>&lt;</xsl:text>
		<xsl:value-of select="name()" />
		<xsl:apply-templates mode="source-reuse-serialize" select="$attrs"/>
		<xsl:text>&gt;</xsl:text>
		<xsl:apply-templates mode="source-reuse-child" />
		<xsl:text>&lt;/</xsl:text>
		<xsl:value-of select="name()" />
		<xsl:text>&gt;</xsl:text>
	</xsl:template>
	
	<xsl:template match="@cms:rid | @cms:rlogicalid | @cms:rwords| @cms:twords | @cms:tstatus | @cms:trid | @cms:tpos | @cms:tlogicalid | @cms:tmatch" mode="source-reuse-child">
        <!-- Simply doing nothing to suppress the CMS attributes. -->
        <!-- Does not include cms:tvalidate because it should be considered in checksums. -->
    </xsl:template>
	
	<xsl:template match="@*" mode="source-reuse-child">
		<!-- Include attributes that are not explicitly matched. -->
		<xsl:copy/>
	</xsl:template>
	
	
	<xsl:template match="@*" mode="source-reuse-serialize">
		<!-- Serialize the attribute information. -->
		<xsl:value-of select="' '"/>
		<xsl:value-of select="name()"/>
		<xsl:text>="</xsl:text>
		<xsl:value-of select="."/>
		<xsl:text>"</xsl:text>
	</xsl:template>
	
	<xsl:template match="@*[namespace-uri()='http://www.simonsoft.se/namespace/cms']" mode="source-reuse-serialize">
		<!-- Serialize the attribute information. -->
		<!-- This template ensures that the prefix for CMS namespace is 'cms'. -->
		<xsl:value-of select="' '"/>
		<xsl:value-of select="concat('cms:', local-name())"/>
		<xsl:text>="</xsl:text>
		<xsl:value-of select="."/>
		<xsl:text>"</xsl:text>
	</xsl:template>
	
	
	<xsl:template match="ph/node()" mode="source-reuse-child" priority="90">
		<!-- Suppress content of ph elements (inserted/replaced during publishing) but allow all attributes to propagate. -->
	</xsl:template>
	
	<xsl:template match="varref/node()" mode="source-reuse-child" priority="90">
		<!-- Suppress content of varref elements (used by some customers before Keyref) but allow all attributes to propagate. -->
	</xsl:template>
	
	
	<!-- Text Normalization -->

	<xsl:template match="*[@xml:space='preserve']/text()" mode="source-reuse-child" priority="5">
        <!-- Not normalizing space when xml:space is set. -->
        <!-- Can not determine normalize from schema, will have to be acceptable -->
        <!-- Important to use source instead of source-reuse for replacement. -->
        <xsl:value-of select="." />
    </xsl:template>
    
    <xsl:template match="text()" mode="source-reuse-child" priority="1">
        <!-- Text: Normalize each text node. -->
        <xsl:value-of select="normalize-space(.)" />
    </xsl:template>
    
    <xsl:template match="text()[starts-with(., ' ')]" mode="source-reuse-child" priority="3">
        <!-- Text: Normalize each text node. Preserve a starting space.-->
        <!-- This template also matches white-space only and presents a single space. -->
        <xsl:value-of select="concat(' ', normalize-space(.))" />
    </xsl:template>
    
    <xsl:template match="text()[ends-with(., ' ')  and normalize-space(.) != '']" mode="source-reuse-child" priority="3">
        <!-- Text: Normalize each text node. Preserve a trailing space. -->
        <xsl:value-of select="concat(normalize-space(.), ' ')" />
    </xsl:template>

    <xsl:template match="text()[starts-with(., ' ') and ends-with(., ' ') and normalize-space(.) != '']" mode="source-reuse-child" priority="4">
        <!-- Text: Normalize each text node. Preserve both starting and trailing space. -->
        <!-- This template should NOT match '  ' (two+ spaces). -->
        <xsl:value-of select="concat(' ', normalize-space(.), ' ')" />
    </xsl:template>
    
	
	<xsl:template match="processing-instruction()" mode="source-reuse-child">
        <xsl:text>&lt;?</xsl:text>
		<xsl:value-of select="name()"/>
		<xsl:text> </xsl:text>
		<!-- Suppressing RIDs to improve pretranslate use of translations prepared with early 2.0 adapters. -->
		<xsl:value-of select="normalize-space(replace(., 'cms:rid=&quot;[a-z0-9]{15}&quot;', ''))"/>
		<xsl:text>?&gt;</xsl:text>
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