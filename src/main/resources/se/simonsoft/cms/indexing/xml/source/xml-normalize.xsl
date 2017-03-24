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
<xsl:stylesheet version="2.0" xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
	xmlns:cms="http://www.simonsoft.se/namespace/cms"
	xmlns:xs="http://www.w3.org/2001/XMLSchema"
	exclude-result-prefixes="#all"
	>

	<!-- elements allowed to contain text -->
	<xsl:param name="stop-tags-param" select="'p title'"/>
	
	<!-- Empty elements. -->
	<xsl:param name="empty-tags-param" select="'xref termref'"/>

	<xsl:param name="nl" select="'&#xA;'"/>
	
	<xsl:variable name="stop-tags" as="xs:string+" select="tokenize($stop-tags-param, ' ')"/>
	<xsl:variable name="empty-tags" as="xs:string+" select="tokenize($empty-tags-param, ' ')"/>
		
	

	<xsl:output method="text"/>

	<xsl:template match="/">
		
		<xsl:apply-templates select="." mode="source-reuse-child"></xsl:apply-templates>
	</xsl:template>



	<!-- Structural elements -->
	<xsl:template match="*" mode="source-reuse-child">
		<!-- NOTE: Empty elements are serialized in long form (start and end tag). -->

		<xsl:value-of select="$nl"/>
		<xsl:text>&lt;</xsl:text>
		<xsl:value-of select="name()"/>
		<xsl:apply-templates mode="source-reuse-serialize" select="@*">
			<!-- Difficult to sort ns-attributes last in list? -->
		</xsl:apply-templates>
		<!-- Debatable whether structural elements should have newline after attrs
		     Having a newline avoids diff on old attr if adding a new last attribute.
		-->
		<xsl:value-of select="$nl"/>
		<xsl:text>&gt;</xsl:text>
		<xsl:apply-templates mode="source-reuse-child"/>
		<xsl:value-of select="$nl"/>
		<xsl:text>&lt;/</xsl:text>
		<xsl:value-of select="name()"/>
		<xsl:text>&gt;</xsl:text>
	</xsl:template>
	
	<!-- Paragraph elements -->
	<xsl:template match="element()[local-name() = $stop-tags]" mode="source-reuse-child">
		<!-- NOTE: Empty elements are serialized in long form (start and end tag). -->
		
		<xsl:value-of select="$nl"/>
		<xsl:text>&lt;</xsl:text>
		<xsl:value-of select="name()"/>
		<xsl:apply-templates mode="source-reuse-serialize" select="@*">
			<!--
			<xsl:sort select="local-name()"/>
			-->
		</xsl:apply-templates>
		<xsl:value-of select="$nl"/>
		<xsl:text>&gt;</xsl:text>
		<xsl:apply-templates mode="source-reuse-child"/>
		<xsl:text>&lt;/</xsl:text>
		<xsl:value-of select="name()"/>
		<xsl:text>&gt;</xsl:text>
	</xsl:template>
	
	<!-- Empty elements (both structural and inline) -->
	<xsl:template match="element()[local-name() = $empty-tags]" mode="source-reuse-child" priority="100">
		<!-- TODO: Fail if there are children (non-attributes) -->
				
		<xsl:text>&lt;</xsl:text>
		<xsl:value-of select="name()"/>
		<xsl:apply-templates mode="source-reuse-serialize" select="@*">
			<!--
			<xsl:sort select="local-name()"/>
			-->
		</xsl:apply-templates>
		<xsl:value-of select="$nl"/>

		<xsl:text>/&gt;</xsl:text>
	</xsl:template>
	
	<!-- Inline elements -->
	<xsl:template match="element()[local-name() = $stop-tags]//*" mode="source-reuse-child">
		<!-- NOTE: Empty elements are serialized in long form (start and end tag). -->
		
		<xsl:text>&lt;</xsl:text>
		<xsl:value-of select="name()"/>
		<xsl:apply-templates mode="source-reuse-serialize" select="@*">
			<!--
			<xsl:sort select="local-name()"/>
			-->
		</xsl:apply-templates>
		<xsl:value-of select="$nl"/>
		<xsl:text>&gt;</xsl:text>
		<xsl:apply-templates mode="source-reuse-child"/>
		<xsl:text>&lt;/</xsl:text>
		<xsl:value-of select="name()"/>
		<xsl:text>&gt;</xsl:text>
	</xsl:template>

	<xsl:template match="@*" mode="source-reuse-child">
		<!-- Include attributes that are not explicitly matched. -->
		<xsl:copy/>
	</xsl:template>

	<xsl:template match="@*" mode="source-reuse-serialize">
		<!-- Serialize the attribute information. -->
		<xsl:value-of select="$nl"/>
		<xsl:value-of select="name()"/>
		<xsl:text>="</xsl:text>
		<xsl:value-of select="."/>
		<xsl:text>"</xsl:text>
	</xsl:template>


	<!-- Text Normalization -->

	<xsl:template match="*[@xml:space='preserve']/text()" mode="source-reuse-child" priority="5">
		<!-- Not normalizing space when xml:space is set. -->
		<!-- Can not determine normalize from schema, will have to be acceptable -->
		<!-- Important to use source instead of source-reuse for replacement. -->
		<xsl:value-of select="."/>
	</xsl:template>

	<xsl:template match="text()" mode="source-reuse-child" priority="1">
		<!-- Text: Normalize each text node. -->
		<xsl:value-of select="normalize-space(.)"/>
	</xsl:template>

	<xsl:template match="text()[starts-with(., ' ')]" mode="source-reuse-child" priority="3">
		<!-- Text: Normalize each text node. Preserve a starting space.-->
		<!-- This template also matches white-space only and presents a single space. -->
		<xsl:value-of select="concat(' ', normalize-space(.))"/>
	</xsl:template>

	<xsl:template match="text()[ends-with(., ' ')  and normalize-space(.) != '']"
		mode="source-reuse-child" priority="3">
		<!-- Text: Normalize each text node. Preserve a trailing space. -->
		<xsl:value-of select="concat(normalize-space(.), ' ')"/>
	</xsl:template>

	<xsl:template
		match="text()[starts-with(., ' ') and ends-with(., ' ') and normalize-space(.) != '']"
		mode="source-reuse-child" priority="4">
		<!-- Text: Normalize each text node. Preserve both starting and trailing space. -->
		<!-- This template should NOT match '  ' (two+ spaces). -->
		<xsl:value-of select="concat(' ', normalize-space(.), ' ')"/>
	</xsl:template>


	<xsl:template name="pi-serialize">
		<xsl:text>&lt;?</xsl:text>
		<xsl:value-of select="name()"/>
		<xsl:text> </xsl:text>
		<!-- Suppressing RIDs to improve pretranslate use of translations prepared with early 2.0 adapters. -->
		<xsl:value-of select="normalize-space(replace(., 'cms:rid=&quot;[a-z0-9]{15}&quot;', ''))"/>
		<xsl:text>?&gt;</xsl:text>
	</xsl:template>

</xsl:stylesheet>
