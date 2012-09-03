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
<xsl:stylesheet version="1.0" xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
	xmlns:saxon="http://saxon.sf.net/">

	<xsl:template match="*">
	
		<doc>

			<field name="name"><xsl:value-of select="name()"/></field>

			<xsl:apply-templates select="@*"/>
		
			<field name="source_serialized">
				<!-- saxon:serialize() not available in Saxon-HE -->
				<!-- This will still be xml in content handler... -->
				<xsl:text disable-output-escaping="yes">&lt;![CDATA[</xsl:text>
				<xsl:copy-of select="."/>
				<xsl:text disable-output-escaping="yes">]]&gt;</xsl:text>
			</field>
			
			<field name="source">
				<xsl:apply-templates select="." mode="source"/>
			</field>
			
			<field name="source_reuse">
				<xsl:apply-templates select="." mode="source-reuse"/>
			</field>
			
			<field name="text"><xsl:value-of select="normalize-space(.)"/></field>
			
			<field name="words_text">TODO (or do together with checksum in java)</field>
			
			<!-- 
			Recursive instead of flat doc structure, for aggregation in java instead of xsl.
			Is there an aggregation strategy for all attributes that does not
			require re-recursion up to root from everey element?
			-->
			<children>
				<xsl:apply-templates select="child::node()"/>
			</children>
			
		</doc>
		
	</xsl:template>
	
	<xsl:template match="@*">
		<field>
			<xsl:attribute name="name">a_<xsl:value-of select="name()"/></xsl:attribute>
			<xsl:value-of select="."/>
		</field>
	</xsl:template>

	<xsl:template match="*" mode="source">
		<xsl:text>&lt;</xsl:text>
		<xsl:value-of select="name()" />
		<xsl:apply-templates select="@*" mode="source" />
		<xsl:text>&gt;</xsl:text>
		<xsl:apply-templates mode="source" />
		<xsl:text>&lt;/</xsl:text>
		<xsl:value-of select="name()" />
		<xsl:text>&gt;</xsl:text>
	</xsl:template>
	
	<xsl:template match="@*" mode="source">
		<xsl:value-of select="' '"/>
		<xsl:value-of select="name()"/>
		<xsl:text>="</xsl:text>
		<xsl:value-of select="."/>
		<xsl:text>"</xsl:text>
	</xsl:template>

	<xsl:template match="text()" mode="source">
		<xsl:value-of select="." />
	</xsl:template>

	<xsl:template match="*" mode="source-reuse">
		<xsl:text>&lt;</xsl:text>
		<xsl:value-of select="name()" />
		<!-- filter attributes -->
		<xsl:apply-templates mode="source-reuse" select="@*[name() != 'cms:rid' and name() != 'cms:rlogicalid']" />
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
		<xsl:value-of select="." />
	</xsl:template>
	
</xsl:stylesheet>