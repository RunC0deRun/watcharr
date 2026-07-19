package main

// #cgo LDFLAGS: -llog
/*
#include <jni.h>
#include <stdlib.h>

static void callStatusCallback(JNIEnv *env, jobject callback, const char *statusStr) {
    jclass cbClass = (*env)->GetObjectClass(env, callback);
    jmethodID methodId = (*env)->GetMethodID(env, cbClass, "onStatusChanged", "(Ljava/lang/String;)V");
    if (methodId != NULL) {
        jstring jStatus = (*env)->NewStringUTF(env, statusStr);
        (*env)->CallVoidMethod(env, callback, methodId, jStatus);
        (*env)->DeleteLocalRef(env, jStatus);
    }
}

static JavaVM *g_vm = NULL;
static jobject g_callback = NULL;

static void initVM(JNIEnv *env, jobject callback) {
    (*env)->GetJavaVM(env, &g_vm);
    if (g_callback != NULL) {
        (*env)->DeleteGlobalRef(env, g_callback);
    }
    g_callback = (*env)->NewGlobalRef(env, callback);
}

static void clearVM(JNIEnv *env) {
    if (g_callback != NULL) {
        (*env)->DeleteGlobalRef(env, g_callback);
        g_callback = NULL;
    }
}

static void notifyStatus(const char *statusStr) {
    if (g_vm == NULL || g_callback == NULL) return;
    JNIEnv *env = NULL;
    int attached = 0;
    int getEnvStat = (*g_vm)->GetEnv(g_vm, (void **)&env, JNI_VERSION_1_6);
    if (getEnvStat == JNI_EDETACHED) {
        #ifdef __ANDROID__
        if ((*g_vm)->AttachCurrentThread(g_vm, &env, NULL) == 0) {
            attached = 1;
        }
        #else
        if ((*g_vm)->AttachCurrentThread(g_vm, (void **)&env, NULL) == 0) {
            attached = 1;
        }
        #endif
    }
    if (env != NULL) {
        callStatusCallback(env, g_callback, statusStr);
        if (attached) {
            (*g_vm)->DetachCurrentThread(g_vm);
        }
    }
}

static const char* getStringChars(JNIEnv *env, jstring jstr) {
    if (jstr == NULL) return NULL;
    return (*env)->GetStringUTFChars(env, jstr, NULL);
}

static void releaseStringChars(JNIEnv *env, jstring jstr, const char* chars) {
    if (jstr != NULL && chars != NULL) {
        (*env)->ReleaseStringUTFChars(env, jstr, chars);
    }
}

#include <android/log.h>

static void goLog(const char* msg) {
    __android_log_write(ANDROID_LOG_DEBUG, "TsnetGo", msg);
}

static int isNull(JNIEnv *env, jobject obj) {
    return obj == NULL;
}

*/
import "C"
import (
	"context"
	"crypto/tls"
	"fmt"
	"io"
	"net"
	"net/http"
	"os"
	"strings"
	"sync"
	"sync/atomic"
	"time"
	"unsafe"

	"tailscale.com/net/netmon"
	"tailscale.com/tsnet"
	"tailscale.com/types/dnstype"
)

// logf logs a message to Android logcat via __android_log_write.
func logf(format string, args ...interface{}) {
	msg := fmt.Sprintf(format, args...)
	cMsg := C.CString(msg)
	defer C.free(unsafe.Pointer(cMsg))
	C.goLog(cMsg)
}

func init() {
	netmon.RegisterInterfaceGetter(func() ([]netmon.Interface, error) {
		return []netmon.Interface{
			{
				Interface: &net.Interface{
					Index: 1,
					MTU:   1500,
					Name:  "wlan0",
					Flags: net.FlagUp | net.FlagMulticast | net.FlagRunning,
				},
				AltAddrs: []net.Addr{
					&net.IPNet{
						IP:   net.ParseIP("192.168.1.100"),
						Mask: net.CIDRMask(24, 32),
					},
				},
			},
		}, nil
	})
}

var (
	mu            sync.Mutex
	tsServer      *tsnet.Server
	cancelFunc    context.CancelFunc
	proxyServer   *http.Server
	proxyPort     int
	activeStreams int32
	latestStatus  string
)

func setStatus(status string) {
	mu.Lock()
	latestStatus = status
	mu.Unlock()
	cStr := C.CString(status)
	defer C.free(unsafe.Pointer(cStr))
	C.notifyStatus(cStr)
}

func goString(env *C.JNIEnv, jStr C.jstring) string {
	if C.isNull(env, C.jobject(jStr)) != 0 {
		return ""
	}
	cStr := C.getStringChars(env, jStr)
	if cStr == nil {
		return ""
	}
	defer C.releaseStringChars(env, jStr, cStr)
	return C.GoString(cStr)
}

func stopServerLocked(env *C.JNIEnv) {
	if cancelFunc != nil {
		cancelFunc()
		cancelFunc = nil
	}
	if proxyServer != nil {
		proxyServer.Close()
		proxyServer = nil
	}
	if tsServer != nil {
		tsServer.Close()
		tsServer = nil
	}
	proxyPort = 0
	atomic.StoreInt32(&activeStreams, 0)
	C.clearVM(env)
}

//export Java_io_github_runc0derun_watcharr_shared_playback_TsnetManager_nativeStart
func Java_io_github_runc0derun_watcharr_shared_playback_TsnetManager_nativeStart(env *C.JNIEnv, clazz C.jclass, jAuthKey C.jstring, jStateDir C.jstring, jCallback C.jobject) C.jint {
	os.Setenv("TS_DEBUG_NO_LINUX_NETLINK", "true")
	os.Setenv("TS_DEBUG_NO_NETLINK", "true")
	os.Setenv("TS_NO_NETLINK", "true")

	mu.Lock()
	defer mu.Unlock()

	stopServerLocked(env)

	C.initVM(env, jCallback)

	authKey := strings.TrimSpace(goString(env, jAuthKey))
	stateDir := strings.TrimSpace(goString(env, jStateDir))

	os.Setenv("TS_LOGS_DIR", stateDir)
	os.Setenv("HOME", stateDir)

	tsServer = &tsnet.Server{
		AuthKey:  authKey,
		Dir:      stateDir,
		Hostname: "watcharr-player",
	}

	ctx, cancel := context.WithCancel(context.Background())
	cancelFunc = cancel

	latestStatus = "Authenticating"

	go func() {
		setStatus("Authenticating")
		if err := tsServer.Start(); err != nil {
			setStatus(fmt.Sprintf("Error: %v", err))
			return
		}

		for {
			select {
			case <-ctx.Done():
				return
			default:
				status := "Authenticating"
				if lc, err := tsServer.LocalClient(); err == nil {
					if st, err := lc.Status(ctx); err == nil {
						switch st.BackendState {
						case "Running":
							status = "Connected"
						case "NeedsLogin":
							status = "Error: Needs Login (Auth Key Expired/Invalid)"
						case "Starting":
							status = "Authenticating (Starting)"
						case "Stopped":
							status = "Error: Stopped"
						default:
							if st.BackendState != "" {
								status = fmt.Sprintf("Authenticating (%s)", st.BackendState)
							}
						}
					}
				}
				
				mu.Lock()
				currentStatus := latestStatus
				mu.Unlock()

				if status == "Connected" && currentStatus == "Dialing Stream" {
					// retain Dialing Stream status
				} else if status != currentStatus {
					setStatus(status)
				}
				time.Sleep(2 * time.Second)
			}
		}
	}()

	listener, err := net.Listen("tcp", "127.0.0.1:0")
	if err != nil {
		return 0
	}
	proxyPort = listener.Addr().(*net.TCPAddr).Port

	// resolveViaTailscaleDNS resolves a hostname using the real upstream DNS
	// resolvers from Tailscale's control plane. It:
	//   1. Checks ExtraRecords (A records pushed by the control plane) for a
	//      direct match.
	//   2. Calls lc.DNSConfig() to find the upstream resolver IP(s) for the
	//      host's domain (split-DNS routes first, then global resolvers).
	//   3. Queries that resolver via tsServer.Dial so the DNS query goes
	//      through the WireGuard tunnel and gets the CGNAT IP.
	resolveViaTailscaleDNS := func(ctx context.Context, host string) string {
		// Use a 5-second timeout to avoid blocking the dialer for too long.
		dnsCtx, cancel := context.WithTimeout(ctx, 5*time.Second)
		defer cancel()

		lc, err := tsServer.LocalClient()
		if err != nil {
			logf("resolveViaTailscaleDNS: LocalClient error: %v", err)
			return ""
		}

		// 1. Check ExtraRecords — A records the Tailscale control plane pushes
		//    directly (configured in the Tailscale admin DNS settings).
		if st, err := lc.Status(dnsCtx); err == nil {
			for _, rec := range st.ExtraRecords {
				if strings.EqualFold(strings.TrimSuffix(rec.Name, "."), host) && rec.Value != "" {
					if net.ParseIP(rec.Value) != nil {
						logf("resolveViaTailscaleDNS: ExtraRecord match %s -> %s", host, rec.Value)
						return rec.Value
					}
				}
			}
		} else {
			logf("resolveViaTailscaleDNS: Status error: %v", err)
		}

		// 2. Get the DNS configuration from Tailscale. This includes split-DNS
		//    routes and global upstream resolvers set in the admin panel.
		dnsConfig, err := lc.DNSConfig(dnsCtx)
		if err != nil || dnsConfig == nil {
			logf("resolveViaTailscaleDNS: DNSConfig error: %v", err)
			return ""
		}

		// pickResolverAddr returns the first plain-IP resolver address from a
		// list, skipping DoH URLs (which start with "https://").
		pickResolverAddr := func(resolvers []*dnstype.Resolver) string {
			for _, r := range resolvers {
				if r.Addr != "" && !strings.HasPrefix(r.Addr, "https://") {
					return r.Addr
				}
			}
			return ""
		}

		// Check split-DNS routes for the host's domain (most specific first).
		var resolverAddr string
		for domain, resolvers := range dnsConfig.Routes {
			domainStr := strings.ToLower(strings.TrimSuffix(string(domain), "."))
			if strings.EqualFold(host, domainStr) ||
				strings.HasSuffix(strings.ToLower(host), "."+domainStr) {
				if addr := pickResolverAddr(resolvers); addr != "" {
					resolverAddr = addr
					break
				}
			}
		}
		// Fall back to global resolvers if no split-DNS route matched.
		if resolverAddr == "" {
			resolverAddr = pickResolverAddr(dnsConfig.Resolvers)
		}

		if resolverAddr == "" {
			logf("resolveViaTailscaleDNS: no upstream IP resolver found for %s", host)
			return ""
		}

		// resolverAddr may be "1.2.3.4" or "1.2.3.4:53" — normalise to host:port.
		resolverHost, resolverPort, err := net.SplitHostPort(resolverAddr)
		if err != nil {
			resolverHost = resolverAddr
			resolverPort = "53"
		}
		resolverEndpoint := net.JoinHostPort(resolverHost, resolverPort)
		logf("resolveViaTailscaleDNS: querying %s for %s", resolverEndpoint, host)

		// 3. Use a Go net.Resolver whose Dial routes through the WireGuard
		//    tunnel via tsServer.Dial.
		r := &net.Resolver{
			PreferGo: true,
			Dial: func(rCtx context.Context, _, _ string) (net.Conn, error) {
				return tsServer.Dial(rCtx, "udp", resolverEndpoint)
			},
		}
		ips, err := r.LookupHost(dnsCtx, host)
		if err != nil || len(ips) == 0 {
			logf("resolveViaTailscaleDNS: lookup failed for %s: %v", host, err)
			return ""
		}
		logf("resolveViaTailscaleDNS: %s -> %s (via %s)", host, ips[0], resolverEndpoint)
		return ips[0]
	}

	dialer := func(ctx context.Context, network, address string) (net.Conn, error) {
		atomic.AddInt32(&activeStreams, 1)
		setStatus("Dialing Stream")

		origAddress := address
		// Pre-resolve the hostname via Tailscale's upstream DNS resolver so
		// that custom domains resolve to their CGNAT IPs, ensuring the
		// connection goes through the WireGuard tunnel.
		if host, port, err := net.SplitHostPort(address); err == nil {
			if net.ParseIP(host) == nil { // only resolve hostnames, not literal IPs
				if ip := resolveViaTailscaleDNS(ctx, host); ip != "" {
					address = net.JoinHostPort(ip, port)
					logf("dialer: rewrote %s -> %s", origAddress, address)
				} else {
					logf("dialer: DNS failed, dialing %s directly", origAddress)
				}
			}
		}

		conn, err := tsServer.Dial(ctx, network, address)
		if err != nil {
			if atomic.AddInt32(&activeStreams, -1) == 0 {
				setStatus("Connected")
			}
			return nil, err
		}

		return &wrappedConn{
			Conn: conn,
			onClose: func() {
				if atomic.AddInt32(&activeStreams, -1) == 0 {
					setStatus("Connected")
				}
			},
		}, nil
	}

	proxyServer = &http.Server{
		Handler: http.HandlerFunc(func(w http.ResponseWriter, r *http.Request) {
			if r.Method == http.MethodConnect {
				handleConnect(w, r, dialer)
			} else {
				handleHTTP(w, r, dialer)
			}
		}),
	}

	go func() {
		proxyServer.Serve(listener)
	}()

	return C.jint(proxyPort)
}

//export Java_io_github_runc0derun_watcharr_shared_playback_TsnetManager_nativeStop
func Java_io_github_runc0derun_watcharr_shared_playback_TsnetManager_nativeStop(env *C.JNIEnv, clazz C.jclass) {
	mu.Lock()
	defer mu.Unlock()
	stopServerLocked(env)
}

type wrappedConn struct {
	net.Conn
	onClose func()
	once    sync.Once
}

func (c *wrappedConn) Close() error {
	var err error
	c.once.Do(func() {
		err = c.Conn.Close()
		c.onClose()
	})
	return err
}

func handleHTTP(w http.ResponseWriter, r *http.Request, dial func(context.Context, string, string) (net.Conn, error)) {
	logf("handleHTTP: %s %s", r.Method, r.RequestURI)
	req, err := http.NewRequestWithContext(r.Context(), r.Method, r.RequestURI, r.Body)
	if err != nil {
		http.Error(w, err.Error(), http.StatusInternalServerError)
		return
	}

	for k, vv := range r.Header {
		for _, v := range vv {
			req.Header.Add(k, v)
		}
	}

	transport := &http.Transport{
		DialContext: func(ctx context.Context, network, addr string) (net.Conn, error) {
			return dial(ctx, network, addr)
		},
		TLSClientConfig: &tls.Config{
			InsecureSkipVerify: true,
		},
	}

	resp, err := transport.RoundTrip(req)
	if err != nil {
		logf("handleHTTP: RoundTrip error: %v", err)
		http.Error(w, err.Error(), http.StatusBadGateway)
		return
	}
	defer resp.Body.Close()

	for k, vv := range resp.Header {
		for _, v := range vv {
			w.Header().Add(k, v)
		}
	}
	w.WriteHeader(resp.StatusCode)
	io.Copy(w, resp.Body)
}

func handleConnect(w http.ResponseWriter, r *http.Request, dial func(context.Context, string, string) (net.Conn, error)) {
	logf("handleConnect: dialing %s", r.Host)
	destConn, err := dial(r.Context(), "tcp", r.Host)
	if err != nil {
		logf("handleConnect: dial error for %s: %v", r.Host, err)
		http.Error(w, err.Error(), http.StatusServiceUnavailable)
		return
	}
	logf("handleConnect: dial OK for %s, remote=%s", r.Host, destConn.RemoteAddr())
	defer destConn.Close()

	hijacker, ok := w.(http.Hijacker)
	if !ok {
		http.Error(w, "Hijacking not supported", http.StatusInternalServerError)
		return
	}
	clientConn, bufrw, err := hijacker.Hijack()
	if err != nil {
		http.Error(w, err.Error(), http.StatusServiceUnavailable)
		return
	}
	defer clientConn.Close()

	clientConn.Write([]byte("HTTP/1.1 200 Connection Established\r\n\r\n"))
	logf("handleConnect: tunnel established %s -> %s", clientConn.RemoteAddr(), destConn.RemoteAddr())

	go func() {
		io.Copy(destConn, bufrw.Reader)
		destConn.Close()
		clientConn.Close()
	}()
	io.Copy(clientConn, destConn)
}

func main() {}

