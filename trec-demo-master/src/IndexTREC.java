import java.io.File;
import java.io.IOException;
import java.util.Date;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.index.IndexWriterConfig.OpenMode;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;
import org.apache.lucene.util.Version;


public class IndexTREC {

	private IndexTREC() {}

	public static void main(String[] args) {
		//WORKFLOW:
		//	We add Document(s) containing Field(s) to IndexWriter which analyzes the
		//	Document(s) using the Analyzer and then creates/open/edit indexes as required
		//	and store/update them in a Directory. IndexWriter is used to update or create
		//	indexes. It is not used to read indexes.

		String usage = "java org.apache.lucene.demo.IndexFiles"
				+ " [-index INDEX_PATH] [-docs DOCS_PATH] [-update]\n\n"
				+ "This indexes the documents in DOCS_PATH, creating a Lucene index"
				+ "in INDEX_PATH that can be searched with SearchFiles";
		String indexPath = "index";
		String docsPath = null;
		boolean create = true;

		//""" sets input commants tp variables. Basicly loops through command and set "-index" to index ect """
		for(int i=0;i<args.length;i++) {
			if ("-index".equals(args[i])) {
				indexPath = args[i+1];
				i++;
			} else if ("-docs".equals(args[i])) {
				docsPath = args[i+1];
				i++;
			} else if ("-update".equals(args[i])) {
				create = false;
			}
		}

		//""" throw error if path is null """
		if (docsPath == null) {
			System.err.println("Usage: " + usage);
			System.exit(1);
		}

		//""" catch error in given path """
		final File docDir = new File(docsPath);
		if (!docDir.exists() || !docDir.canRead()) {
			System.out.println("Document directory '" +docDir.getAbsolutePath()+ "' does not exist or is not readable, please check the path");
			System.exit(1);
		}

		// set start date
		Date start = new Date();

		//""" create LUCENE directory, analyser and indexwriterconfig for indexing """
		try {
			System.out.println("Indexing to directory '" + indexPath + "'...");

			// reading the data
			Directory dir = FSDirectory.open(new File(indexPath).toPath());
			// initiate the analizer. This is what tokenizes the stream of docs https://www.tutorialspoint.com/lucene/lucene_standardanalyzer.htm
			Analyzer analyzer = new StandardAnalyzer();
			// creates a configfile for a indexwriter
			IndexWriterConfig iwc = new IndexWriterConfig(analyzer);

			//""" if -update is passed as command we manipulate existing index """
			if (create) {
				iwc.setOpenMode(OpenMode.CREATE); // Create a new index in the directory, removing any previously indexed documents:
			} else {
				iwc.setOpenMode(OpenMode.CREATE_OR_APPEND); // Add new documents to an existing index:
			}

			// Optional: for better indexing performance, if you
			// are indexing many documents, increase the RAM
			// buffer.  But if you do this, increase the max heap
			// size to the JVM (eg add -Xmx512m or -Xmx1g):
			//
			iwc.setRAMBufferSizeMB(256.0);

			//""" find and index doc files """
			//https://www.tutorialspoint.com/lucene/lucene_indexwriter.htm
			IndexWriter writer = new IndexWriter(dir, iwc);
			indexDocs(writer, docDir); // calls helperfunction for

			// NOTE: if you want to maximize search performance,
			// you can optionally call forceMerge here.  This can be
			// a terribly costly operation, so generally it's only
			// worth it when your index is relatively static (ie
			// you're done adding documents to it):
			//
			// writer.forceMerge(1);

			writer.close();

			//""" print time spent on indexing to user """
			Date end = new Date();
			System.out.println(end.getTime() - start.getTime() + " total milliseconds"); // print time spent indexing to user

		} catch (IOException e) {
			System.out.println(" caught a " + e.getClass() +
					"\n with message: " + e.getMessage());
		}
	}

	/**
	 * Indexes the given file using the given writer, or if a directory is given,
	 * recurses over files and directories found under the given directory.
	 *
	 * NOTE: This method indexes one document per input file.  This is slow.  For good
	 * throughput, put multiple documents into your input file(s).  An example of this is
	 * in the benchmark module, which can create "line doc" files, one document per line,
	 * using the
	 * <a href="../../../../../contrib-benchmark/org/apache/lucene/benchmark/byTask/tasks/WriteLineDocTask.html"
	 * >WriteLineDocTask</a>.
	 *
	 * @param writer Writer to the index where the given file/dir info will be stored
	 * @param file The file to index, or the directory to recurse into to find files to index
	 * @throws IOException If there is a low-level I/O error
	 */
	static void indexDocs(IndexWriter writer, File file)

			throws IOException {
		if (file.canRead()) { // do not try to index files that cannot be read

			//""" if file is so recurse over files and subdirectories """
			if (file.isDirectory()) {
				String[] files = file.list();
				if (files != null) { 	// an IO error could occur
					for (int i = 0; i < files.length; i++) {
						indexDocs(writer, new File(file, files[i])); // creates index for each file in directory
					}
				}

			//""" write current document to writer  using TrecDocIterator """
			} else {
				TrecDocIterator docs = new TrecDocIterator(file); // start iterator
				Document doc;
				while (docs.hasNext()) { // loop through lines
					doc = docs.next();
					if (doc != null && doc.getField("contents") != null)
						writer.addDocument(doc);
				}
			}
		}
	}
}
