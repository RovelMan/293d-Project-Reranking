import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.util.List;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Arrays;
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
import org.apache.lucene.search.Explanation;
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
import org.apache.lucene.search.BooleanQuery;
import org.apache.lucene.search.BooleanClause;
import org.apache.lucene.search.TermQuery;
import org.apache.lucene.index.Term;
import org.apache.lucene.search.DoubleValuesSource;
/** Simple command-line based search demo. */
public class BatchSearch {
	public static List<String> store_simf = new ArrayList<String>();
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

		

		// creating the reader to read from our indexing
		IndexReader reader = DirectoryReader.open(FSDirectory.open(new File(index).toPath()));
		// creating a searcher to search through the indexes https://www.tutorialspoint.com/lucene/lucene_indexsearcher.htm
		IndexSearcher searcher = new IndexSearcher(reader);
		// analyser to tokennize stream
		Analyzer analyzer = new StandardAnalyzer();
		// initiate reader of the user quereies
		BufferedReader in = null;
		if (queries != null) { // if not spesified from the commmand line:
			in = new BufferedReader(new InputStreamReader(new FileInputStream(queries), "UTF-8"));
		} else { // use default hardcoded queries folder
			in = new BufferedReader(new InputStreamReader(new FileInputStream("queries"), "UTF-8"));
		}
		// initiate queryparser to parse the actual queries one by one
		QueryParser parser = new QueryParser("contents", analyzer);
		QueryParser title_parser = new QueryParser("title", analyzer);
		QueryParser body_parser = new QueryParser("body", analyzer);
		QueryParser cn_parser = new QueryParser("cn",analyzer);

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
			//result is  on form +title:"full query" body:"full query"
			Query query1 = title_parser.parse('"'+line+ '"');
			Query query2 = body_parser.parse('"'+line+ '"');
			BooleanQuery booleanQuery = new BooleanQuery.Builder()
			.add(query1, BooleanClause.Occur.SHOULD)
			.add(query2, BooleanClause.Occur.SHOULD)
			.build();
			*/

			//result is title: full title: query  body:full body:query
			Query query = parser.parse(line);
			Query query_title = title_parser.parse(line);
			Query query_body = body_parser.parse(line);
			Query query_cn = cn_parser.parse(line);

			BooleanQuery boolean_title = new BooleanQuery.Builder()
			.add(query_title, BooleanClause.Occur.SHOULD)
			.build();
			BooleanQuery boolean_body = new BooleanQuery.Builder()
			.add(query_body, BooleanClause.Occur.SHOULD)
			.build();
			BooleanQuery boolean_cn = new BooleanQuery.Builder()
			.add(query_cn, BooleanClause.Occur.SHOULD)
			.build();
			// BooleanQuery
			
			List<double[]> features = new ArrayList<double[]>();
			String[] simfunctions = {"default","bm25","dfr", "lm"};
			Similarity simfn = null;
			for (int i = 0; i < simfunctions.length; i++) {
				simstring = simfunctions[i];
				if ("default".equals(simfunctions[i])) {
					simfn = new ClassicSimilarity();
				} else if ("bm25".equals(simfunctions[i])) {
					simfn = new BM25Similarity();
				} else if ("dfr".equals(simfunctions[i])) {
					simfn = new DFRSimilarity(new BasicModelP(), new AfterEffectL(), new NormalizationH2());
				} else if ("lm".equals(simfunctions[i])) {
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
				// set similarity function TODO: do actual math
				searcher.setSimilarity(simfn);
				features.add(doBatchSearch(in, searcher, pair[0], query, simstring));
				features.add(doBatchSearch(in, searcher, pair[0], boolean_title, simstring));
				features.add(doBatchSearch(in, searcher, pair[0], boolean_body, simstring));
				features.add(doBatchSearch(in, searcher, pair[0], boolean_cn, simstring));
				
			}

			System.out.println(features.size());
			int relevance_label = 0;
			String query_id = pair[0];
			String print_string = relevance_label + " qid:" + query_id;
			int counter = 1;
			for (int i = 0; i < features.size(); i++) {
				if (i==0||i==1||i==2||i==3) {
					for (int j = 0; j < features.get(0).length-1; j+=4) {
						// System.out.println(Arrays.toString(features.get(i));
						//tfidf score
						print_string+=(" "+counter+":"+features.get(i)[j]);
						counter+=1;
						//tf score
						print_string+=(" "+counter+":"+features.get(i)[j+1]);
						counter+=1;
						//idf score
						print_string+=(" "+counter+":"+features.get(i)[j+2]);
						counter+=1;
						//dl
						print_string+=(" "+counter+":"+features.get(i)[j+3]);
						counter+=1;
					}	
				}
				for (int j = 0; j < features.get(5).length-1; j++) {
					//other sim function scores
					print_string+=(" "+counter+":"+features.get(i)[j]);
					counter+=1;
				}
									
			}
			print_string+=(" "+counter+":"+features.get(0)[3]);
			// print_string+=("#docid" = FT941-sim_ 1);
			//whole
			// double[] tf_w = new double[features.get(0).length/4];
			// double[] idf_w = new double[features.get(0).length/4];
			// double[] tfidf_w = new double[features.get(0).length/4];
			// double[] dl = new double[features.get(0).length/4];
			// //title
			// double[] tf_t = new double[features.get(0).length/4];
			// double[] idf_t = new double[features.get(0).length/4];
			// double[] tfidf_t = new double[features.get(0).length/4];
			// //body
			// double[] tf_b = new double[features.get(0).length/4];
			// double[] idf_b = new double[features.get(0).length/4];
			// double[] tfidf_b = new double[features.get(0).length/4];
			// //country
			// double[] tf_cn = new double[features.get(0).length/4];
			// double[] idf_cn = new double[features.get(0).length/4];
			// double[] tfidf_cn = new double[features.get(0).length/4];

			// System.out.println(features.get(0).length);
			// int counter = 0;
			// for (int i = 0; i < (features.get(0).length-1); i=i+4) {
			// 	tfidf_w[counter] = features.get(0)[i];
			// 	tf_w[counter] = features.get(0)[i+1];
			// 	idf_w[counter] = features.get(0)[i+2];
			// 	dl[counter] = features.get(0)[i+3];
			// 	//title
			// 	tfidf_t[counter] = features.get(1)[i];
			// 	tf_t[counter] = features.get(1)[i+1];
			// 	idf_t[counter] = features.get(1)[i+2];
			// 	//body
			// 	tfidf_b[counter] = features.get(2)[i];
			// 	tf_b[counter] = features.get(2)[i+1];
			// 	idf_b[counter] = features.get(2)[i+2];
			// 	//cn
			// 	tfidf_cn[counter] = features.get(3)[i];
			// 	tf_cn[counter] = features.get(3)[i+1];
			// 	idf_cn[counter] = features.get(3)[i+2];
			// 	counter++;
			// }
			// for (int i = 0; i < tfidf.length; i++) {
			// 	// Document length
			// 	int relevance_label = 0;
			// 	String query_id = pair[0];
			// 	//tf
			// 	double feature_TF_w = tf_w[i];
			// 	double feature_TF_t = tf_t[i];
			// 	double feature_TF_b = tf_b[i];
			// 	double feature_TF_cn = tf_cn[i];
			// 	//idf
			// 	double feature_IDF_w = idf_w[i];
			// 	double feature_IDF_t = idf_t[i];
			// 	double feature_IDF_t = idf_b[i];
			// 	double feature_IDF_cn = idf_c[i];
			// 	//tfidf
			// 	double feature_TF_IDF_w = tfidf_w[i];
			// 	double feature_TF_IDF_t = tfidf_t[i];
			// 	double feature_TF_IDF_b = tfidf_b[i];
			// 	double feature_TF_IDF_cn = tfidf_cn[i];
			// 	//bm25
			// 	double feature_BM25_w = features.get(4)[i];
			// 	double feature_BM25_t = features.get(5)[i];
			// 	double feature_BM25_b = features.get(6)[i];
			// 	double feature_BM25_cn = features.get(7)[i];
			// 	//dfr
			// 	double feature_DFR_w = features.get(8)[i];
			// 	double feature_DFR_t = features.get(9)[i];
			// 	double feature_DFR_b = features.get(10)[i];
			// 	double feature_DFR_cn = features.get(11)[i];
			// 	//LM
			// 	double feature_LM_w = features.get(12)[i];
			// 	double feature_LM_t = features.get(13)[i];
			// 	double feature_LM_b = features.get(14)[i];
			// 	double feature_LM_cn = features.get(15)[i];
			// 	//dl
			// 	double feature_DL = dl[i];
			// 	// print format here
			// 	// <line> .=. <target> qid:<qid> <feature>:<value> <feature>:<value> ... <feature>:<value> # <info>
			// 	System.out.println(relevance_label + " qid:" + query_id +
			// 			" 1:" + feature_TF_w +
			// 			" 2:" + feature_TF_t +
			// 			" 3:" + feature_TF_b +
			// 			" 4:" + feature_TF_cn +
			// 			" 5:" + feature_IDF_w +
			// 			" 6:" + feature_IDF_t +
			// 			" 7:" + feature_IDF_b +
			// 			" 8:" + feature_IDF_cn +
			// 			" 9:" + feature_TF_IDF_w +
			// 			" 10:" + feature_TF_IDF_t +
			// 			" 11:" + feature_TF_IDF_b +
			// 			" 12:" + feature_TF_IDF_cn + 
			// 			" 13:" + feature_BM25_w +
			// 			" 14:" + feature_BM25_t +
			// 			" 15:" + feature_BM25_b +
			// 			" 16:" + feature_BM25_cn +
			// 			" 5:" + feature_DFR +
			// 			" 5:" + feature_DFR + 
			// 			" 5:" + feature_DFR + 
			// 			" 5:" + feature_DFR + 
			// 			" 6:" + feature_LM + 
			// 			" 7:" + feature_DL + 
			// 			" #" +
			// 			
			// }
				
			//print whole shit
			Date end = new Date();
			time = time + (end.getTime() - start.getTime());
			query_count = query_count + 1;
		}
		Date end_total = new Date();
		System.out.println("\n" +time + " total milliseconds spendt"); // print time spent indexing to user
		System.out.println(query_count + " total queries"); // print time spent indexing to user
		System.out.println(time/query_count + " average milliseconds spendt per query"); // print time spent indexing to user
		//System.out.println((end_total.getTime() - start_total.getTime())/query_count + " average milliseconds spendt per query"); // print time spent indexing to user


		reader.close();
	}

	public static double[] doBatchSearch(BufferedReader in, IndexSearcher searcher, String qid, Query query, String runtag)
			throws IOException {
		//System.out.println("numTotalHits");
		System.out.println("feature");
		/*****
		 *
		 * MAKE A BOOLEAN QUERY HERE SOMEWHERE
		 * oboleanClause.Occur.Must means that the clause is compulsory
		 * BooleanQuery booleanQuery = new BooleanQuery();
		 * booleanQuery.add(query);
		 *
		*****/


		// Represents hits returned by IndexSearcher.search(Query,int).

		// Can be 0, 1 or 2


		// System.out.println(runtag);
		// double TF =  searcher.termStatistics();
		// double DL = searcher.doc.getLength();
		TopDocs results = searcher.search(query, 1000); // Finds the top 1000 hits for query.
		ScoreDoc[] hits = results.scoreDocs;
		HashMap<String, String> seen = new HashMap<String, String>(1000);
		long numTotalHits = results.totalHits;		
		//System.out.println("	" + numTotalHits);

		//""" set start to 0 and end to min to min hits """
		int start = 0;
		long end = Math.min(numTotalHits, 1000);
		// System.out.println("end: "+(int)end);
		String q_field = query.toString().replaceAll("[()]","").split(":")[0];
		String[] terms = query.toString().replaceAll("[()]","").replaceAll(q_field,"").split(" ");
		//""" Loop through all hits for current query """
		List<Double> result_list = new ArrayList<Double>();
		for (int i = start; i < (int)end; i++) {
			

			// There are duplicate document numbers in the FR collection, so only output a given
			// docno once.
			String explanation = "";	
			result_list.add((double)hits[i].score);
			Document doc = searcher.doc(hits[i].doc);
			String docno = doc.get("docno");
			String doc_arr = doc.get(q_field);
			double doc_len = 0;
			if(q_field.equals("contents")){
				doc_len = doc.toString().length();
			}else if(doc_arr.length() > 1){
				doc_len = doc_arr.length();
			}else{
				doc_len = 0;
			}
			// This might help us?
			//System.out.println("NEW DOCUMENT START");
			//System.out.println("ID: <" + i + ">\tSIMF: <" + runtag.toUpperCase() + ">\tDOCNO: <" + docno + ">");
			explanation = searcher.explain(query, i).toString();
			// System.out.println(explanation);
			
			store_simf.add(docno);
			if (seen.containsKey(docno)) {
				continue;
			}
			seen.put(docno, docno);
			
			if (("default").equals(runtag)) {
				String[] array = explanation.split("\n");
				double tfs = 0.0;
				double idfs = 0.0;
				int counter = 1;
				// System.out.println(explanation);
				for (int j = 1; j < array.length-1; j=j+8){
					if(array.length>1){
						String term = array[j].split(q_field+":")[1].split(" ")[0];
						double tf_score = Double.parseDouble(array[j+2].trim().split(" ")[0]);
						String tfreq = array[j+3].trim().replaceAll("=termFreq="," ").split(" ")[0];
						double idfreq = Double.parseDouble(array[j+4].trim().split(" ")[0]);
						String docFreq = array[j+5].trim().split(" ")[0];
						tfs=tfs+tf_score;
						idfs=idfs+idfreq;
						counter++;
					}
				}
				tfs = tfs/counter;
				idfs = idfs/counter;
				result_list.add(tfs);
				result_list.add(idfs);
				result_list.add(doc_len);
			} else if (("bm25").equals(runtag)) {
				// BM25 score
			} else if (("dfr").equals(runtag)) {
				// DFR score
			} else if (("lm").equals(runtag)) {
				// 
			} else {
				//nothing
			}
		}
		int result_size = result_list.size();
		double[] res_list = new double[result_size];
		for (int j = 0; j < result_size; j++) {
			res_list[j] = result_list.get(j);
			// System.out.println(runtag+"   "+res_list[j]);
		}
		return res_list;
	}

	private static String tokenizeStopStem(String input, boolean stemming) throws Exception {

		File file = new File("./test-data/stop_words.txt");
		BufferedReader br = new BufferedReader(new FileReader(file));
		Collection<String> stop_word_list = new ArrayList<String>();
		String line;
		while ((line = br.readLine()) != null) {
			stop_word_list.add(line);
		}

		CharArraySet stop_word_data = new CharArraySet(stop_word_list, false);
		Analyzer analyzer = new StandardAnalyzer();
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

		return sb.toString();
	}

}
