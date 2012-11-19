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

  static final ArrayList<ME_Model> posModels;
  static final ArrayList<ME_Model> chunkingModels;

  static {
    posModels = Util.newArrayList(16, ME_Model.CONSTRUCTOR);
    for (int i = 0; i < 16; i++) {
      posModels.get(i).load_from_file("/models_medline/model.bidir." + i);
    }

    chunkingModels = Util.newArrayList(8, ME_Model.CONSTRUCTOR);
    for (int i = 0; i < 8; i += 2) {
      chunkingModels.get(i).load_from_file("/models_chunking/model.bidir." + i);
    }

    MorphDic.init();
    NamedEntity.init();
  }

  public static Sentence analyze(final String line, boolean dont_tokenize) {
    if (line.matches(".*[\n\r\u0085\u2028\u2029].*"))
      throw new IllegalArgumentException("The input line cannot have any line terminator");
    String trimmedLine = line.trim();
    if (trimmedLine.isEmpty()) return new Sentence();

    Sentence sentence = createSentence(line, dont_tokenize, trimmedLine);

    Bidir.bidir_decode_beam(sentence, posModels);
    Chunking.bidir_chunking_decode_beam(sentence, chunkingModels);
    setBaseForm(sentence);
    NamedEntity.netagging(sentence);

    return sentence;
  }

  private static Sentence createSentence(final String line, boolean dont_tokenize, String trimmedLine) {
    final List<String> lt = (dont_tokenize) ?
        Arrays.asList(trimmedLine.split("\\s+")) // jenia: see genia's README
        :
        tokenize(line);

    Sentence sentence = new Sentence(lt.size());
    for (String slt : lt) {
      // s = ParenConverter.Ptb2Pos(s);
      sentence.add(new Token(slt));
    }
    return sentence;
  }

  private static void setBaseForm(Sentence sentence) {
    for (Token t : sentence) {
      t.baseForm = MorphDic.base_form(t.text, t.pos);
    }
  }
}
