#include <iostream>
#include <fstream>
#include <string>
#include <vector>
#include <iomanip>
#include <unistd.h>
#include <fcntl.h>
#include <sys/prctl.h>
#include <linux/seccomp.h>
#include <linux/filter.h>
#include <linux/audit.h>
#include <sys/syscall.h>
#include <errno.h>
#include <string.h>
#include <stddef.h>
#include <stdint.h>

// Minimal SHA-256 implementation
#define SHA256_ROTR(a,b) (((a)>>(b))|((a)<<(32-(b))))
#define SHA256_CH(x,y,z) (((x)&(y))^((~(x))&(z)))
#define SHA256_MAJ(x,y,z) (((x)&(y))^((x)&(z))^((y)&(z)))
#define SHA256_EP0(x) (SHA256_ROTR(x,2)^SHA256_ROTR(x,13)^SHA256_ROTR(x,22))
#define SHA256_EP1(x) (SHA256_ROTR(x,6)^SHA256_ROTR(x,11)^SHA256_ROTR(x,25))
#define SHA256_SIG0(x) (SHA256_ROTR(x,7)^SHA256_ROTR(x,18)^((x)>>3))
#define SHA256_SIG1(x) (SHA256_ROTR(x,17)^SHA256_ROTR(x,19)^((x)>>10))

static const uint32_t k[64] = {
    0x428a2f98,0x71374491,0xb5c0fbcf,0xe9b5dba5,0x3956c25b,0x59f111f1,0x923f82a4,0xab1c5ed5,
    0xd807aa98,0x12835b01,0x243185be,0x550c7dc3,0x72be5d74,0x80deb1fe,0x9bdc06a7,0xc19bf174,
    0xe49b69c1,0xefbe4786,0x0fc19dc6,0x240ca1cc,0x2de92c6f,0x4a7484aa,0x5cb0a9dc,0x76f988da,
    0x983e5152,0xa831c66d,0xb00327c8,0xbf597fc7,0xc6e00bf3,0xd5a79147,0x06ca6351,0x14292967,
    0x27b70a85,0x2e1b2138,0x4d2c6dfc,0x53380d13,0x650a7354,0x766a0abb,0x81c2c92e,0x92722c85,
    0xa2bfe8a1,0xa81a664b,0xc24b8b70,0xc76c51a3,0xd192e819,0xd6990624,0xf40e3585,0x106aa070,
    0x19a4c116,0x1e376c08,0x2748774c,0x34b0bcb5,0x391c0cb3,0x4ed8aa4a,0x5b9cca4f,0x682e6ff3,
    0x748f82ee,0x78a5636f,0x84c87814,0x8cc70208,0x90befffa,0xa4506ceb,0xbef9a3f7,0xc67178f2
};

struct SHA256_CTX {
    uint32_t state[8];
    uint64_t bitlen;
    uint8_t data[64];
    uint32_t datalen;
};

void sha256_transform(SHA256_CTX *ctx, const uint8_t data[]) {
    uint32_t a, b, c, d, e, f, g, h, i, j, t1, t2, m[64];
    for (i = 0, j = 0; i < 16; ++i, j += 4)
        m[i] = (data[j] << 24) | (data[j + 1] << 16) | (data[j + 2] << 8) | (data[j + 3]);
    for ( ; i < 64; ++i)
        m[i] = SHA256_SIG1(m[i - 2]) + m[i - 7] + SHA256_SIG0(m[i - 15]) + m[i - 16];
    a = ctx->state[0]; b = ctx->state[1]; c = ctx->state[2]; d = ctx->state[3];
    e = ctx->state[4]; f = ctx->state[5]; g = ctx->state[6]; h = ctx->state[7];
    for (i = 0; i < 64; ++i) {
        t1 = h + SHA256_EP1(e) + SHA256_CH(e,f,g) + k[i] + m[i];
        t2 = SHA256_EP0(a) + SHA256_MAJ(a,b,c);
        h = g; g = f; f = e; e = d + t1;
        d = c; c = b; b = a; a = t1 + t2;
    }
    ctx->state[0] += a; ctx->state[1] += b; ctx->state[2] += c; ctx->state[3] += d;
    ctx->state[4] += e; ctx->state[5] += f; ctx->state[6] += g; ctx->state[7] += h;
}

void sha256_init(SHA256_CTX *ctx) {
    ctx->datalen = 0;
    ctx->bitlen = 0;
    ctx->state[0] = 0x6a09e667; ctx->state[1] = 0xbb67ae85;
    ctx->state[2] = 0x3c6ef372; ctx->state[3] = 0xa54ff53a;
    ctx->state[4] = 0x510e527f; ctx->state[5] = 0x9b05688c;
    ctx->state[6] = 0x1f83d9ab; ctx->state[7] = 0x5be0cd19;
}

void sha256_update(SHA256_CTX *ctx, const uint8_t data[], size_t len) {
    for (size_t i = 0; i < len; ++i) {
        ctx->data[ctx->datalen] = data[i];
        ctx->datalen++;
        if (ctx->datalen == 64) {
            sha256_transform(ctx, ctx->data);
            ctx->bitlen += 512;
            ctx->datalen = 0;
        }
    }
}

void sha256_final(SHA256_CTX *ctx, uint8_t hash[]) {
    uint32_t i = ctx->datalen;
    if (ctx->datalen < 56) {
        ctx->data[i++] = 0x80;
        while (i < 56) ctx->data[i++] = 0x00;
    } else {
        ctx->data[i++] = 0x80;
        while (i < 64) ctx->data[i++] = 0x00;
        sha256_transform(ctx, ctx->data);
        memset(ctx->data, 0, 56);
    }
    ctx->bitlen += ctx->datalen * 8;
    ctx->data[63] = ctx->bitlen;
    ctx->data[62] = ctx->bitlen >> 8;
    ctx->data[61] = ctx->bitlen >> 16;
    ctx->data[60] = ctx->bitlen >> 24;
    ctx->data[59] = ctx->bitlen >> 32;
    ctx->data[58] = ctx->bitlen >> 40;
    ctx->data[57] = ctx->bitlen >> 48;
    ctx->data[56] = ctx->bitlen >> 56;
    sha256_transform(ctx, ctx->data);
    for (i = 0; i < 4; ++i) {
        hash[i]      = (ctx->state[0] >> (24 - i * 8)) & 0x000000ff;
        hash[i + 4]  = (ctx->state[1] >> (24 - i * 8)) & 0x000000ff;
        hash[i + 8]  = (ctx->state[2] >> (24 - i * 8)) & 0x000000ff;
        hash[i + 12] = (ctx->state[3] >> (24 - i * 8)) & 0x000000ff;
        hash[i + 16] = (ctx->state[4] >> (24 - i * 8)) & 0x000000ff;
        hash[i + 20] = (ctx->state[5] >> (24 - i * 8)) & 0x000000ff;
        hash[i + 24] = (ctx->state[6] >> (24 - i * 8)) & 0x000000ff;
        hash[i + 28] = (ctx->state[7] >> (24 - i * 8)) & 0x000000ff;
    }
}

std::string hashFile(int fd) {
    SHA256_CTX ctx;
    sha256_init(&ctx);
    
    uint8_t buffer[8192];
    ssize_t bytesRead;
    while ((bytesRead = read(fd, buffer, sizeof(buffer))) > 0) {
        sha256_update(&ctx, buffer, bytesRead);
    }
    
    uint8_t hash[32];
    sha256_final(&ctx, hash);
    
    std::string hex;
    const char* hexChars = "0123456789abcdef";
    for (int i = 0; i < 32; i++) {
        hex += hexChars[(hash[i] >> 4) & 0x0F];
        hex += hexChars[hash[i] & 0x0F];
    }
    return hex;
}

void fail(const std::string& msg) {
    std::cerr << "FAIL: " << msg << std::endl;
    exit(1);
}

// BPF Macro Definitions for Seccomp
#define DENY_SYSCALL(name) \
    BPF_JUMP(BPF_JMP | BPF_JEQ | BPF_K, __NR_##name, 0, 1), \
    BPF_STMT(BPF_RET | BPF_K, SECCOMP_RET_ERRNO | (EPERM & SECCOMP_RET_DATA))

void apply_seccomp() {
    struct sock_filter filter[] = {
        // Load architecture
        BPF_STMT(BPF_LD | BPF_W | BPF_ABS, (offsetof(struct seccomp_data, arch))),
        // Check if x86_64
        BPF_JUMP(BPF_JMP | BPF_JEQ | BPF_K, AUDIT_ARCH_X86_64, 1, 0),
        BPF_STMT(BPF_RET | BPF_K, SECCOMP_RET_KILL_PROCESS),
        
        // Load syscall number
        BPF_STMT(BPF_LD | BPF_W | BPF_ABS, (offsetof(struct seccomp_data, nr))),
        
        // Deny list
#ifdef __NR_ptrace
        DENY_SYSCALL(ptrace),
#endif
#ifdef __NR_bpf
        DENY_SYSCALL(bpf),
#endif
#ifdef __NR_unshare
        DENY_SYSCALL(unshare),
#endif
#ifdef __NR_mount
        DENY_SYSCALL(mount),
#endif
#ifdef __NR_umount2
        DENY_SYSCALL(umount2),
#endif
#ifdef __NR_clone3
        DENY_SYSCALL(clone3),
#endif
#ifdef __NR_chroot
        DENY_SYSCALL(chroot),
#endif
#ifdef __NR_pivot_root
        DENY_SYSCALL(pivot_root),
#endif
#ifdef __NR_process_vm_readv
        DENY_SYSCALL(process_vm_readv),
#endif
#ifdef __NR_process_vm_writev
        DENY_SYSCALL(process_vm_writev),
#endif
#ifdef __NR_kcmp
        DENY_SYSCALL(kcmp),
#endif
        // Default allow
        BPF_STMT(BPF_RET | BPF_K, SECCOMP_RET_ALLOW)
    };
    
    struct sock_fprog prog = {
        .len = (unsigned short)(sizeof(filter) / sizeof(filter[0])),
        .filter = filter
    };
    
    // PR_SET_NO_NEW_PRIVS is required before setting seccomp without CAP_SYS_ADMIN
    if (prctl(PR_SET_NO_NEW_PRIVS, 1, 0, 0, 0) != 0) {
        perror("prctl(PR_SET_NO_NEW_PRIVS)");
        exit(1);
    }
    
    if (prctl(PR_SET_SECCOMP, SECCOMP_MODE_FILTER, &prog) != 0) {
        perror("prctl(PR_SET_SECCOMP)");
        exit(1);
    }
}

int main(int argc, char* argv[]) {
    // Inspection mode used by Host (Kotlin) to query running Sandbox capabilities
    if (argc >= 3 && std::string(argv[1]) == "--inspect") {
        std::cerr << "{\"error\": \"Inspect mode handled externally on Linux\"}" << std::endl;
        return 1;
    }

    if (argc < 7) {
        std::cerr << "Usage: arkham-launcher-linux <expected_hash> <max_ram_bytes> <max_cpu_percent> <jar_path> <java_bin> <args...>" << std::endl;
        return 1;
    }

    std::string expectedHash = argv[1];
    std::string jarPath = argv[4];
    
    // TOCTOU mitigation: Open the JAR file first
    int fd = open(jarPath.c_str(), O_RDONLY);
    if (fd < 0) {
        fail("Failed to open jar file");
    }
    
    // Hash the file descriptor contents
    std::string actualHash = hashFile(fd);
    
    // Close the file descriptor, but the path is still protected because the whole launcher is wrapped inside `bwrap` 
    // which has mounted `jarPath` as read-only (--ro-bind).
    close(fd);

    if (actualHash != expectedHash) {
        fail("Artifact hash mismatch. Expected: " + expectedHash + " Actual: " + actualHash + " Path: " + jarPath);
    }
    
    // 2. Output WORKER_PID to stderr so the host can extract it cleanly (avoids race condition)
    std::cerr << "WORKER_PID:" << getpid() << std::endl;
    
    // 3. Prepare arguments for execvp
    std::string javaBin = argv[5];
    std::vector<char*> execArgs;
    for (int i = 5; i < argc; ++i) {
        execArgs.push_back(argv[i]);
    }
    execArgs.push_back(nullptr);
    
    // 4. Apply Seccomp immediately before execvp
    apply_seccomp();
    
    // 5. Exec into the JVM (replacing the launcher process)
    if (execvp(javaBin.c_str(), execArgs.data()) == -1) {
        perror("execvp failed");
        return 1;
    }
    
    return 0; // unreachable
}
