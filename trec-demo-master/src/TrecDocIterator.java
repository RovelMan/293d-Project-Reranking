import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.Iterator;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.StringField;
import org.apache.lucene.document.TextField;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.util.List;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.io.Reader;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.io.StringReader;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.search.similarities.*;
import org.apache.lucene.store.FSDirectory;
import org.apache.lucene.util.Version;
import java.util.Date;
import org.apache.lucene.analysis.en.PorterStemFilter;
import org.apache.lucene.analysis.core.StopFilter;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.TokenFilter;
import org.apache.lucene.analysis.tokenattributes.KeywordAttribute;
import org.apache.lucene.analysis.standard.StandardTokenizer;
//import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;

import org.apache.lucene.analysis.core.LowerCaseTokenizer;
import org.apache.lucene.analysis.Tokenizer;
import org.apache.lucene.analysis.tokenattributes.OffsetAttribute;
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;
import org.apache.lucene.analysis.Analyzer.TokenStreamComponents;
//import org.apache.lucene.analysis.util.CharArraySet;
import org.apache.lucene.analysis.CharArraySet;
import org.apache.lucene.document.FloatDocValuesField;


public class TrecDocIterator implements Iterator<Document> {

	protected BufferedReader rdr;
	protected boolean at_eof = false;
	/*
		CLASS THAT READS XML DOCUMENTS, AND ITERATES THROUGH <doc> TAGS WITH .next()
	*/
	public TrecDocIterator(File file) throws FileNotFoundException {
		rdr = new BufferedReader(new FileReader(file));
		System.out.println("Reading " + file.toString());
	}

	@Override
	public boolean hasNext() {
		return !at_eof;
	}
	int counter = 0;
	@Override // Function that read ONE DOC
	public Document next() {
		
		if((counter%1000)==0){
			System.out.println("Have processed "+counter+" documents. Smile");
		}
		counter++;
		Document doc = new Document();
		StringBuffer sb = new StringBuffer();
		try {
			String line;

			// Define prefix to search and match for
			Pattern docno_tag = Pattern.compile("<DOCNO>\\s*(\\S+)\\s*<");
			Pattern title_tag = Pattern.compile("<HEADLINE>(.+?)</HEADLINE>", Pattern.MULTILINE); //Pattern.MULTILINE); // retrieve doc id from <DOCNO> TAG
			Pattern body_tag = Pattern.compile("<TEXT>(.+?)</TEXT>"); //Pattern.MULTILINE); // retrieve doc id from <DOCNO> TAG
			Pattern cn_tag = Pattern.compile("<CN>(.+?)</CN>");

			boolean in_doc = false;
			while (true) {
				line = rdr.readLine();
				if (line == null) {
					at_eof = true;
					break;
				}
				if (!in_doc) {
					if (line.startsWith("<DOC>"))
						in_doc = true;
					else
						continue;
				}
				if (line.startsWith("</DOC>")) {
					in_doc = false;
					sb.append(line);
					break;
				}
				// """ Matching DOCN tag and adding it to the doc"""
				Matcher m = docno_tag.matcher(line);
				if (m.find()) {
					String docno = m.group(1);
					doc.add(new StringField("docno", docno, Field.Store.YES));
					//System.out.print("docneo "+ docno);
				}


				sb.append(line);
			}
			if (sb.length() > 0)
				// System.out.println(sb.toString().toLowerCase());
				// IS THIS FOR INDEXING THE WHOLE DOCUMENT?
				try {
					//""" Matching TITLE tag and adding it to the doc"""
					Matcher t = title_tag.matcher(sb.toString()); //
					if (t.find()) {
						String title = t.group(1);
						String stemmed_title = tokenizeStopStem(title.toLowerCase(), true);

						//System.out.println(stemmed_title);
					 	Field titleField = new TextField("title", stemmed_title, Field.Store.YES);
						doc.add(titleField);
						//doc.add(new FloatDocValuesField("title", 10.0f));
					}

					//""" Matching BODY tag and adding it to the doc"""
					Matcher b = body_tag.matcher(sb.toString()); //
					if (b.find()) {
						String body = b.group(1);
						String stemmed_body = tokenizeStopStem(body.toLowerCase(), true);

						//System.out.print(title);
					 	Field bodyField = new TextField("body", stemmed_body, Field.Store.YES);
						doc.add(bodyField);
						//doc.add(new FloatDocValuesField("body", 10.0f));

					}
					Matcher c = cn_tag.matcher(sb.toString());
					if (c.find()) {
						String cn = c.group(1);
						String stemmed_cn = tokenizeStopStem(cn.toLowerCase(), true);

						//System.out.print(title);
					 	Field cnField = new TextField("cn", stemmed_cn, Field.Store.YES);
						doc.add(cnField);
						//doc.add(new FloatDocValuesField("body", 10.0f));

					}
					String stemmed_content = tokenizeStopStem(sb.toString().toLowerCase(), true);
					doc.add(new TextField("contents", stemmed_content, Field.Store.NO));


				} catch(Exception e) {
					e.printStackTrace();
				}

		} catch (IOException e) {
			doc = null;
		}
		return doc;
	}

	@Override
	public void remove() {
		// Do nothing, but don't complain
	}


	private static String tokenizeStopStem(String input, boolean stemming) throws Exception {

		// Stopwords-file converted to a collection
		File file = new File("./test-data/stop_words.txt");
		BufferedReader br = new BufferedReader(new FileReader(file));
		Collection<String> stop_word_list = new ArrayList<String>();
		String line;
		while ((line = br.readLine()) != null) {
			stop_word_list.add(line);
		 }

		//  CharArraySet(Collection<?> c, boolean ignoreCase)
		CharArraySet stop_word_data = new CharArraySet(stop_word_list, false);

		// A TokenStream enumerates (opplister) the sequence of tokens, either from fields of a Document or from query text
		// A grammar-based tokenizer constructed with JFlex.
		Analyzer analyzer = new StandardAnalyzer();

		//TokenStream tokenStream = new StandardTokenizer(new StringReader(input));
		TokenStream tokenStream = analyzer.tokenStream("contents", new StringReader(input));

		// Stopfilter removes stop words from a token stream
		tokenStream = new StopFilter( tokenStream, stop_word_data);
		if(stemming){
			// Transforms the token stream as per the Porter stemming algorithm. Note: the input to the stemming filter must already be in lower case
			tokenStream = new PorterStemFilter(tokenStream);
		}


		// The start and end character offset of a Token.
		OffsetAttribute offsetAttribute = tokenStream.addAttribute(OffsetAttribute.class);
		// The term text of a Token
		CharTermAttribute charTermAttribute = tokenStream.addAttribute(CharTermAttribute.class);
		StringBuilder sb = new StringBuilder();

		//This method is called by a consumer before it begins consumption using incrementToken().
		tokenStream.reset();

		if (tokenStream != null) {
			while (tokenStream.incrementToken()) {
				String term = charTermAttribute.toString();
				if (sb.length() == 0) {
					sb.append(term);
				} else {
					sb.append(" " + term);
				}
			}

		}
		br.close();
		return sb.toString();
	}

}
