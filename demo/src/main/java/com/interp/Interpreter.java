package com.interp;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Scanner;
import java.util.function.Function;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonPrimitive;

/**
 * CSC 417 Interpreter Project
 *
 */
public class Interpreter
{
    static boolean lexicalScope = false;
    /**
     * Main class, loops through a switch statement and exits with non zero exit status on errors.
     * @param args
     */
    public static void main( String[] args ) {
        // Create initial environment
        HashMap<String, Value> initialBindings = new HashMap<>();

        // Value creation for built-in functions
        Value addValue = new Value("built-in add procedure +", argsList -> {
            if (argsList.size() != 2 || 
                !(argsList.get(0) instanceof Long) ||
                !(argsList.get(1) instanceof Long)) {
                System.out.println("Add function requires exactly two signed integers.");
                System.exit(1);
            }

            Long a = (Long) argsList.get(0);
            Long b = (Long) argsList.get(1);
            return a + b;
        });

        Value subValue = new Value("built-in subtract procedure -", argsList -> {
            if (argsList.size() != 2 || 
                !(argsList.get(0) instanceof Long) ||
                !(argsList.get(1) instanceof Long)) {
                System.out.println("Sub function requires exactly two signed integers.");
                System.exit(1);
            }

            Long a = (Long) argsList.get(0);
            Long b = (Long) argsList.get(1);
            return a - b;
        });

        Value mulValue = new Value("built-in multiply procedure *", argsList -> {
            System.out.println("\nItems being multiplied: ");
            for (Object o : argsList) {
                System.out.print(o + ", ");
            }
            if (argsList.isEmpty() || argsList.stream().anyMatch(arg -> !(arg instanceof Long))) {
                throw new IllegalArgumentException("Function requires all arguments to be integers.");
            }

            return argsList.stream().mapToLong(arg -> (Long) arg).reduce(1, (a, b) -> a * b);
        });

        Value eqValue = new Value("built-in equals procedure =", argsList -> {
            if (argsList.size() != 2 || 
                !(argsList.get(0) instanceof Long) ||
                !(argsList.get(1) instanceof Long)) {
                System.out.println("Eq function requires exactly two signed integers.");
                System.exit(1);
            }

            Long a = (Long) argsList.get(0);
            Long b = (Long) argsList.get(1);
            return Objects.equals(a, b);
        });

        Value zeroValue = new Value("built-in zero? procedure", argsList -> {
            return ((long)argsList.get(0) == 0);
        });

        initialBindings.put("add", addValue);
        initialBindings.put("sub", subValue);
        initialBindings.put("mul", mulValue);
        initialBindings.put("eq", eqValue);
        initialBindings.put("true", new Value(true));
        initialBindings.put("false", new Value(false));
        initialBindings.put("zero?", zeroValue);
        Long x = (long) 10;
        Long v = (long) 5;
        Long i = (long) 1;
        initialBindings.put("x", new Value(x));
        initialBindings.put("v", new Value(v));
        initialBindings.put("i", new Value(i));
        
        Environment initialEnv = new Environment(initialBindings);
        
        try (Scanner scan = new Scanner(System.in)) {
            String input = scan.nextLine();
                
            // Parse the JSON string into a JsonObject
            JsonElement inputProgram = JsonParser.parseString(input);
                
            // Extract the program array
            System.out.println(eval(inputProgram, initialEnv).getData());
        }

    }

    /**
     * Interpreter main body where code dispatches to handle each different construct
     * in the 417 language
     * @param exp
     * @param env
     * @return
     */
    private static Value eval(JsonElement exp, Environment env) {

        // Tracing print
        //System.out.println("Evaluating " + exp.toString() + ": ");

        // Value result = new Value();

        // handle integer values and strings
        if (exp.isJsonPrimitive()) {
            JsonPrimitive prim = exp.getAsJsonPrimitive();
            // Case for number primitive
            if (prim.isNumber()) {
                if (prim.getAsLong() >= Long.MAX_VALUE || prim.getAsLong() <= Long.MIN_VALUE) {
                    System.out.println("417: Improper number (not a 64-bit integer)");
                    System.exit(1);
                }
                //return new Value(prim.getAsLong());
                return new Value(prim.getAsLong());
            }
            // Case for string literal
            else if (prim.isString()) {
                //return new Value(prim.getAsString());
                return new Value(prim.getAsString());
            }
            else {
                System.out.println("Error 417: Improper Json literal");
                System.exit(1);
            }
        }
        // SPECIAL FORM (keyword) or functionality
        else if (exp.isJsonObject()) {
            JsonObject expObj = exp.getAsJsonObject();

            for (Map.Entry<String, JsonElement> entry : expObj.entrySet()) {
                String key = entry.getKey();
                JsonElement value = entry.getValue();

                if (value.isJsonArray()) {
                    JsonArray specialFormArray = value.getAsJsonArray();
                    switch (key) {
                        case "Application":
                            return applyFunction(specialFormArray, env);
                        case "Lambda":
                            return makeFunction(specialFormArray.get(0), specialFormArray.get(1), env);
                        case "Block":
                            return executeBlock(specialFormArray, env);
                        case "Cond":
                            return applyConditional(specialFormArray, env);
                        case "Let":
                            return applyLet(specialFormArray, env);
                        case "Assignment":
                            return performAssignment(specialFormArray, env);
                        default:
                            System.out.println("417: unknown expression type");
                            System.exit(1);
                    }
                }
                else {
                    switch (key) {
                        case "Identifier":
                            //System.out.println("Value returned from lookup " + key + ": " + lookup(value.getAsString(), env).getData());
                            return lookup(value.getAsString(), env);
                        default:
                            System.out.println("417: unknown expression type");
                            System.exit(1);
                    }
                }
            }
        }
        // Error for invalid exp
        else {
            System.out.println("417: unknown expression type");
            System.exit(1);
        }
        return null; // never reached
    }

    /**
     * 
     * @param letExp
     * @param env
     * @return
     */
    private static Value applyLet(JsonElement letExp, Environment env) {
        JsonArray letArray = letExp.getAsJsonArray();

        JsonElement var = letArray.get(0);
        JsonElement rhs = letArray.get(1);
        JsonElement body = letArray.get(2);

        Value val = eval(rhs, env);

        HashMap<String, Value> newBinding = new HashMap<>();
        String key = var.getAsJsonObject().get("Identifier").getAsString();
        newBinding.put(key, val);

        return eval(body, extendEnvironment(newBinding, env));
    }

    /**
     * 
     * @param letExp
     * @param env
     * @return
     */
    private static Value performAssignment(JsonElement letExp, Environment env) {

        JsonArray letArray = letExp.getAsJsonArray();

        JsonElement id = letArray.get(0);
        JsonElement rhs = letArray.get(1);

        Value val = eval(rhs, env);

        JsonObject identifierObj = id.getAsJsonObject();
        String key = identifierObj.get("Identifier").getAsString();
        if (env.bindings.get(key) == null) {
            System.out.println("Error 417: No existing binding.");
            System.exit(1);
        }

        env.getBindings().put(key, val);
        
        return val;
    }

    /**
     * 
     * @param clauses to be evaluated
     * @param env
     * @return Value with either true or false as data
     */
    private static Value applyConditional(JsonElement clauses, Environment env) {
        JsonArray clauseArray = clauses.getAsJsonArray();

        if (clauseArray.isEmpty()) {
            return new Value(false);
        }

        JsonElement clause = clauseArray.remove(0);
        JsonObject clauseObj = clause.getAsJsonObject();
        JsonArray clauseArgs = clauseObj.get("Clause").getAsJsonArray();
        
        JsonElement test = clauseArgs.get(0);
        JsonElement consequence = clauseArgs.get(1);

        Value testVal = eval(test, env);
        if (!(testVal.getData() instanceof Boolean)) {
            System.out.println("Error 417: Not a boolean value.");
            System.exit(1);
        }

        if ((boolean) testVal.getData()) {
            return eval(consequence, env);
        }
        else {
            return applyConditional(clauses, env);
        }
    }

    /**
     * 
     * @return
     */
    private static Value executeBlock(JsonArray exps, Environment env) {
        // return false if empty
        if (exps.isEmpty()) {
            return new Value(false);
        }

        if (exps.size() == 1) {
            return eval(exps.get(0), env);
        }
        else {
            eval(exps.remove(0), env);
            return executeBlock(exps, env);
        }
    }

    /**
     * 
     * @param id identifier for expression
     * @param env
     * @return index of searched key
     */
    private static Value lookup(String id, Environment env) {
        System.out.println("Key being looked up: " + id);
        System.out.print(" Size of env: " + env.bindings.size());
        System.out.print(" Keys in bindings:");
        
        for (String key : env.bindings.keySet()) {
            System.out.print(key + ": " + env.bindings.get(key).getData() + ", ");
        }

        Value result = env.bindings.get(id);
        if (result == null) {
            System.out.println("Error 417: Identifier not found.");
            System.exit(1);
        }
        return result;
    }

    /**
     * Function for making a user-defined function given
     * @return
     */
    private static Value makeFunction(JsonElement args, JsonElement block, Environment env) {
        return new UserDefinedFunction(args, block, env);
    }

    /**
     * 
     * @param fn
     * @param args
     * @param env
     * @return
     */
    private static Value applyFunction(JsonElement specialFormArray, Environment env) {
        
        JsonArray fnArray = specialFormArray.getAsJsonArray();
        JsonElement fn = specialFormArray.getAsJsonArray().remove(0);

        List<Value> args = new ArrayList<>();
        for (JsonElement arg : fnArray) {
            args.add(eval(arg, env));
        }

        Value operator = eval(fn, env);

        // Check that operator is actually a function
        if (!operator.isFunction()) {
            System.out.println("Error 417: Not an applicable function.");
            System.exit(1);
        }

        // Check if operator is a built in function
        if (operator.getData() instanceof String) {
            System.out.println("Built-in application bindings: ");
            for (Map.Entry<String, Value> entry : env.getBindings().entrySet()) {
                String key = entry.getKey();
                Value val = entry.getValue();
    
                System.out.print(key + ": " + val.getData() + ", ");
            } 
            // Evaluate args for function
            List<Object> operands = new ArrayList<>();
            // System.out.println("\nExpressions in args");
            for (Value exp : args) {
                // System.out.print(exp.getData() + ", \n");
                operands.add(exp.getData());
            }

            return new Value(operator.invoke(operands));
        }
        else {
            UserDefinedFunction userOperator = (UserDefinedFunction) operator;
            //JsonArray userFnParams = userOperator.getParams().getAsJsonArray();
            JsonObject userFnParamObject = userOperator.getParams().getAsJsonObject();
            JsonArray userFnParamArray = userFnParamObject.get("Parameters").getAsJsonArray();

            // check scoping
            
            Environment functionEnv = userOperator.getEnvironment();
            if (!lexicalScope) {
                functionEnv = env;
            }

            if (userFnParamArray.size() != args.size()) {
                System.out.println("Error 417: Incorrect number of args for function application.");
                System.exit(1);
            }
            // Make new bindings for the evaluated args
            HashMap<String, Value> newBindings = new HashMap<>();
            for (int i = 0; i < args.size(); i++) {
                String key = userFnParamArray.get(i).getAsJsonObject().get("Identifier").getAsString();
                System.out.println("Key in new bindings: " + key + ", Value: " + args.get(i).getData() + ", isFunction: " + args.get(i).isFunction());
                newBindings.put(key, args.get(i));
            }
            // Extend environment with new bindings
            Environment newFunctionEnv = extendEnvironment(newBindings, functionEnv);

            /* System.out.println("NewFunctionEnv Bindings: ");
            for (Map.Entry<String, Value> entry : newFunctionEnv.getBindings().entrySet()) {
            String key = entry.getKey();
            Value val = entry.getValue();

            System.out.print(key + ": " + val.getData() + ", ");
            } */

            // Eval the function block with this extended environment
            return eval(userOperator.getBody(), newFunctionEnv);
        }
    }
    /**
     * Helper method for extending environments to simmulate scope
     * @param newBindings
     * @param initEnv
     * @return
     */
    private static Environment extendEnvironment(HashMap<String, Value> newBindings, Environment initEnv) {
        if (newBindings.isEmpty()) {
            return initEnv;
        }

        HashMap<String, Value> resultBindings = new HashMap<>();
        resultBindings.putAll(initEnv.getBindings());

        System.out.println("New Bindings: ");
        for (Map.Entry<String, Value> entry : newBindings.entrySet()) {
            String key = entry.getKey();
            Value val = entry.getValue();

            //System.out.print(key + ": " + val.getData() + ", ");
            resultBindings.put(key, val);
        }

        Environment resultEnvironment = new Environment(resultBindings);
        initEnv.next = resultEnvironment;
        resultEnvironment.next = null;

        /* System.out.println("Result Bindings: ");
        for (Map.Entry<String, Value> entry : resultEnvironment.getBindings().entrySet()) {
            String key = entry.getKey();
            Value val = entry.getValue();

            System.out.print(key + ": " + val.getData() + ", ");
        } */

        return resultEnvironment;
    }

    /**
     * Class to represent the Environment in the 417 language.
     * Will be initialized with built in functions and bindings for variables.
     * 
     * Extensions of the environment can be made when new bindings are introduced.
     */
    public static class Environment {
        HashMap<String, Value> bindings;
        Environment next;

        public Environment(HashMap<String, Value> initBindings) {
            this.bindings = initBindings;
            this.next = null;
        }

        public HashMap<String, Value> getBindings() {
            return this.bindings;
        }
    }

    /**
     * Class to represent any value in the 417 language.
     * Mapped to string identifiers in the environment.
     */
    public static class Value {
        Object data; // Holds regular data (returned for eval when the identifier of a function is given)
        Function<List<Object>, Object> function; // Holds function for application (if applicable)

        // default constructor (sets function to null/assumes there is no apply)
        public Value() {
            this.data = 0;
            this.function = null;
        }

        public Value(Object inputData) {
            this.data = inputData;
            this.function = null;
        }

        public Value(Function<List<Object>, Object> inputFunction) {
            this.data = 0;
            this.function = inputFunction;
        }

        public Value(Object inputData, Function<List<Object>, Object> inputFunction) {
            this.data = inputData;
            this.function = inputFunction;
        }

        private Object getData() {
            return this.data;
        }

        public boolean isFunction() {
            return this.function != null;
        }

        public Object invoke(List<Object> args) {
            if (!isFunction()) {
                throw new IllegalStateException("This value does not represent a built-in function.");
            }
            return this.function.apply(args);
        }
    }

    public static class UserDefinedFunction extends Value {
        JsonElement fnParams;
        JsonElement fnBody;
        Environment fnEnv;

        public UserDefinedFunction() {
            this.function = (x -> x); // AAAAAAAAAA >>???
            this.fnParams = null;
            this.fnBody = null;
            this.fnEnv = null;
        }

        public UserDefinedFunction(JsonElement params, JsonElement body, Environment env) {
            this.function = (x -> x); // LOL 
            this.fnParams = params;
            this.fnBody = body;
            this.fnEnv = env;
        }

        private JsonElement getParams() {
            return this.fnParams;
        }

        private JsonElement getBody() {
            return this.fnBody;
        }

        private Environment getEnvironment() {
            return this.fnEnv;
        }
    } 
}
