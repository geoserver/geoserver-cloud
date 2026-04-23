# Prerequisites

This page walks you through the one-time setup needed to run GeoServer Cloud in a **local** Kubernetes cluster. If you already have a cluster with an ingress controller and a DNS alias for `gscloud.localhost`, you can skip ahead to the [Helm example](./helm/example-pgconfig.md).

This guide presents two cluster choices:

- **[kind](https://kind.sigs.k8s.io/)** — cross-platform (macOS, Linux, Windows/WSL2). Default if you're on Linux.
- **[OrbStack](https://orbstack.dev/)** — macOS only, noticeably faster on Apple Silicon. Ships an integrated Kubernetes cluster. Recommended on macOS.

A third option, [k3d](https://k3d.io/), is available as a collapsed section at the bottom of this page.

kind runs Kubernetes inside Docker containers, so if you choose kind you also need a Docker runtime: [Docker Desktop](https://www.docker.com/products/docker-desktop/) on macOS or Windows (with WSL2 integration enabled on Windows), or the [Docker Engine](https://docs.docker.com/engine/install/) on Linux. OrbStack bundles its own Docker runtime.

## 1. Install tooling

You always need `kubectl` and `helm` (v3 or later). Pick the tab that matches your platform and cluster choice.

=== "macOS + kind"

    ```bash
    brew install kubectl helm kind
    ```

=== "macOS + OrbStack"

    ```bash
    brew install kubectl helm
    brew install --cask orbstack
    ```

    Open OrbStack and enable Kubernetes in **Settings → Kubernetes**. OrbStack adds an `orbstack` context to your kubeconfig automatically.

=== "Linux + kind"

    ```bash
    # kubectl
    curl -LO "https://dl.k8s.io/release/$(curl -L -s https://dl.k8s.io/release/stable.txt)/bin/linux/amd64/kubectl"
    sudo install -o root -g root -m 0755 kubectl /usr/local/bin/kubectl

    # helm
    curl https://raw.githubusercontent.com/helm/helm/main/scripts/get-helm-3 | bash

    # kind
    [ "$(uname -m)" = x86_64 ] && curl -Lo ./kind https://kind.sigs.k8s.io/dl/v0.31.0/kind-linux-amd64
    [ "$(uname -m)" = aarch64 ] && curl -Lo ./kind https://kind.sigs.k8s.io/dl/v0.31.0/kind-linux-arm64
    chmod +x ./kind && sudo mv ./kind /usr/local/bin/kind
    ```

Verify:

```bash
kubectl version --client
helm version
```

## 2. Create a local cluster

=== "kind"

    kind needs a small config so the control-plane node can host an ingress controller and expose ports 80 and 443 on the host.

    Save this as `kind-config.yaml`:

    ```yaml
    kind: Cluster
    apiVersion: kind.x-k8s.io/v1alpha4
    nodes:
      - role: control-plane
        kubeadmConfigPatches:
          - |
            kind: InitConfiguration
            nodeRegistration:
              kubeletExtraArgs:
                node-labels: "ingress-ready=true"
        extraPortMappings:
          - containerPort: 80
            hostPort: 80
            protocol: TCP
          - containerPort: 443
            hostPort: 443
            protocol: TCP
    ```

    Create the cluster:

    ```bash
    kind create cluster --name gscloud --config kind-config.yaml
    ```

    Expected: kind prints progress for a minute or so, then `kubectl cluster-info --context kind-gscloud` shows a control plane URL.

=== "OrbStack"

    OrbStack creates and runs the cluster when you enable Kubernetes in its settings (done in Section 1 above). Select the context and confirm:

    ```bash
    kubectl config use-context orbstack
    kubectl get nodes
    ```

    Expected: one node in `Ready` status.

## 3. Install the nginx ingress controller

Neither kind nor OrbStack ships an ingress controller by default — install nginx-ingress.

=== "kind"

    ```bash
    kubectl apply -f https://raw.githubusercontent.com/kubernetes/ingress-nginx/main/deploy/static/provider/kind/deploy.yaml

    kubectl wait --namespace ingress-nginx \
      --for=condition=ready pod \
      --selector=app.kubernetes.io/component=controller \
      --timeout=90s

    kubectl patch ingressclass nginx \
      -p '{"metadata":{"annotations":{"ingressclass.kubernetes.io/is-default-class":"true"}}}'
    ```

    Expected: the final `patch` command prints `ingressclass.networking.k8s.io/nginx patched`.

=== "OrbStack"

    Use the **cloud** provider manifest — OrbStack binds the resulting `LoadBalancer` service to `127.0.0.1:80` on the host automatically, so the existing `/etc/hosts` entry in Section 4 works unchanged.

    ```bash
    kubectl apply -f https://raw.githubusercontent.com/kubernetes/ingress-nginx/main/deploy/static/provider/cloud/deploy.yaml

    kubectl wait --namespace ingress-nginx \
      --for=condition=ready pod \
      --selector=app.kubernetes.io/component=controller \
      --timeout=90s

    kubectl patch ingressclass nginx \
      -p '{"metadata":{"annotations":{"ingressclass.kubernetes.io/is-default-class":"true"}}}'
    ```

    Expected: the final `patch` command prints `ingressclass.networking.k8s.io/nginx patched`.

## 4. Add a DNS alias for `gscloud.localhost`

The Helm example exposes GeoServer Cloud via an Ingress on the host `gscloud.localhost`. On most modern systems this already resolves to `127.0.0.1` automatically (RFC 6761) — test with:

```bash
curl -sI -m 2 http://gscloud.localhost/ 2>&1 | head -1
```

If you see an HTTP status line (e.g. `HTTP/1.1 404`) the name resolves fine and you can skip the rest of this section. If you see `Could not resolve host`, add a line to `/etc/hosts`:

```bash
echo "127.0.0.1 gscloud.localhost" | sudo tee -a /etc/hosts
```

Verify:

```bash
getent hosts gscloud.localhost 2>/dev/null || grep gscloud.localhost /etc/hosts
```

## 5. Tear down when you're done

=== "kind"

    ```bash
    kind delete cluster --name gscloud
    ```

    This removes everything — cluster, ingress controller, any workloads.

=== "OrbStack"

    Disable Kubernetes from **Settings → Kubernetes** (toggle it off). This stops the cluster but keeps images cached for a faster next start. To wipe it entirely, use the "Reset" button in the same panel.

The `/etc/hosts` entry is harmless to leave in place.

---

## Alternative: k3d

??? note "Use k3d instead of kind or OrbStack"

    [k3d](https://k3d.io/) runs [k3s](https://k3s.io/) (a lightweight Kubernetes distribution) inside Docker. It ships its own ingress (Traefik) and is a fine alternative.

    ```bash
    # Install k3d
    wget -q -O - https://raw.githubusercontent.com/k3d-io/k3d/main/install.sh | bash

    # Create a cluster and bind host ports 80/443
    k3d cluster create gscloud -p "80:80@loadbalancer" -p "443:443@loadbalancer"
    ```

    The rest of the example works unchanged: k3d's built-in Traefik will pick up the Ingress resource that the Helm chart creates. You can skip Section 3 on this page.

    Tear down:

    ```bash
    k3d cluster delete gscloud
    ```

---

**Next:** head over to the [Helm example](./helm/example-pgconfig.md) to install GeoServer Cloud on your new cluster.
