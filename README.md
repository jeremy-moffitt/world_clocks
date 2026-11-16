# World Clocks

A Java HTTP server chooses eight different cities from a catalog of major cities.
Each city uses its own IANA time-zone ID. The server reshuffles the cities every
five minutes and returns their current local times from `GET /api/clocks`. The
React app shows a live digital clock for every city and refreshes the city list
from the server every 30 seconds.

## Requirements

- JDK 11 or newer
- Node.js 20.19+ or 22.12+ and npm

## Start the Java server

From the `world_clocks` directory, compile and run the server in PowerShell:

```powershell
New-Item -ItemType Directory -Force backend\out
javac --add-modules jdk.httpserver -d backend\out backend\src\main\java\com\example\worldclocks\WorldClockServer.java
java --add-modules jdk.httpserver -cp backend\out com.example.worldclocks.WorldClockServer
```

In Git Bash, from the `world_clocks` directory:

```bash
mkdir -p backend/out
javac --add-modules jdk.httpserver -d backend/out backend/src/main/java/com/example/worldclocks/WorldClockServer.java
java --add-modules jdk.httpserver -cp backend/out com.example.worldclocks.WorldClockServer
```

## Run the backend tests

The backend tests use JUnit 5 and Maven. Install Maven, then run the following
from the `world_clocks` directory.

In PowerShell:

```powershell
Set-Location backend
mvn test
```

In Git Bash:

```bash
cd backend
mvn test
```

Maven downloads the test dependencies on the first run. Test reports are written
to `target/surefire-reports` inside the `backend` directory.

The API listens on `http://localhost:8080`. To inspect its response, open
`http://localhost:8080/api/clocks`.

## Launch the React app

In a second PowerShell window, from the `world_clocks` directory:

```powershell
Set-Location frontend
npm install
npm run dev
```

Or, in Git Bash from the `world_clocks` directory:

```bash
cd frontend
npm install
npm run dev
```

Open the local URL printed by Vite (normally `http://localhost:5173`). The Vite
development server proxies `/api` requests to the Java server on port 8080, so
start the Java server before loading the UI.
