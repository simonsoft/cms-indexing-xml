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
	
	<!-- key definition for cms:rid lookup -->
	<xsl:key name="rid" use="@cms:rid" match="*"/>
	
	<!-- Experimental support for determining which declared namespaces are not actually used. -->
	<xsl:key name="ns_elem" match="*[namespace-uri() != '']" use="namespace-uri()"/>
    <xsl:key name="ns_attr" match="*/@*[namespace-uri() != '']" use="namespace-uri()"/>


	<!-- Will only match the initial context element since all further processing is done with specific modes. -->
	<xsl:template match="*">
		<xsl:variable name="root" select="."/>
		<xsl:variable name="titles" select="//title"/>
	
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
			<!-- name and attributes -->
			<!-- using the embd_ field for now -->
			<!-- TODO: Decision on field names, some test coverage in xml-tracking. -->
			<field name="embd_name"><xsl:value-of select="name($root)"/></field>
			<xsl:for-each select="$root/@*">
				<xsl:variable name="fieldname" select="concat('embd_a_', replace(name(.), ':', '.'))"/>
				<field name="{$fieldname}"><xsl:value-of select="."/></field>
			</xsl:for-each>
			
			<!-- Title, there is a specific field in repositem schema but there will be a separate handler making a selection. -->
			<!-- Do we need to normalize the content? -->
			<xsl:if test="$titles">
				<field name="embd_title"><xsl:value-of select="$titles[1]"/></field>
			</xsl:if>
			
			
			<!-- What about number of elements? -->	
			<field name="count_elements"><xsl:value-of select="count(//element())"/></field>
			
			<xsl:if test="@cms:twords">
				<field name="count_twords"><xsl:value-of select="@cms:twords"/></field>
				
				<xsl:for-each select="distinct-values(//@cms:tstatus)">
					<xsl:variable name="status" select="."/> 
					<xsl:variable name="fieldname" select="concat('count_twords_', $status)"></xsl:variable>
					<field name="{$fieldname}"><xsl:value-of select="sum($root/descendant-or-self::*[@cms:tstatus=$status]/@cms:twords) - sum($root/descendant-or-self::*[@cms:tstatus=$status]/descendant::*[@cms:tstatus]/@cms:twords)"/></field>
				</xsl:for-each>
			</xsl:if>
			
			
			<!-- Just concat of the tokens/words. Somehow becomes space-separated. -->
			<field name="text"><xsl:value-of select="$text"/></field>
			
			<!-- Word count is simple when each word is a text node. -->
			<field name="count_words_text"><xsl:value-of select="count($text)"/></field>

			
			<!-- Lists all duplicated RID (duplicates included twice) -->
			<!-- Rids should not be duplicated in the document, but that can only be identified from the root node.-->
			<!-- This field can only identify duplicates among its children, not whether the element itself is a duplicate in the document context. -->
			<!-- Disabling reusevalue from the root node of the XML onto all children (done by an XmlIndexFieldExtraction class). -->
			<!-- TODO: Consider if we should only perform this extraction for root node (depth = 1), if performance is heavily degraded. -->
			<xsl:variable name="ridduplicate" as="attribute()*"
				select="descendant-or-self::*[count(key('rid', @cms:rid)) > 1]/@cms:rid"/>
			
			<!--  
			<field name="xml_reuseridduplicate">
				<xsl:value-of select="$ridduplicate"/>
			</field>
			-->
			<!-- Boolean flag for rid duplicate. -->
			<xsl:if test="count($ridduplicate) > 0">
				<field name="flag">
					<xsl:value-of select="'hasridduplicate'"/>
				</field>
			</xsl:if>
			
			<!-- Experimental support for determining which declared namespaces are not actually used. -->
			<!-- TODO: Discuss whether the feature is worth the performance hit.  -->
			<!-- Tests with 860k indicates 5-10% higher execution time, close to 5% when test file has no unused ns. -->
			<!-- Likely increasing processing time with a larger number of declared namespaces. -->
			<!-- 
			<field name="ns_unused">
				<xsl:apply-templates select="." mode="i-ns-unused"/>
			</field>
			-->
		</doc>
		
	</xsl:template>

	

	
	<!-- Experimental support for determining which declared namespaces are not actually used. -->
	<!-- Investigates all namespaces declared on parent, which means they are inherited by this element. -->
	<xsl:template match="*" mode="i-ns-unused">
		<xsl:variable name="namespace-parent" select="parent::node()/namespace::*[string() != 'http://www.w3.org/XML/1998/namespace']"/>
		
		<xsl:for-each select="$namespace-parent">
            <xsl:variable name="nsuri" select="."></xsl:variable>
            
            <xsl:if test="count(key('ns_attr', $nsuri)) = 0 and count(key('ns_elem', $nsuri)) = 0">
                <!-- TODO: Support multi-value fields using Solr arr/str notation. -->
                    <xsl:value-of select="$nsuri"></xsl:value-of>
					<xsl:value-of select="'&#xA;'"></xsl:value-of>
            </xsl:if>
            
        </xsl:for-each>
		
	</xsl:template>
	
</xsl:stylesheet>