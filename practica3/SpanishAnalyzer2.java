
/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */


import org.apache.lucene.analysis.*;
import org.apache.lucene.analysis.es.SpanishLightStemFilter;
import org.apache.lucene.analysis.miscellaneous.SetKeywordMarkerFilter;
import org.apache.lucene.analysis.snowball.SnowballFilter;
import org.apache.lucene.analysis.standard.StandardTokenizer;

import java.io.FileReader;
import java.io.IOException;
import java.io.Reader;

/**
 * {@link Analyzer} for Spanish.
 *
 * @since 3.1
 */
public final class SpanishAnalyzer2 extends StopwordAnalyzerBase {
    private final CharArraySet stemExclusionSet;

    /** File containing default Spanish stopwords. */
    public final static String DEFAULT_STOPWORD_FILE = "spanish_stop.txt";

    /**
     * Returns a modifiable instance of the teacher default stop words set.
     * (es la funcion createStopSet2() de la practica 1)
     * @return teacher default stop words set.
     */
    public static CharArraySet createStopSet2(){
        String[] stopWords = {"el", "la", "lo", "en"};
        CharArraySet stopSet = StopFilter.makeStopSet(stopWords);
        return stopSet;
    }

    /**
     * Returns an unmodifiable instance of the default stop words set.
     * @return default stop words set.
     */
    public static CharArraySet createStopSet3(){
        try {
            CharArraySet stopSet = WordlistLoader.getSnowballWordSet(
                    new FileReader(DEFAULT_STOPWORD_FILE));
            return stopSet;
        } catch (IOException ex) {
            throw new RuntimeException("Unable to load default stopword set");
        }
    }

    /**
     * Builds an analyzer with the default stop words: {@link #DEFAULT_STOPWORD_FILE}.
     */
    public SpanishAnalyzer2() {
        this(createStopSet3(), CharArraySet.EMPTY_SET);
    }

    /**
     * Builds an analyzer with the given stop words.
     *
     * @param stopwords a stopword set
     */
    public SpanishAnalyzer2(CharArraySet stopwords) {
        this(stopwords, CharArraySet.EMPTY_SET);
    }

    /**
     * Builds an analyzer with the given stop words. If a non-empty stem exclusion set is
     * provided this analyzer will add a {@link SetKeywordMarkerFilter} before
     * stemming.
     *
     * @param stopwords a stopword set
     * @param stemExclusionSet a set of terms not to be stemmed
     */
    public SpanishAnalyzer2(CharArraySet stopwords, CharArraySet stemExclusionSet) {
        super(stopwords);
        this.stemExclusionSet = CharArraySet.unmodifiableSet(CharArraySet.copy(stemExclusionSet));
    }

    /**
     * Creates a
     * {@link org.apache.lucene.analysis.Analyzer.TokenStreamComponents}
     * which tokenizes all the text in the provided {@link Reader}.
     *
     * @return A
     *         {@link org.apache.lucene.analysis.Analyzer.TokenStreamComponents}
     *         built from an {@link StandardTokenizer} filtered with
     *         {@link LowerCaseFilter}, {@link StopFilter}
     *         , {@link SetKeywordMarkerFilter} if a stem exclusion set is
     *         provided and {@link SpanishLightStemFilter}.
     */
    @Override
    protected TokenStreamComponents createComponents(String fieldName) {
        final Tokenizer source = new StandardTokenizer();
        TokenStream result = new LowerCaseFilter(source);
        result = new StopFilter(result, stopwords);
        if(!stemExclusionSet.isEmpty())
            result = new SetKeywordMarkerFilter(result, stemExclusionSet);
        result = new SnowballFilter(result, "Spanish");
        return new TokenStreamComponents(source, result);
    }

    @Override
    protected TokenStream normalize(String fieldName, TokenStream in) {
        return new LowerCaseFilter(in);
    }
}
