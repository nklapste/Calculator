import java.util.*;
import java.util.regex.Pattern;


import java.util.regex.Matcher;

/**
 * Assignment 3: Exception handling <br />
 * Calculator using BNF
 * Name: Nathan Klapstien
 * ID: 1449872
 */

// custom created exceptions using RuntimeException for ease of use
class SyntaxError extends RuntimeException{
    SyntaxError(String s){
        super(s);
    }
}
class RuntimeError extends RuntimeException{
    RuntimeError(String s){
        super(s);
    }
}

public class Calculator {


    private Map<String, Operator> ops = new HashMap<String, Operator>() {{
        put("+", Operator.ADD);
        put("-", Operator.SUBTRACT);
        put("*", Operator.MULTIPLY);
        put("/", Operator.DIVIDE);
        put("^", Operator.EXPONENTIATION);
        put("=", Operator.EQUALS);
    }};

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
                "(let y = 3) ^ (let x = 2);"                                                            // 9, returns 9
        };
        for (int i = 0; i < inputs.length; i++) {
            System.out.println(String.format("%d -- %-90s %d", i + 1, inputs[i], calc.execExpression(inputs[i])));
        }
        // Part 2
        inputs = new String[]{
                "1 + (2 * 3;",                  // 1, syntax error: ')' expected
                "(let x 5) + x;",               // 2, syntax error: '=' expected
                "(let x = 5) (let y = 6);",     // 3, syntax error: operator expected
                "(let x = 5 let y = 6);",       // 4, syntax error: ')' expected
                "(ler x = 5) ^ (let y = 6);",   // 5, runtime error: 'ler' undefined
                "(let x = 5) + y;"              // 6, runtime error: 'y' undefined
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
     * Check if a string is a valid operator (+ - * % ^ =)
     */
    private boolean isOperator(String op) {
        return ops.containsKey(op);
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
     * Convert a expression string to a reverse polish notation expression string and then convert it to a
     * expression tree
     *
     * @param exp {@code String}    A normal formatted expression string
     * @return {@code ExpressionTree}
     */
    private ExpressionTree treeify(String exp) {
        //todo validate lets and other vars
        exp = exp.replaceAll("let ", "");
        exp = exp.replaceAll(";", "");
        exp = exp.replaceAll("\\(", "( ");
        exp = exp.replaceAll("\\)", " )");

        ShuntingYard shuntingYard = new ShuntingYard();
        String rpnExp = shuntingYard.postfix(exp);

        return new ExpressionTree(rpnExp);

    }

    /**
     * Execute the expression, and return the correct value
     *
     * @param exp {@code String} The expression string
     * @return {@code int}    The value of the expression
     */
    private int execExpression(String exp) {
        return new Parser().parse(treeify(exp).root);
    }

    private enum Operator {
        ADD(1), SUBTRACT(2), MULTIPLY(3), DIVIDE(4), EXPONENTIATION(6), EQUALS(0);
        final int precedence;

        Operator(int p) {
            precedence = p;
        }
    }

    /**
     * Dijkstra's shuntingyard algorithm converting normal notation math to reverse polish notation
     */
    private class ShuntingYard {

        private boolean isHigherPrec(String op, String sub) {
            return (ops.containsKey(sub) && ops.get(sub).precedence >= ops.get(op).precedence);
        }

        private String postfix(String infix) {
            StringBuilder output = new StringBuilder();

            // stack for managing operations within brackets
            Deque<String> stringStack = new LinkedList<>();

            // separate stage for just tallying the brackets
            Deque<String> bracketStack = new LinkedList<>();

            String[] expList = infix.split("\\s");

            //TODO CLEAN
            boolean prevOperator = true;
            String prevValue = "null";

            for (String token : expList) {

                // operator
                if (ops.containsKey(token)) {
                    while (!stringStack.isEmpty() && isHigherPrec(token, stringStack.peek()))
                        output.append(stringStack.pop()).append(' ');
                    stringStack.push(token);
                    prevOperator = true;

                    // left parenthesis
                } else if (token.equals("(")) {
                    stringStack.push(token);
                    bracketStack.push(token);

                    // right parenthesis
                } else if (token.equals(")")) {
                    if (!bracketStack.pop().equals("(")) {
                        throw new SyntaxError("'(' expected");
                    }
                    while (!stringStack.peek().equals("("))
                        output.append(stringStack.pop()).append(' ');
                    stringStack.pop();
                } else {

                        // todo error on incorrect operators
                    Pattern p = Pattern.compile("[^a-zA-Z0-9]");
                    Matcher m =  p.matcher(token);
                    if (m.find()){
                        throw new SyntaxError(String.format("'%s' is contains an illegal character '%s'", token, m.group()));
                    } else {
                        if (prevOperator) {
                            prevOperator = false;
                            prevValue = token;
                            output.append(token).append(' ');
                        } else {
                            throw new SyntaxError(String.format("Missing operator between %s and %s", prevValue, token));
                        }
                    }
                }
            }

            while (!stringStack.isEmpty())
                output.append(stringStack.pop()).append(' ');

            if (!bracketStack.isEmpty()) {
                throw new SyntaxError("')' expected");
            }
            return output.toString();
        }
    }

    /**
     * The Expression tree for parsing and equating a reverse polish notation string
     */
    private class ExpressionTree {

        private TreeNode root;

        public ExpressionTree(String postfix) {
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

    /**
     * Parser class that contains the method of parsing a Expression tree to obtain an integer result
     */
    private class Parser {

        // Parser memory hashmap
        private HashMap<String, Integer> memory = new HashMap<>();

        /**
         * Simple integer only exponentiation
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

        private int parse(ExpressionTree.TreeNode node) {

            if (isValue(node.value)) {
                return Integer.valueOf(node.value);
            } else if (isVariable(node.value)) {
                if (memory.get(node.value) != null) {
                    return memory.get(node.value);
                } else {
                    throw new RuntimeError(String.format("Variable: '%s' not defined.", node.value));
                }
            }
            switch (node.value) {
                case ("+"):
                    return parse(node.rChild) + parse(node.lChild);
                case ("-"):
                    return parse(node.rChild) - parse(node.lChild);
                case ("*"):
                    return parse(node.rChild) * parse(node.lChild);
                case ("/"):
                    return parse(node.rChild) / parse(node.lChild);
                case ("^"):
                    return myPow(parse(node.rChild), parse(node.lChild));
                case ("="):
                    int val;
                    String var;
                    try {
                        try {
                            val = Integer.valueOf(node.lChild.value);
                        } catch (NumberFormatException e1) {
                            val = parse(node.lChild);
                        }
                        var = node.rChild.value;
                    } catch (NumberFormatException e) {
                        try {
                            val = Integer.valueOf(node.rChild.value);
                        } catch (NumberFormatException e2) {
                            val = parse(node.rChild);
                        }
                        var = node.lChild.value;
                    }
                    memory.put(var, val);
                    return val;

                default:
                    //TODO INSPECT
                    throw new SyntaxError(String.format("Incorrect Operator %s", node.value));
            }
        }
    }
}
