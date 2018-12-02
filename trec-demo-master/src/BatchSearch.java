import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.util.List;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Arrays;
import java.io.Reader;
import java.util.Scanner;
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
	public static List<String[]> store_simf = new ArrayList<String[]>();
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
		File file = new File("./test-data/relevancy_test.txt");
		Scanner sc = new Scanner(file);
		
		List<String[]> relevancy = new ArrayList<String[]>();
		
		while(sc.hasNextLine()) {
			relevancy.add(sc.nextLine().split(" "));
		}
		sc.close();
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
		int tot_count = 0;
		//FOR EACH QUERY
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
			List<Query> query_list = new ArrayList<Query>();
			Query query = parser.parse(line);
			Query query_title = title_parser.parse(line);
			Query query_body = body_parser.parse(line);
			Query query_cn = cn_parser.parse(line);
			query_list.addAll(Arrays.asList(query,query_title,query_body,query_cn));

			List<BooleanQuery> boolean_list = new ArrayList<BooleanQuery>();
			BooleanQuery boolean_title = new BooleanQuery.Builder()
			.add(query_title, BooleanClause.Occur.SHOULD)
			.add(query_body, BooleanClause.Occur.SHOULD)
			.build();
			// BooleanQuery
			
			List<List<double[]>> part_list = new ArrayList<List<double[]>>();
			List<double[]> features_w = new ArrayList<double[]>();
			List<double[]> features_t = new ArrayList<double[]>();
			List<double[]> features_b = new ArrayList<double[]>();
			List<double[]> features_cn = new ArrayList<double[]>();
			String[] simfunctions = {"default","bm25","dfr", "lm"};
			Similarity simfn = null;
			//1 QUERY, ALL SIM FUNCTIONS
			for (int i = 0; i < simfunctions.length; i++) {
				String print_string = "";
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
				

				
			}
			//know that each sim function will return the same amout of documents.
			//number of docs will vary for each query, though
			simfn = new ClassicSimilarity();
			simstring = "default";
			searcher.setSimilarity(simfn);
			String feat = doBatchSearch(in, searcher, pair[0], query_list, simstring,relevancy);
		
			//print whole shit
			System.out.printf(feat);

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

	public static String doBatchSearch(BufferedReader in, IndexSearcher searcher, String qid, List<Query> query_list, String runtag, List<String[]> relevancy)
			throws IOException {

		/*****
		 *
		 * MAKE A BOOLEAN QUERY HERE SOMEWHERE
		 * oboleanClause.Occur.Must means that the clause is compulsory
		 * BooleanQuery booleanQuery = new BooleanQuery();
		 * booleanQuery.add(query);
		 *
		*****/
		// Represents hits returned by IndexSearcher.search(Query,int).

		// TopDocs results = searcher.search(query, 1000); // Finds the top 1000 hits for query.
		// ScoreDoc[] hits = results.scoreDocs;
		// HashMap<String, String> seen = new HashMap<String, String>(1000);
		// long numTotalHits = results.totalHits;
		// long end = Math.min(numTotalHits, 1000);
		List<TopDocs> topdocs = new ArrayList<TopDocs>();
		List<ScoreDoc[]> query_hits = new ArrayList<ScoreDoc[]>();
		List<Long> totalHits = new ArrayList<Long>();
		List<Long> ends = new ArrayList<Long>();
		int max_hits = 100;
		for (Query query : query_list) {
			topdocs.add(searcher.search(query,max_hits));
		}
		//Extracting info for each result
		for (TopDocs result : topdocs) {
			query_hits.add(result.scoreDocs);
			totalHits.add(result.totalHits);
		}

		int start = 0;
		//Adding length of hits for each query
		for (long hits : totalHits) {
			ends.add(Math.min(hits, max_hits));
		}
		//got results for each query: topdocs, hits, seen and total hits. All on the form list.size() = 4
		//""" set start to 0 and end to min to min hits """
		String field = query_list.get(0).toString().replaceAll("[()]","").split(":")[0].split(" ")[0];
		//""" Loop through all hits for all queries """
		int num_docs = ends.get(0).intValue();
		List<Document> docs_found = new ArrayList<Document>();
		List<String> doc_numbers = new ArrayList<String>();
		List<Integer> relevances = new ArrayList<Integer>();
		List<Double> feat_w = new ArrayList<Double>(); 
		List<Double> feat_t = new ArrayList<Double>();
		List<Double> feat_b = new ArrayList<Double>();
		List<Double> feat_cn = new ArrayList<Double>();
		double[] doc_lengths = new double[num_docs];
		for (int i = start; i < ends.get(0); i++) {
			// There are duplicate document numbers in the FR collection, so only output a given
			// docno once.
			Boolean t_exist = false;
			Boolean b_exist = false;
			Boolean cn_exist = false;
			String explanation = "";	
			//query_hits(0) = whole, query_hits(1) = title, query_hits(2) = body, query_hits(3) = county,
			Document doc_w = searcher.doc(query_hits.get(0)[i].doc);
			String docno_w = doc_w.get("docno");
			docs_found.add(doc_w);
			doc_numbers.add(docno_w);
			for (String[] r : relevancy) {
				if (r[0].equals(qid) && r[2].equals(docno_w)) {
					relevances.add(Integer.valueOf(r[3]));
					break;
				}else{
					relevances.add(0);
				}
			}

			doc_lengths[i] = doc_w.toString().length();
			feat_w.add((double)query_hits.get(0)[i].score);
			explanation = searcher.explain(query_list.get(0), i).toString();
			if (("default").equals(runtag)) {
				String[] array = explanation.split("\n");
				double tfs = 0.0;
				double idfs = 0.0;
				int counter = 1;
				// System.out.println(explanation);
				for (int j = 1; j < array.length; j=j+8){
					if(array.length>1){
						double tf_score = Double.parseDouble(array[j+2].trim().split(" ")[0]);
						String tfreq = array[j+3].trim().replaceAll("=termFreq="," ").split(" ")[0];
						double idfreq = Double.parseDouble(array[j+4].trim().split(" ")[0]);
						tfs=tfs+tf_score;
						idfs=idfs+idfreq;
						counter++;
					}
				}
				tfs = tfs/counter;
				idfs = idfs/counter;
				feat_w.add(tfs);
				feat_w.add(idfs);
			}
			// title
			for (int j = 0; j < ends.get(1); j++) {
				Document doc_t = searcher.doc(query_hits.get(1)[j].doc);
				String docno_t = doc_t.get("docno");
				if(docno_t.equals(docno_w)){
					t_exist = true;
					feat_t.add((double)query_hits.get(1)[j].score);
					if (("default").equals(runtag)) {
						explanation = searcher.explain(query_list.get(1), j).toString();
						String[] array = explanation.split("\n");
						double tfs = 0.0;
						double idfs = 0.0;
						int counter = 1;
						// System.out.println(explanation);
						for (int k = 1; k < array.length; k=k+8){
							if(array.length>1){
								double tf_score = Double.parseDouble(array[k+2].trim().split(" ")[0]);
								String tfreq = array[k+3].trim().replaceAll("=termFreq="," ").split(" ")[0];
								double idfreq = Double.parseDouble(array[k+4].trim().split(" ")[0]);
								tfs=tfs+tf_score;
								idfs=idfs+idfreq;
								counter++;
							}
						}
						tfs = tfs/counter;
						idfs = idfs/counter;
						feat_t.add(tfs);
						feat_t.add(idfs);
						break;

					}
				}
			}
			//body
			for (int j = 0; j < ends.get(2); j++) {
				Document doc_b = searcher.doc(query_hits.get(2)[j].doc);
				String docno_b = doc_b.get("docno");
				if(docno_b.equals(docno_w)){
					b_exist = true;
					feat_b.add((double)query_hits.get(2)[j].score);
					if (("default").equals(runtag)) {
						explanation = searcher.explain(query_list.get(2), j).toString();
						String[] array = explanation.split("\n");
						double tfs = 0.0;
						double idfs = 0.0;
						int counter = 1;
						// System.out.println(explanation);
						for (int k = 1; k < array.length; k=k+8){
							if(array.length>1){
								double tf_score = Double.parseDouble(array[k+2].trim().split(" ")[0]);
								String tfreq = array[k+3].trim().replaceAll("=termFreq="," ").split(" ")[0];
								double idfreq = Double.parseDouble(array[k+4].trim().split(" ")[0]);
								tfs=tfs+tf_score;
								idfs=idfs+idfreq;
								counter++;
							}
						}
						tfs = tfs/counter;
						idfs = idfs/counter;
						feat_b.add(tfs);
						feat_b.add(idfs);
						break;
					}		
				}
			}
			//cn
			for (int j = 0; j < ends.get(3); j++) {
				Document doc_cn = searcher.doc(query_hits.get(3)[j].doc);
				String docno_cn = doc_cn.get("docno");
				if(docno_cn.equals(docno_w)){
					cn_exist = true;
					feat_cn.add((double)query_hits.get(3)[j].score);
					if (("default").equals(runtag)) {
						explanation = searcher.explain(query_list.get(3), j).toString();
						String[] array = explanation.split("\n");
						double tfs = 0.0;
						double idfs = 0.0;
						int counter = 1;
						// System.out.println(explanation);
						for (int k = 1; k < array.length; k=k+8){
							if(array.length>1){
								double tf_score = Double.parseDouble(array[k+2].trim().split(" ")[0]);
								String tfreq = array[k+3].trim().replaceAll("=termFreq="," ").split(" ")[0];
								double idfreq = Double.parseDouble(array[k+4].trim().split(" ")[0]);
								tfs=tfs+tf_score;
								idfs=idfs+idfreq;
								counter++;
							}
						}
						tfs = tfs/counter;
						idfs = idfs/counter;
						feat_cn.add(tfs);
						feat_cn.add(idfs);
						break;
					}
				}
			}
			if(!t_exist){
				if(("default").equals(runtag)){
					feat_t.add(0.0);
					feat_t.add(0.0);
					feat_t.add(0.0);
				}else{
					feat_t.add(0.0);
				}
				
			}
			if(!b_exist){
				if(("default").equals(runtag)){
					feat_b.add(0.0);
					feat_b.add(0.0);
					feat_b.add(0.0);
				}else{
					feat_b.add(0.0);
				}
				
			}	
			if(!cn_exist){
				if(("default").equals(runtag)){
					feat_cn.add(0.0);
					feat_cn.add(0.0);
					feat_cn.add(0.0);
				}else{
					feat_cn.add(0.0);
				}
				
			}		
		}
		String printer = "";
		for (int i = 0; i < docs_found.size(); i++) {
			int feat_num=1;
			printer+=relevances.get(i)+" qid: "+qid;
			if(("default").equals(runtag)){
				int def_count = i*3;
				for (int j = 0; j < 3; j++) {
					printer += " "+feat_num+":" +feat_w.get(def_count+j);
					feat_num++;
					printer += " "+feat_num+":" +feat_t.get(def_count+j);
					feat_num++;
					printer += " "+feat_num+":" +feat_b.get(def_count+j);
					feat_num++;
					printer += " "+feat_num+":" +feat_cn.get(def_count+j);
					feat_num++;
				}
			}else{
				printer += " "+feat_num+":" +feat_w.get(i);
				feat_num++;
				printer += " "+feat_num+":" +feat_t.get(i);
				feat_num++;
				printer += " "+feat_num+":" +feat_b.get(i);
				feat_num++;
				printer += " "+feat_num+":" +feat_cn.get(i);
				feat_num++;
			}
			printer+=" "+feat_num+":"+doc_lengths[i];
			printer+=" #docno: "+doc_numbers.get(i)+"\n";	
		}

		return printer;
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
