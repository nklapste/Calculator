import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Assignment 3: Exception handling <br />
 * Calculator using BNF
 * <p>
 * Name: Nathan Klapstien
 * ID: 1449872
 */


public class Calculator {

    // Error report stings for clarity
    private static String BAD_CHARACTER_ERROR = "illegal character '%s'";
    private static String BRACKET_OPEN_ERROR = "'(' expected";
    private static String BRACKET_CLOSE_ERROR = "')' expected";
    private static String COLON_ERROR = "closing ';' expected";
    private static String EQUAL_ERROR = "'=' expected";
    private static String LET_ERROR = "'let' in 'let var = val' operation expected";
    private static String OPERATOR_ERROR = "operator expected";
    private static String UNDEFINED_ERROR = "%s undefined";

    /**
     * Main entry
     *
     * @param args {@code String[]} Command line arguments
     */
    public static void main(String[] args) {
        Calculator calc = new Calculator();

        // Part 1
        String[] inputs = {
                "let x = 1;",                                                                           // 1, returns 1
                "(let x = 1) + x;",                                                                     // 2, returns 2
                "(let a = 2) + 3 * a - 5;",                                                             // 3, returns 3
                "(let x = (let y = (let z = 1))) + x + y + z;",                                         // 4, returns 4
                "1 + (let x = 1) + (let y = 2) + (1 + x) * (1 + y) - (let x = y) - (let y = 1) - x;",   // 5, returns 5
                "1 + (let a = (let b = 1) + b) + a + 1;",                                               // 6, returns 6
                "(let a = (let a = (let a = (let a = 2) + a) + a) + a) - 9;",                           // 7, returns 7
                "(let x = 2) ^ (let y = 3);",                                                           // 8, returns 8
                "(let y = 3) ^ (let x = 2);",                                                           // 9, returns 9
                // custom tests
                "(let x = 1 + (let y = 2));"        // 10, returns 3

        };
        for (int i = 0; i < inputs.length; i++) {
            System.out.println(String.format("%d -- %-90s %d", i + 1, inputs[i], calc.execExpression(inputs[i])));
        }
        // Part 2
        inputs = new String[]{
                "1 + (2 * 3;",                  // 1, syntax error: ')' expected
                "(let x 5) + x;",               // 2, syntax error: '=' expected
                "(let x = 5) (let y = 6);",     // 3, syntax error: operator expected
                "(let x = 5 let y = 6);",       // 4, syntax error: ')' expected TODO: NOTE: should this be an operator error?
                "(ler x = 5) ^ (let y = 6);",   // 5, runtime error: 'ler' undefined TODO: NOTE: should this be a missing let operator syntax error as specified in assignment?
                "(let x = 5) + y;",              // 6, runtime error: 'y' undefined
                // custom tests
                "(let x =",
                "(let x = %$;",
                "(let let x = 5) + y;",
        };
        for (int i = 0; i < inputs.length; i++) {
            try {
                System.out.println(String.format("%d -- %-30s %d", i + 1, inputs[i], calc.execExpression(inputs[i])));
            } catch (Exception e) {
                e.printStackTrace(System.out);
            }
        }
    }

    /**
     * Check if all "let variable =" components are valid syntax
     *
     * @param exp {@code string}
     */
    private void validateLets(String exp) {
        Pattern p;
        Matcher m;

        // find all matches of a "variable ="  occurrence
        p = Pattern.compile("([a-zA-Z][a-zA-Z0-9]*) =");
        m = p.matcher(exp);
        // Check all occurrences
        while (m.find()) {
            if (m.start() <= 3) {
                throw new SyntaxError(LET_ERROR);
            } else {
                String check = exp.substring(m.start() - 4, m.start() - 1);
                if (!check.equals("let")) {
                    throw new SyntaxError(LET_ERROR);
                }
            }
        }

        // find all matches of a "let variable" occurrence
        p = Pattern.compile("let ([a-zA-Z][a-zA-Z0-9]*)");
        m = p.matcher(exp);
        // Check all occurrences
        while (m.find()) {
            if (exp.length() <= m.end() + 4) {
                throw new SyntaxError(EQUAL_ERROR);
            } else {
                String check = exp.substring(m.end(), m.end() + 3);
                if (!check.equals(" = ")) {
                    throw new SyntaxError(EQUAL_ERROR);
                }
            }
        }

        // TODO: READ THIS
        // NOTE: this part is really dealing with checking for an error I think is wrongly categorized.
        // Error run 4 should really be attributed to a 'expected operator' error as it could be that we could
        // have the case (let x = 1 + (let y = 2)) this seems valid (as equals is a very low priority math character
        // thus stuff on the right of the equal should be dealt with first). But if I try to accommodate
        // (let x = 5 let y = 4) its seems that it doesn't need an ')' some operator and a second level of
        // parenthesis to work.


        // TODO: READ THIS
        // consider this segment a hotfix to deal with error 4's weird suggestion on errors
        // validate brackets on lets (if let has a space behind it is wrong syntax)
        p = Pattern.compile(" let");
        m = p.matcher(exp);
        // Check all occurrences
        if (m.find()){
            throw new SyntaxError(BRACKET_CLOSE_ERROR);
        }

    }

    /**
     * Check that an ending semicolon exists other existing (and erroneous) semi colon's will be caught later
     *
     * @param exp {@code String}
     */
    private void validateSemiColon(String exp) {
        Pattern p = Pattern.compile(";$");
        Matcher m = p.matcher(exp);
        if (!m.find()) {
            throw new SyntaxError(COLON_ERROR);
        }
    }

    /**
     * Check if a string is a valid operator (+ - * % ^ =)
     */
    private boolean isOperator(String op) {
        return new ShuntingYard().ops.containsKey(op);
    }

    /**
     * Check if a string is a valid value (any number)
     */
    private boolean isValue(String val) {
        return Pattern.matches("[0-9]+", val);
    }

    /**
     * Check if a string is a valid variable (Starting with a letter then any alpha-numeric afterwards)
     */
    private boolean isVariable(String var) {
        return Pattern.matches("[a-zA-Z][a-zA-Z0-9]*", var);
    }

    /**
     * Execute the expression, and return the correct value
     *
     * @param exp {@code String} The expression string
     * @return {@code int}    The value of the executed expression
     */
    private int execExpression(String exp) {

        // validate the 'let var=val' operations
        validateLets(exp);

        // validate that semi exist correctly
        validateSemiColon(exp);

        // chop of ending ';' as it is not needed
        exp = exp.replaceAll(";$", "");

        // add spaces between "(" and ")" to make parsing easier
        exp = exp.replaceAll("\\(", "( ");
        exp = exp.replaceAll("\\)", " )");

        // convert expression string to reverse polish notation (rpn)
        String rpnExp = new ShuntingYard().postfix(exp);

        // convert rpn expression string into an expression tree object
        ExpressionTree expTree = new ExpressionTree(rpnExp);

        // parse the expression tree and obtain its results
        return expTree.parse();
    }

    // custom created exceptions using RuntimeException for ease of use
    static class SyntaxError extends RuntimeException {
        SyntaxError(String s) {
            super(s);
        }
    }

    static class RuntimeError extends RuntimeException {
        RuntimeError(String s) {
            super(s);
        }
    }

    /**
     * Dijkstra's shuntingyard algorithm converting normal notation math to reverse polish notation (rpn)
     */
    public static class ShuntingYard {


        private Map<String, Operator> ops = new HashMap<>() {{
            put("+", Operator.ADD);
            put("-", Operator.SUBTRACT);
            put("*", Operator.MULTIPLY);
            put("/", Operator.DIVIDE);
            put("^", Operator.EXPONENTIATION);
            put("=", Operator.EQUALS);
        }};

        /**
         * Check that a inputted expression character token is a valid character
         * If invalid report the illegal character
         *
         * @param token {@code String}
         */
        private void validateToken(String token) {
            Pattern p = Pattern.compile("[^a-zA-Z0-9]");
            Matcher m = p.matcher(token);
            if (m.find())
                throw new SyntaxError(String.format(BAD_CHARACTER_ERROR, m.group()));
        }

        private boolean isHigherPrec(String op, String sub) {
            return (ops.containsKey(sub) && ops.get(sub).precedence >= ops.get(op).precedence);
        }

        /**
         * Iterative methods that converts expression strings to reverse polish notation
         *
         * @param infix {@code String}
         * @return {@code String}
         */
        private String postfix(String infix) {
            // StringBuilder to have a rpn expression created on top
            StringBuilder output = new StringBuilder();

            // stack for managing operations within brackets
            Deque<String> stringStack = new LinkedList<>();

            // separate stage for just tallying the brackets
            Deque<String> bracketStack = new LinkedList<>();

            // flag noting that an operator was the previous token assume true for start of parsing
            boolean prevOperator = true;

            for (String token : infix.split("\\s")) {

                // operator
                if (ops.containsKey(token)) {

                    while (!stringStack.isEmpty() && isHigherPrec(token, stringStack.peek()))
                        output.append(stringStack.pop()).append(' ');
                    stringStack.push(token);

                    // set flag noting that an operator was the last token
                    prevOperator = true;

                    // left parenthesis
                } else if (token.equals("(")) {
                    // check if operator was before
                    if (!prevOperator) {
                        throw new SyntaxError(OPERATOR_ERROR);
                    }

                    stringStack.push(token);
                    bracketStack.push(token);


                    // right parenthesis
                } else if (token.equals(")")) {
                    if (!bracketStack.pop().equals("(")) {
                        throw new SyntaxError(BRACKET_OPEN_ERROR);
                    }

                    // add contents contained in bracket pair to string stack
                    while (!stringStack.peek().equals("("))
                        output.append(stringStack.pop()).append(' ');
                    stringStack.pop();

                    // set prev operator as false
                    prevOperator = false;

                    // ignore let values
                } else if (token.equals("let")) {

                    // other token values
                } else {

                    // check if the token is a valid character
                    validateToken(token);

                    if (prevOperator) {
                        prevOperator = false;
                        output.append(token).append(' ');
                    } else {
                        throw new SyntaxError(OPERATOR_ERROR);
                    }

                }
            }

            while (!stringStack.isEmpty())
                output.append(stringStack.pop()).append(' ');

            // if the bracket stack was not correctly emptied this their is a bracket problem
            if (!bracketStack.isEmpty()) {
                throw new SyntaxError(BRACKET_CLOSE_ERROR);
            }
            return output.toString();
        }

        public enum Operator {
            ADD(1), SUBTRACT(2), MULTIPLY(3), DIVIDE(4), EXPONENTIATION(6), EQUALS(0);
            final int precedence;

            Operator(int p) {
                precedence = p;
            }
        }
    }

    /**
     * The Expression tree for parsing and equating a reverse polish notation string
     */
    private class ExpressionTree {

        private TreeNode root;
        // Parser memory hashmap
        private HashMap<String, Integer> memory = new HashMap<>();

        private ExpressionTree(String postfix) {
            if (postfix.length() == 0) {
                throw new IllegalArgumentException("The postfix cannot be empty!");
            }
            final Stack<TreeNode> nodes = new Stack<>();
            String[] tokens = postfix.split("\\s");
            for (String token : tokens) {
                if (isOperator(token)) {
                    TreeNode leftNode = nodes.pop();
                    TreeNode rightNode = nodes.pop();
                    nodes.push(new TreeNode(token, leftNode, rightNode));
                } else {
                    nodes.add(new TreeNode(token, null, null));
                }
            }
            root = nodes.pop();
        }

        /**
         * Simple integer only exponentiation for parsing
         *
         * @param base     base to be raised by the exponent
         * @param exponent exponent power to multiply the base by (must be positive integer)
         * @return result
         */
        private int myPow(int base, int exponent) {
            int result = 1;
            for (int i = 0; i < exponent; i++) {
                result *= base;
            }
            return result;
        }

        /**
         * parse Expression tree via internal methods to obtain an integer result
         *
         * @ return {@code int}
         */
        private int parse() {
            return parser(root);
        }

        /**
         * method of parsing a Expression tree recursively to obtain an integer result
         *
         * @param node {@code TreeNode}
         * @return {@code int}
         */
        private int parser(TreeNode node) {

            if (isValue(node.value)) {
                return Integer.valueOf(node.value);
            } else if (isVariable(node.value)) {
                if (memory.get(node.value) != null) {
                    return memory.get(node.value);
                } else {
                    throw new RuntimeError(String.format(UNDEFINED_ERROR, node.value));
                }
            }
            switch (node.value) {
                case ("+"):
                    return parser(node.rChild) + parser(node.lChild);
                case ("-"):
                    return parser(node.rChild) - parser(node.lChild);
                case ("*"):
                    return parser(node.rChild) * parser(node.lChild);
                case ("/"):
                    return parser(node.rChild) / parser(node.lChild);
                case ("^"):
                    return myPow(parser(node.rChild), parser(node.lChild));
                case ("="):
                    int val;
                    String var;
                    try {
                        val = parser(node.lChild);
                        var = node.rChild.value;
                    } catch (NumberFormatException e) {
                        val = parser(node.rChild);
                        var = node.lChild.value;
                    }
                    memory.put(var, val);
                    return val;

                default:
                    throw new SyntaxError(String.format(BAD_CHARACTER_ERROR, node.value));
            }
        }

        /**
         * Node of an Expression Tree
         */
        private class TreeNode {
            private String value;
            private TreeNode lChild = null, rChild = null;

            private TreeNode(String value, TreeNode lChild, TreeNode rChild) {
                this.value = value;
                this.lChild = lChild;
                this.rChild = rChild;
            }
        }
    }
}
