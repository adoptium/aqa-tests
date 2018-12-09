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

import java.util.Set;
import java.util.ArrayList;
import java.util.Collections;
import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.Messager;
import javax.annotation.processing.RoundEnvironment;
import javax.annotation.processing.SupportedAnnotationTypes;
import javax.annotation.processing.SupportedSourceVersion;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.Element;
import javax.lang.model.element.TypeElement;
import javax.tools.Diagnostic;
import javax.lang.model.element.AnnotationMirror;

@SupportedSourceVersion(SourceVersion.RELEASE_6)
@SupportedAnnotationTypes("*")
public class AnnotationProcessor extends AbstractProcessor
{
    @Override
    public boolean process(
            Set<? extends TypeElement> annotations, RoundEnvironment roundEnv)
    {
       ArrayList<String> list= new ArrayList<String>();
       for(TypeElement annotation : annotations) {
          for(Element element : roundEnv.getElementsAnnotatedWith(annotation)) {
				list.add(String.format(
                                         "annotation : "+element.getAnnotationMirrors().toString()+"%n"+
				         "kind: "+element.getKind().toString()+"%n"+
				         "modifier: "+element.getModifiers().toString()+"%n"+
				         "type: "+element.asType().toString()+"%n"+
				         "element: "+element.toString()+"%n"));
            }
        }
        Collections.sort(list);
        for (String item : list) {
            System.out.println(item);
        }
        return false;
    }
}
