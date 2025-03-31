/*
 * Copyright 2025 Google LLC
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package ai.patterns.utils;

public class Ansi {

    public static String red(String msg) {
        return "\u001B[31m\u001B[1m" + msg + "\u001B[22m\u001B[0m";
    }

    public static String green(String msg) {
        return "\u001B[32m\u001B[1m" + msg + "\u001B[22m\u001B[0m";
    }

    public static String blue(String msg) {
        return "\u001B[34m\u001B[1m" + msg + "\u001B[22m\u001B[0m";
    }

    public static String yellow(String msg) {
        return "\u001B[33m\u001B[1m" + msg + "\u001B[22m\u001B[0m";
    }

    public static String cyan(String msg) {
        return "\u001B[36m\u001B[1m" + msg + "\u001B[22m\u001B[0m";
    }

    public static String underline(String msg) {
        return "\u001B[4m" + msg + "\u001B[0m";
    }

    public static String bold(String msg) {
        return "\u001B[1m" + msg + "\u001B[0m";
    }

    public static String strikethrough(String msg) {
        return "\u001B[9m" + msg + "\u001B[0m";
    }

    public static String italic(String msg) {
        return "\u001B[3m" + msg + "\u001B[0m";
    }

    public static String markdown(String msg) {
        return msg
            // Bold
            .replaceAll("\\*\\*(.*?)\\*\\*", "\u001B[1m$1\u001B[0m")
            // Italic
            .replaceAll("\\*(.*?)\\*", "\u001B[3m$1\u001B[0m")
            // Underline
            .replaceAll("__(.*?)__", "\u001B[4m$1\u001B[0m")
            // Strikethrough
            .replaceAll("~~(.*?)~~", "\u001B[9m$1\u001B[0m")

            // Blockquote
            .replaceAll("(> ?.*)", "\u001B[3m\u001B[34m\u001B[1m$1\u001B[22m\u001B[0m")

            // Lists (bold magenta number and bullet)
            .replaceAll("([\\d]+\\.|-|\\*) (.*)", "\u001B[35m\u001B[1m$1\u001B[22m\u001B[0m $2")

            // Block code (black on gray)
            .replaceAll("(?s)```(\\w+)?\\n(.*?)\\n```", "\u001B[3m\u001B[1m$1\u001B[22m\u001B[0m\n\u001B[57;107m$2\u001B[0m\n")
            // Inline code (black on gray)
            .replaceAll("`(.*?)`", "\u001B[57;107m$1\u001B[0m")

            // Headers (cyan bold)
            .replaceAll("(#{1,6}) (.*?)\n", "\u001B[36m\u001B[1m$1 $2\u001B[22m\u001B[0m\n")
            // Headers with a single line of text followed by 2 or more equal signs
            .replaceAll("(.*?\n={2,}\n)", "\u001B[36m\u001B[1m$1\u001B[22m\u001B[0m\n")
            // Headers with a single line of text followed by 2 or more dashes
            .replaceAll("(.*?\n-{2,}\n)", "\u001B[36m\u001B[1m$1\u001B[22m\u001B[0m\n")


            // Images (blue underlined)
            .replaceAll("!\\[(.*?)]\\((.*?)\\)", "\u001B[34m$1\u001B[0m (\u001B[34m\u001B[4m$2\u001B[0m)")
            // Links (blue underlined)
            .replaceAll("!?\\[(.*?)]\\((.*?)\\)", "\u001B[34m$1\u001B[0m (\u001B[34m\u001B[4m$2\u001B[0m)")
            ;
    }

    public static void main(String[] args) {
        String markdownText = """
            Main title
            ==========
            Big title
            
            Subtitle
            --------
            Small title
            
            # Bold and italic
            
            Some **bold text**.
            Bits of *italicized text*.
            It's __underlined__.
            And ~~striked through~~.
            
            ## Links
            
            A [link](https://www.example.com) to an article.
            
            ![alt text](image.jpg)
            
            ### Quoting
            
            > a quote of someone famous, potentially wrapping around multiple lines.
            
            # Lists
            
            1. First item
            2. Second item
            3. Third item
            
            - First item
            - Second item
            - Third item
            
            # Code
            
            Some inline `code` inside a paragraph.
            Return type is `void` and args are `String[]`.
            
            A fenced code block:
            
            ```java
            public class Hello {
                public static void main(String[] args) {
                    System.out.println("Hello World!");
                }
            }
            
            ```
            """;

        System.out.println(markdown(markdownText));
    }
}
