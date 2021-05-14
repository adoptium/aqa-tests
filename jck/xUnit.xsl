<?xml version="1.0"?>
<xsl:stylesheet version="1.0" xmlns:xsl="http://www.w3.org/1999/XSL/Transform">
<xsl:output method="xml" indent="yes"/>
<xsl:strip-space elements="*"/>

<xsl:template match="/">
	<xsl:element name="testsuite">
		<xsl:attribute name="name"> 
			<xsl:value-of select="//StandardValues/Tests/Item/@value"/>
		</xsl:attribute>
		<xsl:attribute name="timestamp">2019-10-29T13:35:54</xsl:attribute>
		<xsl:attribute name="hostname">localhost</xsl:attribute>
		<xsl:attribute name="tests">
			<xsl:value-of select="count(//TestResult)" />
		</xsl:attribute>
		<xsl:attribute name="failures">
			<xsl:value-of select="count(//TestResult[@status='FAILED'])"/>
		</xsl:attribute>
		<xsl:attribute name="errors">
			<xsl:value-of select="count(//TestResult/Sections/Section/Output[contains(normalize-space(.), 'Error:')])"/>
		</xsl:attribute>
		<xsl:attribute name="time">
			<xsl:value-of select="format-number(sum(Report/TestResults/TestResult/ResultProperties/Property[@name='totalTime']/@value) div 1000, '###,###.000')" />
		</xsl:attribute>
		
 		<properties>
			<xsl:for-each select="//Environment/Property"> 
	 			<xsl:element name="property"> 
	     			<xsl:attribute name="name"> 
	     				<xsl:value-of select="@name"/>
	     			</xsl:attribute> 
	     			<xsl:attribute name="value"> 
	     				<xsl:value-of select="@value"/>
	     			</xsl:attribute> 
	   			</xsl:element>
			</xsl:for-each>
		</properties>
		
		<xsl:for-each select="//TestResult">
			<xsl:element name="testcase">
				<xsl:attribute name="classname"> 
					<xsl:value-of select="@url"/>
				</xsl:attribute>
				<xsl:attribute name="name"> 
					<xsl:value-of select="DescriptionData/Property[@name='title']/@value"/>
				</xsl:attribute>
				<xsl:attribute name="time"> 
					<xsl:value-of select="format-number(ResultProperties/Property[@name='totalTime']/@value div 1000, '###,###.000')"/>
				</xsl:attribute>
				<xsl:attribute name="time"> 
					<xsl:value-of select="format-number(ResultProperties/Property[@name='totalTime']/@value div 1000, '###,###.000')"/>
				</xsl:attribute>
				<xsl:if test="self::node()[@status='FAILED']">
					<xsl:element name="failure">
						<xsl:value-of select="Sections/Section[@status='FAILED']/Output[@title='messages']"/>
						<xsl:value-of select="Sections/Section[@status='FAILED']/Output[@title='out1']"/>
						<xsl:value-of select="Sections/Section[@status='FAILED']/Output[@title='out2']"/> 
					</xsl:element>
				</xsl:if>
			</xsl:element>
		</xsl:for-each>
		<system-out/>
		<system-err/>
	</xsl:element>
</xsl:template>
</xsl:stylesheet>
