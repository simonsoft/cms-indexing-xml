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
<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
                xmlns:xs="http://www.w3.org/2001/XMLSchema"
                xmlns:map="http://www.w3.org/2005/xpath-functions/map"
                exclude-result-prefixes="xs map"
                version="3.0">

    <xsl:param name="properties" as="map(xs:string, xs:string)"/>

    <xsl:output method="xml" indent="yes"/>

    <xsl:template match="subjectScheme">

        <xsl:variable name="root" select="."/>
        <xsl:variable name="subjects" select="//enumerationdef[elementdef[@name='cms:property']][subjectdef[@keyref]]"/>

        <xsl:element name="doc">

            <xsl:for-each select="map:keys($properties)">

                <xsl:variable name="property-name" select="." as="xs:string"/>
                <xsl:variable name="property-name-nocolon" select="translate($property-name, ':', '.')"/>
                <xsl:variable name="property-values" select="tokenize(map:get($properties, $property-name), ' ')" as="xs:string*"/>

                <xsl:variable name="subject-key" select="$subjects[attributedef[@name=$property-name]]/subjectdef/@keyref"/>

                <xsl:for-each select="$property-values">

                    <xsl:variable name="property-value" select="." as="xs:string"/>
                    <xsl:variable name="property-value-title" select="$root//subjectdef[@keys=$subject-key]/descendant::subjectdef[@keys=$property-value]/topicmeta/navtitle"/>

                    <xsl:element name="field">

                        <xsl:attribute name="name">
                            <xsl:value-of select="concat('meta_s_m_prop_', $property-name-nocolon)"/>
                        </xsl:attribute>

                        <xsl:choose>
                            <xsl:when test="$property-value-title">
                                <xsl:value-of select="$property-value-title"/>
                            </xsl:when>
                            <xsl:otherwise>
                                <xsl:value-of select="$property-value"/>
                            </xsl:otherwise>
                        </xsl:choose>

                    </xsl:element>

                </xsl:for-each>

            </xsl:for-each>

        </xsl:element>

    </xsl:template>

</xsl:stylesheet>
