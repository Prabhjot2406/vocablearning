# ArgoCD & Kubernetes - Common Doubts & Clarifications

This document addresses all the questions and doubts that came up during the ArgoCD setup and learning process.

---

## Table of Contents
1. [Jenkins vs ArgoCD - Why Kubernetes?](#jenkins-vs-argocd---why-kubernetes)
2. [Pods vs Cluster - What's the Difference?](#pods-vs-cluster---whats-the-difference)
3. [ArgoCD Pods Explained](#argocd-pods-explained)
4. [Nodes vs Pods - Understanding the Difference](#nodes-vs-pods---understanding-the-difference)
5. [Pod Distribution Across Nodes](#pod-distribution-across-nodes)
6. [Namespaces - Why So Many?](#namespaces---why-so-many)
7. [Checking Pods on Specific Nodes](#checking-pods-on-specific-nodes)
8. [Control-Plane vs Worker Nodes](#control-plane-vs-worker-nodes)
9. [Cluster Addressing - Two Addresses for One Cluster](#cluster-addressing---two-addresses-for-one-cluster)
10. [ServiceAccount Creation - How Did It Happen?](#serviceaccount-creation---how-did-it-happen)
11. [kind-argocd-cluster vs argocd-cluster](#kind-argocd-cluster-vs-argocd-cluster)
12. [Why ServiceAccount in kube-system Namespace?](#why-serviceaccount-in-kube-system-namespace)

---

## Jenkins vs ArgoCD - Why Kubernetes?

### Question:
"I have worked on Jenkins; I have never created any cluster or pods for Jenkins, then why do I have to make a cluster for ArgoCD? Secondly, is it pods or cluster of ArgoCD?"

### Answer:

**Jenkins:**
- Runs as a **standalone application** (like a regular server process)
- Can be installed directly on a VM/EC2 instance
- Doesn't require Kubernetes to function
- You probably ran it as: `java -jar jenkins.war` or as a system service

**ArgoCD:**
- Is a **Kubernetes-native application**
- Designed to run **inside** Kubernetes as pods
- Its purpose is to **manage Kubernetes deployments**
- Cannot run standalone - it needs Kubernetes to exist

### Key Understanding:

You're **NOT creating a cluster FOR ArgoCD**. Here's what's happening:

1. **The Kind cluster** = Your **target environment** where you'll deploy your Spring Boot app
2. **ArgoCD pods** = Will run **inside** this cluster to manage deployments

```
┌─────────────────────────────────────────┐
│     Kubernetes Cluster (Kind)           │
│                                         │
│  ┌──────────────┐  ┌─────────────────┐ │
│  │ ArgoCD Pods  │  │ Your Spring Boot│ │
│  │ (Manager)    │──▶│ App Pods        │ │
│  └──────────────┘  └─────────────────┘ │
│                                         │
└─────────────────────────────────────────┘
```

**Answer:** ArgoCD **pods** are running inside the Kind **cluster** you created.

---

## Pods vs Cluster - What's the Difference?

### Question:
"So, my ArgoCD pods are running inside the Kind cluster that I have created before, right?"

### Answer:

**Exactly! Yes, 100% correct.** 🎯

When you run the ArgoCD installation commands, the ArgoCD pods are created and running **inside the Kind cluster** you already created.

```
Your Kind Cluster (argocd-cluster)
├── control-plane node
│   └── ArgoCD pods (some will run here)
├── worker node 1
│   └── ArgoCD pods (some will run here)
└── worker node 2
    └── ArgoCD pods (some will run here)
```

After installation, you can verify this by running:
```bash
kubectl get pods -n argocd
```

All running **inside** your existing Kind cluster. No new cluster is created.

---

## ArgoCD Pods Explained

### Question:
"What are these different pods?"

### Answer:

When you install ArgoCD, **7 different pods** are created, each with a specific role:

### 1. `argocd-server` 🖥️
- **The Web UI and API**
- What you access in your browser
- Handles user authentication and API requests

### 2. `argocd-application-controller` 🎯
- **The Brain** - monitors your applications
- Compares Git state vs Kubernetes state
- Triggers deployments when differences detected
- Performs sync operations

### 3. `argocd-repo-server` 📦
- **Git Repository Manager**
- Clones and caches your Git repos
- Generates Kubernetes manifests from your code
- Handles Helm charts, Kustomize, plain YAML

### 4. `argocd-dex-server` 🔐
- **Authentication Provider**
- Handles SSO (Single Sign-On) with external providers
- Integrates with GitHub, GitLab, LDAP, etc.

### 5. `argocd-redis` 💾
- **Cache Database**
- Stores temporary data for performance
- Caches Git repo data and application state

### 6. `argocd-applicationset-controller` 🔄
- **Multi-App Manager**
- Creates multiple ArgoCD applications from templates
- Useful for managing many similar apps

### 7. `argocd-notifications-controller` 📢
- **Alert System**
- Sends notifications (Slack, email, etc.)
- Alerts on deployment success/failure

### Simple Flow:
```
You → argocd-server (UI) 
       ↓
argocd-application-controller (monitors)
       ↓
argocd-repo-server (fetches from Git)
       ↓
Deploys to Kubernetes
       ↓
argocd-notifications-controller (notifies you)
```

---

## Nodes vs Pods - Understanding the Difference

### Question:
"What is the difference between pods and nodes? According to me, I have a KIND cluster that has 3 nodes (1 master, 2 workers), and inside those nodes one nginx pod is running, and my ArgoCD is installed in each node. Am I correct?"

### Answer:

Let me correct your understanding:

### **Node** = Physical/Virtual Machine 🖥️
- The **hardware** (server/VM)
- Provides CPU, memory, storage
- **Runs the operating system**
- Can host **multiple pods**

### **Pod** = Running Application Container 📦
- The **smallest deployable unit** in Kubernetes
- Runs **inside** a node
- Contains one or more containers
- Your actual application code runs here

### ❌ **INCORRECT Understanding:**
> "ArgoCD is installed in each node"

### ✅ **CORRECT Understanding:**

```
Kind Cluster
├── Node 1: argocd-cluster-control-plane (Master)
│   ├── Kubernetes system pods (kube-apiserver, etcd, etc.)
│   └── NO ArgoCD pods
│
├── Node 2: argocd-cluster-worker (Worker)
│   ├── argocd-dex-server pod
│   ├── argocd-notifications-controller pod
│   └── argocd-redis pod
│
└── Node 3: argocd-cluster-worker2 (Worker)
    ├── argocd-application-controller pod
    ├── argocd-applicationset-controller pod
    ├── argocd-repo-server pod
    └── argocd-server pod
```

### Key Points:

1. **You have 3 nodes** (machines) ✅
2. **You have 7 ArgoCD pods** (applications) ✅
3. **ArgoCD is NOT installed on each node** ❌
4. **ArgoCD pods are distributed across 2 worker nodes** ✅
5. **Control-plane node has NO ArgoCD pods** ✅

### Real-World Analogy:

**Node** = Apartment Building (physical structure)
**Pod** = Tenant living in an apartment (application)

```
Building 1 (control-plane node)
└── Manager's office (Kubernetes system)

Building 2 (worker node)
├── Tenant A (argocd-dex-server pod)
├── Tenant B (argocd-redis pod)
└── Tenant C (argocd-notifications pod)

Building 3 (worker node)
├── Tenant D (argocd-server pod)
├── Tenant E (argocd-repo-server pod)
└── Tenant F (argocd-controller pod)
```

**ArgoCD is NOT the building** - it's the **tenants (pods) living in the buildings (nodes)**!

---

## Pod Distribution Across Nodes

### Question:
"Why are 2 nodes running in different nodes?"

### Answer:

This is **completely normal and intentional**. Here's why:

### 1. High Availability (HA) 🛡️
- If one worker node crashes, ArgoCD still works
- Pods on other nodes keep running
- No single point of failure

### 2. Load Balancing ⚖️
- Distributes CPU/memory usage across nodes
- Prevents one node from being overloaded
- Better resource utilization

### 3. Kubernetes Scheduler 🤖
- Automatically decides pod placement
- Considers:
  - Available resources (CPU, memory)
  - Node health
  - Pod anti-affinity rules
  - Even distribution

### Your Current Distribution:

```
argocd-cluster-worker (3 pods):
├── argocd-dex-server
├── argocd-notifications-controller
└── argocd-redis

argocd-cluster-worker2 (4 pods):
├── argocd-application-controller
├── argocd-applicationset-controller
├── argocd-repo-server
└── argocd-server

argocd-cluster-control-plane (0 pods):
└── (Usually avoided for workloads)
```

### Why Control-Plane Has No Pods?

By default, Kubernetes **taints** the control-plane node to prevent regular workloads from running there. It's reserved for:
- Kubernetes system components (kube-apiserver, etcd, etc.)
- Cluster management tasks

### Key Point:

Even though pods are on different nodes, they communicate seamlessly through **Kubernetes networking**. The `ClusterIP` service acts as a load balancer, routing traffic to the correct pod regardless of which node it's on.

**This is a feature, not a bug!** 🎯

---

## Namespaces - Why So Many?

### Question:
"Why do I have so many namespaces? Also, how many namespaces do we get when we have a fresh KIND cluster?"

### Answer:

### Fresh Kind Cluster Has 5-6 Default Namespaces:

When you create a fresh Kind cluster, you get these namespaces automatically:

#### Default Kubernetes Namespaces (5):
1. **default** - Where your apps go if you don't specify a namespace
2. **kube-system** - Kubernetes core components (DNS, kube-proxy, etc.)
3. **kube-public** - Publicly readable cluster info
4. **kube-node-lease** - Node heartbeat/health data
5. **kube-public** - Public cluster configuration

#### Kind-Specific Namespace (1):
6. **local-path-storage** - Kind's storage provisioner (specific to Kind clusters)

### Your Current Namespaces (6):

| Namespace | Source | Purpose |
|-----------|--------|---------|
| **default** | Kubernetes | Your apps (nginx pods) |
| **kube-system** | Kubernetes | Core K8s components |
| **kube-public** | Kubernetes | Public cluster info |
| **kube-node-lease** | Kubernetes | Node health tracking |
| **local-path-storage** | Kind | Storage provisioner |
| **argocd** | You created it | ArgoCD installation |

### Why So Many?

**It's normal!** Namespaces provide:

1. **Isolation** - Separate resources logically
2. **Organization** - Group related resources
3. **Security** - Apply different permissions per namespace
4. **Resource Limits** - Set quotas per namespace

Think of namespaces like **folders on your computer** - they organize things.

### The One You Created:

```bash
kubectl create namespace argocd  # ← You ran this
```

This created the **argocd** namespace where all ArgoCD pods live.

**Summary:** 5 default + 1 Kind-specific + 1 you created = **6 total** ✅

---

## Checking Pods on Specific Nodes

### Question:
"How can I know in which node (worker node or master node) the ArgoCD pods are running?"

### Answer:

Use this command to see which node each pod is running on:

```bash
kubectl get pods -n argocd -o wide
```

The `-o wide` flag shows additional columns including:
- **NODE** - which node the pod is running on
- **IP** - pod's internal IP address
- **NOMINATED NODE** - scheduling info
- **READINESS GATES** - readiness status

### Alternative Commands:

```bash
# Show pod name and node name only
kubectl get pods -n argocd -o custom-columns=POD:metadata.name,NODE:spec.nodeName

# All pods on control-plane
kubectl get pods -n argocd --field-selector spec.nodeName=argocd-cluster-control-plane

# All pods on worker 1
kubectl get pods -n argocd --field-selector spec.nodeName=argocd-cluster-worker

# All pods on worker 2
kubectl get pods -n argocd --field-selector spec.nodeName=argocd-cluster-worker2
```

### To See What's Inside Each Namespace:

```bash
# See all resources in each namespace
kubectl get all -n argocd
kubectl get all -n default
kubectl get all -n kube-system

# Or check all namespaces at once
kubectl get all -A
```

---

## Control-Plane vs Worker Nodes

### Question:
"So, control-plane only has pods related to Kubernetes, and the rest of the worker nodes are responsible for running the application on them, right?"

### Answer:

**Exactly! 100% correct!** 🎯

### **Control-Plane Node** (Master)
- **Role:** Manages the cluster
- **Runs:** Kubernetes system components only
  - kube-apiserver (API server)
  - etcd (database)
  - kube-scheduler (decides where pods go)
  - kube-controller-manager (manages cluster state)
- **Does NOT run:** Your application pods (ArgoCD, Nginx, etc.)

### **Worker Nodes**
- **Role:** Run your applications
- **Runs:** Your application pods
  - ArgoCD pods
  - Nginx pods
  - Your Spring Boot app (when you deploy it)
  - Any other workloads

### Analogy:

```
Control-Plane = Company CEO/Management Office
├── Makes decisions
├── Manages operations
└── Doesn't do the actual work

Worker Nodes = Factory Floor/Employees
├── Execute the actual work
├── Run the applications
└── Handle customer requests
```

### Your Cluster:

```
Control-Plane Node (argocd-cluster-control-plane)
├── kube-apiserver ← Manages cluster
├── etcd ← Stores cluster data
├── kube-scheduler ← Schedules pods
└── kube-controller-manager ← Maintains state

Worker Node 1 (argocd-cluster-worker)
├── argocd-dex-server ← Your apps
├── argocd-redis
├── argocd-notifications
└── nginx pods (some)

Worker Node 2 (argocd-cluster-worker2)
├── argocd-server ← Your apps
├── argocd-repo-server
├── argocd-controller
└── nginx pods (some)
```

### Check Control-Plane Pods:

```bash
# See all pods on control-plane node
kubectl get pods -A --field-selector spec.nodeName=argocd-cluster-control-plane
```

This will show you all the **Kubernetes system pods** running on the control-plane node.

**You've got it perfectly!** The control-plane is the "brain" that manages, and workers are the "hands" that do the actual work. 🧠💪

---

## Cluster Addressing - Two Addresses for One Cluster

### Question:
"Why is it showing me 2 clusters? ArgoCD is a pod."

```
SERVER                          NAME            VERSION  STATUS
https://192.168.2.59:33893      argocd-cluster           Unknown
https://kubernetes.default.svc  in-cluster               Unknown
```

### Answer:

You're seeing **2 entries for the SAME cluster**, just accessed differently.

### They're NOT 2 Different Clusters

You have **ONE** physical cluster, but ArgoCD sees it through **2 different addresses**:

### 1. `https://kubernetes.default.svc` (in-cluster)

**What it is:**
- **Internal DNS name** within the cluster
- Only accessible from **inside** the cluster (pod-to-pod)
- ArgoCD pods use this to talk to Kubernetes API

**How it works:**
```
ArgoCD Pod (inside cluster)
    ↓ uses internal DNS
https://kubernetes.default.svc
    ↓ resolves to
Kubernetes API Server (same cluster)
```

**Analogy:** Like calling your coworker's desk extension (x1234) from inside the office

### 2. `https://192.168.2.59:33893` (argocd-cluster)

**What it is:**
- **External IP address** of your cluster
- Accessible from **outside** the cluster (your laptop, other machines)
- Created when you ran `argocd cluster add`

**How it works:**
```
Your Laptop / External Client
    ↓ uses external IP
https://192.168.2.59:33893
    ↓ connects to
Kubernetes API Server (same cluster)
```

**Analogy:** Like calling your coworker's direct phone number (555-1234) from outside the office

### Visual Representation:

```
┌─────────────────────────────────────────────┐
│         Your Kind Cluster                   │
│                                             │
│  ┌──────────────┐                          │
│  │ ArgoCD Pods  │                          │
│  └──────┬───────┘                          │
│         │                                   │
│         │ uses internal address             │
│         │ kubernetes.default.svc            │
│         ↓                                   │
│  ┌─────────────────────┐                   │
│  │  Kubernetes API     │◄──────────────────┼─── External access
│  │  Server             │   192.168.2.59:33893
│  └─────────────────────┘                   │
│                                             │
└─────────────────────────────────────────────┘
         SAME CLUSTER, 2 ADDRESSES
```

### Why Do You Have Both?

When you ran:
```bash
argocd cluster add kind-argocd-cluster --name argocd-cluster --insecure
```

ArgoCD:
1. Read your kubeconfig
2. Found the external address: `192.168.2.59:33893`
3. Created the `argocd-manager` ServiceAccount
4. Added it as a "cluster" entry

But ArgoCD **already had** the internal address (`kubernetes.default.svc`) by default.

### Which One Should You Use?

**For deploying apps to the same cluster:**
Use `https://kubernetes.default.svc` ✅

**Why:**
- Faster (no external network hop)
- More reliable (internal DNS)
- Standard practice for in-cluster deployments

### How External Address Gets Set:

In your `kind-config.yaml`:
```yaml
networking:
  apiServerAddress: "192.168.2.59"  # ← This creates external address
  apiServerPort: 33893
```

**Without this:** Kind would use `127.0.0.1:RANDOM_PORT` (localhost only)

**With this:** Kind uses your specified IP, making it accessible from other machines

**Summary:** 
- **1 physical cluster** = Your Kind cluster
- **2 addresses** = Internal (kubernetes.default.svc) + External (192.168.2.59:33893)
- **Use internal** for deploying apps to the same cluster 🎯

---

## ServiceAccount Creation - How Did It Happen?

### Question:
"My question is that how can this command create a separate service account? I am asking because there is no command that I remember running to create a separate service account."

### Answer:

Great question! Let me explain how `argocd cluster add` creates the ServiceAccount **automatically behind the scenes**.

### What Happens When You Run `argocd cluster add`

The `argocd cluster add` command does **multiple things automatically**:

#### Step-by-Step Process:

1. **Reads your kubeconfig** to get cluster connection details
2. **Connects to the Kubernetes cluster** using kubectl context
3. **Automatically creates these resources** in the cluster:
   ```bash
   # Creates ServiceAccount
   kubectl create serviceaccount argocd-manager -n kube-system
   
   # Creates ClusterRole with permissions
   kubectl create clusterrole argocd-manager-role --verb=* --resource=*
   
   # Creates ClusterRoleBinding
   kubectl create clusterrolebinding argocd-manager-role-binding \
     --clusterrole=argocd-manager-role \
     --serviceaccount=kube-system:argocd-manager
   ```
4. **Extracts the ServiceAccount token**
5. **Stores the token as a Secret in ArgoCD**

### Why You Didn't See It

You ran **ONE command**:
```bash
argocd cluster add kind-argocd-cluster --name argocd-cluster --insecure
```

But this command **internally executed multiple kubectl commands** for you!

### Verify It Was Created

Check if the ServiceAccount exists:

```bash
# Check ServiceAccount
kubectl get serviceaccount argocd-manager -n kube-system

# Check ClusterRole
kubectl get clusterrole argocd-manager-role

# Check ClusterRoleBinding
kubectl get clusterrolebinding argocd-manager-role-binding
```

If you see these resources, it means `argocd cluster add` created them automatically! ✅

### Analogy

Think of it like:

**What you did:**
```bash
argocd cluster add kind-argocd-cluster
```

**What ArgoCD did behind the scenes:**
```bash
kubectl create serviceaccount argocd-manager -n kube-system
kubectl create clusterrole argocd-manager-role ...
kubectl create clusterrolebinding argocd-manager-role-binding ...
kubectl get secret <token> ...
argocd store token ...
```

It's like ordering a pizza online - you click "Order", but the system automatically:
1. Processes payment
2. Sends order to kitchen
3. Assigns delivery driver
4. Sends you tracking info

You only ran one command, but ArgoCD did all the work! 🎯

---

## kind-argocd-cluster vs argocd-cluster

### Question:
"What is the difference between kind-argocd-cluster and argocd-cluster?"

### Answer:

In the command `argocd cluster add kind-argocd-cluster --name argocd-cluster --insecure`:

### `kind-argocd-cluster`
- **What it is:** kubectl context name (from your kubeconfig file)
- **Where it comes from:** Created when you ran `kind create cluster --name argocd-cluster`
- **Purpose:** Tells ArgoCD which cluster to connect to
- **Format:** `kind-<cluster-name>` (Kind automatically prefixes with "kind-")

### `argocd-cluster`
- **What it is:** Friendly display name for ArgoCD UI
- **Where it comes from:** You chose this name with `--name` flag
- **Purpose:** What you see in ArgoCD UI/CLI
- **Format:** Whatever you want to call it

### Breakdown:

```bash
argocd cluster add kind-argocd-cluster --name argocd-cluster --insecure
                   ↑                          ↑
                   |                          |
            kubectl context name      ArgoCD display name
         (must match kubeconfig)      (your choice)
```

### Example:

When you created the Kind cluster:
```bash
kind create cluster --name argocd-cluster
```

Kind automatically created kubectl context: `kind-argocd-cluster`

Then when adding to ArgoCD, you reference that context and give it a display name:
```bash
argocd cluster add kind-argocd-cluster --name argocd-cluster
```

### In `argocd cluster list`:

```
SERVER                      NAME              
https://192.168.2.59:33893  argocd-cluster  ← This is the display name you chose
```

**Summary:** `kind-argocd-cluster` = technical kubectl context name, `argocd-cluster` = friendly display name you chose for ArgoCD 🎯

---

## Why ServiceAccount in kube-system Namespace?

### Question:
"And why is service account and clusterrole and clusterrolebinding inside kube-system namespace?"

### Answer:

Let me clarify:

### ServiceAccount Location

**ServiceAccount** is in `kube-system` namespace:
```bash
kubectl get serviceaccount argocd-manager -n kube-system
```

**Why `kube-system`?**
- Convention for **cluster-level management tools**
- `kube-system` is where Kubernetes system components live
- ArgoCD is managing the **entire cluster**, not just one namespace
- Keeps it separate from application namespaces

### ClusterRole & ClusterRoleBinding Location

**Important:** ClusterRole and ClusterRoleBinding are **NOT in any namespace**!

They are **cluster-scoped resources** (not namespaced):

```bash
# These DON'T have -n flag because they're cluster-wide
kubectl get clusterrole argocd-manager-role
kubectl get clusterrolebinding argocd-manager-role-binding
```

### Namespace vs Cluster-Scoped

#### Namespaced Resources (need `-n namespace`):
- Pods
- Services
- Deployments
- **ServiceAccounts** ← Lives in a namespace

#### Cluster-Scoped Resources (no namespace):
- Nodes
- Namespaces themselves
- **ClusterRoles** ← Cluster-wide
- **ClusterRoleBindings** ← Cluster-wide
- PersistentVolumes

### Visual Representation

```
Cluster Level (No Namespace)
├── ClusterRole: argocd-manager-role
└── ClusterRoleBinding: argocd-manager-role-binding

kube-system Namespace
└── ServiceAccount: argocd-manager

argocd Namespace
├── ArgoCD pods
└── ArgoCD services

default Namespace
└── Your application pods
```

### Why This Design?

**ServiceAccount in `kube-system`:**
- Signals it's a system/management account
- Separates it from application accounts
- Standard practice for cluster management tools

**ClusterRole/ClusterRoleBinding cluster-scoped:**
- They define **cluster-wide permissions**
- Not limited to one namespace
- Can grant access to resources across all namespaces

### Check It Yourself

```bash
# ServiceAccount (namespaced) - needs -n
kubectl get serviceaccount argocd-manager -n kube-system ✅

# ClusterRole (cluster-scoped) - no -n
kubectl get clusterrole argocd-manager-role ✅

# ClusterRoleBinding (cluster-scoped) - no -n
kubectl get clusterrolebinding argocd-manager-role-binding ✅
```

**Summary:** ServiceAccount is in `kube-system` namespace, but ClusterRole and ClusterRoleBinding exist at the cluster level (no namespace)! 🎯

---

## Quick Reference Commands

### Cluster & Node Information
```bash
# View cluster info
kubectl cluster-info

# List all nodes
kubectl get nodes

# List all contexts
kubectl config get-contexts

# Switch context
kubectl config use-context kind-argocd-cluster
```

### Pod Information
```bash
# List pods in specific namespace
kubectl get pods -n argocd

# List all pods in all namespaces
kubectl get pods -A

# See which node each pod is on
kubectl get pods -n argocd -o wide

# Pods on specific node
kubectl get pods -A --field-selector spec.nodeName=argocd-cluster-worker
```

### Namespace Information
```bash
# List all namespaces
kubectl get namespaces

# See all resources in a namespace
kubectl get all -n argocd

# See all resources in all namespaces
kubectl get all -A
```

### RBAC Information
```bash
# List ServiceAccounts
kubectl get serviceaccounts -A

# List ClusterRoles
kubectl get clusterroles

# List ClusterRoleBindings
kubectl get clusterrolebindings

# Check specific ServiceAccount
kubectl get serviceaccount argocd-manager -n kube-system
```

### ArgoCD Commands
```bash
# List clusters in ArgoCD
argocd cluster list

# List applications
argocd app list

# Get application details
argocd app get <app-name>
```

---

## Key Takeaways

1. **ArgoCD runs as pods inside your Kubernetes cluster** - not as a separate cluster
2. **Nodes are machines, pods are applications** - pods run inside nodes
3. **Control-plane manages, workers execute** - clear separation of responsibilities
4. **Pod distribution is intentional** - for high availability and load balancing
5. **Multiple namespaces are normal** - they provide organization and isolation
6. **One cluster, two addresses** - internal (kubernetes.default.svc) and external (IP:PORT)
7. **argocd cluster add creates resources automatically** - ServiceAccount, ClusterRole, ClusterRoleBinding
8. **ClusterRole and ClusterRoleBinding are cluster-scoped** - not in any namespace
9. **ServiceAccount in kube-system** - convention for cluster management tools

---

**Created:** 2024
**Purpose:** Clarification of common doubts during ArgoCD learning
**Author:** Learning Session Notes
