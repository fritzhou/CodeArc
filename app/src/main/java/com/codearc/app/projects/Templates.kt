package com.codearc.app.projects

object Templates {
 val languages = listOf("Python", "JavaScript", "C", "C++", "Java", "Kotlin", "Lua")
 val names = listOf("Empty Project", "Hello World", "Console Application", "Basic Calculator", "Input / Output Example")
 fun file(language: String) = when(language) { "Python" -> "main.py"; "JavaScript" -> "main.js"; "C" -> "main.c"; "C++" -> "main.cpp"; "Java" -> "Main.java"; "Kotlin" -> "main.kt"; else -> "main.lua" }
 fun languageFor(fileName: String): String = when (fileName.substringAfterLast('.', "").lowercase()) {
  "py" -> "Python"; "js" -> "JavaScript"; "c" -> "C"; "cpp","cc","cxx" -> "C++"; "java" -> "Java"; "kt" -> "Kotlin"; "lua" -> "Lua"; else -> "Python"
 }
 fun code(language: String, template: String): String {
  if(template == "Empty Project") return ""
  val input = template == "Input / Output Example"
  val calc = template == "Basic Calculator"
  return when(language) {
   "Python" -> if(input) "name = input(\"Your name: \")\nprint(\"Hello,\", name)\n" else if(calc) "a = float(input(\"First number: \"))\nb = float(input(\"Second number: \"))\nprint(\"Sum:\", a + b)\n" else "def main():\n    print(\"Hello, CodeArc!\")\n\nif __name__ == \"__main__\":\n    main()\n"
   "JavaScript" -> if(input || calc) "const readline = require('node:readline');\nconst rl = readline.createInterface({ input: process.stdin, output: process.stdout });\n" + (if(calc) "rl.question('First number: ', a => rl.question('Second number: ', b => {\n  console.log('Sum:', Number(a) + Number(b));\n  rl.close();\n}));\n" else "rl.question('Your name: ', name => { console.log('Hello,', name); rl.close(); });\n") else "function main() {\n  console.log('Hello, CodeArc!');\n}\nmain();\n"
   "C" -> "#include <stdio.h>\nint main(void) {\n" + (if(calc) "    double a, b;\n    printf(\"Enter two numbers: \");\n    if (scanf(\"%lf %lf\", &a, &b) != 2) return 1;\n    printf(\"Sum: %g\\n\", a + b);\n" else if(input) "    char name[80];\n    printf(\"Your name: \");\n    if (scanf(\"%79s\", name) != 1) return 1;\n    printf(\"Hello, %s\\n\", name);\n" else "    puts(\"Hello, CodeArc!\");\n") + "    return 0;\n}\n"
   "C++" -> "#include <iostream>\n#include <string>\nint main() {\n" + (if(calc) "    double a, b;\n    std::cin >> a >> b;\n    std::cout << \"Sum: \" << a + b << std::endl;\n" else if(input) "    std::string name;\n    std::getline(std::cin, name);\n    std::cout << \"Hello, \" << name << std::endl;\n" else "    std::cout << \"Hello, CodeArc!\" << std::endl;\n") + "    return 0;\n}\n"
   "Java" -> "import java.util.Scanner;\npublic class Main {\n    public static void main(String[] args) {\n" + (if(calc) "        Scanner in = new Scanner(System.in);\n        double a = in.nextDouble(), b = in.nextDouble();\n        System.out.println(\"Sum: \" + (a + b));\n" else if(input) "        Scanner in = new Scanner(System.in);\n        System.out.println(\"Hello, \" + in.nextLine());\n" else "        System.out.println(\"Hello, CodeArc!\");\n") + "    }\n}\n"
   "Kotlin" -> "fun main() {\n" + (if(calc) "    val a = readln().toDouble()\n    val b = readln().toDouble()\n    println(a + b)\n" else if(input) "    println(\"Your name:\")\n    println(\"Hello, \" + readln())\n" else "    println(\"Hello, CodeArc!\")\n") + "}\n"
   else -> if(calc) "io.write('First number: ')\nlocal a = tonumber(io.read())\nio.write('Second number: ')\nlocal b = tonumber(io.read())\nprint('Sum:', a + b)\n" else if(input) "io.write('Your name: ')\nlocal name = io.read()\nprint('Hello, ' .. name)\n" else "print('Hello, CodeArc!')\n"
  }
 }
}
