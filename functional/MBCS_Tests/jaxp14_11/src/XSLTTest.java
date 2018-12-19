/*******************************************************************************
* Licensed under the Apache License, Version 2.0 (the "License");
* you may not use this file except in compliance with the License.
* You may obtain a copy of the License at
*
*      https://www.apache.org/licenses/LICENSE-2.0
*
* Unless required by applicable law or agreed to in writing, software
* distributed under the License is distributed on an "AS IS" BASIS,
* WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
* See the License for the specific language governing permissions and
* limitations under the License.
*******************************************************************************/

import java.util.*;
import java.io.File;
import javax.xml.parsers.*;
import org.w3c.dom.*;
import javax.xml.transform.*;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamSource;
import javax.xml.transform.stream.StreamResult;
import org.xml.sax.SAXParseException;  

public class XSLTTest {

   public static void main(String[] args){
      if(args.length < 2){
         System.out.println("Usage: java XSLTTest <xml file> <xsl file>");
      }else{
         DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
         factory.setNamespaceAware(true);
         Document document = null;
         try {
            DocumentBuilder builder = factory.newDocumentBuilder();
            document = builder.parse( new File(args[0]) );
         } catch (Exception e) {
            e.printStackTrace(System.err);
            System.exit(1);
         }

         if(document != null){
            try{
               TransformerFactory tFactory = TransformerFactory.newInstance();
               StreamSource ssource = new StreamSource(new File(args[1]));
               Transformer transformer = tFactory.newTransformer(ssource);

               DOMSource source = new DOMSource(document);
               StreamResult result = new StreamResult(System.out);
               transformer.transform(source, result);
            } catch (Exception e){
               e.printStackTrace();
               System.exit(1);
            }
         }
      }
   }
}
