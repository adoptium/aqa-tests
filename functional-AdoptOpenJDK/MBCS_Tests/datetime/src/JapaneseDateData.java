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

import java.time.chrono.JapaneseChronology;
import java.time.chrono.JapaneseDate;
import java.time.chrono.JapaneseEra;
import java.time.chrono.Era;
import java.time.chrono.IsoEra;
import java.util.ArrayList;

public class JapaneseDateData{

    public ArrayList<PairOfDate> dateList;
    public ArrayList<PairOfDate> invalidList;

    public JapaneseDateData(){
        dateList = new ArrayList<PairOfDate>();
        // dateList should be sorted from future to past.
        dateList.add(new PairOfDate(JapaneseEra.HEISEI, 25,
                                    2, 28, 2013, false));
        dateList.add(new PairOfDate(JapaneseEra.HEISEI, 12,
                                    2, 29, 2000, true));
        dateList.add(new PairOfDate(JapaneseEra.HEISEI, 1,
                                    1, 8, 1989, false));
        dateList.add(new PairOfDate(JapaneseEra.SHOWA, 64,
                                    1, 7, 1989, false));
        dateList.add(new PairOfDate(JapaneseEra.SHOWA, 1,
                                    12, 25, 1926, false));
        dateList.add(new PairOfDate(JapaneseEra.TAISHO, 15,
                                    12, 24, 1926, false));
        dateList.add(new PairOfDate(JapaneseEra.TAISHO, 1,
                                    7, 30, 1912, true));
        dateList.add(new PairOfDate(JapaneseEra.MEIJI, 45,
                                    7, 29, 1912, true));
        dateList.add(new PairOfDate(JapaneseEra.MEIJI, 6,
                                    1, 1, 1873, false));
        // invalid
        invalidList = new ArrayList<PairOfDate>();
        invalidList.add(new PairOfDate(JapaneseEra.HEISEI, 1,
                                    1, 7, 1989, false));
        invalidList.add(new PairOfDate(JapaneseEra.HEISEI, 0,
                                    1, 1, 1988, true));
        invalidList.add(new PairOfDate(JapaneseEra.HEISEI, -1,
                                    1, 1, 1987, false));
        invalidList.add(new PairOfDate(JapaneseEra.SHOWA, 64,
                                    1, 8, 1989, false));
        invalidList.add(new PairOfDate(JapaneseEra.SHOWA, 1,
                                    12, 24, 1926, false));
        invalidList.add(new PairOfDate(JapaneseEra.SHOWA, 0,
                                    12, 24, 1925, false));
        invalidList.add(new PairOfDate(JapaneseEra.SHOWA, -1,
                                    12, 24, 1924, false));
        invalidList.add(new PairOfDate(JapaneseEra.TAISHO, 15,
                                    12, 25, 1912, true));
        invalidList.add(new PairOfDate(JapaneseEra.TAISHO, 1,
                                    7, 29, 1912, true));
        invalidList.add(new PairOfDate(JapaneseEra.TAISHO, 0,
                                    7, 29, 1911, false));
        invalidList.add(new PairOfDate(JapaneseEra.TAISHO, -1,
                                    7, 29, 1910, false));
        invalidList.add(new PairOfDate(JapaneseEra.MEIJI, 45,
                                    7, 30, 1912, true));
        // Only meiji 6 and later are supported.
        // Because, before meiji 5, Gregorio calender is not used.
        invalidList.add(new PairOfDate(JapaneseEra.MEIJI, 5,
                                    12, 31, 1872, true));
        invalidList.add(new PairOfDate(JapaneseEra.MEIJI, 1,
                                    1, 1, 1868, true));
        invalidList.add(new PairOfDate(JapaneseEra.MEIJI, 0,
                                    1, 1, 1867, false));
        invalidList.add(new PairOfDate(JapaneseEra.MEIJI, -1,
                                    1, 1, 1866, false));
        invalidList.add(new PairOfDate(IsoEra.CE, 2013,
                                    1, 1, 2013, false));
    }
}

