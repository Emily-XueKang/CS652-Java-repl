package cs652.repl;

/**
 * Created by xuekang on 1/29/17.
 */

import java.io.BufferedReader;
import java.io.EOFException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Stack;

public class NestedReader{
    StringBuilder buf;
    BufferedReader input;
    int c;

    public NestedReader(BufferedReader input){
        buf = new StringBuilder();
        this.input = input;
    }
    public String getNestedString() throws IOException{
        c = input.read();
        Stack<Character> stack = new Stack<Character>();
        while(true) {
            // a flag of end of statement
            boolean EOS = false;
            switch (c) {
                case '{': stack.push('}'); break;
                case '[': stack.push(']'); break;
                case '(': stack.push(')'); break;
                case '}': { if (stack.isEmpty() || stack.pop() != '}') EOS = true; break; }
                case ']': { if (stack.isEmpty() || stack.pop() != ']') EOS = true; break; }
                case ')': { if (stack.isEmpty() || stack.pop() != ')') EOS = true; break; }
                case '/': { while(c != '\n') {c = input.read();} break;} //when encounter /,read this line to the end, while do not send this line to new code.
                case -1: throw new EOFException();  //when encounter end of file(input character -1), give out an End_of_File exception,which will be catched by method in JavaREPL.
            }
            if ((stack.empty() && c == '\n') || EOS == true) break;
            this.consume();
        }
        // reset the variables for next call
        String result = buf.toString();
        buf = new StringBuilder();
        c = 0;
        return result.trim();
    }

    public void consume() throws IOException{
        buf.append((char)c);
        c = input.read();
    }

    public static void main(String[] args) {
        BufferedReader stdin = new BufferedReader(new InputStreamReader(System.in));
        NestedReader reader = new NestedReader(stdin);
        while (true) {
            System.out.print("> ");
            try {
                String java = reader.getNestedString();
                System.out.println(java);
            } catch(IOException e) {
                System.out.println(e.toString());
                break;
            }
        }
    }
}

