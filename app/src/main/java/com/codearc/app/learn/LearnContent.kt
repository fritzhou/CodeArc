package com.codearc.app.learn

import com.codearc.app.execution.RuntimeManager

/** Everything CodeArc knows about the Learn system, independent of a user's progress (that
 *  lives in LearningRepository/Room). Lesson content is plain data — nothing here is fetched
 *  from anywhere, so Learn works fully offline. Interactive Examples and Practice both run
 *  through ExecutionManager.runSnippet(), the same engine the editor uses (Phase 4); this file
 *  never implements its own compiler.
 *
 *  Only Python has real lesson content, matching the fact that only Python has a real offline
 *  runtime (RuntimeManager, Phase 4). Every other language's Learn path is present in the
 *  registry (so the screen has something real to show) but honestly marked "Coming soon" —
 *  same scoping principle Phase 4 used for languages without a runtime. Python Intermediate
 *  ships as a real, titled syllabus with locked lessons rather than full content; see PHASE5.md
 *  for why. */

enum class QuizType { MULTIPLE_CHOICE, TRUE_FALSE, PREDICT_OUTPUT, IDENTIFY_ERROR }

data class QuizQuestion(
    val prompt: String,
    val type: QuizType,
    val choices: List<String>,
    val correctIndex: Int
)

data class PracticeChallenge(
    val instruction: String,
    val starterCode: String,
    val hint: String,
    val solution: String
)

data class Lesson(
    val id: String,
    val title: String,
    val level: String,
    val order: Int,
    val durationMinutes: Int,
    val objective: String,
    val explanation: String,
    val codeExample: String,
    val notes: String,
    val commonMistakes: String,
    val practice: PracticeChallenge,
    val quiz: QuizQuestion
)

data class GuidedProject(
    val id: String,
    val title: String,
    val level: String,
    val description: String,
    val template: String?
)

data class LanguageCourse(
    val languageId: String,
    val displayName: String,
    val beginner: List<Lesson> = emptyList(),
    val intermediateTitles: List<String> = emptyList(),
    val beginnerProjects: List<GuidedProject> = emptyList(),
    val intermediateProjects: List<GuidedProject> = emptyList()
) {
    val available: Boolean get() = RuntimeManager.isInstalled(languageId)
}

object PythonCourse {
    private fun q(prompt: String, type: QuizType, choices: List<String>, correct: Int) = QuizQuestion(prompt, type, choices, correct)
    private fun p(instruction: String, starter: String, hint: String, solution: String) = PracticeChallenge(instruction, starter, hint, solution)

    val beginner = listOf(
        Lesson(
            "py_b1", "Introduction to Python", "Beginner", 1, 5,
            "Understand what Python is and run your first line of code.",
            "Python is a general-purpose programming language known for reading almost like plain English. CodeArc runs your code with a real Python interpreter bundled inside the app — no separate install needed. Every Python program is just a sequence of statements executed top to bottom.",
            "print(\"Hello, CodeArc!\")",
            "print(...) writes text to the Output panel. Text you want shown literally goes inside quotes — that's called a string.",
            "Forgetting the parentheses — Python 3 always needs print(...), not print \"...\" like older versions.",
            p("Change the program to print your own name instead.", "print(\"Hello, CodeArc!\")", "Replace the text between the quotes, but keep the quotes themselves.", "print(\"Hello, Fritz!\")"),
            q("Which line correctly prints text in Python 3?", QuizType.MULTIPLE_CHOICE, listOf("print(\"Hi\")", "print \"Hi\"", "echo(\"Hi\")", "Hi.print()"), 0)
        ),
        Lesson(
            "py_b2", "Setting Up Python", "Beginner", 2, 4,
            "Understand how CodeArc runs Python for you.",
            "CodeArc bundles a real CPython interpreter (check the Languages screen — Python shows as Installed). Python files use the .py extension, and CodeArc looks for a project's main file to know where to start running. Comments start with # and are ignored when the code runs.",
            "# Comments explain code without CodeArc trying to run them\nprint(\"CodeArc runs this with its built-in Python interpreter.\")",
            "A comment is just a note for humans reading the code — it has no effect on the program.",
            "Assuming you need to install Python yourself — CodeArc already has it bundled offline.",
            p("Add a comment above the print() line explaining what it does.", "print(\"CodeArc runs this with its built-in Python interpreter.\")", "Start a new line with # before your note.", "# This prints a message about CodeArc's interpreter\nprint(\"CodeArc runs this with its built-in Python interpreter.\")"),
            q("Do you need to install Python separately before CodeArc can run it?", QuizType.TRUE_FALSE, listOf("True", "False"), 1)
        ),
        Lesson(
            "py_b3", "Variables and Data Types", "Beginner", 3, 7,
            "Store and label values using variables, and recognize Python's basic types.",
            "A variable is a name that points to a value. Python figures out the type automatically: whole numbers are int, decimals are float, text is str, and True/False are bool. You don't declare a type up front — just assign a value with =.",
            "name = \"Fritz\"\nage = 19\nis_student = True\nprint(name, age, is_student)",
            "print() can take several values separated by commas — it prints them with a space between each.",
            "Wrapping numbers in quotes by accident, which turns them into text (str) instead of numbers you can do math with.",
            p("Add a fourth variable for a favorite programming language and include it in the print() call.", "name = \"Fritz\"\nage = 19\nis_student = True\nprint(name, age, is_student)", "Create a new variable, then add it as another argument to print().", "name = \"Fritz\"\nage = 19\nis_student = True\nlanguage = \"Python\"\nprint(name, age, is_student, language)"),
            q("What does this print?\nx = 5\nx = x + 1\nprint(x)", QuizType.PREDICT_OUTPUT, listOf("5", "6", "Error", "x + 1"), 1)
        ),
        Lesson(
            "py_b4", "Input and Output", "Beginner", 4, 6,
            "Read a value from the user and use it in your program.",
            "input(\"prompt\") asks the user for text and returns whatever they typed — always as a string, even if it looks like a number. CodeArc's Run button opens a small dialog for supplying that input up front, since there's no live interactive terminal yet.",
            "name = input(\"Your name: \")\nprint(\"Hello,\", name)",
            "Because input() always returns a string, convert it with int(...) or float(...) before doing arithmetic on it.",
            "Doing math directly on input() without converting it first, which raises a TypeError.",
            p("Ask for the user's age with input() and print a sentence containing it.", "name = input(\"Your name: \")\nprint(\"Hello,\", name)", "Add a second input() call and include its result in a second print().", "name = input(\"Your name: \")\nage = input(\"Your age: \")\nprint(\"Hello,\", name, \"you are\", age, \"years old.\")"),
            q("What's wrong with this code?\nage = input(\"Age: \")\nprint(age + 1)", QuizType.IDENTIFY_ERROR, listOf("Nothing, it works", "input() returns a string, so it can't be added to an int without converting it", "print() can't take a variable", "You must import input first"), 1)
        ),
        Lesson(
            "py_b5", "Operators", "Beginner", 5, 6,
            "Use arithmetic, comparison and logical operators.",
            "Python supports the usual arithmetic operators (+ - * /), plus // for floor division, % for remainder, and ** for exponents. Comparison operators (== != < > <= >=) produce True or False, and and / or / not combine those results.",
            "a, b = 7, 2\nprint(a + b, a - b, a * b, a / b, a // b, a % b, a ** b)",
            "/ always gives a float (true division); // gives a whole-number result (floor division).",
            "Confusing / with // and getting an unexpected decimal, or vice versa.",
            p("Add a line that prints whether a is greater than b using a comparison operator.", "a, b = 7, 2\nprint(a + b, a - b, a * b, a / b, a // b, a % b, a ** b)", "Use > between a and b inside a print() call.", "a, b = 7, 2\nprint(a + b, a - b, a * b, a / b, a // b, a % b, a ** b)\nprint(a > b)"),
            q("What does this print?\nprint(7 // 2)", QuizType.PREDICT_OUTPUT, listOf("3.5", "3", "4", "Error"), 1)
        ),
        Lesson(
            "py_b6", "Conditional Statements", "Beginner", 6, 7,
            "Make decisions in code with if / elif / else.",
            "if runs a block only when its condition is True. elif checks another condition if the first was False, and else catches everything left over. Python uses indentation (not braces) to mark which lines belong to which block — that indentation is not optional.",
            "score = 82\nif score >= 90:\n    print(\"Grade: A\")\nelif score >= 80:\n    print(\"Grade: B\")\nelse:\n    print(\"Grade: C or below\")",
            "Every colon (:) starts a new indented block; keep indentation consistent (CodeArc's editor auto-indents for you).",
            "Using = (assignment) instead of == (comparison) inside a condition, or mismatching indentation.",
            p("Add another elif branch for a grade of C (60-79).", "score = 82\nif score >= 90:\n    print(\"Grade: A\")\nelif score >= 80:\n    print(\"Grade: B\")\nelse:\n    print(\"Grade: C or below\")", "Insert an elif score >= 60 line before the final else.", "score = 82\nif score >= 90:\n    print(\"Grade: A\")\nelif score >= 80:\n    print(\"Grade: B\")\nelif score >= 60:\n    print(\"Grade: C\")\nelse:\n    print(\"Grade: Below C\")"),
            q("Which operator checks equality (not assignment) in Python?", QuizType.MULTIPLE_CHOICE, listOf("=", "==", "eq", "==="), 1)
        ),
        Lesson(
            "py_b7", "Loops", "Beginner", 7, 7,
            "Repeat work with for and while loops.",
            "for i in range(n) repeats a block once per number from 0 up to (not including) n. while repeats as long as a condition stays True — you're responsible for changing something inside the loop so it eventually becomes False.",
            "for i in range(1, 6):\n    print(\"Count:\", i)\n\nn = 3\nwhile n > 0:\n    print(n)\n    n -= 1",
            "range(1, 6) produces 1, 2, 3, 4, 5 — the second number is excluded.",
            "Writing a while loop that never updates its condition variable, which runs forever until it times out.",
            p("Change the for loop to count from 1 to 10 instead of 1 to 5.", "for i in range(1, 6):\n    print(\"Count:\", i)", "Change the second number passed to range().", "for i in range(1, 11):\n    print(\"Count:\", i)"),
            q("How many times does this loop print?\nfor i in range(3):\n    print(i)", QuizType.PREDICT_OUTPUT, listOf("2", "3", "4", "It never stops"), 1)
        ),
        Lesson(
            "py_b8", "Functions", "Beginner", 8, 7,
            "Package reusable logic into a function with def.",
            "def name(parameters): starts a function definition; return sends a value back to whoever called it. A function with no return statement quietly returns None — calling it doesn't fail, but using its result won't do what you expect.",
            "def greet(name):\n    return \"Hello, \" + name\n\nprint(greet(\"CodeArc\"))",
            "Parameters are just local names the function uses; the value you pass in when calling it is called an argument.",
            "Writing the calculation but forgetting the return statement, so the function's result is None.",
            p("Write a function add(a, b) that returns the sum, and print the result of calling it.", "def greet(name):\n    return \"Hello, \" + name\n\nprint(greet(\"CodeArc\"))", "Define a new function with two parameters and a return a + b line.", "def add(a, b):\n    return a + b\n\nprint(add(2, 3))"),
            q("What does this print?\ndef add(a, b):\n    a + b\n\nprint(add(2, 3))", QuizType.IDENTIFY_ERROR, listOf("5", "None — the function never returns a value", "Error — Python won't run it", "23"), 1)
        ),
        Lesson(
            "py_b9", "Lists", "Beginner", 9, 7,
            "Store an ordered, changeable collection of values.",
            "A list holds multiple values in one variable, written with square brackets. Items are accessed by position starting at 0, and lists can grow or shrink after creation using methods like .append().",
            "languages = [\"Python\", \"JavaScript\", \"Kotlin\"]\nlanguages.append(\"Lua\")\nprint(languages[0])\nprint(len(languages))\nprint(languages)",
            "len(list) tells you how many items are in it; the last valid index is always len(list) - 1.",
            "Using an index that's out of range (for example languages[10] on a 4-item list), which raises an IndexError.",
            p("Add code that prints the last item in the list using a negative index.", "languages = [\"Python\", \"JavaScript\", \"Kotlin\"]\nlanguages.append(\"Lua\")\nprint(languages[0])\nprint(len(languages))\nprint(languages)", "Python allows languages[-1] to mean \"the last item\".", "languages = [\"Python\", \"JavaScript\", \"Kotlin\"]\nlanguages.append(\"Lua\")\nprint(languages[0])\nprint(len(languages))\nprint(languages)\nprint(languages[-1])"),
            q("What does this print?\nnums = [10, 20, 30]\nprint(nums[1])", QuizType.PREDICT_OUTPUT, listOf("10", "20", "30", "Error"), 1)
        ),
        Lesson(
            "py_b10", "Tuples", "Beginner", 10, 5,
            "Store a fixed collection of values that can't be changed after creation.",
            "A tuple looks like a list but uses parentheses and is immutable — once created, its contents can't be reassigned. Tuples are useful for values that naturally belong together, like coordinates, and can be unpacked directly into separate variables.",
            "point = (3, 4)\nx, y = point\nprint(x, y)\nprint(point[0])",
            "Reading from a tuple works exactly like a list (point[0]); only writing to it is blocked.",
            "Trying to reassign an item — point[0] = 5 — which raises a TypeError because tuples are immutable.",
            p("Create a tuple of three colors and unpack it into three variables, then print them.", "point = (3, 4)\nx, y = point\nprint(x, y)\nprint(point[0])", "Make a new tuple with three items and unpack it the same way point was unpacked into x, y.", "colors = (\"red\", \"green\", \"blue\")\nc1, c2, c3 = colors\nprint(c1, c2, c3)"),
            q("Can a tuple's contents be changed after it's created?", QuizType.TRUE_FALSE, listOf("True", "False"), 1)
        ),
        Lesson(
            "py_b11", "Dictionaries", "Beginner", 11, 7,
            "Store values under meaningful keys instead of numeric positions.",
            "A dictionary maps keys to values, written with curly braces: {key: value}. Look up a value with dict[key], and loop over both keys and values at once with .items(). Dictionaries are unordered by key — you look things up by name, not position.",
            "student = {\"name\": \"Fritz\", \"strand\": \"TVL\", \"year\": 4}\nprint(student[\"name\"])\nstudent[\"year\"] = 5\nfor key, value in student.items():\n    print(key, \"->\", value)",
            "Assigning to a new key (student[\"gpa\"] = 1.5) adds it; assigning to an existing key updates it.",
            "Accessing a key that doesn't exist with square brackets, which raises a KeyError — use .get(key) when the key might be missing.",
            p("Add a new key \"section\" to the dictionary and print the full updated dictionary.", "student = {\"name\": \"Fritz\", \"strand\": \"TVL\", \"year\": 4}\nprint(student[\"name\"])\nstudent[\"year\"] = 5\nfor key, value in student.items():\n    print(key, \"->\", value)", "Assign student[\"section\"] to a string value, then print(student).", "student = {\"name\": \"Fritz\", \"strand\": \"TVL\", \"year\": 4}\nstudent[\"section\"] = \"A\"\nprint(student)"),
            q("What happens here?\nstudent = {\"name\": \"Fritz\"}\nprint(student[\"age\"])", QuizType.IDENTIFY_ERROR, listOf("Prints an empty value", "Raises a KeyError because \"age\" isn't in the dictionary", "Prints None", "Adds \"age\" automatically"), 1)
        ),
        Lesson(
            "py_b12", "File Handling", "Beginner", 12, 6,
            "Write to and read from a text file.",
            "open(path, mode) opens a file — \"w\" to write (overwriting existing content), \"r\" to read, \"a\" to append. Using with open(...) as f: automatically closes the file when the block ends, even if an error happens inside it. Files created here live in this lesson's own sandbox, separate from your real projects.",
            "with open(\"notes.txt\", \"w\") as f:\n    f.write(\"CodeArc lesson complete.\\n\")\n\nwith open(\"notes.txt\", \"r\") as f:\n    print(f.read())",
            "\"w\" mode erases the file's previous contents before writing — use \"a\" (append) to add to it instead.",
            "Opening a file without with and forgetting to close it, which can leave data unwritten or the file locked.",
            p("Write a second line to notes.txt using append mode (\"a\") after the first write, then read and print the whole file.", "with open(\"notes.txt\", \"w\") as f:\n    f.write(\"CodeArc lesson complete.\\n\")\n\nwith open(\"notes.txt\", \"r\") as f:\n    print(f.read())", "Add a third with open(\"notes.txt\", \"a\") block that calls f.write() again before the final read.", "with open(\"notes.txt\", \"w\") as f:\n    f.write(\"CodeArc lesson complete.\\n\")\n\nwith open(\"notes.txt\", \"a\") as f:\n    f.write(\"Second line.\\n\")\n\nwith open(\"notes.txt\", \"r\") as f:\n    print(f.read())"),
            q("Which mode opens a file for writing and overwrites its existing content?", QuizType.MULTIPLE_CHOICE, listOf("r", "w", "a", "x"), 1)
        )
    )

    val intermediateTitles = listOf(
        "Advanced Functions", "Lambda Functions", "List Comprehensions", "Exception Handling",
        "Modules", "Packages", "Object-Oriented Programming", "Classes", "Inheritance",
        "JSON", "APIs", "Intermediate Project"
    )

    val beginnerProjects = listOf(
        GuidedProject("proj_calculator", "Calculator", "Beginner", "Take two numbers from the user and print their sum, difference, product and quotient.", "Basic Calculator"),
        GuidedProject("proj_guess", "Number Guessing Game", "Beginner", "Pick a secret number and let the player guess, printing higher/lower hints until they get it right.", "Empty Project"),
        GuidedProject("proj_quiz", "Quiz", "Beginner", "Ask a handful of multiple-choice questions from a list and print a final score out of the total.", "Empty Project"),
        GuidedProject("proj_grade", "Grade Calculator", "Beginner", "Turn a list of scores into an average and a letter grade using conditionals.", "Empty Project"),
        GuidedProject("proj_todo", "To-Do CLI", "Beginner", "Manage a simple list of tasks — add, list and mark complete — from the console.", "Console Application")
    )
    val intermediateProjects = listOf(
        GuidedProject("proj_expense", "Expense Tracker", "Intermediate", "Coming soon.", null),
        GuidedProject("proj_contacts", "Contact Manager", "Intermediate", "Coming soon.", null),
        GuidedProject("proj_jsonnotes", "JSON Notes", "Intermediate", "Coming soon.", null),
        GuidedProject("proj_apiviewer", "API Data Viewer", "Intermediate", "Coming soon.", null),
        GuidedProject("proj_organizer", "File Organizer", "Intermediate", "Coming soon.", null)
    )

    val course = LanguageCourse("python", "Python", beginner, intermediateTitles, beginnerProjects, intermediateProjects)
}

/** JavaScript, C, C++, Java, Kotlin and Lua are registered so Learn has something real to point
 *  at, matching LanguageRegistry — but none has lesson content yet because none has a real
 *  offline runtime (RuntimeManager.isInstalled returns false for all of them). Learn shows
 *  these as "Coming soon" rather than inventing content that can't actually run. */
object PlaceholderCourses {
    val rest = listOf(
        LanguageCourse("javascript", "JavaScript"),
        LanguageCourse("c", "C"),
        LanguageCourse("cpp", "C++"),
        LanguageCourse("java", "Java"),
        LanguageCourse("kotlin", "Kotlin"),
        LanguageCourse("lua", "Lua")
    )
}

object CourseRegistry {
    val all: List<LanguageCourse> = listOf(PythonCourse.course) + PlaceholderCourses.rest
    fun forLanguageId(id: String) = all.find { it.languageId == id }
}
