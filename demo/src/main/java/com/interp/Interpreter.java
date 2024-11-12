package com.interp;

import java.util.HashMap;
import java.util.Scanner;

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
    /**
     * Main class, loops through a switch statement and exits with non zero exit status on errors.
     * @param args
     */
    public static void main( String[] args ) {
        System.out.println( "Hello World!" );

        // Create initial environment
        HashMap<String, Value> initialBindings = new HashMap();
        Environment initialEnv = new Environment(initialBindings);

        // BINDINGS FOR INIT ONCE I FIGURE THIS OUT
       /*  initialBindings.put("add", );
        initialBindings.put("sub", );
        initialBindings.put("mul", );
        initialBindings.put("true", );
        initialBindings.put("false", );
        initialBindings.put("x", );
        initialBindings.put("v", );
        initialBindings.put("i", ); */

        while (true) { 
            try (Scanner scan = new Scanner(System.in)) {
                String input = scan.nextLine();
                
                // Parse the JSON string into a JsonObject
                JsonObject inputProgram = JsonParser.parseString(input).getAsJsonObject();
                
                // Extract the program array
                JsonArray programArray = inputProgram.getAsJsonArray();
                
                // Iterate through each element of json array
                for (JsonElement element : programArray) {
                    eval(element, initialEnv);
                }
            }
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

        Value result = new Value();

        // handle integer values and strings
        if (exp.isJsonPrimitive()) {
            JsonPrimitive prim = exp.getAsJsonPrimitive();
            // Case for number primitive
            if (prim.isNumber()) {
                if (prim.getAsLong() >= Long.MAX_VALUE || prim.getAsLong() <= Long.MIN_VALUE) {
                    System.out.println("417: Improper number (not a 64-bit integer)");
                    System.exit(1);
                }
                result.setData(prim.getAsLong());
            }
            // Case for string literal
            else if (prim.isString()) {
                result.setData(prim.getAsString());
            }
        }
        

        return result;
    }

    /**
     * Helper method for introducing new bindings to an environment
     * @param id
     * @param val
     * @param env
     */
    private static void bind(String id, Value val, Environment env) {
        env.bindings.put(id, val);
    }

    /**
     * Helper method for extending environments to simmulate scope
     * @param newBindings
     * @param initEnv
     * @return
     */
    private Environment extendEnvironment(HashMap<String, Value> newBindings, Environment initEnv) {
        if (newBindings.isEmpty()) {
            return initEnv;
        }

        HashMap<String, Value> resultBindings = initEnv.bindings;

        for (String id : newBindings.keySet()) {
            resultBindings.put(id, newBindings.get(id));
        }

        Environment resultEnvironment = new Environment(resultBindings);
        initEnv.next = resultEnvironment;
        resultEnvironment.next = null;

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
    }

    /**
     * Class to represent any value in the 417 language.
     * Mapped to string identifiers in the environment.
     */
    public static class Value {
        Object data;

        public Value() {
            this.data = 0;
        }

        private void setData(Object newData) {
            this.data = newData;
        }

        public Object getData() {
            return this.data;
        }
    }
}
