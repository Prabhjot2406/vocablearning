# ArgoCD & Kubernetes Concepts - Quick Reference Guide

This guide covers all the key concepts discussed during ArgoCD setup and configuration.

---

## Table of Contents
1. [Jenkins vs ArgoCD](#jenkins-vs-argocd)
2. [ArgoCD Architecture](#argocd-architecture)
3. [ArgoCD Pods Explained](#argocd-pods-explained)
4. [ArgoCD CLI](#argocd-cli)
5. [Pod Distribution Across Nodes](#pod-distribution-across-nodes)
6. [Kubernetes RBAC](#kubernetes-rbac)
7. [Cluster Addressing](#cluster-addressing)
8. [Installation Commands](#installation-commands)

---

## Jenkins vs ArgoCD

### Jenkins (Traditional CI/CD)
- Runs as a **standalone application** (like a regular server process)
- Can be installed directly on a VM/EC2 instance
- Doesn't require Kubernetes to function
- Typically run as: `java -jar jenkins.war` or as a system service

### ArgoCD (Kubernetes-Native GitOps)
- Is a **Kubernetes-native application**
- Designed to run **inside** Kubernetes as pods
- Its purpose is to **manage Kubernetes deployments**
- Cannot run standalone - it needs Kubernetes to exist

### Key Difference
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

**You're NOT creating a cluster FOR ArgoCD** - ArgoCD runs **inside** your cluster to manage deployments.

---

## ArgoCD Architecture

### What You Created
1. **Kind Cluster** = Your target environment where apps will be deployed
2. **ArgoCD Pods** = Run inside this cluster to manage deployments

kind create cluster --name argocd-cluster --config kind-config.yaml


### Deployment Flow
```
With Jenkins (Traditional):
Jenkins builds → pushes to server → manual deployment

With ArgoCD + Kubernetes:
ArgoCD watches Git repo → automatically deploys to Kubernetes
Kubernetes handles scaling, restarts, load balancing
GitOps: Your Git repo is the source of truth
```

---

## ArgoCD Pods Explained

When you install ArgoCD, these pods are created:

# 1. Create namespace
kubectl create namespace argocd

# 2. Install ArgoCD
kubectl apply -n argocd -f https://raw.githubusercontent.com/argoproj/argo-cd/stable/manifests/install.yaml

# 3. Wait for pods to be ready
kubectl wait --for=condition=ready pod -l app.kubernetes.io/name=argocd-server -n argocd --timeout=300s

# 4. Get admin password
kubectl -n argocd get secret argocd-initial-admin-secret -o jsonpath="{.data.password}" | base64 -d && echo

command to check on which pod my argoCD is running?

temp@MacBookAir vocablearning % kubectl get pods -n argocd -o wide

NAME                                               READY   STATUS    RESTARTS       AGE    IP           NODE                     NOMINATED NODE   READINESS GATES
argocd-application-controller-0                    1/1     Running   0              2d1h   10.244.1.4   argocd-cluster-worker2   <none>           <none>
argocd-applicationset-controller-bbff79c6f-dhcmn   1/1     Running   0              2d1h   10.244.1.2   argocd-cluster-worker2   <none>           <none>
argocd-dex-server-6877ddf4f8-2k8bv                 1/1     Running   2 (2d1h ago)   2d1h   10.244.2.3   argocd-cluster-worker    <none>           <none>
argocd-notifications-controller-7b5658fc47-lhn66   1/1     Running   0              2d1h   10.244.2.5   argocd-cluster-worker    <none>           <none>
argocd-redis-7d948674-r7tkv                        1/1     Running   0              2d1h   10.244.2.4   argocd-cluster-worker    <none>           <none>
argocd-repo-server-7679dc55f5-97tw5                1/1     Running   0              2d1h   10.244.1.3   argocd-cluster-worker2   <none>           <none>
argocd-server-7d769b6f48-7xntf                     1/1     Running   0              2d1h   10.244.1.5   argocd-cluster-worker2   <none>           <none>

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

### Simple Flow
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

## ArgoCD CLI

### What is it?
- Command-line tool to interact with ArgoCD
- Alternative to using the Web UI
- Like `kubectl` is for Kubernetes, `argocd` is for ArgoCD

### Installation (macOS)

**Option 1: Using Homebrew**
```bash
brew install argocd
```

**Option 2: Direct Download**
```bash
# Download binary
curl -sSL -o argocd https://github.com/argoproj/argo-cd/releases/latest/download/argocd-darwin-amd64

# Make executable
chmod +x argocd

# Move to PATH
sudo mv argocd /usr/local/bin/argocd

# Verify
argocd version
```

### Common CLI Commands

```bash
# Login to ArgoCD
argocd login <ARGOCD_SERVER> --username admin --password <PASSWORD>

# Create an application
argocd app create myapp \
  --repo https://github.com/user/repo \
  --path ./k8s \
  --dest-server https://kubernetes.default.svc \
  --dest-namespace default

# Sync an application
argocd app sync myapp

# Get application status
argocd app get myapp

# List all applications
argocd app list

# List clusters
argocd cluster list
```

### Do You Need It?
- **Optional** - You can do everything via Web UI
- **Recommended** - Faster for automation and scripting

---

## Pod Distribution Across Nodes

### Why Pods Run on Different Nodes

This is **completely normal and intentional**:

#### 1. High Availability (HA) 🛡️
- If one worker node crashes, ArgoCD still works
- Pods on other nodes keep running
- No single point of failure

#### 2. Load Balancing ⚖️
- Distributes CPU/memory usage across nodes
- Prevents one node from being overloaded
- Better resource utilization

#### 3. Kubernetes Scheduler 🤖
- Automatically decides pod placement
- Considers:
  - Available resources (CPU, memory)
  - Node health
  - Pod anti-affinity rules
  - Even distribution

### Example Distribution
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

### Check Pod Distribution
```bash
# See which node each pod is running on
kubectl get pods -n argocd -o wide

# Show pod name and node name only
kubectl get pods -n argocd -o custom-columns=POD:metadata.name,NODE:spec.nodeName

# All pods on a specific node
kubectl get pods -n argocd --field-selector spec.nodeName=argocd-cluster-worker
```

---

## Kubernetes RBAC (Role-Based Access Control)

### The Three Components

Think of it like a company security system:

#### 1. ServiceAccount = Employee ID Badge 🪪

**What it is:**
- An identity for pods/applications running in Kubernetes
- Like a username, but for applications (not humans)

**What it does:**
- Identifies "who" is making requests to Kubernetes API
- Every pod runs with a ServiceAccount (default if not specified)

**Example:**
```yaml
apiVersion: v1
kind: ServiceAccount
metadata:
  name: argocd-manager
  namespace: kube-system
```

#### 2. ClusterRole = Job Description / Permission List 📋

**What it is:**
- Defines **WHAT** actions are allowed
- A list of permissions (verbs + resources)

**What it does:**
- Specifies which operations can be performed on which resources
- Cluster-wide permissions (not limited to one namespace)

**Example:**
```yaml
apiVersion: rbac.authorization.k8s.io/v1
kind: ClusterRole
metadata:
  name: argocd-manager-role
rules:
- apiGroups: ["*"]
  resources: ["deployments", "services", "pods"]
  verbs: ["get", "list", "create", "update", "delete"]
```

**Permissions breakdown:**
- **apiGroups**: Which API groups (apps, core, etc.)
- **resources**: Which Kubernetes objects (pods, services, etc.)
- **verbs**: Which actions (get, create, delete, etc.)

#### 3. ClusterRoleBinding = Assignment Letter 🔗

**What it is:**
- Links a ServiceAccount to a ClusterRole
- Says "**WHO** gets **WHAT** permissions"

**Example:**
```yaml
apiVersion: rbac.authorization.k8s.io/v1
kind: ClusterRoleBinding
metadata:
  name: argocd-manager-role-binding
roleRef:
  apiGroup: rbac.authorization.k8s.io
  kind: ClusterRole
  name: argocd-manager-role        # ← The permissions
subjects:
- kind: ServiceAccount
  name: argocd-manager              # ← The identity
  namespace: kube-system
```

### How They Work Together

```
┌─────────────────────┐
│  ServiceAccount     │  "WHO am I?"
│  argocd-manager     │  (Identity)
└──────────┬──────────┘
           │
           │ linked by
           │
┌──────────▼──────────┐
│ ClusterRoleBinding  │  "WHO gets WHAT?"
│                     │  (Assignment)
└──────────┬──────────┘
           │
           │ grants
           │
┌──────────▼──────────┐
│   ClusterRole       │  "WHAT can I do?"
│ - create pods       │  (Permissions)
│ - delete services   │
│ - list deployments  │
└─────────────────────┘
```

### Complete Flow Example

and what was the command using which I have created a service account?
argocd cluster add kind-argocd-cluster --name argocd-cluster --insecure

When ArgoCD tries to create a deployment:

1. **ArgoCD pod** makes API request: "Create deployment"
2. **Kubernetes API** asks: "Who are you?"
3. **ServiceAccount** responds: "I'm argocd-manager"
4. **Kubernetes** checks: "What can argocd-manager do?"
5. **ClusterRoleBinding** says: "argocd-manager has argocd-manager-role"
6. **ClusterRole** says: "argocd-manager-role can create deployments"
7. **Kubernetes** responds: "✅ Permission granted, deployment created"

### The `argocd-manager` ServiceAccount

#### Without `argocd cluster add`:
- ArgoCD uses `argocd-application-controller` ServiceAccount
- Has permissions within the cluster
- Works fine for single-cluster setups

#### With `argocd cluster add`:
- Creates dedicated `argocd-manager` ServiceAccount
- More explicit permission management
- Better for production/security best practices

#### What Gets Created:
```bash
# ServiceAccount
kubectl get serviceaccount argocd-manager -n kube-system

# ClusterRole
kubectl get clusterrole argocd-manager-role

# ClusterRoleBinding
kubectl get clusterrolebinding argocd-manager-role-binding

# Secret (token)
kubectl get secrets -n argocd | grep cluster
```

#### Why Use It?

**Security Benefits:**
1. **Isolation** - Separate permissions for deployment vs ArgoCD operations
2. **Auditability** - Track what argocd-manager did separately
3. **Revocability** - Remove access without breaking ArgoCD itself
4. **Multi-Cluster** - Each cluster has its own argocd-manager

**Comparison:**
```
Without argocd-manager:
ArgoCD = Building Manager with MASTER KEY (risky)

With argocd-manager:
ArgoCD = Building Manager with SPECIFIC KEY for maintenance (secure)
```

### Useful Commands

```bash
# See all ServiceAccounts
kubectl get serviceaccounts -A

# See all ClusterRoles
kubectl get clusterroles

# See all ClusterRoleBindings
kubectl get clusterrolebindings

# Check what permissions a ServiceAccount has
kubectl auth can-i --list --as=system:serviceaccount:kube-system:argocd-manager
```

---

## Cluster Addressing

### Two Ways to Access the Same Cluster

When you run `argocd cluster list`, you might see:

```
SERVER                          NAME            VERSION  STATUS
https://192.168.2.59:33893      argocd-cluster           Unknown
https://kubernetes.default.svc  in-cluster               Unknown
```

**These are NOT 2 different clusters!** They're 2 addresses for the SAME cluster.

### 1. Internal Address: `https://kubernetes.default.svc`

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

### 2. External Address: `https://192.168.2.59:33893`

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

### Visual Representation

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

When you ran `argocd cluster add kind-argocd-cluster`, ArgoCD:
1. Read your kubeconfig
2. Found the external address: `192.168.2.59:33893`
3. Created the `argocd-manager` ServiceAccount
4. Added it as a "cluster" entry

But ArgoCD **already had** the internal address (`kubernetes.default.svc`) by default.

### Which One to Use?

**For deploying apps to the same cluster:**
Use `https://kubernetes.default.svc` ✅

**Why:**
- Faster (no external network hop)
- More reliable (internal DNS)
- Standard practice for in-cluster deployments

**Example:**
```yaml
destination:
  server: https://kubernetes.default.svc  # ← Use this
  namespace: default
```

### How External Address Gets Set

In your `kind-config.yaml`:
```yaml
networking:
  apiServerAddress: "192.168.2.59"  # ← This creates external address
  apiServerPort: 33893
```

**Without this:** Kind would use `127.0.0.1:RANDOM_PORT` (localhost only)

**With this:** Kind uses your specified IP, making it accessible from other machines

---

## Installation Commands

### 1. Create Kind Cluster

```bash
# Create cluster with config
kind create cluster --name argocd-cluster --config kind-config.yaml

# Verify cluster
kubectl cluster-info
kubectl get nodes
```

### 2. Install ArgoCD

```bash
# Create namespace
kubectl create namespace argocd

# Install ArgoCD
kubectl apply -n argocd -f https://raw.githubusercontent.com/argoproj/argo-cd/stable/manifests/install.yaml

# Wait for pods to be ready
kubectl wait --for=condition=ready pod -l app.kubernetes.io/name=argocd-server -n argocd --timeout=300s

# Verify installation
kubectl get pods -n argocd
kubectl get svc -n argocd
```

### 3. Access ArgoCD

```bash
# Get initial admin password
kubectl -n argocd get secret argocd-initial-admin-secret -o jsonpath="{.data.password}" | base64 -d && echo

# Port forward (for local access)
kubectl port-forward svc/argocd-server -n argocd 8080:443

# OR change to NodePort (for external access)
kubectl patch svc argocd-server -n argocd -p '{"spec": {"type": "NodePort"}}'
kubectl get svc argocd-server -n argocd
```

**Access UI:**
- Username: `admin`
- Password: (from get secret command)
- URL: `https://localhost:8080` or `https://<node-ip>:<nodeport>`

### 4. Install ArgoCD CLI (Optional)

```bash
# macOS with Homebrew
brew install argocd

# OR direct download
curl -sSL -o argocd https://github.com/argoproj/argo-cd/releases/latest/download/argocd-darwin-amd64
chmod +x argocd
sudo mv argocd /usr/local/bin/argocd
```

### 5. Add Cluster to ArgoCD (Optional)

```bash
# Login first
argocd login <ARGOCD_SERVER> --username admin --password <PASSWORD>

# Add cluster (creates argocd-manager ServiceAccount)
argocd cluster add kind-argocd-cluster --name argocd-cluster --insecure

# List clusters
argocd cluster list
```

### 6. Connect GitHub Repository

**Via UI:**
1. Settings → Repositories → Connect Repo
2. Enter GitHub URL
3. Choose connection method (HTTPS/SSH)

**Via CLI:**
```bash
argocd repo add https://github.com/username/repo.git
```

### 7. Create Application

**Via UI:**
1. Click "+ NEW APP"
2. Fill in details (name, repo, path, destination)
3. Click CREATE

**Via CLI:**
```bash
argocd app create my-app \
  --repo https://github.com/username/repo.git \
  --path k8s \
  --dest-server https://kubernetes.default.svc \
  --dest-namespace default \
  --sync-policy automated
```

**Via YAML:**
```bash
kubectl apply -f - <<EOF
apiVersion: argoproj.io/v1alpha1
kind: Application
metadata:
  name: my-app
  namespace: argocd
spec:
  project: default
  source:
    repoURL: https://github.com/username/repo.git
    targetRevision: HEAD
    path: k8s
  destination:
    server: https://kubernetes.default.svc
    namespace: default
  syncPolicy:
    automated:
      prune: true
      selfHeal: true
EOF
```

---

## Useful Commands Reference

### Cluster Management
```bash
# List all Kind clusters
kind get clusters

# Switch kubectl context
kubectl config use-context kind-argocd-cluster

# View current context
kubectl config current-context

# List all contexts
kubectl config get-contexts

# Delete a cluster
kind delete cluster --name my-cluster
```

### ArgoCD Operations
```bash
# Check ArgoCD pods
kubectl get pods -n argocd -o wide

# Check ArgoCD services
kubectl get svc -n argocd

# View ArgoCD logs
kubectl logs -n argocd deployment/argocd-server

# Restart ArgoCD server
kubectl rollout restart deployment argocd-server -n argocd
```

### RBAC Inspection
```bash
# List ServiceAccounts
kubectl get serviceaccounts -A

# Check specific ServiceAccount
kubectl get serviceaccount argocd-manager -n kube-system

# List ClusterRoles
kubectl get clusterroles | grep argocd

# List ClusterRoleBindings
kubectl get clusterrolebindings | grep argocd

# Check permissions
kubectl auth can-i --list --as=system:serviceaccount:kube-system:argocd-manager
```

---

## Summary

### Key Takeaways

1. **ArgoCD runs inside Kubernetes** - It's not a standalone tool like Jenkins
2. **Multiple pods work together** - Each has a specific role (UI, controller, repo server, etc.)
3. **Pods are distributed across nodes** - This is intentional for high availability
4. **RBAC controls access** - ServiceAccount (WHO) + ClusterRole (WHAT) + ClusterRoleBinding (WHO gets WHAT)
5. **Two addresses, one cluster** - Internal (kubernetes.default.svc) and external (IP:PORT)
6. **Use internal address for deployments** - Faster and more reliable for in-cluster apps
7. **argocd-manager is optional** - But recommended for better security practices

### Best Practices

- ✅ Use `https://kubernetes.default.svc` for in-cluster deployments
- ✅ Create `argocd-manager` ServiceAccount for production
- ✅ Use automated sync policies for GitOps
- ✅ Keep ArgoCD and apps in separate namespaces
- ✅ Use Git as the single source of truth
- ✅ Monitor ArgoCD notifications for deployment status

---

**Created:** 2024
**Last Updated:** 2024
**Author:** Learning Notes from ArgoCD Setup Session
