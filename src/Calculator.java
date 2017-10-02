
import javafx.css.Match;

import java.util.LinkedList;

import java.util.*;
import java.util.regex.*;

/**
 * Assignment 3: Exception handling <br />
 * Calculator using BNF
 */
public class Calculator {


    public enum Operator {
        ADD(1), SUBTRACT(2), MULTIPLY(3), DIVIDE(4), EXPONENTIATION(5), EQUALS(6);
        final int precedence;
        Operator(int p) { precedence = p; }
    }

    private  Map<String, Operator> ops = new HashMap<String, Operator>() {{
        put("+", Operator.ADD);
        put("-", Operator.SUBTRACT);
        put("*", Operator.MULTIPLY);
        put("/", Operator.DIVIDE);
        put("^", Operator.EXPONENTIATION);
        put("=", Operator.EQUALS);
    }};

    public class ShuntingYard {

        private boolean isHigerPrec(String op, String sub) {
            return (ops.containsKey(sub) && ops.get(sub).precedence >= ops.get(op).precedence);
        }

        public String postfix(String infix) {
            StringBuilder output = new StringBuilder();
            Deque<String> stack  = new LinkedList<>();

            String[] expList = infix.split("\\s");

            for (String token : expList) {

                //TODO  fix ( and ) config

                // operator
                if (ops.containsKey(token)) {
                    while ( ! stack.isEmpty() && isHigerPrec(token, stack.peek()))
                        output.append(stack.pop()).append(' ');
                    stack.push(token);

                    // left parenthesis
                } else if (token.equals("(")) {
                    stack.push(token);

                    // right parenthesis
                } else if (token.equals(")")) {
                    while (!stack.peek().equals("("))
                        output.append(stack.pop()).append(' ');
                    stack.pop();
                } else {
                    output.append(token).append(' ');
                }
            }

            while ( ! stack.isEmpty())
                output.append(stack.pop()).append(' ');

            return output.toString();
        }
    }


    // todo clean
    private boolean isOperator(String token){
        return ops.containsKey(token);
    }

    /**
     * Check if a string is a valid value (any number)
     */
    private boolean isValue(String val){
        Pattern valuePattern = Pattern.compile("[0-9]+");
        Matcher m = valuePattern.matcher(val);
        return m.matches();
    }


    /**
     * Check if a string is a valid variable (Starting with a letter then any alpha-numeric afterwards)
     */
    private boolean isVariable(String var){
        Pattern variablePattern = Pattern.compile("[a-zA-Z][a-zA-Z0-9]*");
        Matcher m = variablePattern.matcher(var);
        return m.matches();
    }


    /**
     * The Expression tree for parsing and equating a reverse polish notation string
     */
    public class ExpressionTree {

        public class TreeNode {
            public String value;
            public TreeNode lChild = null, rChild = null;

            public TreeNode(String value, TreeNode lChild, TreeNode rChild) {
                this.value = value;
                this.lChild = lChild;
                this.rChild = rChild;
            }
        }

        private TreeNode root;

        public ExpressionTree(String postfix) {
            if (postfix.length() == 0) {
                throw new IllegalArgumentException("The postfix cannot be empty!");
            }
            final Stack<TreeNode> nodes = new Stack<>();
            String[] tokens = postfix.split("\\s");
            for (String token : tokens) {
                token = token.replaceAll("[()]", "");
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
    }


    public ExpressionTree treeify(String exp){
        // remove syntax parts that don't help
        exp = exp.replaceAll("let ", "");
        exp = exp.replaceAll(";", "");

        ShuntingYard shuntingYard = new ShuntingYard();
        String rpnexp = shuntingYard.postfix(exp);

        //TODO DEBUG
        System.out.println("RPN: "+ rpnexp);

        return new ExpressionTree(rpnexp);

    }


    // global memory hashmap
    HashMap<String, Integer> memory = new HashMap<>();

    public int parse(ExpressionTree.TreeNode node) {

        // TODO DEBUG
        System.out.println("Value: " + node.value);
        if (node.lChild != null) {
            System.out.println("Left: " + node.lChild.value);
        }
        if (node.rChild != null) {
            System.out.println("Right: " + node.rChild.value);
        }

        if (isValue(node.value)) {
            return Integer.valueOf(node.value);
        } else if (isVariable(node.value)) {
            if (memory.get(node.value) != null) {
                return memory.get(node.value);
            } else {
                throw new IllegalArgumentException("Variable was not initialized.");
            }

        } else if (isOperator(node.value)) {
            switch (node.value) {
                case ("+"):
                    return parse(node.rChild) + parse(node.lChild);
                case ("-"):
                    return parse(node.rChild) - parse(node.lChild);
                case ("*"):
                    return parse(node.rChild) * parse(node.lChild);
                case ("/"):
                    return parse(node.rChild) / parse(node.lChild);
                case ("="):

                    int val;
                    String var;
                    try  {
                        val = Integer.valueOf(node.lChild.value);
                        var = node.rChild.value;
                    } catch (NumberFormatException e) {
                        val = Integer.valueOf(node.rChild.value);
                        var = node.lChild.value;
                    }
                    memory.put(var, val);
                    return  val;

                default:
                    throw new IllegalArgumentException("Incorrect Operator.");
            }
        } else {
            throw new IllegalArgumentException("Incorrect input.");
        }
    }


    /**
     * Execute the expression, and return the correct value
     * @param exp           {@code String} The expression string
     * @return              {@code int}    The value of the expression
     */
    public int execExpression(String exp) {
        int returnValue = -1;
        // TODO: Assignment 3 Part 1 -- parse, calculate the expression, and return the correct value
        ExpressionTree expTree = treeify(exp);

        returnValue = parse(expTree.root);
        // TODO: Assignment 3 Part 2-1 -- when come to illegal expressions, raise proper exceptions

        return returnValue;
    }

    /**
     * Main entry
     * @param args          {@code String[]} Command line arguments
     */
    public static void main(String[] args) {
        Calculator calc = new Calculator();
        // Part 1
        String[] inputs = {
            "let x = 1;",                                                                           // 1, returns 1
            "(let x = 1) + x;",                                                                     // 2, returns 2
            "(let a = 2) + 3 * a - 5;",                                                             // 3, returns 3
            "(let x = (let y = (let z = 1))) + x + y + z;",                                         // 4, returns 4 TODO: failing
            "1 + (let x = 1) + (let y = 2) + (1 + x) * (1 + y) - (let x = y) - (let y = 1) - x;",   // 5, returns 5
            "1 + (let a = (let b = 1) + b) + a + 1;",                                               // 6, returns 6
            "(let a = (let a = (let a = (let a = 2) + a) + a) + a) - 9;",                           // 7, returns 7
            "(let x = 2) ^ (let y = 3);",                                                           // 8, returns 8
            "(let y = 3) ^ (let x = 2);"                                                            // 9, returns 9
        };
        for (int i = 0; i < inputs.length; i++) {
            try {
                //TODO DEBUG
                System.out.println(String.format("TEST %d %-90s", i+1, inputs[i]));
                System.out.println(String.format("%d -- %-90s %d", i + 1, inputs[i], calc.execExpression(inputs[i])));
            } catch (Exception e) { //TODO
                e.printStackTrace(System.out);
            }
        }
        // Part 2
        inputs = new String[] {
                "1 + (2 * 3;",                  // 1, syntax error: ')' expected
                "(let x 5) + x;",               // 2, syntax error: '=' expected
                "(let x = 5) (let y = 6);",     // 3, syntax error: operator expected
                "(let x = 5 let y = 6);",       // 4, syntax error: ')' expected
                "(ler x = 5) ^ (let y = 6);",   // 5, runtime error: 'ler' undefined
                "(let x = 5) + y;"              // 6, runtime error: 'y' undefined
        };
        // TODO: Assignment 3 Part 2-2 -- catch and deal with your exceptions here
        for (int i = 0; i < inputs.length; i++)

            try {
                //TODO DEBUG
                System.out.println(String.format("TEST %d %-90s", i+1, inputs[i]));
                System.out.println(String.format("%d -- %-30s %d", i + 1, inputs[i], calc.execExpression(inputs[i])));
            } catch (Exception e){ //TODO
                e.printStackTrace(System.out);
            }
    }

}
