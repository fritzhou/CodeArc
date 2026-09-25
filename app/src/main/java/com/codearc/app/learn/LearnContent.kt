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
    val intermediateProjects: List<GuidedProject> = emptyList(),
    // Defaults to the RuntimeManager-derived answer (Python, and every placeholder-only
    // language) but can be overridden — HTML/CSS/JavaScript genuinely run offline via WebView
    // (see WebRuntime.kt) without needing RuntimeManager.isInstalled("javascript") to become
    // true, which would change the separate, pre-existing standalone JavaScript project's
    // cloud-fallback behavior. This keeps that decoupled while still being honest: these three
    // really do work, just through a different real engine than RuntimeManager tracks.
    val available: Boolean = RuntimeManager.isInstalled(languageId)
)

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

/** HTML, CSS and JavaScript now have real offline execution (WebView, see WebRuntime.kt and
 *  ExecutionManager.runSnippet), so — unlike C/C++/Java/Kotlin/Lua below — they get real
 *  beginner content too, following the exact same shape Python's course uses: real lessons,
 *  a titled Intermediate syllabus, Advanced left to the universal "coming soon" (CourseActivity
 *  shows that for every language). JavaScript here teaches browser JavaScript (console.log,
 *  DOM-free basics) — the separate Node-flavored "JavaScript" project template in Templates.kt
 *  is a different, pre-existing thing and is untouched by this. */
object HtmlCourse {
    private fun q(prompt: String, type: QuizType, choices: List<String>, correct: Int) = QuizQuestion(prompt, type, choices, correct)
    private fun p(instruction: String, starter: String, hint: String, solution: String) = PracticeChallenge(instruction, starter, hint, solution)

    val beginner = listOf(
        Lesson(
            "html_b1", "Introduction to HTML", "Beginner", 1, 5,
            "Understand what HTML is and write your first element.",
            "HTML (HyperText Markup Language) describes the structure of a web page using elements. An element is usually an opening tag, some content, and a closing tag: <tagname>content</tagname>. CodeArc renders your HTML with Android's real WebView — the same rendering engine behind Chrome — entirely offline.",
            "<h1>Hello, CodeArc!</h1>\n<p>This is a paragraph of text.</p>",
            "<h1> is a top-level heading; <p> is a paragraph. Tags almost always come in opening/closing pairs.",
            "Forgetting the closing tag, or mismatching tag names (<h1>...</p>), which can make the rest of the page render unpredictably.",
            p("Add a second paragraph below the first one introducing yourself.", "<h1>Hello, CodeArc!</h1>\n<p>This is a paragraph of text.</p>", "Add another <p>...</p> line after the first paragraph.", "<h1>Hello, CodeArc!</h1>\n<p>This is a paragraph of text.</p>\n<p>My name is Fritz and I'm learning HTML.</p>"),
            q("Which tag defines the largest, top-level heading?", QuizType.MULTIPLE_CHOICE, listOf("<h6>", "<heading>", "<h1>", "<head>"), 2)
        ),
        Lesson(
            "html_b2", "Document Structure", "Beginner", 2, 6,
            "Learn the skeleton every HTML page starts from.",
            "A real HTML page starts with <!DOCTYPE html>, then an <html> element containing a <head> (metadata — <title>, <meta charset>, linked CSS) and a <body> (everything visible). CodeArc's own \"HTML\" project template already scaffolds this — index.html links style.css and script.js automatically.",
            "<!DOCTYPE html>\n<html lang=\"en\">\n<head>\n  <meta charset=\"UTF-8\">\n  <title>My Page</title>\n</head>\n<body>\n  <h1>Welcome</h1>\n</body>\n</html>",
            "<title> sets the browser tab's title, not anything visible on the page itself.",
            "Putting visible content inside <head> instead of <body> — the browser won't display it there.",
            p("Add a <meta name=\"description\" content=\"...\"> line inside <head> describing the page.", "<!DOCTYPE html>\n<html lang=\"en\">\n<head>\n  <meta charset=\"UTF-8\">\n  <title>My Page</title>\n</head>\n<body>\n  <h1>Welcome</h1>\n</body>\n</html>", "Add another <meta> tag inside <head>, before or after the existing one.", "<!DOCTYPE html>\n<html lang=\"en\">\n<head>\n  <meta charset=\"UTF-8\">\n  <meta name=\"description\" content=\"My first page\">\n  <title>My Page</title>\n</head>\n<body>\n  <h1>Welcome</h1>\n</body>\n</html>"),
            q("Where does visible page content go?", QuizType.MULTIPLE_CHOICE, listOf("<head>", "<body>", "<title>", "<meta>"), 1)
        ),
        Lesson(
            "html_b3", "Text and Headings", "Beginner", 3, 6,
            "Structure text with headings, paragraphs and emphasis.",
            "<h1> through <h6> are headings from most to least important — use them for structure, not just to make text big. <strong> marks important text (bold), <em> marks emphasized text (italic), and <br> forces a line break inside a paragraph.",
            "<h2>Section Title</h2>\n<p>Some <strong>important</strong> text and some <em>emphasized</em> text.</p>",
            "Don't skip heading levels for visual size alone (e.g. using <h1> just because it's bigger) — screen readers rely on the actual hierarchy.",
            "Using multiple <h1> tags for unrelated headings on one page instead of a proper h1 → h2 → h3 structure.",
            p("Add an <h3> subheading below the paragraph.", "<h2>Section Title</h2>\n<p>Some <strong>important</strong> text and some <em>emphasized</em> text.</p>", "Add a new line with <h3>...</h3> after the paragraph.", "<h2>Section Title</h2>\n<p>Some <strong>important</strong> text and some <em>emphasized</em> text.</p>\n<h3>A Subheading</h3>"),
            q("Which tag renders text in italics to show emphasis?", QuizType.MULTIPLE_CHOICE, listOf("<strong>", "<i>", "<em>", "<b>"), 2)
        ),
        Lesson(
            "html_b4", "Links and Images", "Beginner", 4, 6,
            "Link to other pages and embed images.",
            "<a href=\"...\">text</a> creates a clickable link — href can point to another page, a section (#id), or an external site. <img src=\"...\" alt=\"...\"> embeds an image; alt describes it for screen readers and shows if the image fails to load. Neither tag has separate closing content the way <p> does for <img> (it's self-closing).",
            "<a href=\"https://example.com\">Visit example.com</a>\n<img src=\"logo.png\" alt=\"CodeArc logo\">",
            "Always include alt text on <img> — it's not optional decoration, it's how screen readers describe the image.",
            "Forgetting the alt attribute, or writing a href without the protocol (https://) for an external link.",
            p("Add a second image tag with a different src and alt.", "<a href=\"https://example.com\">Visit example.com</a>\n<img src=\"logo.png\" alt=\"CodeArc logo\">", "Add another <img src=\"...\" alt=\"...\"> line.", "<a href=\"https://example.com\">Visit example.com</a>\n<img src=\"logo.png\" alt=\"CodeArc logo\">\n<img src=\"banner.png\" alt=\"Site banner\">"),
            q("What does the alt attribute on <img> do?", QuizType.MULTIPLE_CHOICE, listOf("Sets the image's alignment", "Describes the image for accessibility and shows if it fails to load", "Sets the image's file format", "Nothing, it's optional decoration"), 1)
        ),
        Lesson(
            "html_b5", "Lists", "Beginner", 5, 5,
            "Group related items with ordered and unordered lists.",
            "<ul> makes an unordered (bulleted) list; <ol> makes an ordered (numbered) list. Either way, each item goes inside its own <li> (list item) tag.",
            "<ul>\n  <li>HTML</li>\n  <li>CSS</li>\n  <li>JavaScript</li>\n</ul>",
            "<li> elements must be direct children of <ul> or <ol> — text can't sit loose between them.",
            "Forgetting to wrap each item in its own <li>, or closing </ul> before all items are listed.",
            p("Add a fourth list item for a topic you want to learn next.", "<ul>\n  <li>HTML</li>\n  <li>CSS</li>\n  <li>JavaScript</li>\n</ul>", "Add another <li>...</li> line before </ul>.", "<ul>\n  <li>HTML</li>\n  <li>CSS</li>\n  <li>JavaScript</li>\n  <li>Python</li>\n</ul>"),
            q("Which tag creates a numbered list?", QuizType.MULTIPLE_CHOICE, listOf("<ul>", "<li>", "<ol>", "<num>"), 2)
        ),
        Lesson(
            "html_b6", "Forms and Inputs", "Beginner", 6, 7,
            "Collect input from the person viewing the page.",
            "<form> wraps a group of inputs. <input type=\"text\"> is a single-line text field (other types: \"number\", \"email\", \"checkbox\"...), and <button> triggers an action — usually read from JavaScript via its id, since CodeArc's forms don't submit to a server. <label for=\"id\"> ties a caption to an input by matching its id.",
            "<label for=\"name\">Your name:</label>\n<input id=\"name\" type=\"text\">\n<button id=\"go\">Submit</button>",
            "An input's value is read in JavaScript with document.getElementById(\"id\").value — that's covered in the JavaScript course.",
            "Giving two inputs the same id — only the first one will ever be found by getElementById.",
            p("Add a second input with type=\"number\" and its own label.", "<label for=\"name\">Your name:</label>\n<input id=\"name\" type=\"text\">\n<button id=\"go\">Submit</button>", "Add a <label for=\"age\">...</label> and <input id=\"age\" type=\"number\"> pair.", "<label for=\"name\">Your name:</label>\n<input id=\"name\" type=\"text\">\n<label for=\"age\">Your age:</label>\n<input id=\"age\" type=\"number\">\n<button id=\"go\">Submit</button>"),
            q("What connects a <label> to its <input>?", QuizType.MULTIPLE_CHOICE, listOf("They must be next to each other", "label's for= matching input's id", "Nothing, it's automatic", "A shared class name"), 1)
        )
    )
    val intermediateTitles = listOf(
        "Semantic HTML", "Tables", "iframes and Embeds", "Data Attributes", "Accessibility (ARIA)",
        "SVG Basics", "Forms Validation", "Meta Tags and SEO", "Responsive Images", "Web Components Intro", "Intermediate Project"
    )
    val beginnerProjects = listOf(
        GuidedProject("proj_html_card", "Profile Card", "Beginner", "Build a simple profile page with a heading, an image, a short bio and a list of skills.", "Hello World"),
        GuidedProject("proj_html_form", "Feedback Form", "Beginner", "Build a small form with a few inputs and a submit button (wire it up with JavaScript in that course).", "Empty Project")
    )
    val intermediateProjects = listOf(
        GuidedProject("proj_html_portfolio", "Personal Portfolio Page", "Intermediate", "Coming soon.", null),
        GuidedProject("proj_html_recipe", "Recipe Page", "Intermediate", "Coming soon.", null)
    )
    val course = LanguageCourse("html", "HTML", beginner, intermediateTitles, beginnerProjects, intermediateProjects, available = true)
}

object CssCourse {
    private fun q(prompt: String, type: QuizType, choices: List<String>, correct: Int) = QuizQuestion(prompt, type, choices, correct)
    private fun p(instruction: String, starter: String, hint: String, solution: String) = PracticeChallenge(instruction, starter, hint, solution)

    val beginner = listOf(
        Lesson(
            "css_b1", "Introduction to CSS", "Beginner", 1, 5,
            "Understand what CSS does and how to attach it to a page.",
            "CSS (Cascading Style Sheets) controls how HTML looks — colors, spacing, layout, fonts. A rule is a selector followed by declarations in braces: selector { property: value; }. CodeArc's HTML project template already links style.css from index.html with <link rel=\"stylesheet\" href=\"style.css\">, so edits there apply the moment you tap Run.",
            "h1 {\n  color: blue;\n}",
            "Every declaration ends with a semicolon; forgetting it can silently break the next declaration in the same rule.",
            "Forgetting the semicolon after a value, or mismatching curly braces.",
            p("Add a second rule that sets all <p> text to gray.", "h1 {\n  color: blue;\n}", "Add a new rule: p { color: gray; }", "h1 {\n  color: blue;\n}\np {\n  color: gray;\n}"),
            q("What does a CSS rule consist of?", QuizType.MULTIPLE_CHOICE, listOf("A tag and an attribute", "A selector and declarations in braces", "A function call", "An HTML comment"), 1)
        ),
        Lesson(
            "css_b2", "Colors and Backgrounds", "Beginner", 2, 5,
            "Color text and backgrounds using named colors, hex and rgb.",
            "color sets text color; background-color sets the background. Values can be a named color (\"tomato\"), a hex code (#ff6347), or rgb(255, 99, 71) — all three describe the same color here.",
            "body {\n  background-color: #f4f7fc;\n  color: #0b1626;\n}",
            "Hex codes always start with # and are 3 or 6 hex digits (#fff or #ffffff).",
            "Confusing color (text color) with background-color (fills the element behind the text).",
            p("Change the background to a light blue and the text to dark navy.", "body {\n  background-color: #f4f7fc;\n  color: #0b1626;\n}", "Change the two hex values to different colors.", "body {\n  background-color: #cfe3ff;\n  color: #0a1a33;\n}"),
            q("Which property changes an element's text color?", QuizType.MULTIPLE_CHOICE, listOf("background-color", "text-color", "color", "font-color"), 2)
        ),
        Lesson(
            "css_b3", "The Box Model", "Beginner", 3, 7,
            "Understand margin, border, padding and content.",
            "Every element is a box: content, wrapped by padding (space inside the border), then border, then margin (space outside the border, between elements). width/height size the content itself by default.",
            ".box {\n  width: 200px;\n  padding: 16px;\n  border: 2px solid #147bff;\n  margin: 12px;\n}",
            "padding pushes content away from the border on the inside; margin pushes other elements away on the outside.",
            "Mixing up padding and margin, or forgetting a border needs a style (solid, dashed...) as well as a width and color.",
            p("Change the box to have a dashed red border instead.", ".box {\n  width: 200px;\n  padding: 16px;\n  border: 2px solid #147bff;\n  margin: 12px;\n}", "Change \"solid\" to \"dashed\" and the color to red.", ".box {\n  width: 200px;\n  padding: 16px;\n  border: 2px dashed red;\n  margin: 12px;\n}"),
            q("Which sits closest to the element's content?", QuizType.MULTIPLE_CHOICE, listOf("margin", "border", "padding", "outline"), 2)
        ),
        Lesson(
            "css_b4", "Text Styling", "Beginner", 4, 6,
            "Control fonts, size, weight and alignment.",
            "font-family sets the typeface (with fallbacks: \"Arial, sans-serif\"), font-size sets how big, font-weight controls boldness (normal/bold or 100-900), and text-align controls horizontal alignment (left/center/right).",
            "h1 {\n  font-family: sans-serif;\n  font-size: 28px;\n  font-weight: bold;\n  text-align: center;\n}",
            "List font-family fallbacks in order — the browser uses the first one it actually has installed.",
            "Forgetting units on font-size (28 instead of 28px) — CSS numbers almost always need a unit.",
            p("Change the heading to be left-aligned and a smaller font-size.", "h1 {\n  font-family: sans-serif;\n  font-size: 28px;\n  font-weight: bold;\n  text-align: center;\n}", "Change text-align to left and reduce font-size.", "h1 {\n  font-family: sans-serif;\n  font-size: 20px;\n  font-weight: bold;\n  text-align: left;\n}"),
            q("What's wrong with font-size: 28;?", QuizType.IDENTIFY_ERROR, listOf("Nothing, it works", "Missing a unit like px", "font-size isn't a real property", "It should be in quotes"), 1)
        ),
        Lesson(
            "css_b5", "Selectors", "Beginner", 5, 6,
            "Target exactly the elements you want to style.",
            "An element selector (p) matches every <p>. A class selector (.card) matches every element with class=\"card\" — one class can be reused on many elements. An id selector (#header) matches the one element with that exact id. A descendant selector (.card p) matches <p> elements inside .card.",
            ".card {\n  background: white;\n}\n.card p {\n  color: gray;\n}\n#header {\n  font-weight: bold;\n}",
            "Classes are reusable across many elements; ids should be unique — one per page.",
            "Using an id when you actually want to style several elements the same way — reach for a class instead.",
            p("Add a rule targeting elements with class=\"highlight\" that sets a yellow background.", ".card {\n  background: white;\n}\n.card p {\n  color: gray;\n}\n#header {\n  font-weight: bold;\n}", "Add .highlight { background: yellow; }", ".card {\n  background: white;\n}\n.card p {\n  color: gray;\n}\n#header {\n  font-weight: bold;\n}\n.highlight {\n  background: yellow;\n}"),
            q("Which selector targets class=\"card\"?", QuizType.MULTIPLE_CHOICE, listOf("#card", "card", ".card", "*card"), 2)
        ),
        Lesson(
            "css_b6", "Flexbox Basics", "Beginner", 6, 7,
            "Lay out elements in a row or column with display: flex.",
            "Setting display: flex on a container turns its direct children into a flex layout. justify-content controls spacing along the main axis (flex-start/center/space-between...), align-items controls the cross axis, and flex-direction switches between row (default) and column.",
            ".nav {\n  display: flex;\n  justify-content: space-between;\n  align-items: center;\n}",
            "justify-content works along the direction items flow (row by default); align-items works across it.",
            "Setting display: flex on the wrong element — it affects that element's direct children, not the element itself.",
            p("Change the layout to stack items in a column instead of a row.", ".nav {\n  display: flex;\n  justify-content: space-between;\n  align-items: center;\n}", "Add flex-direction: column;", ".nav {\n  display: flex;\n  flex-direction: column;\n  justify-content: space-between;\n  align-items: center;\n}"),
            q("What does display: flex do?", QuizType.MULTIPLE_CHOICE, listOf("Hides the element", "Turns the element's direct children into a flex layout", "Makes text bold", "Adds a border"), 1)
        )
    )
    val intermediateTitles = listOf(
        "CSS Grid", "Positioning", "Transitions and Animations", "Pseudo-classes and Pseudo-elements",
        "Media Queries", "CSS Variables", "z-index and Stacking", "Responsive Units (rem, vw, vh)", "Intermediate Project"
    )
    val beginnerProjects = listOf(
        GuidedProject("proj_css_card", "Styled Card Layout", "Beginner", "Style a simple profile or product card using the box model, colors and a border-radius.", "Hello World")
    )
    val intermediateProjects = listOf(
        GuidedProject("proj_css_landing", "Landing Page Layout", "Intermediate", "Coming soon.", null)
    )
    val course = LanguageCourse("css", "CSS", beginner, intermediateTitles, beginnerProjects, intermediateProjects, available = true)
}

object JsCourse {
    private fun q(prompt: String, type: QuizType, choices: List<String>, correct: Int) = QuizQuestion(prompt, type, choices, correct)
    private fun p(instruction: String, starter: String, hint: String, solution: String) = PracticeChallenge(instruction, starter, hint, solution)

    val beginner = listOf(
        Lesson(
            "js_b1", "Introduction to JavaScript", "Beginner", 1, 5,
            "Write your first line of JavaScript and see real output.",
            "JavaScript is the language that makes web pages interactive. CodeArc runs it through the same real WebView JavaScript engine that renders your HTML projects — entirely offline. console.log(...) prints a value to the console, which CodeArc shows in the Output panel.",
            "console.log(\"Hello, CodeArc!\");",
            "Statements in JavaScript usually end with a semicolon — not strictly required everywhere, but leaving it off in the wrong place can change how the next line is parsed.",
            "Using print(...) out of habit from another language — JavaScript uses console.log(...).",
            p("Log your own name instead.", "console.log(\"Hello, CodeArc!\");", "Change the text inside the quotes.", "console.log(\"Hello, Fritz!\");"),
            q("Which line prints a value in JavaScript?", QuizType.MULTIPLE_CHOICE, listOf("print(\"Hi\")", "console.log(\"Hi\")", "echo \"Hi\"", "log.console(\"Hi\")"), 1)
        ),
        Lesson(
            "js_b2", "Variables and Data Types", "Beginner", 2, 7,
            "Store values with let and const, and recognize JavaScript's basic types.",
            "let declares a variable that can be reassigned; const declares one that can't. Basic types: string (text), number (both integers and decimals), boolean (true/false), and undefined/null for \"no value\". JavaScript figures out the type automatically from the value you assign.",
            "let name = \"Fritz\";\nconst age = 19;\nlet isStudent = true;\nconsole.log(name, age, isStudent);",
            "Prefer const by default and only use let when you know the variable needs to change later.",
            "Trying to reassign a const, which throws a TypeError — use let instead if the value needs to change.",
            p("Add a fourth variable for a favorite language and log it too.", "let name = \"Fritz\";\nconst age = 19;\nlet isStudent = true;\nconsole.log(name, age, isStudent);", "Declare a new const or let, then add it to the console.log call.", "let name = \"Fritz\";\nconst age = 19;\nlet isStudent = true;\nconst language = \"JavaScript\";\nconsole.log(name, age, isStudent, language);"),
            q("What happens if you reassign a const?", QuizType.IDENTIFY_ERROR, listOf("It works fine", "TypeError — const can't be reassigned", "It becomes a let automatically", "Nothing happens"), 1)
        ),
        Lesson(
            "js_b3", "Operators", "Beginner", 3, 6,
            "Use arithmetic, comparison and logical operators.",
            "Arithmetic: + - * / % (remainder), ** (exponent). Comparison === and !== check both value AND type (preferred over == / != which convert types first). Logical && (and), || (or), ! (not) combine boolean results.",
            "let a = 7, b = 2;\nconsole.log(a + b, a - b, a * b, a / b, a % b, a ** b);",
            "Always prefer === over == — == can silently convert types in surprising ways (e.g. \"5\" == 5 is true).",
            "Using == instead of === for comparisons, letting JavaScript's type coercion cause unexpected results.",
            p("Add a line that logs whether a is greater than b using a comparison operator.", "let a = 7, b = 2;\nconsole.log(a + b, a - b, a * b, a / b, a % b, a ** b);", "Use > between a and b inside a console.log call.", "let a = 7, b = 2;\nconsole.log(a + b, a - b, a * b, a / b, a % b, a ** b);\nconsole.log(a > b);"),
            q("What does 7 % 2 evaluate to?", QuizType.PREDICT_OUTPUT, listOf("3.5", "1", "3", "0"), 1)
        ),
        Lesson(
            "js_b4", "Conditionals", "Beginner", 4, 6,
            "Make decisions in code with if / else if / else.",
            "if runs a block only when its condition is true. else if checks another condition if the first was false, and else catches everything left over. Conditions are wrapped in parentheses and blocks in curly braces — indentation alone doesn't define a block in JavaScript, unlike Python.",
            "let score = 82;\nif (score >= 90) {\n  console.log(\"Grade: A\");\n} else if (score >= 80) {\n  console.log(\"Grade: B\");\n} else {\n  console.log(\"Grade: C or below\");\n}",
            "Curly braces { } define the block, not indentation — indentation is just for readability in JavaScript.",
            "Using = (assignment) instead of === (comparison) inside a condition.",
            p("Add another else if branch for a grade of C (60-79).", "let score = 82;\nif (score >= 90) {\n  console.log(\"Grade: A\");\n} else if (score >= 80) {\n  console.log(\"Grade: B\");\n} else {\n  console.log(\"Grade: C or below\");\n}", "Insert an else if (score >= 60) block before the final else.", "let score = 82;\nif (score >= 90) {\n  console.log(\"Grade: A\");\n} else if (score >= 80) {\n  console.log(\"Grade: B\");\n} else if (score >= 60) {\n  console.log(\"Grade: C\");\n} else {\n  console.log(\"Grade: Below C\");\n}"),
            q("Which operator checks equality without type coercion?", QuizType.MULTIPLE_CHOICE, listOf("=", "==", "===", "eq"), 2)
        ),
        Lesson(
            "js_b5", "Loops", "Beginner", 5, 7,
            "Repeat work with for and while loops.",
            "for (let i = 0; i < n; i++) { ... } repeats once per value of i from 0 up to (not including) n. while (condition) { ... } repeats as long as condition stays true — you're responsible for changing something inside the loop so it eventually becomes false.",
            "for (let i = 1; i <= 5; i++) {\n  console.log(\"Count:\", i);\n}\n\nlet n = 3;\nwhile (n > 0) {\n  console.log(n);\n  n--;\n}",
            "for (let i = 1; i <= 5; i++) runs 5 times (1 through 5 inclusive, since the condition uses <=).",
            "Writing a while loop that never updates its condition variable, which runs until CodeArc's timeout stops it.",
            p("Change the for loop to count from 1 to 10 instead of 1 to 5.", "for (let i = 1; i <= 5; i++) {\n  console.log(\"Count:\", i);\n}", "Change the 5 in the condition to 10.", "for (let i = 1; i <= 10; i++) {\n  console.log(\"Count:\", i);\n}"),
            q("How many times does this loop run?\nfor (let i = 0; i < 3; i++) { console.log(i); }", QuizType.PREDICT_OUTPUT, listOf("2", "3", "4", "It never stops"), 1)
        ),
        Lesson(
            "js_b6", "Functions", "Beginner", 6, 7,
            "Package reusable logic with function declarations and arrow functions.",
            "function name(parameters) { ... return value; } declares a function; return sends a value back to the caller. Arrow functions are a shorter syntax: const name = (parameters) => { ... }. A function with no return statement returns undefined.",
            "function greet(name) {\n  return \"Hello, \" + name;\n}\nconsole.log(greet(\"CodeArc\"));\n\nconst add = (a, b) => a + b;\nconsole.log(add(2, 3));",
            "An arrow function with a single expression body (no braces) returns that expression automatically — no return keyword needed.",
            "Writing the calculation but forgetting return in a regular function body, so the result is undefined.",
            p("Write a function multiply(a, b) that returns the product, and log the result of calling it.", "function greet(name) {\n  return \"Hello, \" + name;\n}\nconsole.log(greet(\"CodeArc\"));", "Define a new function with two parameters and a return a * b line.", "function multiply(a, b) {\n  return a * b;\n}\nconsole.log(multiply(2, 3));"),
            q("What does this log?\nfunction add(a, b) { a + b; }\nconsole.log(add(2, 3));", QuizType.IDENTIFY_ERROR, listOf("5", "undefined — the function never returns a value", "An error", "23"), 1)
        )
    )
    val intermediateTitles = listOf(
        "Arrays and Array Methods", "Objects", "The DOM (getElementById, querySelector)", "Event Listeners",
        "Template Literals", "Destructuring", "Closures", "Promises and Async/Await", "JSON", "Fetch and APIs", "Intermediate Project"
    )
    val beginnerProjects = listOf(
        GuidedProject("proj_js_calc", "Basic Calculator", "Beginner", "Wire up two number inputs and a button to show their sum on the page.", "Basic Calculator"),
        GuidedProject("proj_js_greeter", "Name Greeter", "Beginner", "Take a name from a text input and show a personalized greeting when a button is tapped.", "Input / Output Example")
    )
    val intermediateProjects = listOf(
        GuidedProject("proj_js_todo", "To-Do List (DOM)", "Intermediate", "Coming soon.", null),
        GuidedProject("proj_js_quiz", "Interactive Quiz App", "Intermediate", "Coming soon.", null)
    )
    val course = LanguageCourse("javascript", "JavaScript", beginner, intermediateTitles, beginnerProjects, intermediateProjects, available = true)
}

/** C, C++, Java, Kotlin and Lua are registered so Learn has something real to point at,
 *  matching LanguageRegistry — but none has lesson content yet because none has a real offline
 *  runtime (RuntimeManager.isInstalled returns false for all of them). Learn shows these as
 *  "Coming soon" rather than inventing content that can't actually run. HTML, CSS and
 *  JavaScript used to be here too, before WebRuntime.kt gave them a real offline engine — see
 *  HtmlCourse/CssCourse/JsCourse above. */
object PlaceholderCourses {
    val rest = listOf(
        LanguageCourse("c", "C"),
        LanguageCourse("cpp", "C++"),
        LanguageCourse("java", "Java"),
        LanguageCourse("kotlin", "Kotlin"),
        LanguageCourse("lua", "Lua")
    )
}

object CourseRegistry {
    val all: List<LanguageCourse> = listOf(PythonCourse.course, HtmlCourse.course, CssCourse.course, JsCourse.course) + PlaceholderCourses.rest
    fun forLanguageId(id: String) = all.find { it.languageId == id }
}
