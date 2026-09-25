package com.codearc.app.projects

object Templates {
 val languages = listOf("Python", "HTML", "JavaScript", "C", "C++", "Java", "Kotlin", "Lua")
 val names = listOf("Empty Project", "Hello World", "Console Application", "Basic Calculator", "Input / Output Example")
 fun file(language: String) = when(language) { "Python" -> "main.py"; "HTML" -> "index.html"; "JavaScript" -> "main.js"; "C" -> "main.c"; "C++" -> "main.cpp"; "Java" -> "Main.java"; "Kotlin" -> "main.kt"; else -> "main.lua" }
 fun languageFor(fileName: String): String = when (fileName.substringAfterLast('.', "").lowercase()) {
  "py" -> "Python"; "html","htm" -> "HTML"; "css" -> "CSS"; "js" -> "JavaScript"; "c" -> "C"; "cpp","cc","cxx" -> "C++"; "java" -> "Java"; "kt" -> "Kotlin"; "lua" -> "Lua"; else -> "Python"
 }
 fun code(language: String, template: String): String {
  if(template == "Empty Project") return ""
  val input = template == "Input / Output Example"
  val calc = template == "Basic Calculator"
  val console = template == "Console Application"
  return when(language) {
   "Python" -> if(input) "name = input(\"Your name: \")\nprint(\"Hello,\", name)\n" else if(calc) "a = float(input(\"First number: \"))\nb = float(input(\"Second number: \"))\nprint(\"Sum:\", a + b)\n" else "def main():\n    print(\"Hello, CodeArc!\")\n\nif __name__ == \"__main__\":\n    main()\n"
   "HTML" -> if(calc) "<!DOCTYPE html>\n<html lang=\"en\">\n<head>\n  <meta charset=\"UTF-8\">\n  <title>Calculator</title>\n  <link rel=\"stylesheet\" href=\"style.css\">\n</head>\n<body>\n  <h1>Basic Calculator</h1>\n  <input id=\"a\" type=\"number\" placeholder=\"First number\">\n  <input id=\"b\" type=\"number\" placeholder=\"Second number\">\n  <button id=\"sum\">Sum</button>\n  <p id=\"result\"></p>\n  <script src=\"script.js\"></script>\n</body>\n</html>\n"
    else if(input) "<!DOCTYPE html>\n<html lang=\"en\">\n<head>\n  <meta charset=\"UTF-8\">\n  <title>Say Hello</title>\n  <link rel=\"stylesheet\" href=\"style.css\">\n</head>\n<body>\n  <h1>What's your name?</h1>\n  <input id=\"name\" type=\"text\" placeholder=\"Your name\">\n  <button id=\"greet\">Say hello</button>\n  <p id=\"output\"></p>\n  <script src=\"script.js\"></script>\n</body>\n</html>\n"
    else if(console) "<!DOCTYPE html>\n<html lang=\"en\">\n<head>\n  <meta charset=\"UTF-8\">\n  <title>Console Demo</title>\n  <link rel=\"stylesheet\" href=\"style.css\">\n</head>\n<body>\n  <h1>Open the Output panel and tap Run</h1>\n  <p>This page logs to the console instead of the page — check the Output panel after Run.</p>\n  <script src=\"script.js\"></script>\n</body>\n</html>\n"
    else "<!DOCTYPE html>\n<html lang=\"en\">\n<head>\n  <meta charset=\"UTF-8\">\n  <title>CodeArc Web Project</title>\n  <link rel=\"stylesheet\" href=\"style.css\">\n</head>\n<body>\n  <h1>Hello, CodeArc!</h1>\n  <p>Edit index.html, style.css and script.js, then tap Run to preview.</p>\n  <script src=\"script.js\"></script>\n</body>\n</html>\n"
   "JavaScript" -> if(input || calc) "const readline = require('node:readline');\nconst rl = readline.createInterface({ input: process.stdin, output: process.stdout });\n" + (if(calc) "rl.question('First number: ', a => rl.question('Second number: ', b => {\n  console.log('Sum:', Number(a) + Number(b));\n  rl.close();\n}));\n" else "rl.question('Your name: ', name => { console.log('Hello,', name); rl.close(); });\n") else "function main() {\n  console.log('Hello, CodeArc!');\n}\nmain();\n"
   "C" -> "#include <stdio.h>\nint main(void) {\n" + (if(calc) "    double a, b;\n    printf(\"Enter two numbers: \");\n    if (scanf(\"%lf %lf\", &a, &b) != 2) return 1;\n    printf(\"Sum: %g\\n\", a + b);\n" else if(input) "    char name[80];\n    printf(\"Your name: \");\n    if (scanf(\"%79s\", name) != 1) return 1;\n    printf(\"Hello, %s\\n\", name);\n" else "    puts(\"Hello, CodeArc!\");\n") + "    return 0;\n}\n"
   "C++" -> "#include <iostream>\n#include <string>\nint main() {\n" + (if(calc) "    double a, b;\n    std::cin >> a >> b;\n    std::cout << \"Sum: \" << a + b << std::endl;\n" else if(input) "    std::string name;\n    std::getline(std::cin, name);\n    std::cout << \"Hello, \" << name << std::endl;\n" else "    std::cout << \"Hello, CodeArc!\" << std::endl;\n") + "    return 0;\n}\n"
   "Java" -> "import java.util.Scanner;\npublic class Main {\n    public static void main(String[] args) {\n" + (if(calc) "        Scanner in = new Scanner(System.in);\n        double a = in.nextDouble(), b = in.nextDouble();\n        System.out.println(\"Sum: \" + (a + b));\n" else if(input) "        Scanner in = new Scanner(System.in);\n        System.out.println(\"Hello, \" + in.nextLine());\n" else "        System.out.println(\"Hello, CodeArc!\");\n") + "    }\n}\n"
   "Kotlin" -> "fun main() {\n" + (if(calc) "    val a = readln().toDouble()\n    val b = readln().toDouble()\n    println(a + b)\n" else if(input) "    println(\"Your name:\")\n    println(\"Hello, \" + readln())\n" else "    println(\"Hello, CodeArc!\")\n") + "}\n"
   else -> if(calc) "io.write('First number: ')\nlocal a = tonumber(io.read())\nio.write('Second number: ')\nlocal b = tonumber(io.read())\nprint('Sum:', a + b)\n" else if(input) "io.write('Your name: ')\nlocal name = io.read()\nprint('Hello, ' .. name)\n" else "print('Hello, CodeArc!')\n"
  }
 }
 /** HTML is the only project language that scaffolds more than one file: a real web project
  *  needs its style.css and script.js siblings created alongside index.html so the three
  *  <link>/<script> references in [code] resolve immediately. Called only when language ==
  *  "HTML" — every other language still creates exactly the one main file it always has. */
 fun webCompanionFiles(template: String): Pair<String, String> {
  if (template == "Empty Project") return "" to ""
  val css = "body {\n  font-family: sans-serif;\n  margin: 2rem;\n  background: #f4f7fc;\n  color: #0b1626;\n}\nh1 {\n  color: #147bff;\n}\nbutton {\n  padding: 0.5rem 1rem;\n  border: none;\n  border-radius: 8px;\n  background: #147bff;\n  color: white;\n  font-size: 1rem;\n}\n"
  val js = when {
   template == "Basic Calculator" -> "document.getElementById('sum').addEventListener('click', () => {\n  const a = Number(document.getElementById('a').value);\n  const b = Number(document.getElementById('b').value);\n  document.getElementById('result').textContent = 'Sum: ' + (a + b);\n});\n"
   template == "Input / Output Example" -> "document.getElementById('greet').addEventListener('click', () => {\n  const name = document.getElementById('name').value || 'friend';\n  document.getElementById('output').textContent = 'Hello, ' + name + '!';\n});\n"
   template == "Console Application" -> "console.log('Hello from script.js!');\nconsole.log('CodeArc runs this through the real WebView JavaScript engine.');\n"
   else -> "console.log('script.js loaded.');\n"
  }
  return css to js
 }
}
