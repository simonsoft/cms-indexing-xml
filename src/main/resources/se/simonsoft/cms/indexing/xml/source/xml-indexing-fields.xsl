<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet version="1.0"
	xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
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
			
		</doc>
		
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