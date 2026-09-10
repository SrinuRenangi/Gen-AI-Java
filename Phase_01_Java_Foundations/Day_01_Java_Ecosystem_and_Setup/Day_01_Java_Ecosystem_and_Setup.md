# ☕ Day 01: Java Ecosystem & Setup
## JDK 21, Maven, Project Architecture & Your First AI-Ready Java Program

| ⬅️ Previous Day | 📚 Course Hub | ➡️ Next Day |
|:---|:---:|---:|
| *🚀 Course Inception* | [All 60 Days Overview](../../README.md) | [Day 02: OOP — Classes, Objects & Memory →](../Day_02_OOP_Classes_Objects_Memory/Day_02_OOP_Classes_Objects_Memory.md) |

[![Phase](https://img.shields.io/badge/Phase_01-Java_Foundations-brightgreen.svg?style=for-the-badge)](../../README.md)
[![Day](https://img.shields.io/badge/Day-01_of_60-blue.svg?style=for-the-badge)](../../README.md)
[![Difficulty](https://img.shields.io/badge/Difficulty-Beginner_Friendly-00cc00.svg?style=for-the-badge)](../../README.md)
[![Java Version](https://img.shields.io/badge/Java-21_LTS-orange.svg?style=for-the-badge)](https://openjdk.org/projects/jdk/21/)

---

## 📌 What Will You Learn Today?

Hey, welcome to Day 01! 🎉 This is where it all begins.

Look, I know you might be thinking — *"I'm just getting started with Java, and this course has 'Generative AI' in the title... am I in over my head?"* Absolutely not. Here's the deal:

**We're going to take this step by step, together.** We're not going to skip anything or assume you already know stuff. If a word sounds confusing, we'll stop and explain it right there — in plain English, not textbook language. Think of me as your friend who happens to know this stuff and is walking you through it on a whiteboard.

Here's what we'll cover today (don't worry, we'll explain everything along the way):
- ✅ **How Java actually works** — what happens behind the scenes when you hit "Run" (the JVM, Bytecode, and why Java code runs on any computer)
- ✅ **JDK vs JRE vs JVM** — these 3 acronyms confuse everyone at first, but they're actually simple once you see the picture
- ✅ **Setting up Java 21** on your machine and making sure everything works
- ✅ **Writing your first program** — and we'll break down every single word in `public static void main(String[] args)` so nothing feels like magic
- ✅ **Peeking inside compiled code** — ever wondered what Java turns your code into? We'll look at actual bytecode (it's cool, trust me)
- ✅ **JShell** — Java's interactive playground where you can test code instantly, just like Python's terminal
- ✅ **Packages** — how Java keeps code organized so big projects don't become a mess
- ✅ **Maven** — the tool that downloads libraries and builds your project (think of it like `npm` for Java)
- ✅ **Why any of this matters for AI** — a quick peek at where this journey is heading (don't worry, we'll explain every AI term when we get there)

---

![Java Execution Pipeline and Ecosystem Architecture](assets/day01_java_ecosystem.jpg)

## 🗺️ Table of Contents

- [1. Why Java for Generative AI?](#1-why-java-for-generative-ai)
- [2. The Java Mental Model: How Code Runs](#2-the-java-mental-model-how-code-runs)
  - [2.1 Python vs. Java Execution Model](#21-python-vs-java-execution-model)
  - [2.2 JDK vs. JRE vs. JVM](#22-jdk-vs-jre-vs-jvm)
- [3. Setting Up the Environment (JDK 21 LTS)](#3-setting-up-the-environment-jdk-21-lts)
  - [3.1 Verifying Your JDK Installation](#31-verifying-your-jdk-installation)
  - [3.2 Setting Up Your IDE](#32-setting-up-your-ide)
- [4. Your First Java Program — Deconstructed Line by Line](#4-your-first-java-program--deconstructed-line-by-line)
  - [4.1 Writing `HelloGenAI.java`](#41-writing-hellogenaijava)
  - [4.2 The Anatomy of `public static void main`](#42-the-anatomy-of-public-static-void-main)
  - [4.3 Compiling and Executing](#43-compiling-and-executing)
  - [4.4 Inspecting the Bytecode with `javap`](#44-inspecting-the-bytecode-with-javap)
- [5. Packages & Namespaces: Organizing Code Like a Pro](#5-packages--namespaces-organizing-code-like-a-pro)
- [6. JShell: The Instant Feedback Loop](#6-jshell-the-instant-feedback-loop)
- [7. Maven: The Enterprise Build Engine](#7-maven-the-enterprise-build-engine)
  - [7.1 What Problem Does Maven Solve?](#71-what-problem-does-maven-solve)
  - [7.2 Standard Directory Layout](#72-standard-directory-layout)
  - [7.3 Understanding `pom.xml`](#73-understanding-pomxml)
  - [7.4 The Maven Lifecycle Commands](#74-the-maven-lifecycle-commands)
- [8. Python vs. Java: The Mental Bridge](#8-python-vs-java-the-mental-bridge)
- [9. Why This Matters for Generative AI](#9-why-this-matters-for-generative-ai)
- [10. Key Takeaways & Summary](#10-key-takeaways--summary)
- [11. Practice Exercises & Solutions](#11-practice-exercises--solutions)

---

# 1. Why Java for Generative AI?

Okay, before we touch any code, let's answer the big question you're probably thinking:

> *"Wait — isn't AI a Python thing? Why would I learn Java for AI?"*

Great question. And yeah, if you scroll through YouTube or Twitter, it looks like AI = Python. That's partly true — Python is where researchers train AI models from scratch using heavy math libraries.

But here's what those tutorials don't tell you: **the real-world companies that actually USE AI in production — banks, e-commerce sites like Amazon and Flipkart, payment systems like Razorpay and Stripe — their entire backend is already built in Java.** They can't just throw away millions of lines of Java code and rewrite everything in Python.

So what do they do? They add AI features **directly into their existing Java applications**. And that's exactly what we'll learn in this course.

> 🆕 **New Word Alert — "Generative AI"**: You know how ChatGPT can write essays, answer questions, and generate code? That's Generative AI. It's software that can *generate* new text, images, or code based on what you ask it. The "AI models" behind it (like GPT-4, Claude, Llama) are called **Large Language Models (LLMs)**. Think of an LLM as a super-smart autocomplete — you give it a question, it generates an answer. Throughout this course, we'll learn how to connect our Java applications to these LLMs.

> 🆕 **New Word Alert — "Token"**: When you send text to an AI model, it doesn't read words the way you do. It breaks your text into small chunks called "tokens". A token is roughly 4 characters or about ¾ of a word. So the sentence "Hello world" is about 2-3 tokens. AI companies charge you based on how many tokens you send and receive. We'll explore this more later — for now, just know that a token ≈ a small piece of text.

Here's the big picture of how this works in real companies:

```
┌─────────────────────────────────────────────────────────────┐
│                  How AI Works in Real Companies              │
│                                                             │
│   [AI Researchers]  ──►  Build & Train AI Models in Python  │
│                                      │                      │
│                          The trained model becomes an API   │
│                          (like a web service you can call)  │
│                                      ▼                      │
│   [Your Java Application] ──►  Calls the AI model's API     │
│    - Your Spring Boot app            - Sends a question     │
│    - Your existing database          - Gets back an answer  │
│    - Your users and business logic   - All inside Java!     │
└─────────────────────────────────────────────────────────────┘
```

So in plain English: **Python builds the brain, Java builds the body that uses the brain.** And in this course, we're building that body.

> 🆕 **New Word Alert — "Spring AI"**: You probably already heard of Spring Boot (we'll learn it properly starting Day 09). Spring AI is just a new addition to the Spring family that makes it super easy to call AI models from your Java code. Instead of writing complicated HTTP requests to OpenAI's API yourself, Spring AI gives you simple Java methods like `chatClient.prompt("What is Java?").call()`. That's it. We'll get there step by step.

You don't need to memorize any of this right now. We'll revisit every single one of these concepts in detail later in the course. For today, the only thing that matters is: **Java is not just relevant for AI — it's essential for production AI, and that's where the jobs and career growth are.**

---

# 2. The Java Mental Model: How Code Runs

Alright, let's get into the actual Java stuff! Before we write any code, let's understand what happens behind the scenes when you press "Run" in Java. This is actually really cool once you get it.

![Java Execution Pipeline and Ecosystem Architecture](assets/day01_java_ecosystem.jpg)

### 💡 The Plain-English Translation (No Jargon!)

If you've been doing core Java (classes, methods, loops), some ecosystem words sound overly academic. Here is what they actually mean in plain, everyday English:

| Jargon Term | What It ACTUALLY Means in Plain English | Real-World Equivalent |
| :--- | :--- | :--- |
| **JDK** | The complete developer toolkit. Contains the compiler (`javac`), runner (`java`), and tools. | The entire mechanic's workshop with all tools and cranes. |
| **JRE** | The runtime environment needed just to execute a Java app. | A car that's ready to drive (has the engine and fuel tank). |
| **JVM** | The software engine that reads `.class` bytecode and runs it on your CPU. | The physical engine under the car's hood. |
| **Bytecode (`.class`)** | An intermediate, universal language that isn't human code and isn't raw machine code. | A sheet of universal musical notes that any musician in any country can play. |
| **JIT Compiler** | A smart assistant inside the JVM that spots code you run frequently and turns it into lightning-fast machine code on the fly. | A chef memorizing a popular recipe so they don't have to read the cookbook every single order. |

---

### 2.1 Python vs. Java Execution Model

In Python, the interpreter reads your code line by line and executes it on the fly:

```
[ Python Script (.py) ] ──► [ Python Interpreter ] ──► [ OS / CPU Executes Immediately ]
```
*If there is a typo or type mismatch on line 50, Python runs lines 1–49 happily and then crashes on line 50 at runtime.*

In Java, code is **compiled first**, and then executed by a virtual machine:

```
Step 1: Compile-time (Developer's Machine)
┌───────────────────────┐         javac          ┌───────────────────────┐
│  HelloGenAI.java      │  ───────────────────►  │  HelloGenAI.class     │
│  (Human-Readable)     │    (Java Compiler)     │  (Bytecode - Portable)│
└───────────────────────┘                        └───────────────────────┘
                                                            │
Step 2: Runtime (Any OS: Windows, Linux, Mac, Cloud)        │
                                                            ▼
┌────────────────────────────────────────────────────────────────────────┐
│                     JVM (Java Virtual Machine)                         │
│                                                                        │
│   ┌──────────────────┐    ┌─────────────────┐    ┌─────────────────┐   │
│   │   Class Loader   │───►│ Execution Engine│───►│  JIT Compiler   │   │
│   │ (Loads Bytecode) │    │  (Interpreter)  │    │  (Hotspot Opt)  │   │
│   └──────────────────┘    └─────────────────┘    └─────────────────┘   │
└────────────────────────────────────────────────────────────────────────┘
                                    │
                                    ▼
                         [ Native CPU Machine Code ]
                         (Intel x86, ARM64, AMD64)
```

### Real-World Analogy: The Architect, Blueprint, and Construction Crew

- **`.java` file**: The architect's handwritten sketches and notes (readable by human engineers).
- **`javac` (Compiler)**: The architectural drafting office. It inspects every line, validates building codes, checks structural integrity, and produces a standardized architectural blueprint. If a wall is missing support, it rejects the design **before** any construction begins!
- **`.class` file (Bytecode)**: The finalized blueprint. It is not made of concrete yet, but it contains precise, universal instructions.
- **`JVM` (Java Virtual Machine)**: The local construction crew on site. Whether the site is in Tokyo (Windows), New York (Linux), or London (macOS), the crew reads the universal blueprint and translates it into physical bricks and steel for that specific terrain.
- **"Write Once, Run Anywhere" (WORA)**: You compile your `.class` bytecode once on your Windows PC. That identical `.class` file can run on an AWS Linux server, a Raspberry Pi, or a Mac without changing a single character!

---

### 2.2 JDK vs. JRE vs. JVM

Beginners often confuse these three acronyms. Here is the definitive distinction:

```
┌─────────────────────────────────────────────────────────────────────────┐
│ JDK (Java Development Kit)                                              │
│  - javac (Compiler)                                                     │
│  - jshell (Interactive REPL)                                            │
│  - javadoc, javap, jdb, jar                                             │
│  - Debuggers, Profilers, Tools                                          │
│                                                                         │
│   ┌─────────────────────────────────────────────────────────────────┐   │
│   │ JRE (Java Runtime Environment)                                  │   │
│   │  - Core Standard Libraries (java.lang, java.util, java.net)     │   │
│   │                                                                 │   │
│   │   ┌─────────────────────────────────────────────────────────┐   │   │
│   │   │ JVM (Java Virtual Machine)                              │   │   │
│   │   │  - ClassLoader                                          │   │   │
│   │   │  - Memory Management (Stack & Heap)                     │   │   │
│   │   │  - Garbage Collector (Automatic Memory Cleanup)         │   │   │
│   │   │  - Just-In-Time (JIT) HotSpot Compiler                  │   │   │
│   │   └─────────────────────────────────────────────────────────┘   │   │
│   └─────────────────────────────────────────────────────────────────┘   │
└─────────────────────────────────────────────────────────────────────────┘
```

| Acronym | Stands For | Who Needs It? | Real-World Analogy |
| :--- | :--- | :--- | :--- |
| **JVM** | **Java Virtual Machine** | The computer running the program | The engine inside a car that converts fuel to motion. |
| **JRE** | **Java Runtime Environment** | End users running pre-built Java apps | The complete car ready to drive on the road (engine + wheels + dashboard). |
| **JDK** | **Java Development Kit** | **Developers (YOU!)** | The auto-manufacturing factory (car + toolboxes, diagnostic computers, assembly cranes). |

> [!TIP]
> Since Java 11, the standalone JRE download was deprecated. When you install Java 21 today, you install the full **JDK**, which includes the compiler, tools, and the JVM automatically.

---

# 3. Setting Up the Environment (JDK 21 LTS)

### Why Java 21 LTS?
In Java's release cadence, LTS stands for **Long-Term Support**. Enterprises do not use experimental versions for production. 
- Java 8 (2014) was historic.
- Java 11 (2018) was the cloud standard.
- Java 17 (2021) was the Spring Boot 3 baseline.
- **Java 21 (Standard through 2029+)** is the modern gold standard. It introduced **Virtual Threads (Project Loom)**, **Records**, **Pattern Matching**, and **Sequenced Collections**—all of which make AI engineering faster and more elegant.

### 3.1 Verifying Your JDK Installation

Open your terminal (PowerShell on Windows, or Terminal on macOS/Linux) and check:

```bash
java -version
```

You should see output similar to:
```text
openjdk version "21.0.x" 2026-xx-xx LTS
OpenJDK Runtime Environment Temurin-21.0.x (build 21.0.x)
OpenJDK 64-Bit Server VM Temurin-21.0.x (build 21.0.x, mixed mode, sharing)
```

Next, verify the compiler:
```bash
javac -version
```

Output:
```text
javac 21.0.x
```

If both commands return `21`, you are ready!

---

### 3.2 Setting Up Your IDE

While you can write Java in Notepad, a professional Java engineer uses an IDE for code completion, refactoring, and instant compilation feedback:

1. **IntelliJ IDEA (Recommended)**: Download **IntelliJ IDEA Community Edition** (100% free and open source). It is the undisputed industry standard for Java and Spring Boot development.
2. **VS Code (Alternative)**: If you prefer VS Code, install the extension pack: **"Extension Pack for Java"** by Microsoft.

---

# 4. Your First Java Program — Deconstructed Line by Line

Alright, this is the fun part! Let's actually write some code. And we're not just going to write it — we're going to take it apart piece by piece so you know exactly why every single word is there.

### 4.1 Writing `HelloGenAI.java`

Create a file named `HelloGenAI.java`. In Java, **the file name must match the name of the `public class` exactly**, including capitalization!

```java
public class HelloGenAI {

    public static void main(String[] args) {
        System.out.println("Hello, Enterprise Gen AI World!");
        System.out.println("Java 21 + Spring AI is ready to rock.");
    }
}
```

---

### 4.2 The Anatomy of `public static void main`

Every beginner asks: *"Why does Java require so many words just to print a sentence, whereas Python only needs `print(...)`?"*

Here is the exact reason for every word:

```
 public  static  void  main ( String[]  args )
   │       │      │     │       │        │
   │       │      │     │       │        └─ Parameter name (array of strings)
   │       │      │     │       └────────── Parameter type (Array of Text inputs)
   │       │      │     └────────────────── Special method name recognized by JVM
   │       │      └──────────────────────── Returns nothing (no exit data)
   │       └─────────────────────────────── Belongs to the class, not an instance
   └─────────────────────────────────────── Accessible from anywhere by the JVM
```

Let's break down each element:

#### 1. `public class HelloGenAI`
- In Java, **everything lives inside a class**. Java is strictly object-oriented.
- `public`: An access modifier meaning this class is visible to the entire world, including the JVM launcher located outside this file.
- `class`: The keyword defining a blueprint.
- `HelloGenAI`: The identifier name. By Java convention, class names use `PascalCase` (e.g., `ChatResponse`, `VectorStore`, `PromptTemplate`).

#### 2. `public` (on the method)
- The JVM needs to call this entry point from the outside. If it were `private`, the JVM would be blocked by security rules and couldn't start your application.

#### 3. `static` (The Most Important Keyword!)
- Normally, to use a method inside a class, you must first create an object using `new` (e.g., `HelloGenAI myObj = new HelloGenAI()`).
- But when your program first boots up, **no objects exist yet**!
- By marking `main` as `static`, you tell the JVM: *"You can run this method directly on the class blueprint without creating an object in memory first."* This solves the chicken-and-egg problem of bootstrapping.

#### 4. `void`
- The return type. It means this function does not return any value back to the caller when it finishes. (Operating system exit codes are handled differently in Java via `System.exit(code)`).

#### 5. `main`
- The exact identifier that the JVM looks for as the official starting whistle of a Java application. If you name it `start` or `run`, the JVM will complain: `Main method not found in class`.

#### 6. `String[] args`
- `String[]`: An array of text strings.
- `args`: Arguments passed to your program from the command line. For example, if you run:
  `java HelloGenAI --model=gpt-4o --temperature=0.7`
  Then `args[0]` will be `"--model=gpt-4o"` and `args[1]` will be `"--temperature=0.7"`.

#### 7. `System.out.println(...)`
- `System`: A built-in core Java class provided by the runtime.
- `out`: The standard output stream (pointing to your terminal console).
- `println`: Short for "print line"—prints the string and moves the cursor to the next line.
- `;` (Semicolon): Every statement in Java must end with a semicolon. It tells the compiler where a single instruction ends, regardless of line breaks.

---

### 4.3 Compiling and Executing

Let's run this manually from the command line so you understand the raw process before letting an IDE hide it from you.

#### Step 1: Compile the source file into bytecode
```bash
javac HelloGenAI.java
```
Notice what happens: A new file named `HelloGenAI.class` appears in your folder!

#### Step 2: Run the compiled bytecode using the JVM
```bash
java HelloGenAI
```
*(Notice: Do NOT add `.class` or `.java` when running `java`! You supply the class name, not the file extension).*

**Output:**
```text
Hello, Enterprise Gen AI World!
Java 21 + Spring AI is ready to rock.
```

> [!TIP]
> **Java 11+ Single-File Execution Shortcut**:
> For small single-file scripts and quick tests, you can run:
> ```bash
> java HelloGenAI.java
> ```
> The JVM compiles the file in memory and executes it immediately without writing a `.class` file to disk! This makes Java feel as quick to experiment with as Python.

---

### 4.4 Inspecting the Bytecode with `javap`

Have you ever wondered what bytecode looks like? Java includes a disassembler called `javap`. Run:

```bash
javap -c HelloGenAI
```

You will see the actual assembly-like instructions that the JVM executes:

```text
Compiled from "HelloGenAI.java"
public class HelloGenAI {
  public HelloGenAI();
    Code:
       0: aload_0
       1: invokespecial #1                  // Method java/lang/Object."<init>":()V
       4: return

  public static void main(java.lang.String[]);
    Code:
       0: getstatic     #7                  // Field java/lang/System.out:Ljava/io/PrintStream;
       3: ldc           #13                 // String Hello, Enterprise Gen AI World!
       5: invokevirtual #15                 // Method java/io/PrintStream.println:(Ljava/lang/String;)V
       8: getstatic     #7                  // Field java/lang/System.out:Ljava/io/PrintStream;
      11: ldc           #21                 // String Java 21 + Spring AI is ready to rock.
      13: invokevirtual #15                 // Method java/io/PrintStream.println:(Ljava/lang/String;)V
      16: return
}
```

Look at instructions `3` and `5`:
- `ldc`: Load constant string `"Hello, Enterprise Gen AI World!"` onto the JVM operand stack.
- `invokevirtual`: Invoke the `println` method!

This bytecode is the secret to Java's cross-platform dominance: **Every JVM on Earth understands these exact bytecode instructions.**

---

# 5. Packages & Namespaces: Organizing Code Like a Pro

In real enterprise projects, you never write naked classes sitting at the root directory. What happens if two developers both write a class named `Document` (e.g., one for PDF documents, one for Vector Store documents)? A name collision occurs.

Java solves this with **Packages**.

### Real-World Analogy: Postal Addresses

Imagine sending a letter to "John Smith". Without a city, state, and country, the post office cannot deliver it.
```
Country (com) -> Company (javagenai) -> Feature (rag) -> Class (Document)
```

In Java, companies reverse their internet domain name to guarantee global uniqueness:
- Domain: `javagenai.com`
- Package: `com.javagenai.day01`

```java
package com.javagenai.day01;

public class SystemProbe {

    public static void main(String[] args) {
        String javaVersion = System.getProperty("java.version");
        String osName = System.getProperty("os.name");
        int availableProcessors = Runtime.getRuntime().availableProcessors();
        long maxMemoryMB = Runtime.getRuntime().maxMemory() / (1024 * 1024);

        System.out.println("=== AI System Diagnostic Probe ===");
        System.out.println("Java Version  : " + javaVersion);
        System.out.println("Operating Sys : " + osName);
        System.out.println("CPU Cores     : " + availableProcessors);
        System.out.println("Max JVM Memory: " + maxMemoryMB + " MB");
    }
}
```

### Folder Structure Rule:
If a class declares `package com.javagenai.day01;`, it **MUST physically reside in a matching directory path**:
`src/main/java/com/javagenai/day01/SystemProbe.java`

If the folder path does not match the package statement, the Java compiler will refuse to compile it. This enforces rigorous organizational cleanliness across million-line enterprise projects.

---

# 6. JShell: The Instant Feedback Loop

Python developers love the interactive REPL (Read-Eval-Print-Loop) or Jupyter Notebooks because you can test a one-line expression without creating a project or compiling.

**Java has had its own REPL since Java 9: `jshell`!**

Let's try it right now. In your terminal, type:

```bash
jshell
```

You will see:
```text
|  Welcome to JShell -- Version 21.0.x
|  For an introduction type: /help intro

jshell>
```

Now try typing Java statements directly—**no class, no `main` method, and semicolons are even optional!**

```java
jshell> int tokens = 1500 + 350
tokens ==> 1850

jshell> double costPer1k = 0.002
costPer1k ==> 0.002

jshell> double totalCost = (tokens / 1000.0) * costPer1k
totalCost ==> 0.0037

jshell> String model = "gpt-4o"
model ==> "gpt-4o"

jshell> String prompt = String.format("Requesting %s for %d tokens, Cost: $%.4f", model, tokens, totalCost)
prompt ==> "Requesting gpt-4o for 1850 tokens, Cost: $0.0037"

jshell> System.out.println(prompt.toUpperCase())
REQUESTING GPT-4O FOR 1850 TOKENS, COST: $0.0037
```

To exit JShell at any time, type:
```bash
/exit
```

> [!TIP]
> Throughout this course, whenever you want to quickly test how a Java method behaves (e.g., string manipulation, math calculations, regex parsing), pop open `jshell`! It takes 1 second and requires zero setup.

---

# 7. Maven: The Enterprise Build Engine

In real projects, you don't run `javac` by hand every time. And you definitely don't want to manually download library files from random websites. That's where **Maven** comes in — it's basically a project manager for your Java code.

Think of Maven like `npm` (if you know JavaScript) or `pip` (if you know Python) — except it also compiles your code, runs your tests, and packages everything into a single file you can deploy.

### 7.1 What Problem Does Maven Solve?

Imagine your Gen AI app needs:
1. Spring Boot Web (to expose REST endpoints)
2. Spring AI OpenAI (to talk to LLMs)
3. PostgreSQL Driver (to connect to the vector database)
4. Jackson (to serialize JSON)

Each of these libraries depends on 20 other libraries (transitive dependencies). If you manage this manually, you will fall into **"Dependency Hell"**—conflicting versions, missing classes, and corrupted builds.

**Maven automates:**
- Downloading the exact versions of all libraries from **Maven Central** (the global registry).
- Compiling all Java files across your project in the right order.
- Running your automated unit and integration tests.
- Packaging your application into a self-contained executable `.jar` file ready for Docker or AWS.

---

### 7.2 Standard Directory Layout

Maven enforces a standard folder structure across every Java project on Earth. If you open a Java project at Google, Netflix, or a 2-person startup, it always looks like this:

```
my-ai-application/
├── pom.xml                        ← The Project Object Model (Maven Blueprint)
└── src/
    ├── main/
    │   ├── java/                  ← Production Java source code
    │   │   └── com/
    │   │       └── javagenai/
    │   │           └── Application.java
    │   └── resources/             ← Config files, SQL scripts, prompts
    │       ├── application.yml
    │       └── prompts/
    │           └── system-prompt.st
    └── test/
        ├── java/                  ← Unit and Integration tests (JUnit 5)
        │   └── com/
        │       └── javagenai/
        │           └── ApplicationTests.java
        └── resources/             ← Test-specific configuration
```

---

### 7.3 Understanding `pom.xml`

The heart of Maven is the `pom.xml` (Project Object Model) file. Here is what an AI-ready `pom.xml` looks like:

```xml
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 
         https://maven.apache.org/xsd/maven-4.0.0.xsd">
    <modelVersion>4.0.0</modelVersion>

    <!-- 1. Coordinates: Who is this project? -->
    <groupId>com.javagenai</groupId>
    <artifactId>day01-setup</artifactId>
    <version>1.0.0</version>
    <packaging>jar</packaging>

    <name>Day 01 - Java Ecosystem and Setup</name>
    <description>First Java Gen AI application</description>

    <!-- 2. Properties: Global variables (Java version, encodings) -->
    <properties>
        <java.version>21</java.version>
        <maven.compiler.source>21</maven.compiler.source>
        <maven.compiler.target>21</maven.compiler.target>
        <project.build.sourceEncoding>UTF-8</project.build.sourceEncoding>
    </properties>

    <!-- 3. Dependencies: External libraries downloaded from Maven Central -->
    <dependencies>
        <!-- Unit Testing with JUnit 5 -->
        <dependency>
            <groupId>org.junit.jupiter</groupId>
            <artifactId>junit-jupiter</artifactId>
            <version>5.11.3</version>
            <scope>test</scope>
        </dependency>
    </dependencies>

    <!-- 4. Build Configuration -->
    <build>
        <plugins>
            <plugin>
                <groupId>org.apache.maven.plugins</groupId>
                <artifactId>maven-compiler-plugin</artifactId>
                <version>3.13.0</version>
                <configuration>
                    <release>21</release>
                </configuration>
            </plugin>
        </plugins>
    </build>
</project>
```

#### The Maven Coordinate System (G-A-V)
Every piece of software in the Java universe is uniquely identified by three coordinates:
- **`groupId`**: The organization or company (e.g., `org.springframework.ai`, `com.google.guava`).
- **`artifactId`**: The specific project/module name (e.g., `spring-ai-openai`, `guava`).
- **`version`**: The release version (e.g., `1.0.0`, `33.0.0-jre`).

---

### 7.4 The Maven Lifecycle Commands

Maven has standard lifecycle phases that run in sequential order:

```
[ validate ] ──► [ compile ] ──► [ test ] ──► [ package ] ──► [ verify ] ──► [ install ]
```

Here are the commands you will use daily:

| Command | What It Does | When To Use It |
| :--- | :--- | :--- |
| `mvn compile` | Compiles all source files in `src/main/java` into `target/classes` | Check if your code has compilation errors. |
| `mvn test` | Compiles test files and executes all JUnit test suites | Verify code correctness before committing. |
| `mvn clean` | Deletes the entire `target/` output folder | Reset state when files get out of sync. |
| `mvn package` | Compiles, runs tests, and packages code into a `.jar` file in `target/` | Prepare a production artifact for deployment. |
| `mvn clean package` | Wipes old builds and generates a pristine, fresh `.jar` | The gold standard pre-deployment build command. |

---

# 8. Python vs. Java: The Mental Bridge

If you are coming from Python, here is your Rosetta Stone to map concepts instantly:

| Concept | Python | Java (Modern Java 21) |
| :--- | :--- | :--- |
| **Typing** | Dynamic (`x = 10`, `x = "hello"`) | Static & Strong (`int x = 10;`, `String s = "hello";` or `var x = 10;`) |
| **Execution** | Interpreted (`python app.py`) | Compiled to Bytecode (`javac App.java`), executed by JVM (`java App`) |
| **File / Class Rule** | Multiple classes in any filename | One `public class` per file matching filename exactly |
| **Null / None** | `None` | `null` (or safer modern `Optional<T>`) |
| **Package Management** | `pip` + `requirements.txt` / `poetry` | Maven (`pom.xml`) or Gradle (`build.gradle`) |
| **Interactive Shell** | Python REPL / Jupyter | `jshell` |
| **Entry Point** | `if __name__ == "__main__":` | `public static void main(String[] args)` |
| **Printing** | `print("Hello")` | `System.out.println("Hello");` |
| **String Interpolation** | `f"Model: {model}"` | `String.format("Model: %s", model)` or `"""Text Blocks"""` |
| **Concurrency** | GIL limitation (asyncio / multiprocessing) | **Virtual Threads** (millions of lightweight threads with zero GIL) |

---

# 9. Why This Matters for Generative AI

Okay, you might be wondering — *"Cool, I learned about JDK, bytecode, and Maven today. But how does any of this connect to AI?"*

Great question! Let me give you a quick preview of **why these Java basics are actually the foundation for everything we'll build later**. Don't worry if some of these ideas sound new — we'll explain each one in detail when we get to it. This is just a sneak peek so you can see the bigger picture:

```
┌────────────────────────────────────────────────────────────────────────┐
│ How Today's Java Basics Connect to AI (A Friendly Preview)            │
├────────────────────────────────────────────────────────────────────────┤
│                                                                        │
│ 1. Java's Type Safety:                                                 │
│    When an AI model sends back a response (like a JSON object with    │
│    a customer name, order total, and invoice number), Java checks     │
│    that all the fields are correct BEFORE your code even runs.        │
│    Python might crash at 3 AM because a field was missing.            │
│    Java catches that mistake right away at compile time.              │
│                                                                        │
│ 2. The JVM's Memory Management:                                        │
│    Later in this course, we'll work with AI features that need to     │
│    store LOTS of data in memory (we'll explain what and why when      │
│    we get there). Java's memory system is built to handle that        │
│    without your application slowing down or crashing.                 │
│                                                                        │
│ 3. Maven:                                                              │
│    As our project grows, we'll need to add libraries for AI, web     │
│    APIs, databases, and security. Maven will download and manage     │
│    all of these for us automatically — no manual file hunting.        │
│                                                                        │
│ 4. Virtual Threads (we'll learn about these on Day 07):                │
│    Imagine a chatbot with 5,000 users chatting at the same time.     │
│    Each user is waiting for the AI to respond. Java 21 can handle    │
│    all 5,000 of those conversations simultaneously without breaking  │
│    a sweat. Most other languages can't do this as efficiently.       │
└────────────────────────────────────────────────────────────────────────┘
```

---

# 10. Key Takeaways & Summary

```
                  ┌───────────────────────────────┐
                  │      DAY 01 CHEAT SHEET       │
                  └──────────────┬────────────────┘
                                 │
         ┌───────────────────────┼───────────────────────┐
         ▼                       ▼                       ▼
  [ Architecture ]        [ Language Rules ]       [ Toolchain ]
  • .java -> .class       • Class name matches     • JDK = Compiler + Tools
  • Bytecode runs on JVM    file name exactly      • JRE = Runtime + Libs
  • JIT turns hot code    • static allows running  • JVM = Virtual Machine
    into native machine     before objects exist   • jshell for fast REPL
    code at runtime       • Strong static typing   • Maven manages builds &
  • Write Once, Run       • Semicolons terminate     dependencies via pom.xml
    Anywhere (WORA)         every statement
```

---

# 11. Practice Exercises & Solutions

To solidify your knowledge, complete these three hands-on exercises.

### 🏋️ Exercise 1: Build a System Info Checker
**Objective**: Write a standalone Java program named `GenAIRuntimeInfo.java` that checks your computer's specs and tells you if your machine is powerful enough to run AI models locally.

**What it should do**:
1. Print Java specification version and vendor.
2. Print available CPU processor cores.
3. Print total available JVM memory in Megabytes and Gigabytes.
4. If available CPU cores $\ge 4$ and memory $\ge 2048$ MB, print `[STATUS]: Host is READY for local Ollama LLM execution!`, otherwise print a warning recommendation.

#### Solution:
```java
public class GenAIRuntimeInfo {

    public static void main(String[] args) {
        String javaVersion = System.getProperty("java.version");
        String javaVendor = System.getProperty("java.vendor");
        String osName = System.getProperty("os.name");
        String osArch = System.getProperty("os.arch");

        int cores = Runtime.getRuntime().availableProcessors();
        long maxMemoryBytes = Runtime.getRuntime().maxMemory();
        long maxMemoryMB = maxMemoryBytes / (1024 * 1024);
        double maxMemoryGB = maxMemoryMB / 1024.0;

        System.out.println("==================================================");
        System.out.println("       ENTERPRISE GEN AI HOST DIAGNOSTIC          ");
        System.out.println("==================================================");
        System.out.printf("Java Runtime : %s (%s)%n", javaVersion, javaVendor);
        System.out.printf("Operating Sys: %s (%s)%n", osName, osArch);
        System.out.printf("CPU Cores    : %d threads available%n", cores);
        System.out.printf("Max Memory   : %d MB (%.2f GB)%n", maxMemoryMB, maxMemoryGB);
        System.out.println("--------------------------------------------------");

        if (cores >= 4 && maxMemoryMB >= 2048) {
            System.out.println("✅ [STATUS]: Host is READY for local Ollama LLM execution!");
        } else {
            System.out.println("⚠️  [STATUS]: Host resources are constrained for large local models.");
            System.out.println("   Recommendation: Use cloud endpoints (OpenAI/Anthropic) or 1B quant models.");
        }
        System.out.println("==================================================");
    }
}
```

---

### 🏋️ Exercise 2: Command-Line Token Cost Calculator
**Objective**: Write a program named `TokenCostCalculator.java` that accepts command-line arguments (`args`) for:
1. Model name (e.g., `gpt-4o-mini`)
2. Input tokens used (e.g., `2500`)
3. Output tokens used (e.g., `800`)

Calculate the total cost assuming:
- Input pricing: \$0.150 per 1,000,000 tokens ($0.00000015 / token)
- Output pricing: \$0.600 per 1,000,000 tokens ($0.00000060 / token)

#### Solution:
```java
public class TokenCostCalculator {

    public static void main(String[] args) {
        if (args.length < 3) {
            System.out.println("Usage: java TokenCostCalculator <modelName> <inputTokens> <outputTokens>");
            System.out.println("Example: java TokenCostCalculator gpt-4o-mini 2500 800");
            return;
        }

        String model = args[0];
        long inputTokens = Long.parseLong(args[1]);
        long outputTokens = Long.parseLong(args[2]);

        // Pricing per million tokens
        double inputPricePerMillion = 0.150;
        double outputPricePerMillion = 0.600;

        double inputCost = (inputTokens / 1_000_000.0) * inputPricePerMillion;
        double outputCost = (outputTokens / 1_000_000.0) * outputPricePerMillion;
        double totalCost = inputCost + outputCost;

        System.out.println("=== LLM API Call Cost Breakdown ===");
        System.out.println("Model Chosen : " + model);
        System.out.printf("Input Tokens : %,d tokens ($%.6f)%n", inputTokens, inputCost);
        System.out.printf("Output Tokens: %,d tokens ($%.6f)%n", outputTokens, outputCost);
        System.out.println("------------------------------------");
        System.out.printf("Total Cost   : $%.6f USD%n", totalCost);
    }
}
```

---

### 🏋️ Exercise 3: JShell Interactive Token Estimation
**Objective**: Launch `jshell` and experiment with string splitting to estimate token counts for an arbitrary prompt. (A standard rule of thumb is that 1 token $\approx$ 4 characters of English text, or roughly 0.75 words).

```java
// Open JShell in your terminal:
// $ jshell

String prompt = "Explain retrieval-augmented generation and vector databases in enterprise Java applications.";

// 1. Calculate character count
int charCount = prompt.length();

// 2. Approximate token count by character length (charCount / 4)
int estimatedTokensByChars = charCount / 4;

// 3. Approximate token count by word count
String[] words = prompt.split("\\s+");
int wordCount = words.length;
int estimatedTokensByWords = (int) Math.ceil(wordCount / 0.75);

System.out.printf("Characters: %d, Words: %d%n", charCount, wordCount);
System.out.printf("Estimated Tokens (Char Rule): %d%n", estimatedTokensByChars);
System.out.printf("Estimated Tokens (Word Rule): %d%n", estimatedTokensByWords);
```

---

## 🧭 Self-Check Quiz

1. **Why does the `main` method have to be `static`?**
   - *Answer*: Because when the JVM boots up, no instance objects of the class exist yet in the heap. Marking `main` as `static` allows the JVM runtime to invoke the method directly on the class metadata without first instantiating an object.
2. **What happens if your Java filename is `App.java` but your public class is `public class Application`?**
   - *Answer*: The compilation fails immediately with an error: `class Application is public, should be declared in a file named Application.java`.
3. **What is the difference between `javac` and `java`?**
   - *Answer*: `javac` is the compiler that transforms human-readable `.java` source code into `.class` bytecode. `java` is the runtime launcher that starts the JVM to execute that bytecode.
4. **Why do we use reverse domain names like `com.javagenai` for package names?**
   - *Answer*: Internet domain names are globally unique. By reversing the domain (`com.javagenai`), package names are guaranteed not to collide with other libraries or organizations across the global Java ecosystem.
5. **What are the 3 GAV coordinates in Maven?**
   - *Answer*: `groupId` (organization/domain), `artifactId` (specific module name), and `version` (release tag).

---

<p align="center">
  <b>Nice work finishing Day 01! 🎉</b><br>
  Tomorrow on <b>Day 02</b>, we'll dive into <b>Object-Oriented Programming (OOP) — Classes, Objects & Memory</b>. We'll learn how Java organizes data, where objects actually live in your computer's memory, and why <code>==</code> doesn't always work the way you'd expect. See you there!
</p>
