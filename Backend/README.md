# Backend RMI Nodes

This module currently implements the Bootstrap Node and the basic Worker Node using Java RMI.

## Bootstrap Node

The Bootstrap Node runs as its own Java process and keeps a thread-safe registry of active workers. It exposes methods to:

- register a worker
- unregister a worker
- get all active workers
- get one randomly selected active worker

It does not participate in leader election or job processing.

## Worker Node

Each Worker Node runs as its own Java process. A worker has:

- unique integer ID
- Job Allocation Counter (JAC), initialized to `0`
- neighbour list, initialized empty
- current coordinator ID, initialized to its own worker ID
- `leaderman`, initialized to `"cs324"`

When a worker starts, it exports itself over RMI, asks the Bootstrap Node for a randomly selected active worker, connects to that worker as a neighbour, updates both workers with the neighbour relationship, and then registers itself with the Bootstrap Node. Leader election is not implemented yet.

## Compile

From the repository root:

```powershell
javac -d Backend/out (Get-ChildItem -Recurse Backend/src/main/java -Filter *.java).FullName
```

## Run Bootstrap Node

Start the Bootstrap Node:

```powershell
java -cp Backend/out cs324.bootstrap.BootstrapServer
```

By default it binds the RMI service as:

```text
rmi://localhost:1099/BootstrapService
```

You can also choose a port and binding name:

```powershell
java -cp Backend/out cs324.bootstrap.BootstrapServer 2099 MyBootstrap
```

## Run Worker Node

Start the Bootstrap Node first. Then, in another terminal, start a worker:

```powershell
java -cp Backend/out cs324.worker.WorkerNode 1 5001
```

The arguments are:

```text
<workerId> <workerRmiPort> [bootstrapHost] [bootstrapPort] [bootstrapBindingName]
```

Example with a custom Bootstrap Node:

```powershell
java -cp Backend/out cs324.worker.WorkerNode 1 5001 localhost 2099 MyBootstrap
```

## Run 6 Worker Nodes From VS Code

Open the project folder in VS Code. Use one VS Code terminal for the Bootstrap Node and six separate VS Code terminals for the six Worker Node processes.

Step 1: Open a VS Code terminal with `Terminal > New Terminal`.

Step 2: Compile the backend:

```powershell
.\Backend\scripts\compile.ps1
```

If PowerShell blocks scripts on your machine, run the compile command directly instead:

```powershell
javac -d Backend/out (Get-ChildItem -Recurse Backend/src/main/java -Filter *.java).FullName
```

Step 3: In terminal 1, start the Bootstrap Node:

```powershell
.\Backend\scripts\start-bootstrap.ps1
```

Or run it directly:

```powershell
java -cp Backend/out cs324.bootstrap.BootstrapServer
```

Step 4: Open six more VS Code terminals. In each terminal, start exactly one worker:

Terminal 2:

```powershell
.\Backend\scripts\start-worker-1.ps1
```

Terminal 3:

```powershell
.\Backend\scripts\start-worker-2.ps1
```

Terminal 4:

```powershell
.\Backend\scripts\start-worker-3.ps1
```

Terminal 5:

```powershell
.\Backend\scripts\start-worker-4.ps1
```

Terminal 6:

```powershell
.\Backend\scripts\start-worker-5.ps1
```

Terminal 7:

```powershell
.\Backend\scripts\start-worker-6.ps1
```

The six workers use these fixed IDs and RMI ports:

```text
Worker 1 -> port 5001
Worker 2 -> port 5002
Worker 3 -> port 5003
Worker 4 -> port 5004
Worker 5 -> port 5005
Worker 6 -> port 5006
```

Each worker runs independently in its own Java process. When workers 2 through 6 start, each one asks the Bootstrap Node for a random active worker and creates a bidirectional neighbour connection using Java RMI.

To inspect any worker, use:

```powershell
java -cp Backend/out cs324.worker.WorkerTestClient localhost 5001 1
java -cp Backend/out cs324.worker.WorkerTestClient localhost 5002 2
java -cp Backend/out cs324.worker.WorkerTestClient localhost 5003 3
java -cp Backend/out cs324.worker.WorkerTestClient localhost 5004 4
java -cp Backend/out cs324.worker.WorkerTestClient localhost 5005 5
java -cp Backend/out cs324.worker.WorkerTestClient localhost 5006 6
```

## Test Bootstrap Node

Leave the server running in one terminal, then open another terminal and run:

```powershell
java -cp Backend/out cs324.bootstrap.BootstrapTestClient
```

For a custom port and binding name:

```powershell
java -cp Backend/out cs324.bootstrap.BootstrapTestClient localhost 2099 MyBootstrap
```

The test client registers two workers, prints the active worker list, prints a random worker, unregisters one worker, and prints the updated list.

## Test Worker Node

Run the Bootstrap Node:

```powershell
java -cp Backend/out cs324.bootstrap.BootstrapServer
```

Run a Worker Node in another terminal:

```powershell
java -cp Backend/out cs324.worker.WorkerNode 1 5001
```

Inspect that worker from a third terminal:

```powershell
java -cp Backend/out cs324.worker.WorkerTestClient localhost 5001 1
```

You should see the worker ID, `JAC` as `0`, its neighbour list, the current coordinator ID, and `leaderman` as `cs324`.

To test neighbour creation, run a second worker while the first worker is still running:

```powershell
java -cp Backend/out cs324.worker.WorkerNode 2 5002
```

Then inspect both workers:

```powershell
java -cp Backend/out cs324.worker.WorkerTestClient localhost 5001 1
java -cp Backend/out cs324.worker.WorkerTestClient localhost 5002 2
```

Worker `1` should list worker `2` as a neighbour, and worker `2` should list worker `1` as a neighbour. Each worker only stores its own direct neighbours.
