package de.abrandl.regex.lucene;

import java.io.*;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.regex.Pattern;

import org.apache.lucene.document.Field;
import org.apache.lucene.document.Field.Index;
import org.apache.lucene.document.Field.Store;
import org.apache.lucene.document.TextField;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.store.Directory;
import org.apache.lucene.util.Version;

import de.abrandl.regex.RegexSearchEngine;
import de.abrandl.regex.SearchFailedException;
import de.abrandl.regex.document.InMemoryDocument;
import de.abrandl.regex.document.SimpleDocument;
import de.abrandl.regex.grammar.RegexParser;
import de.abrandl.regex.grammar.RegexParsingException;
import de.abrandl.regex.grammar.tree.RegexNode;
import de.abrandl.regex.query.NGramQueryTransformation;
import de.abrandl.regex.query.bool.Expression;

public class LuceneRegexSearchEngine implements RegexSearchEngine {

	private final Version luceneVersion;
	private final Directory directory;
	private final NGramAnalyzer analyzer;
	private final NGramQueryTransformation queryTransformation;

	private int documentCount = 0;

	public LuceneRegexSearchEngine(Version luceneVersion, Directory directory) {
		this.luceneVersion = luceneVersion;
		this.directory = directory;

		analyzer = new NGramAnalyzer(luceneVersion);
		queryTransformation = new NGramQueryTransformation();
	}

	private class Reader implements RegexSearchEngine.Reader {

		private DirectoryReader index;

		@Override
		public void close() throws IOException {
			if (index != null) {
				index.close();
				index = null;
			}
		}

		@Override
		public void open() throws IOException {
			if (index == null) {
				index = DirectoryReader.open(directory);
			}
		}

		@Override
		public Collection<SimpleDocument> search(String regex) throws SearchFailedException {
			try {
				open();
				IndexSearcher isearcher = new IndexSearcher(index);
				RegexNode parsedRegex = RegexParser.parse(regex);
				Expression expression = queryTransformation.expressionFor(parsedRegex);

				System.out.println(expression);

				Query query = expression.accept(new LuceneExpressionQuery("trigrams"));

				System.out.println(query);

				ScoreDoc[] hits = isearcher.search(query, null, documentCount).scoreDocs;
				System.out.println(String.format("Got %d hits", hits.length));

				Collection<SimpleDocument> resultSet = new HashSet<SimpleDocument>(hits.length);

				Pattern pattern = Pattern.compile(regex);

				for (int i = 0; i < hits.length; i++) {
					org.apache.lucene.document.Document doc = isearcher.doc(hits[i].doc);
					String path = doc.get("identifier");
					String content = doc.get("content");
					if (pattern.matcher(content).find()) {
						resultSet.add(new InMemoryDocument(path, content));
					}
				}

				return resultSet;
			} catch (RegexParsingException | IOException e) {
				throw new SearchFailedException(e);
			}
		}

		private String readFile(File file) throws FileNotFoundException, IOException {
			StringBuffer content = new StringBuffer();
			try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
				String line = null;
				while ((line = reader.readLine()) != null) {
					content.append(line);
				}
			}
			return content.toString();
		}

	}

	private class Writer implements RegexSearchEngine.Writer {

		private IndexWriter writer = null;

		@Override
		public void close() throws IOException {
			if (writer != null) {
				writer.close();
				writer = null;
			}
		}

		@Override
		public void open() throws IOException {
			if (writer == null) {
				IndexWriterConfig config = new IndexWriterConfig(luceneVersion, analyzer);
				writer = new IndexWriter(directory, config);
			}
		}

		@Override
		public void add(Iterator<SimpleDocument> docs) throws IOException {
			while (docs.hasNext()) {
				add(docs.next());
			}
		}

		private void add(SimpleDocument document) throws IOException {
			open();
			org.apache.lucene.document.Document ldoc = new org.apache.lucene.document.Document();
			ldoc.add(new Field("identifier", document.getIdentifier(), Store.YES, Index.NOT_ANALYZED));
			String content = document.getContent();
			ldoc.add(new Field("content", content, Store.YES, Index.NOT_ANALYZED));
			ldoc.add(new Field("trigrams", content, TextField.TYPE_STORED));
			writer.addDocument(ldoc);

			documentCount++;
		}

	}

	@Override
	public de.abrandl.regex.RegexSearchEngine.Writer getWriter() {
		return new Writer();
	}

	@Override
	public de.abrandl.regex.RegexSearchEngine.Reader getReader() {
		return new Reader();
	}

	@Override
	public Collection<SimpleDocument> search(String regex) throws SearchFailedException, IOException {
		try (RegexSearchEngine.Reader reader = getReader()) {
			return reader.search(regex);
		}
	}

}