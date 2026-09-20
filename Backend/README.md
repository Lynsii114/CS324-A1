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
