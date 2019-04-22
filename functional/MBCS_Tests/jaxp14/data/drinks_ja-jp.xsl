<?xml version="1.0" encoding="Shift_JIS"?>
<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform" version="1.0">
<xsl:output method="html"/>

<xsl:template match="/">
   <html lang="ja">
      <head>
         <style type="text/css">
            table{border:solid 1pt black;}
            th{background-color:orange;padding:5pt;white-space:nowrap;}
            td{border:solid 1pt gray;padding:10pt;font-size:small;white-space:nowrap;}
         </style>
      </head>
      <body>
         <xsl:apply-templates select="飲料データベース" />
      </body>
   </html>
</xsl:template>

<xsl:template match="飲料データベース">
   <h3>飲料データベース</h3>
   <xsl:for-each select="飲料">
   <xsl:sort select="種類" />
      <p><table>
         <tr>
            <th>種類</th>
            <th>名前</th>
            <th>メーカー名</th>
            <th>値段</th>
            <th>内容量</th>
         </tr>
         <tr>
            <td><xsl:value-of select="種類"/></td>
            <td><xsl:value-of select="名前"/></td>
            <td><xsl:value-of select="メーカー名"/></td>
            <td><xsl:value-of select="値段"/> <xsl:value-of select="値段/@通貨"/></td>
            <td><xsl:value-of select="内容量"/> <xsl:value-of select="内容量/@単位"/></td>
         </tr>
      </table></p>
   </xsl:for-each>
</xsl:template>

</xsl:stylesheet>