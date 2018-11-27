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
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;
import org.apache.lucene.analysis.core.LowerCaseTokenizer;
import org.apache.lucene.analysis.Tokenizer;
import org.apache.lucene.analysis.tokenattributes.OffsetAttribute;
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;
import org.apache.lucene.analysis.Analyzer.TokenStreamComponents;
import org.apache.lucene.analysis.util.CharArraySet;
import org.apache.lucene.search.BooleanQuery;
import org.apache.lucene.search.BooleanClause;
import org.apache.lucene.search.TermQuery;
import org.apache.lucene.index.Term;

/** Simple command-line based search demo. */
public class BatchSearch {

	private BatchSearch() {
	}

	/** Simple command-line based search demo. */
	public static void main(String[] args) throws Exception {
		String usage = "Usage:\tjava BatchSearch [-index dir] [-simfn similarity] [-field f] [-queries file]";
		if (args.length > 0 && ("-h".equals(args[0]) || "-help".equals(args[0]))) {
			System.out.println(usage);
			System.out.println("Supported similarity functions:\ndefault: DefaultSimilary (tfidf)\n");
			System.exit(0);
		}

		String index = "index";
		String field = "contents";
		String queries = null;
		String simstring = "default";

		for (int i = 0; i < args.length; i++) {
			if ("-index".equals(args[i])) {
				index = args[i + 1];
				i++;
			} else if ("-field".equals(args[i])) {
				field = args[i + 1];
				i++;
			} else if ("-queries".equals(args[i])) {
				queries = args[i + 1];
				i++;
			} else if ("-simfn".equals(args[i])) {
				simstring = args[i + 1];
				i++;
			}
		}

		Similarity simfn = null;
		if ("default".equals(simstring)) {
			simfn = new DefaultSimilarity();
		} else if ("bm25".equals(simstring)) {
			simfn = new BM25Similarity();
		} else if ("dfr".equals(simstring)) {
			simfn = new DFRSimilarity(new BasicModelP(), new AfterEffectL(), new NormalizationH2());
		} else if ("lm".equals(simstring)) {
			simfn = new LMDirichletSimilarity();
		}
		if (simfn == null) {
			System.out.println(usage);
			System.out.println("Supported similarity functions:\ndefault: DefaultSimilary (tfidf)");
			System.out.println("bm25: BM25Similarity (standard parameters)");
			System.out.println("dfr: Divergence from Randomness model (PL2 variant)");
			System.out.println("lm: Language model, Dirichlet smoothing");
			System.exit(0);
		}

		// creating the reader to read from our indexing
		IndexReader reader = DirectoryReader.open(FSDirectory.open(new File(index)));
		// creating a searcher to search through the indexes https://www.tutorialspoint.com/lucene/lucene_indexsearcher.htm
		IndexSearcher searcher = new IndexSearcher(reader);
		// set similarity function TODO: do actual math
		searcher.setSimilarity(simfn);
		// analyser to tokennize stream
		Analyzer analyzer = new StandardAnalyzer(Version.LUCENE_41);
		// initiate reader of the user quereies
		BufferedReader in = null;
		if (queries != null) { // if not spesified from the commmand line:
			in = new BufferedReader(new InputStreamReader(new FileInputStream(queries), "UTF-8"));
		} else { // use default hardcoded queries folder
			in = new BufferedReader(new InputStreamReader(new FileInputStream("queries"), "UTF-8"));
		}
		// initiate queryparser to parse the actual queries one by one
		QueryParser parser = new QueryParser(Version.LUCENE_41, "contents", analyzer);
		QueryParser title_parser = new QueryParser(Version.LUCENE_41, "title", analyzer);
		QueryParser body_parser = new QueryParser(Version.LUCENE_41, "body", analyzer);

		double time = 0.;
		int query_count = 0;
		Date start_total = new Date();
		// while still more queries--> search the current query
		while (true) {
			Date start = new Date();
			String line = in.readLine(); // read querey


			if (line == null || line.length() == -1) { // stop if query is empty
				break;
			}

			line = line.trim(); // delete whitespaces in start and end
			if (line.length() == 0) { // stop length is empty
				break;
			}
			String[] pair = line.split(" ", 2);
			String raw_line = pair[1].toLowerCase();

			line = tokenizeStopStem(raw_line, true); // porter stem and delete stopwords in query
			//line=raw_line;
			if  (line.length() == 0) {
				line = raw_line;
			}

			//////// ALTERNATIVE QUERIES ////////////////////



		//standard query. Without " " it will nonly assign first word to field, all others to contents/ field writton to Queryparser.
		//body:dette contents:er contents:en contents:norsk contents:tittel title:dette contents:er contents:en contents:norsk contents:tittel
		//String special = "body:" + line + " OR title:" +line;
		//Query query = parser.parse(special);

		//boolea query, only showing if it whole query appears in both
		//+body:"dette er en norsk tittel" +title:"dette er en norsk tittel"
		//String special = "body:" + '"' + line +'"' + " AND title:" + '"'+line+'"';
		//Query query = parser.parse(special);

		//boolean query, showing results if whole query appears in ONE OR MORE of the fields
		// body:"dette er en norsk tittel" title:"dette er en norsk tittel"
		//String special = "body:" + '"' + line +'"' + " OR title:" + '"'+line+'"';
		//Query query = parser.parse(special);

		//boolea query, only showing if query appears in at least title DOES NOT WORK WHEN WE DO NOT STEM CONTENT FIELD
		//body:"dette er en norsk tittel" +title:"dette er en norsk tittel"
		//String special = "body:" + '"' + line +'"' + " OR +title:" + '"'+line+'"';
		//Query query = parser.parse(special);

		//////// DEMO QUERIES- play with Occur  /////////////////////
		//Query booleanQuery = parser.parse(line);


/*

		//result is +title:"dette er en norsk tittel" body:"dette er en norsk tittel"
		BooleanQuery booleanQuery = new BooleanQuery();
		Query query1 = title_parser.parse('"'+line+ '"');
		Query query2 = body_parser.parse('"'+line+ '"');
		booleanQuery.add(query1, BooleanClause.Occur.MUST);
		booleanQuery.add(query2, BooleanClause.Occur.SHOULD);
		// Use BooleanClause.Occur.MUST instead of BooleanClause.Occur.SHOULD
		// for AND queries
*/
		//UNCOMMENT FOR BOOLEAN QUERY
		//Boolean query +title:dette body:dette +title:er body:er +title:en body:en +title:norsk body:norsk +title:tittel body:tittel
		BooleanQuery booleanQuery = new BooleanQuery();
		String[] stemmedLineSplitted = line.split(" ");
		for(int i=0; i < stemmedLineSplitted.length; i++){
			Query tQuery = title_parser.parse(stemmedLineSplitted[i]);
			Query bQuery = body_parser.parse(stemmedLineSplitted[i]);
			booleanQuery.add(tQuery, BooleanClause.Occur.SHOULD);
			booleanQuery.add(bQuery, BooleanClause.Occur.SHOULD);
		}
		System.out.println("Query:" + booleanQuery);

		doBatchSearch(in, searcher, pair[0], booleanQuery, simstring);
		Date end = new Date();
		time = time + (end.getTime() - start.getTime());
		query_count = query_count + 1;
		}
		Date end_total = new Date();
		System.out.println("\n" +time + " total milliseconds spendt"); // print time spent indexing to user
		System.out.println(query_count + " total queries"); // print time spent indexing to user
		System.out.println(time/query_count + " average milliseconds spendt per query"); // print time spent indexing to user
		System.out.println((end_total.getTime() - start_total.getTime())/query_count + " average milliseconds spendt per query"); // print time spent indexing to user


		reader.close();
	}

	public static void doBatchSearch(BufferedReader in, IndexSearcher searcher, String qid, Query query, String runtag)
			throws IOException {
		//System.out.println("numTotalHits");

		/*****
		 *
		 * MAKE A BOOLEAN QUERY HERE SOMEWHERE
		 * oboleanClause.Occur.Must means that the clause is compulsory
		 * BooleanQuery booleanQuery = new BooleanQuery();
		 * booleanQuery.add(query);
		 *
		*****/
		
		/** USED FOR GENERATING RANKLIB FILE **/
		// Can be 0, 1 or 2
		int relevance_label = 0;
		int query_id = 0;
		double feature_1_TF = 0.0000;
		double feature_2_IDF = 0.0000;
		double feature_3_TF_IDF = 0.0000;
		double feature_4_BM25 = 0.0000;
		double feature_5_DL = 0.0000; // Document length

		// Represents hits returned by IndexSearcher.search(Query,int).

		TopDocs results = searcher.search(query, 1000); // Finds the top 1000 hits for query.
		ScoreDoc[] hits = results.scoreDocs;
		HashMap<String, String> seen = new HashMap<String, String>(1000);
		int numTotalHits = results.totalHits;
		//System.out.println(query);

		//""" set start to 0 and end to min to min hits """
		int start = 0;
		int end = Math.min(numTotalHits, 1000);


		//""" Loop through all hits for current query """
		for (int i = start; i < end; i++) {
			Document doc = searcher.doc(hits[i].doc);
			String docno = doc.get("docno");
			//System.out.print(doc.get("docno"));
			// There are duplicate document numbers in the FR collection, so only output a given
			// docno once.
			if (seen.containsKey(docno)) {
				continue;
			}
			seen.put(docno, docno);
			/**
			THIS IS PRINT
			**/
			// <line> .=. <target> qid:<qid> <feature>:<value> <feature>:<value> ... <feature>:<value> # <info>
			System.out.println(relevance_label + " qid:" + query_id +
					" 1:" + feature_1_TF +
					" 2:" + feature_2_IDF +
					" 3:" + feature_3_TF_IDF + 
					" 4:" + feature_4_BM25 +
					" 5:" + feature_5_DL + 
					" #" +
					"docid = "+ docno + " " + i + " " + hits[i].score + " " + runtag);
			System.out.println("	--> Title field: " + doc.get("title"));

		}
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
		CharArraySet stop_word_data = new CharArraySet(Version.LUCENE_41, stop_word_list, false);

		// A TokenStream enumerates (opplister) the sequence of tokens, either from fields of a Document or from query text
		// A grammar-based tokenizer constructed with JFlex.
		TokenStream tokenStream = new StandardTokenizer(Version.LUCENE_41, new StringReader(input));

		// Stopfilter removes stop words from a token stream
		tokenStream = new StopFilter(Version.LUCENE_41, tokenStream, stop_word_data);
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

		return sb.toString();
	}

}
