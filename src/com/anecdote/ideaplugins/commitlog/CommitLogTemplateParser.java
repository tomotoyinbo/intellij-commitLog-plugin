package com.anecdote.ideaplugins.commitlog;

import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.StringTokenizer;

/**
 * Author: omotoyt
 * Created date: 1/10/2017.
 */
public class CommitLogTemplateParser {

    public static final String VALUE_PLACEHOLDER_SYMBOL = "$";
    public static final String BLOCK_PLACEHOLDER_OPEN_SYMBOL = "[";
    public static final String BLOCK_PLACEHOLDER_CLOSE_SYMBOL = "]";
    public static final String ESCAPE_SYMBOL = "\\";

    List<TextTemplateNode> parseTextTemplate(String textTemplate) throws CommitLogTemplateParser.TextTemplateParserException {

        try {

            StringTokenizer tokens = new StringTokenizer(textTemplate, "\\$[]", true);

            List result = new LinkedList();
            boolean inPlaceholder = false;
            boolean inBlockPlaceholder = false;
            int nextLocation = 0;

            while (tokens.hasMoreTokens()) {

                int tokenLocation = nextLocation;
                String token = tokens.nextToken();
                nextLocation += token.length();

                if ("$".equals(token)) {

                    if (inBlockPlaceholder) {
                        throwParserException("Block placeholders may not contain '$'", tokenLocation);
                    }

                    inPlaceholder = !inPlaceholder;

                } else if ("[".equals(token)) {

                    if (inPlaceholder) {
                        throwParserException("Placeholders may not contain '['", tokenLocation);
                    } else {
                        inPlaceholder = true;
                        inBlockPlaceholder = true;
                    }

                } else if ("]".equals(token)) {

                    if (inPlaceholder) {
                        inPlaceholder = false;
                        inBlockPlaceholder = false;
                    } else {
                        throwParserException("Template may not contain unescaped ']' - use '\\]' instead", tokenLocation);
                    }

                } else {

                    if ("\\".equals(token)) {

                        if (tokens.hasMoreTokens()) {
                            tokenLocation = nextLocation;
                            token = tokens.nextToken();
                            nextLocation += token.length();

                            if ((!"$".equals(token)) && (!"\\".equals(token)) && (!"]".equals(token)) && (!"[".equals(token))) {
                                throwParserException("'\\' may only precede '$', '[', ']' or '\\'", tokenLocation);
                            }

                        } else {
                            throwParserException("'\\' must be followed by '$', '[', ']' or '\\'", tokenLocation);
                        }
                    }

                    if (inPlaceholder) {

                        if (inBlockPlaceholder) {

                            if (tokens.hasMoreTokens()) {

                                if (token.contains("\n")) {
                                    throwParserException("Block Placeholders may not contain linefeeds", tokenLocation + token.indexOf('\n') - 1);
                                }

                                result.add(createTextTemplateNode(TextTemplateNodeType.BLOCK_PLACEHOLDER_NODE, token, tokenLocation));

                            } else {
                                throwParserException("Opening [ detected with no closing ]", tokenLocation + token.length() - 1);
                            }

                        } else if (tokens.hasMoreTokens()) {

                            if (token.contains("\n")) {
                                throwParserException("Value Placeholders may not contain linefeeds", tokenLocation + token.indexOf('\n') - 1);
                            }

                            result.add(createTextTemplateNode(TextTemplateNodeType.VALUE_PLACEHOLDER_NODE, token, tokenLocation));

                        } else {
                            throwParserException("Opening $ detected with no closing $", tokenLocation + token.length() - 1);
                        }

                    } else {

                        result.add(createTextTemplateNode(TextTemplateNodeType.TEXT_NODE, token, tokenLocation));
                    }
                }
            }

            return result;

        } catch (TextTemplateParserException e) {
            throw new TextTemplateParserException(e.getMessage(), e, e.getLocation());
        }
    }

    private static void throwParserException(String message, int tokenLocation) throws CommitLogTemplateParser.TextTemplateParserException {
        throw new TextTemplateParserException("Illegal text template - error at index " + tokenLocation + " : " + message, tokenLocation);
    }

    public static void main(String[] args) {

        CommitLogTemplateParser p = new CommitLogTemplateParser();
        p.test("Low accuracy on Interface $DOMAIN-ELEMENT.bestname$ of $TARGET.name$");
        p.test("Low accuracy on Interface $DOMAIN-ELEMENT.bestname$ of $TARGET.name$.");
        p.test("$DOMAIN-ELEMENT.bestname Low accuracy on Interface of $TARGET.name.");
        p.test("$DOMAIN-ELEMENT.bestname$ Low accuracy on Interface of $TARGET.name$.");
        p.test("$DOMAIN-ELEMENT.bestname$ Low accuracy on Interface of $TARGET.name.");
        p.test("Low accuracy on Interface the bestname of $DOMAIN-ELEMENT$ of $TARGET.name");
        p.test("Low accuracy on Interface the <B>bestname of $DOMAIN-ELEMENT$ of the name of $TARGET$</B>");
        p.test("Low accuracy on Interface the bestname of $DOMAIN-ELEMENT$ of the name of $TARGET.$");
        p.test("Price is $PRICE$\\$");
        p.test("Price is $PRICE$\\$ down \\\\ from $OLD_PRICE$\\$");
    }

    private void test(String text) {

        System.out.println("Testing input : " + text);

        try {

            Iterator result = parseTextTemplate(text).iterator();
            System.out.println("Results : ");

            while (result.hasNext()) {

                TextTemplateNode node = (TextTemplateNode) result.next();
                System.out.println("Node of type " + node.getType() + " with text : " + node.getText() + "#END#");
            }

        } catch (TextTemplateParserException e) {
            e.printStackTrace(System.out);
        }
    }

    private TextTemplateNode createTextTemplateNode(final TextTemplateNodeType type, final String text, final int location) {

        return new TextTemplateNode() {

            public CommitLogTemplateParser.TextTemplateNodeType getType() {
                return type;
            }

            public String getText() {
                return text;
            }

            public String toString() {
                return text;
            }

            public int getLocation() {
                return location;
            }
        };
    }

    static class TextTemplateParserException extends Exception {

        private final int _location;

        public TextTemplateParserException(Throwable cause, int location) {
            super();
            this._location = location;
        }

        TextTemplateParserException(String message, int location) {
            super();
            this._location = location;
        }

        TextTemplateParserException(String message, Throwable cause, int location) {
            super(cause);
            this._location = location;
        }

        int getLocation() {
            return this._location;
        }
    }

    interface TextTemplateNode {

        CommitLogTemplateParser.TextTemplateNodeType getType();

        String getText();

        int getLocation();
    }

    enum TextTemplateNodeType {
        TEXT_NODE, VALUE_PLACEHOLDER_NODE, BLOCK_PLACEHOLDER_NODE;
    }
}
