# Nexis — Production-Grade Distributed Real-Time Collaborative IDE Platform

Nexis is an enterprise-grade, microservices-based real-time collaborative development engine designed to handle concurrent multi-user workspace editing and secure sandboxed code execution. Architected to mimic a developer-focused Google Docs, this platform demonstrates production engineering patterns capable of scaling horizontally across distributed infrastructures.

---

## 🏗️ Core Architectural Topology & Service Landscape

The platform is engineered around a 6-service microservices architecture where each service owns its data tier independently to prevent tight coupling.

| Service Name | Port | Primary Core Responsibilities | Technology Runtime Stack | Local Status |
| :--- | :--- | :--- | :--- | :--- |
| **API Gateway** | `8080` | Unified ingress edge routing, token validation, rate-limiting, and circuit breaking. | Spring Cloud Gateway, WebFlux, Redis. | 🟢 Functional |
| **Auth Service** | `8081` | Identity provisioning, stateless token lifecycle tracking, and RBAC security filters. | Spring Boot 3.x, Security, PostgreSQL, Redis. | 🟢 Functional |
| **WebSocket Service** | `8082` | Low-latency state sync, OT message validation, and connection status broadcasting. | Spring WebSocket (STOMP), Redis, RabbitMQ. | 🟢 Functional |
| **Execution Service** | `8083` | Secure sandboxed code compilation and event-driven task queue routing. | Spring Boot, Docker Engine SDK, RabbitMQ. | 🟢 Functional |
| **Storage Service** | `8084` | Workspace binary file tiering, archival version mapping, and pre-signed content delivery. | Spring Boot, MinIO Object Storage, PostgreSQL. | 🟡 In Progress |
| **Recording Service** | `8085` | Stream logging capture, user event auditing, and chronological playback time-series rendering. | Spring Boot, PostgreSQL (JSONB tracking), RabbitMQ. | 🔴 Planned |

---

## 🛠️ Deep Technical Implementation Profiles

### 1. API Gateway (Port 8080) — Reactive Routing & Perimeter Edge Hardening
* **Stateless Token Slicing:** Built as a Spring WebFlux application, a global `JwtAuthenticationFilter` intercepts incoming HTTP vectors to parse and validate signatures on the fly without database I/O overhead. Extracted identity metrics (`X-User-Id`, `X-User-Role`) are mutated straight into downstream request headers.
* **Distributed Rate Limiting:** Implements an asynchronous Redis-backed Token Bucket algorithm tracking real-time traffic spikes capped rigidly at a baseline of **100 req/sec** with a max capacity ceiling of **200** burst entries.
* **Fault Isolation Topologies:** Resilience4j Circuit Breakers encircle every localized downstream service route with custom sliding-window threshold constraints, gracefully failing requests into fallback handlers during node blackouts.

### 2. Auth Service (Port 8081) — Enterprise Identity Strategy
* **Cryptographic Signatures:** Issues dual stateless token matrices containing a 15-minute cryptographically signed Access Token alongside a high-entropy 7-day storage-backed Refresh Token encrypted through the HS512 engine.
* **Malicious Footprint Neutralization:** Employs a real-time Redis-backed token blacklist lifecycle rule that actively drops stolen or logged-out token vectors during incoming handshakes. Global exception handlers mitigate user-enumeration threat profiles by obscuring system exceptions and database stack leaks.

### 3. WebSocket Service (Port 8082) — Distributed State Engine
* **Raw TCP Perimeter Verification:** Bypasses layer-7 proxy restrictions by establishing a custom STOMP `ChannelInterceptor` that actively hooks raw TCP handshakes to validate access tokens before socket session upgrades are finalized.
* **Conflict-Free Convergence Mechanics:** Resolves multi-user concurrent coordinate editing over a custom Operational Transformation (OT) engine designed to transform `INSERT`, `DELETE`, and `RETAIN` actions into index-corrected operations without document forks.
* **Scale-Out Event Pipelines:** Combines low-latency Redis Pub/Sub backbones for ephemeral coordinate state transmissions (typing indicators, live mouse cursors) with durable asynchronous RabbitMQ exchanges mapping persistent edit snapshots. Redisson distributed locking blocks race conditions across multi-node horizontal instances.

### 4. Execution Service (Port 8083) — Isolated Code Sandbox Architecture
* **Decoupled Job Queue Orchestration:** Submission tasks escape the main request path by getting pushed into a dedicated RabbitMQ event bus (`QUEUED` -> `PROCESSING`) where decoupled engine workers process incoming execution instructions.
* **Zero-Trust Hardened Container Isolation:** Hooks straight into the Docker Engine Java SDK to dynamically construct scratch runtime images (Python, Node.js, OpenJDK) with absolute restrictions enforced:
    * **Resource Ceilings:** Strictly bounded at a maximum of **128MB** RAM allocation and **50%** CPU core runtime limitations.
    * **Network Blockades:** Total networking teardown is mapped inside the runtime engine to eliminate outbound connection routing threats.
    * **System Read-Only Locks:** Read-only root configurations drop file alterations outside of target `/tmp` staging boundaries.
    * **Host Defenses:** Zero-privileged namespaces execute runs using isolated, non-root user groups. Containers are instantly context-destroyed via daemon termination limits after a strict 30-second processing window passes.

### 5. Storage Service (Port 8084) — S3 Object Infrastructure
* **Multi-Tenant Chunking Layer:** Implements isolated S3 Object Storage bucket spaces split securely per tenant workspace boundary utilizing a MinIO integration cluster.
* **Ephemeral URL Strategy:** Generates stateless binary access pathways through cryptographic pre-signed URLs capped at a tight 15-minute Time-To-Live (TTL) security window.
* **Historical Matrix Archival Schema:** Employs relational version snapshot models using isolated table separations (`files` and `file_versions` frames mapped inside a localized PostgreSQL instance) enabling seamless rollback operations.

---

## 🗄️ Database Design Topology

Nexis operates a strictly isolated database-per-service pattern. All transactional persistence points rely on PostgreSQL 15 engines configured with UUID primary keys to guarantee absolute uniqueness across horizontally scaled distributed nodes.

### Auth Service Relational Model (`auth_db`)

```sql
-- 1. Users Table (Maps to UserEntity)
CREATE TABLE users (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    email VARCHAR(255) NOT NULL,
    password VARCHAR(255),
    fullname VARCHAR(255),
    avatar VARCHAR(255),
    provider_id VARCHAR(255),
    provider_type VARCHAR(50), -- Enum string tracking identity provider (Google, GitHub)
    created_at TIMESTAMP WITHOUT TIME ZONE
);

-- 2. Workspaces Table (Maps to WorkspaceEntity)
CREATE TABLE workspaces (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    owner_id UUID NOT NULL,
    name VARCHAR(255) NOT NULL,
    description TEXT,
    visibility VARCHAR(50)
);

-- 3. Workspace Members Bridge Table (Maps to WorkspaceMemberEntity)
CREATE TABLE workspace_member_entity (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    workspace_id UUID NOT NULL REFERENCES workspaces(id) ON DELETE CASCADE,
    user_id UUID REFERENCES users(id) ON DELETE CASCADE,
    role VARCHAR(50) NOT NULL, -- Enum string (OWNER, ADMIN, MEMBER)
    joined_at TIMESTAMP WITHOUT TIME ZONE
);

-- Query Optimization Indexes
CREATE INDEX idx_users_email ON users(email);
CREATE INDEX idx_workspaces_owner ON workspaces(owner_id);
CREATE INDEX idx_workspace_members_composite ON workspace_member_entity(workspace_id, user_id);
```
### 🏷️ Architectural Design Note: The Badge Analogy

    To maintain strict decoupling and clean distributed access tracking, relationships rely on an explicit bridge entity rather than standard loose collections:

        The Building & The Person: Imagine a physical building (Workspace) and a physical person (User). To gain entry, a user requires a physical Badge (WorkspaceMemberEntity).

        Multi-Tenant Lanyards: If a single user belongs to three distinct workspaces, they do not carry one magic card that dynamically changes its data state. They carry three separate physical badges on their lanyard.

            Badge 1: Linked to You -> Linked to Workspace A (Role: OWNER)

            Badge 2: Linked to You -> Linked to Workspace B (Role: MEMBER)

            Badge 3: Linked to You -> Linked to Workspace C (Role: ADMIN)

        The Bridge: Consequently, each individual badge pointer resolves to exactly one user record and one workspace record. Without this explicit bridge entity, a workspace environment loses tracking of target identity variables, breaking zero-trust execution contexts.

### 🚀 Environment Staging & Local Setup Guide

The entire microservice system is wired to compile, optimize, and launch via multi-stage Docker configurations. The execution footprint is streamlined using alpine JRE foundations to trim final deployed file payloads from ~600MB down to a tight ~200MB target size.
Prerequisites

    Runtime Infrastructure: Docker Engine with Docker Compose installed.

    Target Operating Environment: Linux / Ubuntu Terminal.

    Development Build System: Maven wrapper configuration.

Local Infrastructure Initialization Sequence

    Clone the master repository onto your local Linux workspace framework:

Bash

git clone [https://github.com/krishnaaarwal/Nexis.git](https://github.com/krishnaaarwal/Nexis.git)
cd Nexis

    Build optimized binaries utilizing the localized project multi-stage Docker wrappers:

Bash

docker compose build

    Bring the entire cluster stack up locally over the customized bridge network configuration:

Bash

docker compose up -d

    Verify microservice connectivity health states through the API Edge Gateway routes:

Bash

curl -X GET http://localhost:8080/actuator/health