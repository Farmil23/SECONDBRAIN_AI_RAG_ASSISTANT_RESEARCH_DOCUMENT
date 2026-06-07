# How to Run the Project

It seems **Maven (mvn)** is not installed or configured in your system "Path", so running it from the command line won't work easily.

Since you have **Eclipse**, the easiest way is to import this project into Eclipse.

## Option 1: Run with Eclipse (Recommended)
1.  Open **Eclipse**.
2.  Go to **File** > **Import...**
3.  Select **Maven** > **Existing Maven Projects** and click **Next**.
4.  Browse to this folder: `D:\LIFE\_KULIAH_ITENAS_\SEMESTER 4\PEMROGRAMAN BERIORIENTASI OBJEK\EVALUASI 1 - PROJE3CT\SecondBrain\scratch`
5.  Click **Finish**.
    *   *Eclipse will download the necessary libraries (Spring Boot, LangChain4j, etc.). This might take a minute.*
6.  Once loaded, in the **Project Explorer**:
    *   Expand `src/main/java`.
    *   Navigate to package `com.secondbrain`.
    *   Right-click `SecondBrainApplication.java`.
    *   Select **Run As** > **Java Application`.

## Option 2: IntelliJ IDEA (If you have it)
1.  Open IntelliJ.
2.  **File** > **Open** > Select the `pom.xml` in this folder.
3.  Click **Open as Project**.
4.  Run `FeatureApplication` main class.

## Option 3: Verify the App
Once the console shows "Started FeatureApplication in ... seconds":
- Open your browser to: [http://localhost:8080](http://localhost:8080)
- You should see the new "Second Brain Interface".

## Troubleshooting
- **Port 8080 used?**
  - Open `src/main/resources/application.properties` and change `server.port=8080` to `8081`.
- **Python Bridge Error?**
  - Ensure your Python server is running on port 5000.
