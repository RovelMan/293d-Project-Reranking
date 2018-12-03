import java.io.BufferedReader;
import java.lang.Math;
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
		// File file = new File("./test-data/relevancy_test.txt");
		// Scanner sc = new Scanner(file);
		
		// List<String[]> relevancy = new ArrayList<String[]>();
		
		// while(sc.hasNextLine()) {
		// 	relevancy.add(sc.nextLine().split(" "));
		// }
		// sc.close();
		// creating the reader to read from our indexing
		IndexReader reader = DirectoryReader.open(FSDirectory.open(new File(index).toPath()));
		IndexSearcher searcher = new IndexSearcher(reader);
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
			line = pair[1];

			// line = tokenizeStopStem(line, true); // porter stem and delete stopwords in query
			// //line=line;
			// if  (line.length() == 0) {
			// 	line = line;
			// }

			//result is title: full title: query  body:full body:query
			List<Query> query_list = new ArrayList<Query>();
			Query query = parser.parse(line);
			Query query_title = title_parser.parse(line);
			Query query_body = body_parser.parse(line);
			Query query_cn = cn_parser.parse(line);
			query_list.addAll(Arrays.asList(query,query_title,query_body,query_cn));
			// query_list.addAll(Arrays.asList(query));

			List<BooleanQuery> boolean_list = new ArrayList<BooleanQuery>();
			BooleanQuery boolean_title = new BooleanQuery.Builder()
			.add(query_title, BooleanClause.Occur.SHOULD)
			.add(query_body, BooleanClause.Occur.SHOULD)
			.build();
			// BooleanQuery
			
			String print_string = "";
			//1 QUERY, ALL SIM FUNCTIONS{	
			//know that each sim function will return the same amout of documents.
			//number of docs will vary for each query, though
			
		
			//print whole shit
			String q_search = doBatchSearch(in, searcher, pair[0], query_list, simstring);
			System.out.printf(q_search);
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

	public static String doBatchSearch(BufferedReader in, IndexSearcher searcher, String qid, List<Query> query_list, String runtag)
			throws IOException {
		String[] simfunctions = {"default","bm25","dfr","lm"};
		Similarity simfn = null;
		List<List<String>> docnumbers = new ArrayList<List<String>>(); //return once
		List<List<Integer>> labels = new ArrayList<List<Integer>>(); //return once
		List<List<Double>> whole = new ArrayList<List<Double>>(); //return
		List<List<Double>> title = new ArrayList<List<Double>>(); //return
		List<List<Double>> body = new ArrayList<List<Double>>(); //return
		List<List<Double>> country = new ArrayList<List<Double>>(); // return
		List<double[]> lengths = new ArrayList<double[]>(); //return once

		for (String sim : simfunctions) {
			String print_string = "";
			if ("default".equals(sim)) {
				simfn = new ClassicSimilarity();
			} else if ("bm25".equals(sim)) {
				simfn = new BM25Similarity();
			} else if ("dfr".equals(sim)) {
				simfn = new DFRSimilarity(new BasicModelP(), new AfterEffectL(), new NormalizationH2());
			} else if ("lm".equals(sim)) {
				simfn = new LMDirichletSimilarity();
			}
			if (simfn == null) {
				System.out.println("Supported similarity functions:\ndefault: DefaultSimilary (tfidf)");
				System.out.println("bm25: BM25Similarity (standard parameters)");
				System.out.println("dfr: Divergence from Randomness model (PL2 variant)");
				System.out.println("lm: Language model, Dirichlet smoothing");
				System.exit(0);
			}
			// set similarity function TODO: do actual math
			searcher.setSimilarity(simfn);

			List<TopDocs> topdocs = new ArrayList<TopDocs>();
			List<ScoreDoc[]> query_hits = new ArrayList<ScoreDoc[]>();
			List<Long> totalHits = new ArrayList<Long>();
			List<Long> ends = new ArrayList<Long>();
			int max_hits = 1000;
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
			List<String> doc_numbers = new ArrayList<String>(); //return once
			// List<Integer> relevances = new ArrayList<Integer>(); //return once
			List<Double> feat_w = new ArrayList<Double>(); //return
			List<Double> feat_t = new ArrayList<Double>(); //return
			List<Double> feat_b = new ArrayList<Double>(); //return
			List<Double> feat_cn = new ArrayList<Double>(); // return
			HashMap<String, String> seen = new HashMap<String, String>(max_hits);
			double[] doc_lengths = new double[num_docs]; //return once
			int count_false=0;
			int matched=0;
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
				doc_numbers.add(docno_w);
				Boolean match = false;

				/*  if we find true relevancy labels for lucene 7.5   */
				// if(("default".equals(sim))){
				// 	for (String[] r : relevancy) {
				// 		if (r[2].equals(docno_w)&&qid.equals(r[0])) {
				// 			System.out.println("hit");
				// 			match = true;
				// 			matched++;
				// 			relevances.add(Integer.valueOf(r[3]));
				// 			break;
				// 		}
				// 	}
				// 	if(!match){
				// 		count_false++;
				// 		relevances.add(0);
				// 	}
				// }
				

				doc_lengths[i] = doc_w.toString().length();
				feat_w.add((double)query_hits.get(0)[i].score);
				explanation = searcher.explain(query_list.get(0), i).toString();
				if (("default").equals(sim)) {
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
						if (("default").equals(sim)) {
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
						if (("default").equals(sim)) {
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
						if (("default").equals(sim)) {
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
					if(("default").equals(sim)){
						feat_t.add(0.0);
						feat_t.add(0.0);
						feat_t.add(0.0);
					}else{
						feat_t.add(0.0);
					}
					
				}
				if(!b_exist){
					if(("default").equals(sim)){
						feat_b.add(0.0);
						feat_b.add(0.0);
						feat_b.add(0.0);
					}else{
						feat_b.add(0.0);
					}
					
				}	
				if(!cn_exist){
					if(("default").equals(sim)){
						feat_cn.add(0.0);
						feat_cn.add(0.0);
						feat_cn.add(0.0);
					}else{
						feat_cn.add(0.0);
					}
					
				}		
				if (seen.containsKey(docno_w)) {
					continue;
				}
				seen.put(docno_w, docno_w);
			}
			if (sim.equals("default")) {
				docnumbers.add(doc_numbers);
				// labels.add(relevances);
				lengths.add(doc_lengths);
			}
			whole.add(feat_w);
			title.add(feat_t);
			body.add(feat_b);
			country.add(feat_cn);
		}
		String result = "";
		for (int i = 0; i < docnumbers.get(0).size(); i++) {
			String line = "";
			double sum = 0.0;
			int relevant = 0;
			int feat_num=1;
			line+=" qid:"+qid;
			for (int k = 0; k < simfunctions.length; k++) {
				if(("default").equals(simfunctions[k])){
					int def_count = i*3;
					for (int j = 0; j < 3; j++) {
						sum+=whole.get(k).get(def_count+j);
						line += " "+feat_num+":" +whole.get(k).get(def_count+j);
						feat_num++;
						sum+=title.get(k).get(def_count+j);
						line += " "+feat_num+":" +title.get(k).get(def_count+j);
						feat_num++;
						sum+=body.get(k).get(def_count+j);
						line += " "+feat_num+":" +body.get(k).get(def_count+j);
						feat_num++;
						sum+=country.get(k).get(def_count+j);
						line += " "+feat_num+":" +country.get(k).get(def_count+j);
						feat_num++;
					}
				}else{
					sum+=whole.get(k).get(i);
					line += " "+feat_num+":" +whole.get(k).get(i);
					feat_num++;
					sum+=title.get(k).get(i);
					line += " "+feat_num+":" +title.get(k).get(i);
					feat_num++;
					sum+=body.get(k).get(i);
					line += " "+feat_num+":" +body.get(k).get(i);
					feat_num++;
					sum+=country.get(k).get(i);
					line += " "+feat_num+":" +country.get(k).get(i);
					feat_num++;
				}
			}
			sum = Math.log(sum);
			if (sum>3.25) {
				relevant = 2;
			}else if(sum>2.75){
				relevant = 1;
			}else{
				relevant = 0;	
			}
			line+=" "+feat_num+":"+lengths.get(0)[i];
			line+=" #docno: "+docnumbers.get(0).get(i);
			line = String.valueOf(relevant)+line+"\n";
			result += line;
		}

		return result;
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
