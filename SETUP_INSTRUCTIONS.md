# Setup Instructions for Agile Team Sprint Tracker

## Prerequisites

1. **Install Java Development Kit (JDK) 11 or higher**
   - Download and install from: https://adoptium.net/
   - Verify installation by running: `java -version`

2. **Install Maven**
   - Download from: https://maven.apache.org/download.cgi
   - Extract the downloaded archive to a directory of your choice
   - Add the Maven bin directory to your system PATH:
     - Windows: 
       - Open System Properties > Advanced > Environment Variables
       - Add the path to Maven's bin directory (e.g., `C:\apache-maven-3.9.6\bin`) to the PATH variable
     - Linux/Mac: 
       - Add `export PATH=/path/to/maven/bin:$PATH` to your `.bashrc` or `.zshrc`
   - Verify installation by running: `mvn -version`

## Building the Project

1. **Clone or download the project**
   - Save it to a directory of your choice

2. **Build the project**
   - Open a terminal/command prompt
   - Navigate to the project directory
   - Run: `mvn clean package`
   - This will create a JAR file in the `target` directory

## Running the Application

**Option 1: Using Maven**
```
mvn exec:java -Dexec.mainClass="com.agileteam.sprinttracker.Main"
```

**Option 2: Using the JAR file**
```
java -jar target/sprint-tracker-1.0-SNAPSHOT-jar-with-dependencies.jar
```

## Manual Setup (Alternative to Maven)

If you don't want to use Maven, you can manually compile and run the project:

1. **Create directories for compiled classes**
   ```
   mkdir -p target/classes
   ```

2. **Compile the project**
   - Download the required dependencies:
     - [Gson](https://mvnrepository.com/artifact/com.google.code.gson/gson/2.10.1)
     - [HttpClient](https://mvnrepository.com/artifact/org.apache.httpcomponents/httpclient/4.5.14)
   - Place the JAR files in a `libs` directory
   - Compile the project:
   ```
   javac -d target/classes -cp "libs/*" src/main/java/com/agileteam/sprinttracker/*.java src/main/java/com/agileteam/sprinttracker/*/*.java
   ```

3. **Run the application**
   ```
   java -cp "target/classes;libs/*" com.agileteam.sprinttracker.Main
   ```
   (Use `:` instead of `;` on Linux/Mac)

## Project Structure

The project follows a standard Maven directory structure:
```
src/main/java/com/agileteam/sprinttracker/
├── Main.java                       # Application entry point
├── model/                          # Data models
│   ├── Sprint.java                 # Represents a sprint
│   ├── UserStory.java              # Represents a user story
│   ├── Task.java                   # Represents a task
│   └── TeamMember.java             # Represents a team member
├── manager/
│   └── SprintManager.java          # Manages sprints and team members
├── storage/
│   └── DataStorage.java            # Handles data persistence
├── github/
│   └── GitHubIntegration.java      # GitHub API integration
└── ui/
    └── UIManager.java              # Console user interface
```

## GitHub Integration

To use the GitHub integration features:
1. Create a GitHub personal access token with `repo` scope
2. Configure the GitHub connection in the application
3. Import issues as user stories or tasks 