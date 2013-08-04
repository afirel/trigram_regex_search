package net.abrandl.lucene.regex.grammar;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import net.abrandl.lucene.regex.grammar.tree.Alternative;
import net.abrandl.lucene.regex.grammar.tree.Concatenation;
import net.abrandl.lucene.regex.grammar.tree.DotAny;
import net.abrandl.lucene.regex.grammar.tree.Literal;
import net.abrandl.lucene.regex.grammar.tree.MatchGroup;
import net.abrandl.lucene.regex.grammar.tree.OneOrMore;
import net.abrandl.lucene.regex.grammar.tree.Optional;
import net.abrandl.lucene.regex.grammar.tree.RegexNode;
import net.abrandl.lucene.regex.grammar.tree.ZeroOrMore;

import org.antlr.runtime.ANTLRStringStream;
import org.antlr.runtime.CommonTokenStream;
import org.antlr.runtime.RecognitionException;
import org.antlr.runtime.tree.CommonTree;

public class RegexTreeTransformer {

	public static RegexTreeTransformer parse(String regex)
			throws RecognitionException {
		return new RegexTreeTransformer(regex);
	}

	private RegexParser parser;
	private CommonTree tree;

	private RegexTreeTransformer(String input) throws RecognitionException {
		super();
		RegexLexer regexLexer = new RegexLexer(new ANTLRStringStream(input));
		parser = new RegexParser(new CommonTokenStream(regexLexer));

		tree = (CommonTree) parser.parse().getTree();
	}

	public String toString() {

		StringBuilder builder = new StringBuilder();
		walk(tree, builder);

		return builder.toString();
	}

	public RegexNode getRegexTree() {
		return buildRegexTree(tree);
	}

	private RegexNode buildRegexTree(CommonTree inputTree) {
		final int childCount = inputTree.getChildCount();
		final int tokenType = inputTree.getType();

		RegexNode[] childTrees = new RegexNode[childCount];
		buildRegexTreeForChildren(inputTree, childTrees);

		switch (tokenType) {
		case RegexParser.ALTERNATIVE:
			return new Alternative(childTrees);
		case RegexParser.GROUP:
			return new MatchGroup(childTrees);
		case RegexParser.CONCATENATION:
			return new Concatenation(childTrees);
		case RegexParser.ONEORMORE:
			return new OneOrMore(childTrees[0]);
		case RegexParser.ZEROORMORE:
			return new ZeroOrMore(childTrees[0]);
		case RegexParser.DOTANY:
			return new DotAny();
		case RegexParser.OPTIONAL:
			return new Optional(childTrees[0]);
		case RegexParser.ELEMENT:
			if (childCount == 1) {
				// only atom given
				return childTrees[0];
			} else {
				throw new RuntimeException("unexpected amount of children");
			}
		case RegexParser.LITERAL:
			return new Literal(inputTree.getText());
		default:
			throw new RuntimeException("not yet implemented type: "
					+ inputTree.getType());
		}
	}

	@SuppressWarnings("unchecked")
	private void buildRegexTreeForChildren(CommonTree parent,
			RegexNode[] childTrees) {
		if (parent.getChildren() != null) {
			int childIndex = 0;
			for (CommonTree child : (List<CommonTree>) parent.getChildren()) {
				RegexNode childTree = buildRegexTree(child);
				childTrees[childIndex] = childTree;
				childIndex++;
			}
		}
	}

	@SuppressWarnings("unchecked")
	private void walk(CommonTree tree, StringBuilder builder) {

		List<CommonTree> firstStack = new ArrayList<CommonTree>();
		firstStack.add(tree);

		List<List<CommonTree>> childListStack = new ArrayList<List<CommonTree>>();
		childListStack.add(firstStack);

		while (!childListStack.isEmpty()) {

			List<CommonTree> childStack = childListStack.get(childListStack
					.size() - 1);

			if (childStack.isEmpty()) {
				childListStack.remove(childListStack.size() - 1);
			} else {
				tree = childStack.remove(0);

				String indent = "";

				for (int i = 0; i < childListStack.size() - 1; i++) {
					indent += (childListStack.get(i).size() > 0) ? "|  "
							: "   ";
				}

				String tokenName = RegexParser.tokenNames[tree.getType()];
				String tokenText = tree.getText();

				builder.append(indent)
						.append(childStack.isEmpty() ? "'- " : "|- ")
						.append(tokenName)
						.append(!tokenName.equals(tokenText) ? "='"
								+ tree.getText() + "'" : "").append("\n");

				if (tree.getChildCount() > 0) {
					childListStack.add(new ArrayList<CommonTree>(
							(List<CommonTree>) tree.getChildren()));
				}
			}
		}
	}

}
