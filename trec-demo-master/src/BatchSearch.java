import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.StringReader;
import java.io.FileReader;
import java.io.FileWriter;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Scanner;
import java.util.Date;


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
import org.apache.lucene.analysis.en.PorterStemFilter;
import org.apache.lucene.analysis.core.StopFilter;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.TokenFilter;
import org.apache.lucene.analysis.tokenattributes.OffsetAttribute;
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;
import org.apache.lucene.analysis.CharArraySet;

/** Simple command-line based search demo. */
public class BatchSearch {

	private BatchSearch() {}

    /** Simple command-line based search demo. */
    static double percent_of = 0.0;
    static double goal = 150.0*4.0;
    static StringBuilder progress = new StringBuilder("....................................................................................................0%");
	public static void main(String[] args) throws Exception {
        
		String usage = "Usage:\tjava BatchSearch [-index dir] [-simfn similarity] [-field f] [-top top] [-train boolean] [-queries file]";
		if (args.length > 0 && ("-h".equals(args[0]) || "-help".equals(args[0]))) {
			System.out.println(usage);
			System.out.println("Supported similarity functions:\ndefault: DefaultSimilary (tfidf)\n");
			System.exit(0);
		}

		String index = "index";
		String field = "contents";
		String queries = null;
		String simstring = "default";
        int top = 150;
		Boolean train = true;

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
			}else if ("-top".equals(args[i])) {
				top = Integer.valueOf(args[i + 1]);
				i++;
			}
			else if ("-train".equals(args[i])) {
				train = Boolean.valueOf(args[i + 1]);
				i++;
			}
		}
		File file = new File("./test-data/qrels.trec6-8.nocr");
		Scanner sc = new Scanner(file);
		
		List<String[]> relevancy = new ArrayList<String[]>();
		
		while(sc.hasNextLine()) {
			relevancy.add(sc.nextLine().split(" "));
		}
		sc.close();
		
		IndexReader reader = DirectoryReader.open(FSDirectory.open(new File(index).toPath()));
		IndexSearcher searcher = new IndexSearcher(reader);
		Analyzer analyzer = new StandardAnalyzer();
		
		BufferedReader in = null;
		if (queries != null) {
			in = new BufferedReader(new InputStreamReader(new FileInputStream(queries), "UTF-8"));
		} else {
			in = new BufferedReader(new InputStreamReader(new FileInputStream("queries"), "UTF-8"));
		}
        QueryParser parser = new QueryParser("contents", analyzer);
		QueryParser title_parser = new QueryParser("title", analyzer);
		QueryParser body_parser = new QueryParser("body", analyzer);
        QueryParser cn_parser = new QueryParser("cn",analyzer);

        // EXTRA
        QueryParser bigram_parser = new QueryParser("contents", analyzer);
        QueryParser maxgram_parser = new QueryParser("contents", analyzer);
        
        double time = 0.0;
		int query_count = 1;
        Date start_total = new Date();
        System.out.println(progress);
		while (true) {
            Date start = new Date();
			String line = in.readLine();

			if (line == null || line.length() == -1) {
				break;
			}

			line = line.trim();
			if (line.length() == 0) {
				break;
            }
            String[] pair = line.split(" ", 2);
            line = pair[1].toLowerCase();
            line = tokenizeStopStem(line, true); // porter stem and delete stopwords in query
			if  (line.length() == 0) {
			 	line = pair[1];
            }

            // EXTRA
            System.out.println("Line: " + line);
            String bigram_line = "";
            String[] grams = line.split(" ");
            if (grams.length > 2) {
                for (int i = 0; i < grams.length-1; i++) {
                    bigram_line += "\"" + grams[i] + " " + grams[i+1] + "\" ";
                }
                bigram_line += "\"" + grams[grams.length-1] + " " + grams[0] + "\" ";
            } else if (grams.length == 2) {
                bigram_line += "\"" + grams[0] + " " + grams[1] + "\" ";
            } else {
                bigram_line += grams[0];
            }
            System.out.println("Changed line: " + bigram_line);
            String maxgram_line = "\"" + line + "\"";
            System.out.println("Changed line: " + maxgram_line);
            
            //unigram field queries
			List<Query> query_list = new ArrayList<Query>();
			Query query = parser.parse(line);
			Query query_title = title_parser.parse(line);
			Query query_body = body_parser.parse(line);
			Query query_cn = cn_parser.parse(line);

            // EXTRA
            Query query_bigram = bigram_parser.parse(bigram_line);
            Query query_maxgram = maxgram_parser.parse(maxgram_line);            

			query_list.addAll(Arrays.asList(query,query_title,query_body,query_cn,query_bigram,query_maxgram));

			String q_search = "";
			File f = null;
			FileWriter w = null;
			BufferedWriter bw = null;
			System.out.println("query: "+pair[0]);
			if (train) {
				q_search = doBatchSearch(in, searcher, pair[0], query_list, simstring, top, relevancy);
				f = new File("../RankLib/data/letor.txt");
				w = new FileWriter(f,true);
				bw = new BufferedWriter(w);
				bw.write(q_search);
            }
            //predict
			if(!train){
				q_search = doBatchSearch(in, searcher, pair[0], query_list, simstring, 10, relevancy);
				f = new File("../RankLib/data/predict.txt");
				w = new FileWriter(f);
				bw = new BufferedWriter(w);
				bw.write(q_search);
			}
			
			bw.close();
			Date end = new Date();
            time = time + (end.getTime() - start.getTime());
            f = new File("../RankLib/data/time.txt");
            w = new FileWriter(f);
            bw = new BufferedWriter(w);
            String q_time = query_count+"/150 queries done\nTime spent: " + time/1000 + " seconds";
            bw.write(q_time);
            bw.close();
			query_count = query_count + 1;
		}
		Date end_total = new Date();
		String info = "\n" +time + " total milliseconds spendt\n" + 
			query_count + " total queries\n" +
			time/query_count + " average milliseconds spendt per query";
		File f = new File("../RankLib/data/time.txt");
		FileWriter w = new FileWriter(f,true);
		BufferedWriter bw = new BufferedWriter(w);
		bw.write(info);
		bw.close();
		//System.out.println((end_total.getTime() - start_total.getTime())/query_count + " average milliseconds spendt per query"); // print time spent indexing to user
		reader.close();
	}

	/**
	 * This function performs a top-1000 search for the query as a basic TREC run.
	 */
	public static String doBatchSearch(BufferedReader in, IndexSearcher searcher, String qid, List<Query> query_list, String runtag, int num_top, List<String[]> relevancy)	 
			throws IOException {
        String[] simfunctions = {"default","bm25","dfr","lm"};
		Similarity simfn = null;
		List<List<String>> docnumbers = new ArrayList<List<String>>(); //return once
		List<Integer> labels = new ArrayList<Integer>(); //return once
		List<List<Double>> whole = new ArrayList<List<Double>>(); //return
		List<List<Double>> title = new ArrayList<List<Double>>(); //return
		List<List<Double>> body = new ArrayList<List<Double>>(); //return
		List<List<Double>> country = new ArrayList<List<Double>>(); // return
        List<double[]> lengths = new ArrayList<double[]>(); //return once
        List<List<Double>> bigrams_whole = new ArrayList<List<Double>>(); //return
        List<List<Double>> maxgrams_whole = new ArrayList<List<Double>>(); //return

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
            int max_hits = num_top;
            for (Query query : query_list) {
                topdocs.add(searcher.search(query,max_hits));
            }
            //Extracting info for each result
            for (TopDocs result : topdocs) {
                query_hits.add(result.scoreDocs);
                totalHits.add(result.totalHits);
            }
            //Adding length of hits for each query
            for (long hits : totalHits) {
                ends.add(Math.min(hits, max_hits));
            }

            int num_docs = ends.get(0).intValue();
            List<String> doc_numbers = new ArrayList<String>(); //return once
            // List<Integer> relevances = new ArrayList<Integer>(); //return once
            double[] doc_lengths = new double[num_docs]; //return once
            
            //per sim funct feat lists
            List<Double> feat_w = new ArrayList<Double>(); //return
            List<Double> feat_t = new ArrayList<Double>(); //return
            List<Double> feat_b = new ArrayList<Double>(); //return
            List<Double> feat_cn = new ArrayList<Double>(); // return
            List<Double> feat_bi_w = new ArrayList<Double>(); // return
            List<Double> feat_max_w = new ArrayList<Double>(); // return
            //for duplicates
            HashMap<String, String> seen = new HashMap<String, String>(1000);
            int start = 0;
            int count_false=0;
            int matched=0;
            //for every document match for the query
            for (int i = start; i < ends.get(0); i++) {
                Boolean t_exist = false;
                Boolean b_exist = false;
                Boolean cn_exist = false;
                Boolean bi_w_exist = false;
                Boolean max_w_exist = false;
                Boolean match = false;
                String explanation = "";
                Document doc_w = searcher.doc(query_hits.get(0)[i].doc);
                String docno_w = doc_w.get("docno");
                doc_lengths[i] = doc_w.toString().length();
                doc_numbers.add(docno_w);
                
                //find features for the whole document i.e "contents"
                //explanation contains most features
                int inc = 0;
                feat_w.add((double)query_hits.get(0)[i].score);
                if (("default").equals(sim)) {
                    explanation = searcher.explain(query_list.get(0), i).toString();
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

                //bigram
                //System.out.println(ends.get(4));
                for (int j = 0; j < ends.get(4); j++) {
                    Document doc_bi_w = searcher.doc(query_hits.get(4)[j].doc);
                    String docno_bi_w = doc_bi_w.get("docno");
                    if(docno_bi_w.equals(docno_w)){
                        System.out.println("bigram");

                        bi_w_exist = true;
                        feat_bi_w.add((double)query_hits.get(4)[j].score);
                        if (("default").equals(sim)) {
                            explanation = searcher.explain(query_list.get(4), j).toString();
                            System.out.println(explanation);
                            String[] array = explanation.split("\n");
                            double tfs = 0.0;
                            double idfs = 0.0;
                            int counter = 1;
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
                            feat_bi_w.add(tfs);
                            feat_bi_w.add(idfs);
                            break;

                        }
                    }
                }

                //maxgram
                for (int j = 0; j < ends.get(5); j++) {
                    Document doc_max_w = searcher.doc(query_hits.get(5)[j].doc);
                    String docno_max_w = doc_max_w.get("docno");
                    //System.out.println("max"+query_list.get(5).toString());
                    if(docno_max_w.equals(docno_w)){
                        max_w_exist = true;
                        feat_max_w.add((double)query_hits.get(5)[j].score);
                        if (("default").equals(sim)) {
                            explanation = searcher.explain(query_list.get(5), j).toString();
                            String[] array = explanation.split("\n");
                            double tfs = 0.0;
                            double idfs = 0.0;
                            int counter = 1;
                            //System.out.println(explanation);
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
                            feat_max_w.add(tfs);
                            feat_max_w.add(idfs);
                            break;

                        }
                    }
                }


                //add relevancy label
                if("default".equals(sim)){
                    for (String[] r : relevancy) {
                        if (r[2].equals(docno_w)&&qid.equals(r[0])) {
                            match = true;
                            matched++;
                            int value = Integer.valueOf(r[3]);
                            if (bi_w_exist) {
                                value = value*1;
                                labels.add(value);
                            } else if (max_w_exist) {
                                value = value*2;
                                labels.add(value);
                            } else {
                                labels.add(Integer.valueOf(r[3]));
                            }
                            break;
                        }
                    }
                    if(!match){
                        count_false++;
                        int x = (int)(Math.random()*((1-0)+1))+0;
                        labels.add(x);
                    }
                }


                //if the documents doesnt match for both whole and field
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
                if(!bi_w_exist){
                    if(("default").equals(sim)){
                        feat_bi_w.add(0.0);
                        feat_bi_w.add(0.0);
                        feat_bi_w.add(0.0);
                    }else{
                        feat_bi_w.add(0.0);
                    }
                    
                }
                if(!max_w_exist){
                    if(("default").equals(sim)){
                        feat_max_w.add(0.0);
                        feat_max_w.add(0.0);
                        feat_max_w.add(0.0);
                    }else{
                        feat_max_w.add(0.0);
                    }
                    
                }   
                // There are duplicate document numbers in the FR collection, so only output a given
                // docno once.
                if (seen.containsKey(docno_w)) {
                    continue;
                }
                seen.put(docno_w, docno_w);
                // System.out.println(qid+" Q0 "+docno+" "+i+" "+hits[i].score+" "+runtag);
            }
            
            if (sim.equals("default")) {
                System.out.println("Matched: "+matched+" Nonmatch: "+count_false+"\n");
				docnumbers.add(doc_numbers);
				// labels.add(relevances);
				lengths.add(doc_lengths);
			}
			whole.add(feat_w);
			title.add(feat_t);
			body.add(feat_b);
            country.add(feat_cn);
            bigrams_whole.add(feat_bi_w);
            maxgrams_whole.add(feat_max_w);
            double prev_percent = (percent_of/goal)*100;
            percent_of+=1;
            double percent = (percent_of/goal)*100;          
            int int_percent = (int)percent; 
            int prog_len = progress.length();
            //progress print
            
            if(int_percent>(int)prev_percent){
                progress.replace(int_percent-1,int_percent,"#");
                progress.replace(prog_len-2,prog_len-1,String.valueOf(int_percent));
                System.out.println(progress.toString());
            }
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
                        sum+=bigrams_whole.get(k).get(def_count+j);
                        line += " "+feat_num+":" +bigrams_whole.get(k).get(def_count+j);
                        feat_num++;
                        sum+=maxgrams_whole.get(k).get(def_count+j);
                        line += " "+feat_num+":" +maxgrams_whole.get(k).get(def_count+j);
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
                    sum+=bigrams_whole.get(k).get(i);
                    line += " "+feat_num+":" +bigrams_whole.get(k).get(i);
                    feat_num++;
                    sum+=maxgrams_whole.get(k).get(i);
                    line += " "+feat_num+":" +maxgrams_whole.get(k).get(i);
                    feat_num++;
				}
			}
			sum = Math.log(sum);
			if (sum>5) {
                labels.set(i,1);
            }
			line+=" "+feat_num+":"+lengths.get(0)[i];
			line+=" #docno: "+docnumbers.get(0).get(i);
			line = String.valueOf(labels.get(i))+line+"\n";
            result += line;
        }
        // System.out.println(result);
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

