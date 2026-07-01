package net.incongru.brewt

import io.kotest.matchers.Matcher
import io.kotest.matchers.MatcherResult
import io.kotest.matchers.should
import io.kotest.matchers.shouldNot
import org.xmlunit.builder.DiffBuilder
import org.xmlunit.builder.Input

import org.xmlunit.diff.DefaultNodeMatcher
import org.xmlunit.diff.ElementSelectors

/**
 * "Similar" XML comparison: ignores insignificant whitespace and treats equivalent-but-different
 * forms (attribute order, namespace prefixes, CDATA vs text) as matching — i.e. structurally alike,
 * not byte-identical.
 */
fun beXmlLike(expected: String, ignoreOrder: Boolean = true) = Matcher<String> { actual ->
    val diff = DiffBuilder.compare(Input.fromString(expected))
        .withTest(Input.fromString(actual))
        .ignoreWhitespace()
        .ignoreComments()
        .checkForSimilar()
        .apply { if (ignoreOrder) withNodeMatcher(DefaultNodeMatcher(ElementSelectors.byNameAndText)) }
        .build()
    MatcherResult(
        !diff.hasDifferences(),
        { "XML not similar to expected:\n${diff.fullDescription()}\n--- actual ---\n$actual" },
        { "XML was similar to expected but should not have been" },
    )
}
infix fun String.shouldBeXmlLike(expected: String): String {
    this should beXmlLike(expected)
    return this
}

/**
 * In PList, we care about the order because key and value elements are only bound by proximity (<key>Hour</key><integer>1</integer>)
 */
infix fun String.shouldBePlistLike(expected: String): String {
    this should beXmlLike(expected, false)
    return this
}

infix fun String.shouldNotBeXmlLike(expected: String): String {
    this shouldNot beXmlLike(expected)
    return this
}