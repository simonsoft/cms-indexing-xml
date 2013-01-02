<?xml version="1.0" encoding="UTF-8"?>
<!--

    Copyright (C) 2009-2012 Simonsoft Nordic AB

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
<xsl:stylesheet version="1.0"
	xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
	xmlns:cms="http://www.simonsoft.se/namespace/cms"
	>

	<!-- 
	TODO implement IndexFieldExtensions and run this, then checksumming,
	and add to indexing chain.
	-->

	<xsl:template match="*">
	
		<!-- <xsl:variable name="whitespace" select="'&#x20;&#xD;&#xA;&#x9;'"/>-->
		<xsl:variable name="whitespace" select="' '"/>
	
		<doc>

			<!-- skip name and attributes, already extracted -->

			<!-- skip plain source, already extracted -->
			
			<field name="source_reuse">
				<xsl:apply-templates select="." mode="source-reuse"/>
			</field>
			
			<field name="text"><xsl:value-of select="normalize-space(.)"/></field>
			
			<field name="words_text"><xsl:value-of select="count(tokenize(string(.), $whitespace))"/></field>
			
			<field name="reusevalue">
				<xsl:apply-templates select="." mode="find-rid-removal"/>
			</field>
			
		</doc>
		
	</xsl:template>

	<xsl:template match="*" mode="source-reuse">
		<xsl:text>&lt;</xsl:text>
		<xsl:value-of select="name()" />
		<!-- filter attributes -->
		<xsl:apply-templates mode="source-reuse" select="@*[
			name() != 'cms:rid' 
			and name() != 'cms:rlogicalid'
			and name() != 'cms:trid'
			and name() != 'cms:tpos'
			and name() != 'cms:tlogicalid'
			and name() != 'cms:tmatch'
			and name() != 'cms:twords'
			]" />
		<xsl:text>&gt;</xsl:text>
		<xsl:apply-templates mode="source-reuse" />
		<xsl:text>&lt;/</xsl:text>
		<xsl:value-of select="name()" />
		<xsl:text>&gt;</xsl:text>
	</xsl:template>
	
	<xsl:template match="@*" mode="source-reuse">
		<xsl:value-of select="' '"/>
		<xsl:value-of select="name()"/>
		<xsl:text>="</xsl:text>
		<xsl:value-of select="."/>
		<xsl:text>"</xsl:text>
	</xsl:template>

	<xsl:template match="text()" mode="source-reuse">
		<!-- TODO normalize space
		BUT how do we handle preformatted?
		Probably ok as long as we use source instead of source-reuse for replacement.
		-->
		<xsl:value-of select="." />
	</xsl:template>
	
	<xsl:template match="*" mode="find-rid-removal">
		<xsl:if test="//*[@cms:rlogicalid and not(@cms:rid)]">-1</xsl:if>
	</xsl:template>
	
</xsl:stylesheet>