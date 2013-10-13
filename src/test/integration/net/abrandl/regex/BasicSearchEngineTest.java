package net.abrandl.regex;

import java.io.IOException;
import java.util.Collection;
import java.util.Iterator;

import net.abrandl.lucene.LuceneRegexSearchEngine;
import net.abrandl.lucene.regex.*;

import org.apache.lucene.store.RAMDirectory;
import org.apache.lucene.util.Version;

import static org.hamcrest.CoreMatchers.equalTo;

import static org.junit.Assert.assertThat;

import org.junit.Before;
import org.junit.Test;

public abstract class BasicSearchEngineTest {

	private RegexSearchEngine exhaustiveSearch;
	private RegexSearchEngine ngramSearch;
	private RegexSearchEngine luceneSearch;

	private final TestDatasets dataset;

	public BasicSearchEngineTest(TestDatasets dataset) {
		this.dataset = dataset;
	}

	@Before
	public void indexDocuments() throws IOException {

		exhaustiveSearch = new ExhaustiveSearchEngine();
		ngramSearch = new NGramRegexSearchEngine(3);
		luceneSearch = new LuceneRegexSearchEngine(Version.LUCENE_44, new RAMDirectory());

		dataset.createIndex(exhaustiveSearch);
		dataset.createIndex(ngramSearch);
		dataset.createIndex(luceneSearch);
	}

	@Test
	public void testQueries() throws SearchFailedException, IOException {
		Iterator<String> queries = dataset.queries();

		while (queries.hasNext()) {
			String regex = queries.next();
			searchDocuments(regex);
		}
	}

	private void searchDocuments(String regex) throws SearchFailedException {
		System.out.println("********************************************************");
		System.out.printf("Query: /%s/\n", regex);

		Collection<Document> expected = exhaustiveSearch.search(regex);

		System.out.printf("expected   [%03d]:   %s\n", expected.size(), expected);

		{
			Collection<Document> result = ngramSearch.search(regex);
			System.out.printf("ngramSearch     [%03d]:   %s\n", result.size(), result);
			assertThat(result, equalTo(expected));
		}

		{
			Collection<Document> result = luceneSearch.search(regex);
			System.out.printf("lucene     [%03d]:   %s\n", result.size(), result);
			assertThat(result, equalTo(expected));
		}
	}

}
