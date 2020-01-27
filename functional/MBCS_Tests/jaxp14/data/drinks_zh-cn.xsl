<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform" version="1.0">
<xsl:output method="html"/>

<xsl:template match="/">
   <html lang="zh-CN">
      <head>
         <style type="text/css">
            table{border:solid 1pt black;}
            th{background-color:orange;padding:5pt;white-space:nowrap;}
            td{border:solid 1pt gray;padding:10pt;font-size:small;white-space:nowrap;}
         </style>
      </head>
      <body>
         <xsl:apply-templates select="抎駡U郂" />
      </body>
   </html>
</xsl:template>

<xsl:template match="抎駡U郂">
   <h3>抎駡U郂</h3>
   <xsl:for-each select="丁七">
   <xsl:sort select="name" />
      <p><table>
         <tr>
            <th>name</th>
            <th>椷</th>
            <th>maker</th>
            <th>椷中</th>
            <th>content</th>
         </tr>
         <tr>
            <td><xsl:value-of select="name"/></td>
            <td><xsl:value-of select="椷"/></td>
            <td><xsl:value-of select="maker"/></td>
            <td><xsl:value-of select="椷中"/>  <xsl:value-of select="椷中/@currency"/></td>
            <td><xsl:value-of select="content"/> <xsl:value-of select="content/@unit"/></td>
         </tr>
      </table></p>
   </xsl:for-each>
</xsl:template>

</xsl:stylesheet>
