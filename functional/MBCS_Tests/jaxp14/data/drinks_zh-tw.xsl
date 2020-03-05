<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform" version="1.0">
<xsl:output method="html"/>

<xsl:template match="/">
   <html lang="zh-TW">
      <head>
         <style type="text/css">
            table{border:solid 1pt black;}
            th{background-color:orange;padding:5pt;white-space:nowrap;}
            td{border:solid 1pt gray;padding:10pt;font-size:small;white-space:nowrap;}
         </style>
      </head>
      <body>
         <xsl:apply-templates select="膞膗膕聧舖" />
      </body>
   </html>
</xsl:template>

<xsl:template match="膞膗膕聧舖">
   <h3>膞膗膕聧舖</h3>
   <xsl:for-each select="膢膙艎">
   <xsl:sort select="膕" />
      <p><table>
         <tr>
            <th>膕</th>
            <th>type</th>
            <th>maker</th>
            <th>price</th>
            <th>content</th>
         </tr>
         <tr>
            <td><xsl:value-of select="膕"/></td>
            <td><xsl:value-of select="type"/></td>
            <td><xsl:value-of select="maker"/></td>
            <td><xsl:value-of select="price"/>  <xsl:value-of select="price/@currency"/></td>
            <td><xsl:value-of select="content"/> <xsl:value-of select="content/@unit"/></td>
         </tr>
      </table></p>
   </xsl:for-each>
</xsl:template>

</xsl:stylesheet>
