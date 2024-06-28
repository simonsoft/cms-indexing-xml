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
                exclude-result-prefixes="xs"
                version="3.0">

    <xsl:param name="properties" as="attribute()*"/>

    <xsl:output method="xml" indent="yes"/>

    <xsl:template match="subjectScheme">

        <xsl:variable name="subjects" select="//enumerationdef[elementdef[@name='cms:property']][subjectdef[@keyref]]"/>

        <xsl:variable name="titles" select="//subjectdef[subjectdef[topicmeta[navtitle]]]"/>

        <xsl:element name="doc">

            <xsl:for-each select="$properties">

                <xsl:variable name="property" select="." as="attribute()"/>

                <xsl:element name="field">

                    <xsl:variable name="property-name" select="name($property)" as="xs:string"/>

                    <xsl:variable name="subject-key" select="$subjects[attributedef[@name=translate($property-name, '.', ':')]]/subjectdef/@keyref"/>

                    <xsl:variable name="propname" select="./attributedef/@name" as="xs:string"/>
                    <xsl:variable name="propname-nocolon" select="translate($propname, ':', '.')"/>

                    <xsl:attribute name="name">
                        <xsl:value-of select="$property-name"/>
                    </xsl:attribute>

                    <xsl:for-each select="tokenize($property, ' ')">

                        <xsl:variable name="property-value" select="." as="xs:string"/>

                        <xsl:variable name="property-value-title" select="$titles[@keys=$subject-key]/subjectdef[@keys=$property-value]/topicmeta/navtitle"/>

                        <xsl:choose>
                            <xsl:when test="$property-value-title">
                                <xsl:value-of select="$property-value-title"/>
                            </xsl:when>
                            <xsl:otherwise>
                                <xsl:value-of select="$property-value"/>
                            </xsl:otherwise>
                        </xsl:choose>

                    </xsl:for-each>

                </xsl:element>

            </xsl:for-each>

        </xsl:element>

    </xsl:template>

</xsl:stylesheet>
