<?xml version="1.0" encoding="UTF-8"?>
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
         <xsl:apply-templates select="음료데타베스" />
      </body>
   </html>
</xsl:template>

<xsl:template match="음료데타베스">
   <h3>음료데타베스</h3>
   <xsl:for-each select="음료">
   <xsl:sort select="종류" />
      <p><table>
         <tr>
            <th>종류</th>
            <th>이름</th>
            <th>메카이름</th>
            <th>값</th>
            <th>내용량</th>
         </tr>
         <tr>
            <td><xsl:value-of select="종류"/></td>
            <td><xsl:value-of select="이름"/></td>
            <td><xsl:value-of select="메카이름"/></td>
            <td><xsl:value-of select="값"/> <xsl:value-of select="값/@통화"/></td>
            <td><xsl:value-of select="내용량"/> <xsl:value-of select="내용량/@단위"/></td>
         </tr>
      </table></p>
   </xsl:for-each>
</xsl:template>

</xsl:stylesheet>
