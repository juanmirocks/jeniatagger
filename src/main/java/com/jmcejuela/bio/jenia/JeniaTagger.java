package com.jmcejuela.bio.jenia;

import static com.jmcejuela.bio.jenia.util.Util.tokenize;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import com.jmcejuela.bio.jenia.common.Sentence;
import com.jmcejuela.bio.jenia.common.Token;
import com.jmcejuela.bio.jenia.maxent.ME_Model;
import com.jmcejuela.bio.jenia.util.Util;

public class JeniaTagger {

  // // public static final int POS_TAGGING = 1; always included, as the rest depend on it
  // public static final int SHALLOW_PARSING = 2;
  // public static final int BASE_FORM = 4;
  // public static final int NER = 8;
  // public static final int ALL_ANALYSES = SHALLOW_PARSING | BASE_FORM | NER;

  static final ArrayList<ME_Model> posModels;
  static final ArrayList<ME_Model> chunkingModels;

  static {
    posModels = Util.newArrayList(16, ME_Model.CONSTRUCTOR);
    for (int i = 0; i < 16; i++) {
      posModels.get(i).load_from_file("/models_medline/model.bidir." + i);
    }
  }

  static {
    chunkingModels = Util.newArrayList(8, ME_Model.CONSTRUCTOR);
    for (int i = 0; i < 8; i += 2) {
      chunkingModels.get(i).load_from_file("/models_chunking/model.bidir." + i);
    }
  }

  // Implicit in MorphDic
  // static {
  // MorphDic.init();
  // }

  // Implicit in NamedEntity
  // static {
  // NamedEntity.init();
  // }

  public static Sentence analyzeAll(final String line, boolean dont_tokenize) {
    Sentence sentence = analyzePos(line, dont_tokenize);

    Chunking.bidir_chunking_decode_beam(sentence, chunkingModels);
    setBaseForm(sentence);
    NamedEntity.netagging(sentence);

    return sentence;
  }

  public static Sentence analyzePosAndChunk(final String line, boolean dont_tokenize) {
    Sentence sentence = analyzePos(line, dont_tokenize);

    Chunking.bidir_chunking_decode_beam(sentence, chunkingModels);

    return sentence;
  }

  public static Sentence analyzePos(final String line, boolean dont_tokenize) {
    if (line.matches(".*[\n\r\u0085\u2028\u2029].*"))
      throw new IllegalArgumentException("The input line cannot have any line terminator");
    String trimmedLine = line.trim();
    if (trimmedLine.isEmpty()) return new Sentence();

    Sentence sentence = createSentence(line, dont_tokenize, trimmedLine);
    Bidir.bidir_decode_beam(sentence, posModels);
    return sentence;
  }

  private static Sentence createSentence(final String line, boolean dont_tokenize, String trimmedLine) {
    final List<String> tokens = (dont_tokenize) ?
        Arrays.asList(trimmedLine.split("\\s+")) // jenia: see genia's README
        :
        tokenize(line);

    Sentence sentence = new Sentence(tokens.size());
    for (String tokenText : tokens) {
      // s = ParenConverter.Ptb2Pos(s);
      sentence.add(new Token(tokenText));
    }
    return sentence;
  }

  private static void setBaseForm(Sentence sentence) {
    for (Token t : sentence) {
      t.baseForm = MorphDic.base_form(t.text, t.pos);
    }
  }
}
