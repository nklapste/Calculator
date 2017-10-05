import java.util.*;
import java.util.regex.Pattern;


import java.util.regex.Matcher;

/**
 * Assignment 3: Exception handling <br />
 * Calculator using BNF
 *
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
                "(ler x = 5) ^ (let y = 6);",   // 5, runtime error: 'ler' undefined NOTE: should this be a missing let operator syntax error?
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

    //TODO

    /**
     * Check if all "let variable =" components are valid syntax throws an SyntaxError if invalid Syntax
     * @param  exp {@code string}
     */
    private void checkEquals(String exp) {
        Pattern p;
        Matcher m;

        // find all matches of a "variable ="  occurrence
        p = Pattern.compile("([a-zA-Z][a-zA-Z0-9]*) =");
        m = p.matcher(exp);
        // Check all occurrences
        while (m.find()) {
            if (m.start() <= 3) {
                throw new SyntaxError("Missing 'let' in 'var =' operation");
            } else {
                String check = exp.substring(m.start() - 4, m.start()-1);
                if (!check.equals("let")){
                    throw new SyntaxError("Missing 'let' in 'var =' operation");
                }
            }
        }

        // find all matches of a "let variable" occurrence
        p = Pattern.compile("let ([a-zA-Z][a-zA-Z0-9]*)");
        m = p.matcher(exp);
        // Check all occurrences
        while (m.find()) {
            if (exp.length() <= m.end()+4) {
                throw new SyntaxError("'=' expected");
            } else {
                String check = exp.substring(m.end(), m.end()+3);
                if (!check.equals(" = ")){
                    throw new SyntaxError("'=' expected");
                }
            }
        }
    }

    /**
     * Check that an ending semicolon exists
     * @param exp
     */
    public void checkSemiColons(String exp){
        Pattern p = Pattern.compile(";$");
        Matcher m =  p.matcher(exp);
        if (!m.find()){
            throw new SyntaxError("Missing closing ';'");
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
        checkEquals(exp);
        checkSemiColons(exp);

        // chop of ending ';' as it is not needed
        exp = exp.replaceAll(";$","");

        // add spaces between "(" and ")" to make parsing easier
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

            // flag noting that an operator was the previous token assume true for start of parsing
            boolean prevOperator = true;

            // variables for additional checking between

            // flag noting that the let operator was the previous token
            boolean prevLet = false;

            // flag noting that a let variable was the previous token
            boolean prevVar = false;

            // flag noting that the equal operator was the previous token
            boolean prevEqual = false;

            // storage of the previous valid value/variable token
            String prevValue = "null";

            for (String token : expList) {

                // operator
                if (ops.containsKey(token)) {
                    //todo clean
                    if (prevVar && token.equals("=")){
                        prevEqual = true;
                        prevVar = false;
                    } else if (prevVar) {
                        throw new SyntaxError("'=' expected");
                    } else if (token.equals("=")) {
                        throw new SyntaxError("Missing 'let' in 'var =' operation");
                    }

                    while (!stringStack.isEmpty() && isHigherPrec(token, stringStack.peek()))
                        output.append(stringStack.pop()).append(' ');
                    stringStack.push(token);

                    // set flag noting that an operator was the last token
                    prevOperator = true;

                    // left parenthesis
                } else if (token.equals("(")) {
                    stringStack.push(token);
                    bracketStack.push(token);

                    // check if operator was before
                    if(!prevOperator){
                        throw new SyntaxError(String.format("Missing operator between %s and %s", prevValue, token));
                    }

                    // right parenthesis
                } else if (token.equals(")")) {
                    if (!bracketStack.pop().equals("(")) {
                        throw new SyntaxError("'(' expected");
                    }

                    // add contents contained in bracket pair to string stack
                    while (!stringStack.peek().equals("("))
                        output.append(stringStack.pop()).append(' ');
                    stringStack.pop();

                    // set prev operator as false
                    prevOperator = false;
                    prevValue = ")";

                } else if (token.equals("let") && !prevLet){
                    //todo clean
                    prevLet = true;
                    // check if operator was before
                    if(!prevOperator){
                        throw new SyntaxError(String.format("Missing operator between %s and %s", prevValue, token));
                    }
                } else {
                    // todo clean
                    if (prevEqual && (isValue(token)|isVariable(token))){
                        prevEqual = false;
                    }   else if (prevEqual){
                        throw new SyntaxError("Variable or value expected after '='");
                    }


                    if(prevLet && isVariable(token)){
                        prevVar = true;
                        prevLet = false;
                    } else if (prevLet){
                        throw new SyntaxError("Variable expected to be declared after 'let'");
                    }

                        // TODO: clean
                    Pattern p = Pattern.compile("[^a-zA-Z0-9]");
                    Matcher m =  p.matcher(token);
                    if (m.find()){
                        throw new SyntaxError(String.format("'%s' is contains an illegal character '%s'", token, m.group()));
                    } else {
                        if (prevOperator) {
                            prevOperator = false;
                            prevValue = token;
                            output.append(token).append(' ');
                        } else if(!prevVar){
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
                    throw new RuntimeError(String.format("'%s' undefined.", node.value));
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
                    throw new SyntaxError(String.format("Incorrect Operator %s", node.value));
            }
        }
    }
}
