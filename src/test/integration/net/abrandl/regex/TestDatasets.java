package net.abrandl.regex;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import net.abrandl.lucene.regex.Document;
import net.abrandl.lucene.regex.RegexSearchEngine;

public enum TestDatasets {

	KEYWORDS("keywords.docs.csv", "keywords.queries.csv");

	private final String documentsPath, queriesPath;

	private TestDatasets(String documents, String queries) {
		this.documentsPath = documents;
		this.queriesPath = queries;
	}

	public void createIndex(RegexSearchEngine engine) throws IOException {
		try (InputStream resourceAsStream = getClass().getResourceAsStream(documentsPath);
				BufferedReader reader = new BufferedReader(new InputStreamReader(resourceAsStream));) {
			String line;

			while ((line = reader.readLine()) != null) {
				Document document = new Document(line, line);
				engine.addDocument(document);
			}
		}
	}

	public Iterator<String> queries() throws IOException {
		List<String> queries = new LinkedList<String>();
		try (InputStream resourceAsStream = getClass().getResourceAsStream(queriesPath);
				final BufferedReader reader = new BufferedReader(new InputStreamReader(resourceAsStream));) {
			String line;

			while ((line = reader.readLine()) != null) {
				queries.add(line);
			}

		}
		return queries.iterator();
	}

}